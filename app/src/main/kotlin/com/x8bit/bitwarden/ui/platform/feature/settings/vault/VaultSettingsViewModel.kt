package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * View model for the vault screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class VaultSettingsViewModel @Inject constructor(
    snackbarRelayManager: SnackbarRelayManager,
    private val firstTimeActionManager: FirstTimeActionManager,
) : BaseViewModel<VaultSettingsState, VaultSettingsEvent, VaultSettingsAction>(
    initialState = run {
        val firstTimeState = firstTimeActionManager.currentOrDefaultUserFirstTimeState
        VaultSettingsState(
            showImportActionCard = firstTimeState.showImportLoginsCardInSettings,
        )
    },
) {
    init {
        firstTimeActionManager
            .firstTimeStateFlow
            .map {
                VaultSettingsAction.Internal.UserFirstTimeStateChanged(
                    showImportLoginsCard = it.showImportLoginsCardInSettings,
                )
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        snackbarRelayManager
            .getSnackbarDataFlow(SnackbarRelay.LOGINS_IMPORTED)
            .map { VaultSettingsAction.Internal.SnackbarDataReceived(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultSettingsAction): Unit = when (action) {
        VaultSettingsAction.BackClick -> handleBackClicked()
        VaultSettingsAction.ExportVaultClick -> handleExportVaultClicked()
        VaultSettingsAction.FoldersButtonClick -> handleFoldersButtonClicked()
        VaultSettingsAction.ImportItemsClick -> handleImportItemsClicked()
        VaultSettingsAction.ImportLoginsCardCtaClick -> handleImportLoginsCardClicked()
        VaultSettingsAction.ImportLoginsCardDismissClick -> handleImportLoginsCardDismissClicked()
        is VaultSettingsAction.Internal -> handleInternalAction(action)
    }

    private fun handleInternalAction(action: VaultSettingsAction.Internal) {
        when (action) {
            is VaultSettingsAction.Internal.UserFirstTimeStateChanged -> {
                handleUserFirstTimeStateChanged(action)
            }

            is VaultSettingsAction.Internal.SnackbarDataReceived -> {
                handleSnackbarDataReceived(action)
            }
        }
    }

    private fun handleSnackbarDataReceived(
        action: VaultSettingsAction.Internal.SnackbarDataReceived,
    ) {
        sendEvent(VaultSettingsEvent.ShowSnackbar(action.data))
    }

    private fun handleImportLoginsCardDismissClicked() {
        if (!state.showImportActionCard) return
        firstTimeActionManager.storeShowImportLoginsSettingsBadge(showBadge = false)
    }

    private fun handleImportLoginsCardClicked() {
        sendEvent(VaultSettingsEvent.NavigateToImportVault)
    }

    private fun handleUserFirstTimeStateChanged(
        action: VaultSettingsAction.Internal.UserFirstTimeStateChanged,
    ) {
        mutableStateFlow.update {
            it.copy(showImportActionCard = action.showImportLoginsCard)
        }
    }

    private fun handleBackClicked() {
        sendEvent(VaultSettingsEvent.NavigateBack)
    }

    private fun handleExportVaultClicked() {
        sendEvent(VaultSettingsEvent.NavigateToExportVault)
    }

    private fun handleFoldersButtonClicked() {
        sendEvent(VaultSettingsEvent.NavigateToFolders)
    }

    private fun handleImportItemsClicked() {
        sendEvent(VaultSettingsEvent.NavigateToImportVault)
    }
}

/**
 * Models the state for the VaultSettingScreen.
 */
data class VaultSettingsState(
    val showImportActionCard: Boolean,
)

/**
 * Models events for the vault screen.
 */
sealed class VaultSettingsEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : VaultSettingsEvent()

    /**
     * Navigate to the import vault URL.
     */
    data object NavigateToImportVault : VaultSettingsEvent()

    /**
     * Navigate to the Export Vault screen.
     */
    data object NavigateToExportVault : VaultSettingsEvent()

    /**
     * Navigate to the Folders screen.
     */
    data object NavigateToFolders : VaultSettingsEvent()

    /**
     * Shows a snackbar with the given [data].
     */
    data class ShowSnackbar(val data: BitwardenSnackbarData) : VaultSettingsEvent(), BackgroundEvent
}

/**
 * Models actions for the vault screen.
 */
sealed class VaultSettingsAction {
    /**
     * User clicked back button.
     */
    data object BackClick : VaultSettingsAction()

    /**
     * Indicates that the user clicked the Export Vault button.
     */
    data object ExportVaultClick : VaultSettingsAction()

    /**
     * Indicates that the user clicked the Folders button.
     */
    data object FoldersButtonClick : VaultSettingsAction()

    /**
     * Indicates that the user clicked the Import Items button.
     */
    data object ImportItemsClick : VaultSettingsAction()

    /**
     * Indicates that the user clicked the CTA on the action card.
     */
    data object ImportLoginsCardCtaClick : VaultSettingsAction()

    /**
     * Indicates that the user dismissed the action card.
     */
    data object ImportLoginsCardDismissClick : VaultSettingsAction()

    /**
     * Internal actions not performed by user interation
     */
    sealed class Internal : VaultSettingsAction() {
        /**
         * Indicates user first time state has changed.
         */
        data class UserFirstTimeStateChanged(
            val showImportLoginsCard: Boolean,
        ) : Internal()

        /**
         * Indicates that the snackbar data has been received.
         */
        data class SnackbarDataReceived(val data: BitwardenSnackbarData) : Internal()
    }
}
