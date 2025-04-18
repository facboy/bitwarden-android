package com.bitwarden.authenticator.data.auth.datasource.disk

import android.content.SharedPreferences
import com.bitwarden.data.datasource.disk.BaseEncryptedDiskSource

private const val AUTHENTICATOR_SYNC_SYMMETRIC_KEY = "authenticatorSyncSymmetricKey"
private const val LAST_ACTIVE_TIME_KEY = "lastActiveTime"
private const val BIOMETRICS_UNLOCK_KEY = "userKeyBiometricUnlock"

/**
 * Primary implementation of [AuthDiskSource].
 */
class AuthDiskSourceImpl(
    encryptedSharedPreferences: SharedPreferences,
    sharedPreferences: SharedPreferences,
) : BaseEncryptedDiskSource(
    encryptedSharedPreferences = encryptedSharedPreferences,
    sharedPreferences = sharedPreferences,
),
    AuthDiskSource {

    override fun getLastActiveTimeMillis(): Long? =
        getLong(key = LAST_ACTIVE_TIME_KEY)

    override fun storeLastActiveTimeMillis(
        lastActiveTimeMillis: Long?,
    ) {
        putLong(
            key = LAST_ACTIVE_TIME_KEY,
            value = lastActiveTimeMillis,
        )
    }

    override fun getUserBiometricUnlockKey(): String? =
        getEncryptedString(key = BIOMETRICS_UNLOCK_KEY)

    override fun storeUserBiometricUnlockKey(
        biometricsKey: String?,
    ) {
        putEncryptedString(
            key = BIOMETRICS_UNLOCK_KEY,
            value = biometricsKey,
        )
    }

    override var authenticatorBridgeSymmetricSyncKey: ByteArray?
        set(value) {
            val asString = value?.let { value.toString(Charsets.ISO_8859_1) }
            putEncryptedString(AUTHENTICATOR_SYNC_SYMMETRIC_KEY, asString)
        }
        get() = getEncryptedString(AUTHENTICATOR_SYNC_SYMMETRIC_KEY)
            ?.toByteArray(Charsets.ISO_8859_1)
}
