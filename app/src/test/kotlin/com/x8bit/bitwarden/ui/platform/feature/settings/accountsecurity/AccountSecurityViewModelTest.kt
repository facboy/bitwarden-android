package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson.Policy
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.model.UserFingerprintResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.BiometricsKeyResult
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.components.toggle.UnlockWithPinState
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.AccountSecurityAction.AuthenticatorSyncToggle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.crypto.Cipher

@Suppress("LargeClass")
class AccountSecurityViewModelTest : BaseViewModelTest() {

    private val fakeEnvironmentRepository = FakeEnvironmentRepository()
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository: AuthRepository = mockk(relaxed = true) {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val vaultRepository: VaultRepository = mockk(relaxed = true)
    private val mutableBiometricsUnlockEnabledFlow = bufferedMutableSharedFlow<Boolean>()
    private val mutablePinUnlockEnabledFlow = bufferedMutableSharedFlow<Boolean>()
    private val settingsRepository: SettingsRepository = mockk {
        every { isAuthenticatorSyncEnabled } returns false
        every { isUnlockWithBiometricsEnabled } returns false
        every { isUnlockWithPinEnabled } returns false
        every { vaultTimeout } returns VaultTimeout.ThirtyMinutes
        every { vaultTimeoutAction } returns VaultTimeoutAction.LOCK
        coEvery { getUserFingerprint() } returns UserFingerprintResult.Success(FINGERPRINT)
        every { isUnlockWithBiometricsEnabledFlow } returns mutableBiometricsUnlockEnabledFlow
        every { isUnlockWithPinEnabledFlow } returns mutablePinUnlockEnabledFlow
    }

    private val mutableFirstTimeStateFlow = MutableStateFlow(FirstTimeState())
    private val firstTimeActionManager: FirstTimeActionManager = mockk {
        every { firstTimeStateFlow } returns mutableFirstTimeStateFlow
        every { storeShowUnlockSettingBadge(any()) } just runs
    }
    private val mutableActivePolicyFlow = bufferedMutableSharedFlow<List<Policy>>()
    private val mutableRemovePinPolicyFlow = bufferedMutableSharedFlow<List<Policy>>()
    private val biometricsEncryptionManager: BiometricsEncryptionManager = mockk {
        every { createCipherOrNull(DEFAULT_USER_STATE.activeUserId) } returns CIPHER
        every { getOrCreateCipher(DEFAULT_USER_STATE.activeUserId) } returns CIPHER
        every {
            isBiometricIntegrityValid(
                userId = DEFAULT_USER_STATE.activeUserId,
                cipher = CIPHER,
            )
        } returns true
    }
    private val policyManager: PolicyManager = mockk {
        every {
            getActivePoliciesFlow(type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT)
        } returns mutableActivePolicyFlow
        every {
            getActivePoliciesFlow(type = PolicyTypeJson.REMOVE_UNLOCK_WITH_PIN)
        } returns mutableRemovePinPolicyFlow
    }

    @BeforeEach
    fun setup() {
        mockkStatic(::isBuildVersionAtLeast)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(::isBuildVersionAtLeast)
    }

    @Test
    fun `initial state should be correct when saved state is set`() {
        val viewModel = createViewModel(initialState = DEFAULT_STATE)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        coVerify { settingsRepository.getUserFingerprint() }
    }

    @Test
    fun `initial state should be correct when saved state is not set`() {
        val viewModel = createViewModel(initialState = null)
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
        verify {
            biometricsEncryptionManager.getOrCreateCipher(DEFAULT_USER_STATE.activeUserId)
            biometricsEncryptionManager.isBiometricIntegrityValid(
                userId = DEFAULT_USER_STATE.activeUserId,
                cipher = CIPHER,
            )
        }
        coVerify { settingsRepository.getUserFingerprint() }
    }

    @Test
    fun `state updates when policies change`() = runTest {
        val viewModel = createViewModel()

        val policyInformation = PolicyInformation.VaultTimeout(
            minutes = 10,
            action = "lock",
        )
        mutableActivePolicyFlow.emit(
            listOf(
                createMockPolicy(
                    isEnabled = true,
                    type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
                    data = Json.encodeToJsonElement(policyInformation).jsonObject,
                ),
            ),
        )

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    vaultTimeoutPolicyMinutes = 10,
                    vaultTimeoutPolicyAction = "lock",
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `state updates when remove pin policies change`() = runTest {
        val viewModel = createViewModel()

        mutableRemovePinPolicyFlow.emit(
            listOf(
                createMockPolicy(
                    isEnabled = true,
                    type = PolicyTypeJson.REMOVE_UNLOCK_WITH_PIN,
                    organizationId = "organizationUser",
                ),
            ),
        )

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    removeUnlockWithPinPolicyEnabled = true,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `remove pin policy is true when user role is ADMIN`() = runTest {
        val viewModel = createViewModel()

        mutableRemovePinPolicyFlow.emit(
            listOf(
                createMockPolicy(
                    organizationId = "organizationAdmin",
                    isEnabled = true,
                    type = PolicyTypeJson.REMOVE_UNLOCK_WITH_PIN,
                ),
            ),
        )

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    removeUnlockWithPinPolicyEnabled = true,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `remove pin policy is true when user role is OWNER`() = runTest {
        val viewModel = createViewModel()

        mutableRemovePinPolicyFlow.emit(
            listOf(
                createMockPolicy(
                    organizationId = "organizationOwner",
                    isEnabled = true,
                    type = PolicyTypeJson.REMOVE_UNLOCK_WITH_PIN,
                ),
            ),
        )

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    removeUnlockWithPinPolicyEnabled = true,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `remove pin policy is true when user role is CUSTOM with manage policies`() = runTest {
        val viewModel = createViewModel()

        mutableRemovePinPolicyFlow.emit(
            listOf(
                createMockPolicy(
                    organizationId = "organizationCustom",
                    isEnabled = true,
                    type = PolicyTypeJson.REMOVE_UNLOCK_WITH_PIN,
                ),
            ),
        )

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    removeUnlockWithPinPolicyEnabled = true,
                ),
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on AuthenticatorSyncToggle should update SettingsRepository and isAuthenticatorSyncChecked`() =
        runTest {
            val viewModel = createViewModel()
            every { settingsRepository.isAuthenticatorSyncEnabled = true } just runs
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(AuthenticatorSyncToggle(enabled = true))
                assertEquals(DEFAULT_STATE.copy(isAuthenticatorSyncChecked = true), awaitItem())
            }
            verify { settingsRepository.isAuthenticatorSyncEnabled = true }
        }

    @Test
    fun `on FingerprintResultReceive should update the fingerprint phrase`() = runTest {
        val fingerprint = "fingerprint"
        val viewModel = createViewModel(initialState = DEFAULT_STATE)
        // Set fingerprint phrase to value received
        viewModel.trySendAction(
            AccountSecurityAction.Internal.FingerprintResultReceive(
                UserFingerprintResult.Success(fingerprint),
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(fingerprintPhrase = fingerprint.asText()),
            viewModel.stateFlow.value,
        )
        // Clear fingerprint phrase
        viewModel.trySendAction(
            AccountSecurityAction.Internal.FingerprintResultReceive(
                fingerprintResult = UserFingerprintResult.Error(error = NoActiveUserException()),
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(fingerprintPhrase = "".asText()),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on AccountFingerprintPhraseClick should show the fingerprint phrase dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(AccountSecurityAction.AccountFingerprintPhraseClick)
        assertEquals(
            DEFAULT_STATE.copy(dialog = AccountSecurityDialog.FingerprintPhrase),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on FingerPrintLearnMoreClick should emit NavigateToFingerprintPhrase`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.FingerPrintLearnMoreClick)
            assertEquals(AccountSecurityEvent.NavigateToFingerprintPhrase, awaitItem())
        }
    }

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.BackClick)
            assertEquals(AccountSecurityEvent.NavigateBack, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ChangeMasterPasswordClick should emit NavigateToChangeMasterPassword with correct URL based on US and EU environments`() =
        runTest {
            fakeEnvironmentRepository.environment = Environment.Us
            val viewModel = createViewModel()
            viewModel.eventFlow.test {

                viewModel.trySendAction(AccountSecurityAction.ChangeMasterPasswordClick)
                assertEquals(
                    AccountSecurityEvent.NavigateToChangeMasterPassword(
                        "https://vault.bitwarden.com/#/settings",
                    ),
                    awaitItem(),
                )

                fakeEnvironmentRepository.environment = Environment.Eu

                viewModel.trySendAction(AccountSecurityAction.ChangeMasterPasswordClick)
                assertEquals(
                    AccountSecurityEvent.NavigateToChangeMasterPassword(
                        "https://vault.bitwarden.eu/#/settings",
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `on DeleteAccountClick should emit NavigateToDeleteAccount`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.DeleteAccountClick)
            assertEquals(AccountSecurityEvent.NavigateToDeleteAccount, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on DismissSessionTimeoutActionDialog should update shouldShowSessionTimeoutActionDialog`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(AccountSecurityAction.DismissDialog)
            assertEquals(DEFAULT_STATE.copy(dialog = null), viewModel.stateFlow.value)
        }

    @Test
    fun `on LockNowClick should call lockVaultForCurrentUser`() {
        every { vaultRepository.lockVaultForCurrentUser(any()) } just runs
        val viewModel = createViewModel()
        viewModel.trySendAction(AccountSecurityAction.LockNowClick)
        verify { vaultRepository.lockVaultForCurrentUser(any()) }
    }

    @Test
    fun `on PendingLoginRequestsClick should emit NavigateToPendingRequests`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.PendingLoginRequestsClick)
            assertEquals(
                AccountSecurityEvent.NavigateToPendingRequests,
                awaitItem(),
            )
        }
    }

    @Test
    fun `on VaultTimeoutTypeSelect should update the selection()`() = runTest {
        every { settingsRepository.vaultTimeout = any() } just runs
        val viewModel = createViewModel()
        viewModel.trySendAction(
            AccountSecurityAction.VaultTimeoutTypeSelect(VaultTimeout.Type.FOUR_HOURS),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                vaultTimeout = VaultTimeout.FourHours,
            ),
            viewModel.stateFlow.value,
        )
        verify { settingsRepository.vaultTimeout = VaultTimeout.FourHours }
    }

    @Test
    fun `on CustomVaultTimeoutSelect should update the selection()`() = runTest {
        every { settingsRepository.vaultTimeout = any() } just runs
        val viewModel = createViewModel()
        viewModel.trySendAction(
            AccountSecurityAction.CustomVaultTimeoutSelect(
                customVaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 360),
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                vaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 360),
            ),
            viewModel.stateFlow.value,
        )
        verify {
            settingsRepository.vaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 360)
        }
    }

    @Test
    fun `on VaultTimeoutActionSelect should update vault timeout action`() = runTest {
        every { settingsRepository.vaultTimeoutAction = any() } just runs
        val viewModel = createViewModel()
        viewModel.trySendAction(
            AccountSecurityAction.VaultTimeoutActionSelect(VaultTimeoutAction.LOGOUT),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                vaultTimeoutAction = VaultTimeoutAction.LOGOUT,
            ),
            viewModel.stateFlow.value,
        )
        verify { settingsRepository.vaultTimeoutAction = VaultTimeoutAction.LOGOUT }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on TwoStepLoginClick should emit NavigateToTwoStepLogin with correct URL based on US and EU environments`() =
        runTest {
            fakeEnvironmentRepository.environment = Environment.Us
            val viewModel = createViewModel()
            viewModel.eventFlow.test {

                viewModel.trySendAction(AccountSecurityAction.TwoStepLoginClick)
                assertEquals(
                    AccountSecurityEvent.NavigateToTwoStepLogin(
                        "https://vault.bitwarden.com/#/settings",
                    ),
                    awaitItem(),
                )

                fakeEnvironmentRepository.environment = Environment.Eu

                viewModel.trySendAction(AccountSecurityAction.TwoStepLoginClick)
                assertEquals(
                    AccountSecurityEvent.NavigateToTwoStepLogin(
                        "https://vault.bitwarden.eu/#/settings",
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `on EnableBiometricsClick should emit ShowBiometricsPrompt`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.EnableBiometricsClick)

            assertEquals(
                AccountSecurityEvent.ShowBiometricsPrompt(cipher = CIPHER),
                awaitItem(),
            )
        }

        verify(exactly = 0) {
            firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = false)
        }
    }

    @Test
    fun `on EnableBiometricsClick should update user show unlock badge status if shown`() {
        mutableFirstTimeStateFlow.update { it.copy(showSetupUnlockCard = true) }
        val viewModel = createViewModel()
        viewModel.trySendAction(AccountSecurityAction.EnableBiometricsClick)

        verify(exactly = 1) {
            firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = false)
        }
    }

    @Test
    fun `on EnableBiometricsClick should show Error dialog when cipher is null`() {
        every {
            biometricsEncryptionManager.createCipherOrNull(DEFAULT_USER_STATE.activeUserId)
        } returns null
        val viewModel = createViewModel()

        viewModel.trySendAction(AccountSecurityAction.EnableBiometricsClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialog = AccountSecurityDialog.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithBiometricToggleDisabled should call clearBiometricsKey and update the state`() =
        runTest {
            val initialState = DEFAULT_STATE.copy(isUnlockWithBiometricsEnabled = true)
            every { settingsRepository.isUnlockWithBiometricsEnabled } returns true
            every {
                biometricsEncryptionManager.clearBiometrics(userId = DEFAULT_USER_ID)
            } just runs
            val viewModel = createViewModel(initialState)
            assertEquals(initialState, viewModel.stateFlow.value)

            viewModel.trySendAction(AccountSecurityAction.UnlockWithBiometricToggleDisabled)

            assertEquals(
                initialState.copy(isUnlockWithBiometricsEnabled = false),
                viewModel.stateFlow.value,
            )
            verify(exactly = 1) {
                biometricsEncryptionManager.clearBiometrics(userId = DEFAULT_USER_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithBiometricToggleDisabled should call clearBiometricsKey, reset the vaultTimeoutAction, and update the state`() =
        runTest {
            val initialState = DEFAULT_STATE.copy(
                isUnlockWithPasswordEnabled = false,
                isUnlockWithBiometricsEnabled = true,
            )
            every { settingsRepository.isUnlockWithBiometricsEnabled } returns true
            every {
                biometricsEncryptionManager.clearBiometrics(userId = DEFAULT_USER_ID)
            } just runs
            every { settingsRepository.vaultTimeoutAction = VaultTimeoutAction.LOGOUT } just runs
            val viewModel = createViewModel(initialState)
            assertEquals(initialState, viewModel.stateFlow.value)

            viewModel.trySendAction(AccountSecurityAction.UnlockWithBiometricToggleDisabled)

            assertEquals(
                initialState.copy(
                    isUnlockWithBiometricsEnabled = false,
                    vaultTimeoutAction = VaultTimeoutAction.LOGOUT,
                ),
                viewModel.stateFlow.value,
            )
            verify(exactly = 1) {
                biometricsEncryptionManager.clearBiometrics(userId = DEFAULT_USER_ID)
                settingsRepository.vaultTimeoutAction = VaultTimeoutAction.LOGOUT
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithBiometricToggleEnabled and setupBiometricsKey error should call update the state accordingly`() =
        runTest {
            coEvery {
                settingsRepository.setupBiometricsKey(cipher = CIPHER)
            } returns BiometricsKeyResult.Error(error = Throwable("Fail!"))
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    AccountSecurityAction.UnlockWithBiometricToggleEnabled(cipher = CIPHER),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = AccountSecurityDialog.Loading(BitwardenString.saving.asText()),
                        isUnlockWithBiometricsEnabled = true,
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = null,
                        isUnlockWithBiometricsEnabled = false,
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) {
                settingsRepository.setupBiometricsKey(cipher = CIPHER)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithBiometricToggleEnabled and setupBiometricsKey success should call update the state accordingly`() =
        runTest {
            coEvery {
                settingsRepository.setupBiometricsKey(cipher = CIPHER)
            } returns BiometricsKeyResult.Success
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    AccountSecurityAction.UnlockWithBiometricToggleEnabled(cipher = CIPHER),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = AccountSecurityDialog.Loading(BitwardenString.saving.asText()),
                        isUnlockWithBiometricsEnabled = true,
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = null,
                        isUnlockWithBiometricsEnabled = true,
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) {
                settingsRepository.setupBiometricsKey(cipher = CIPHER)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithPinToggle Disabled should set pin unlock to false and clear the PIN in settings`() {
        val initialState = DEFAULT_STATE.copy(
            isUnlockWithPinEnabled = true,
        )
        every { settingsRepository.clearUnlockPin() } just runs
        val viewModel = createViewModel(initialState = initialState)
        viewModel.trySendAction(
            AccountSecurityAction.UnlockWithPinToggle(UnlockWithPinState.Disabled),
        )
        assertEquals(
            initialState.copy(isUnlockWithPinEnabled = false),
            viewModel.stateFlow.value,
        )
        verify { settingsRepository.clearUnlockPin() }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithPinToggle Disabled should set pin unlock to false, reset the vaultTimeoutAction, and clear the PIN in settings`() {
        val initialState = DEFAULT_STATE.copy(
            isUnlockWithPasswordEnabled = false,
            isUnlockWithPinEnabled = true,
        )
        every { settingsRepository.clearUnlockPin() } just runs
        every { settingsRepository.vaultTimeoutAction = VaultTimeoutAction.LOGOUT } just runs
        val viewModel = createViewModel(initialState = initialState)
        viewModel.trySendAction(
            AccountSecurityAction.UnlockWithPinToggle(UnlockWithPinState.Disabled),
        )
        assertEquals(
            initialState.copy(
                vaultTimeoutAction = VaultTimeoutAction.LOGOUT,
                isUnlockWithPinEnabled = false,
            ),
            viewModel.stateFlow.value,
        )
        verify {
            settingsRepository.clearUnlockPin()
            settingsRepository.vaultTimeoutAction = VaultTimeoutAction.LOGOUT
        }
    }

    @Test
    fun `on UnlockWithPinToggle PendingEnabled should set pin unlock to true`() {
        val initialState = DEFAULT_STATE.copy(
            isUnlockWithPinEnabled = false,
        )
        val viewModel = createViewModel(initialState = initialState)
        viewModel.trySendAction(
            AccountSecurityAction.UnlockWithPinToggle(UnlockWithPinState.PendingEnabled),
        )
        assertEquals(
            initialState.copy(isUnlockWithPinEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithPinToggle Enabled should set pin unlock to true and set the PIN in settings`() {
        val initialState = DEFAULT_STATE.copy(
            isUnlockWithPinEnabled = false,
        )
        every { settingsRepository.storeUnlockPin(any(), any()) } just runs

        val viewModel = createViewModel(initialState = initialState)
        viewModel.trySendAction(
            AccountSecurityAction.UnlockWithPinToggle(
                UnlockWithPinState.Enabled(
                    pin = "1234",
                    shouldRequireMasterPasswordOnRestart = true,
                ),
            ),
        )
        assertEquals(
            initialState.copy(isUnlockWithPinEnabled = true),
            viewModel.stateFlow.value,
        )
        verify {
            settingsRepository.storeUnlockPin(
                pin = "1234",
                shouldRequireMasterPasswordOnRestart = true,
            )
        }

        verify(exactly = 0) {
            firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = false)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithPinToggle Enabled should update show unlock badge state if card is visible`() {
        mutableFirstTimeStateFlow.update { it.copy(showSetupUnlockCard = true) }
        val initialState = DEFAULT_STATE.copy(
            isUnlockWithPinEnabled = false,
        )
        every { settingsRepository.storeUnlockPin(any(), any()) } just runs

        val viewModel = createViewModel(initialState = initialState)
        viewModel.trySendAction(
            AccountSecurityAction.UnlockWithPinToggle(
                UnlockWithPinState.Enabled(
                    pin = "1234",
                    shouldRequireMasterPasswordOnRestart = true,
                ),
            ),
        )
        assertEquals(
            initialState.copy(isUnlockWithPinEnabled = true, shouldShowUnlockActionCard = true),
            viewModel.stateFlow.value,
        )
        verify {
            settingsRepository.storeUnlockPin(
                pin = "1234",
                shouldRequireMasterPasswordOnRestart = true,
            )
            firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = false)
        }
    }

    @Test
    fun `on LogoutClick should show confirm log out dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(AccountSecurityAction.LogoutClick)
        assertEquals(
            DEFAULT_STATE.copy(dialog = AccountSecurityDialog.ConfirmLogout),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on ConfirmLogoutClick should call logout and hide confirm dialog`() = runTest {
        every { authRepository.logout(reason = any()) } just runs
        val viewModel = createViewModel()
        viewModel.trySendAction(AccountSecurityAction.ConfirmLogoutClick)
        assertEquals(DEFAULT_STATE.copy(dialog = null), viewModel.stateFlow.value)
        verify(exactly = 1) {
            authRepository.logout(
                reason = LogoutReason.Click(source = "AccountSecurityViewModel"),
            )
        }
    }

    @Test
    fun `on DismissDialog should hide dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(AccountSecurityAction.DismissDialog)
        assertEquals(DEFAULT_STATE.copy(dialog = null), viewModel.stateFlow.value)
    }

    @Test
    fun `on PushNotificationConfirm should send NavigateToApplicationDataSettings event`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(AccountSecurityAction.PushNotificationConfirm)
                assertEquals(
                    AccountSecurityEvent.NavigateToApplicationDataSettings,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `when version is at least 31 should show authenticator sync UI`() {
        every { isBuildVersionAtLeast(Build.VERSION_CODES.S) } returns true
        val vm = createViewModel(initialState = null)
        assertEquals(
            DEFAULT_STATE.copy(shouldShowEnableAuthenticatorSync = true),
            vm.stateFlow.value,
        )
    }

    @Test
    fun `when version is under 31 should not show authenticator sync UI`() {
        every { isBuildVersionAtLeast(Build.VERSION_CODES.S) } returns false
        val vm = createViewModel(initialState = null)
        assertEquals(
            DEFAULT_STATE,
            vm.stateFlow.value,
        )
    }

    @Test
    fun `when showUnlockBadgeFlow updates value, should update state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableFirstTimeStateFlow.update { it.copy(showSetupUnlockCard = true) }
            assertEquals(DEFAULT_STATE.copy(shouldShowUnlockActionCard = true), awaitItem())
        }
    }

    @Test
    fun `when UnlockActionCardDismiss action received, should dismiss unlock action card`() {
        mutableFirstTimeStateFlow.update { it.copy(showSetupUnlockCard = true) }
        val viewModel = createViewModel()
        viewModel.trySendAction(AccountSecurityAction.UnlockActionCardDismiss)
        verify {
            firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = false)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when UnlockActionCardCtaClick action received, should dismiss unlock action card and send NavigateToSetupUnlockScreen event`() =
        runTest {
            mutableFirstTimeStateFlow.update { it.copy(showSetupUnlockCard = true) }
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(AccountSecurityAction.UnlockActionCardCtaClick)
                assertEquals(
                    AccountSecurityEvent.NavigateToSetupUnlockScreen,
                    awaitItem(),
                )
            }
            verify(exactly = 0) {
                firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = false)
            }
        }

    @Test
    fun `when BiometricLockUpdate action is handled, should update the state`() = runTest {
        val viewModel = createViewModel()
        mutableBiometricsUnlockEnabledFlow.emit(true)
        val expectedState = DEFAULT_STATE.copy(isUnlockWithBiometricsEnabled = true)
        assertEquals(
            viewModel.stateFlow.value,
            expectedState,
        )
    }

    @Test
    fun `when PinProtectedLockUpdate action is handled, should update the state`() = runTest {
        val viewModel = createViewModel()
        mutablePinUnlockEnabledFlow.emit(true)
        val expectedState = DEFAULT_STATE.copy(isUnlockWithPinEnabled = true)
        assertEquals(
            viewModel.stateFlow.value,
            expectedState,
        )
    }

    @Suppress("LongParameterList")
    private fun createViewModel(
        initialState: AccountSecurityState? = DEFAULT_STATE,
        authRepository: AuthRepository = this.authRepository,
        vaultRepository: VaultRepository = this.vaultRepository,
        environmentRepository: EnvironmentRepository = this.fakeEnvironmentRepository,
        settingsRepository: SettingsRepository = this.settingsRepository,
        biometricsEncryptionManager: BiometricsEncryptionManager = this.biometricsEncryptionManager,
        policyManager: PolicyManager = this.policyManager,
    ): AccountSecurityViewModel = AccountSecurityViewModel(
        authRepository = authRepository,
        vaultRepository = vaultRepository,
        settingsRepository = settingsRepository,
        environmentRepository = environmentRepository,
        biometricsEncryptionManager = biometricsEncryptionManager,
        policyManager = policyManager,
        savedStateHandle = SavedStateHandle().apply {
            set("state", initialState)
        },
        firstTimeActionManager = firstTimeActionManager,
    )
}

private val CIPHER = mockk<Cipher>()
private const val FINGERPRINT: String = "fingerprint"

private const val DEFAULT_USER_ID: String = "activeUserId"
private val DEFAULT_USER_STATE = UserState(
    activeUserId = DEFAULT_USER_ID,
    accounts = listOf(
        UserState.Account(
            userId = "activeUserId",
            name = "Active User",
            email = "active@bitwarden.com",
            avatarColorHex = "#aa00aa",
            environment = Environment.Us,
            isPremium = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = listOf(
                Organization(
                    id = "organizationUser",
                    name = "Organization User",
                    shouldUseKeyConnector = false,
                    shouldManageResetPassword = false,
                    role = OrganizationType.USER,
                    keyConnectorUrl = null,
                    userIsClaimedByOrganization = false,
                ),
                Organization(
                    id = "organizationAdmin",
                    name = "Organization Admin",
                    shouldUseKeyConnector = false,
                    shouldManageResetPassword = false,
                    role = OrganizationType.ADMIN,
                    keyConnectorUrl = null,
                    userIsClaimedByOrganization = false,
                ),
                Organization(
                    id = "organizationOwner",
                    name = "Organization Owner",
                    shouldUseKeyConnector = false,
                    shouldManageResetPassword = false,
                    role = OrganizationType.OWNER,
                    keyConnectorUrl = null,
                    userIsClaimedByOrganization = false,
                ),
                Organization(
                    id = "organizationCustom",
                    name = "Organization Owner",
                    shouldUseKeyConnector = false,
                    shouldManageResetPassword = false,
                    role = OrganizationType.CUSTOM,
                    keyConnectorUrl = null,
                    userIsClaimedByOrganization = false,
                ),
            ),
            needsMasterPassword = false,
            trustedDevice = null,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            onboardingStatus = OnboardingStatus.COMPLETE,
            firstTimeState = FirstTimeState(showImportLoginsCard = true),
        ),
    ),
)

private val DEFAULT_STATE: AccountSecurityState = AccountSecurityState(
    dialog = null,
    fingerprintPhrase = FINGERPRINT.asText(),
    isAuthenticatorSyncChecked = false,
    isUnlockWithBiometricsEnabled = false,
    isUnlockWithPasswordEnabled = true,
    isUnlockWithPinEnabled = false,
    userId = DEFAULT_USER_ID,
    vaultTimeout = VaultTimeout.ThirtyMinutes,
    vaultTimeoutAction = VaultTimeoutAction.LOCK,
    vaultTimeoutPolicyMinutes = null,
    vaultTimeoutPolicyAction = null,
    shouldShowEnableAuthenticatorSync = false,
    shouldShowUnlockActionCard = false,
    removeUnlockWithPinPolicyEnabled = false,
)
