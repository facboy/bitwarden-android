package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FlagKeyTest {

    @Test
    fun `Feature flags have the correct key name set`() {
        assertEquals(
            FlagKey.EmailVerification.keyName,
            "email-verification",
        )
        assertEquals(
            FlagKey.CredentialExchangeProtocolImport.keyName,
            "cxp-import-mobile",
        )
        assertEquals(
            FlagKey.CredentialExchangeProtocolExport.keyName,
            "cxp-export-mobile",
        )
        assertEquals(
            FlagKey.CipherKeyEncryption.keyName,
            "cipher-key-encryption",
        )
        assertEquals(
            FlagKey.RestrictCipherItemDeletion.keyName,
            "pm-15493-restrict-item-deletion-to-can-manage-permission",
        )
        assertEquals(
            FlagKey.UserManagedPrivilegedApps.keyName,
            "pm-18970-user-managed-privileged-apps",
        )
        assertEquals(
            FlagKey.RemoveCardPolicy.keyName,
            "pm-16442-remove-card-item-type-policy",
        )
    }

    @Test
    fun `All feature flags have the correct default value set`() {
        assertTrue(
            listOf(
                FlagKey.EmailVerification,
                FlagKey.CredentialExchangeProtocolImport,
                FlagKey.CredentialExchangeProtocolExport,
                FlagKey.CipherKeyEncryption,
                FlagKey.RestrictCipherItemDeletion,
                FlagKey.UserManagedPrivilegedApps,
                FlagKey.RemoveCardPolicy,
            ).all {
                !it.defaultValue
            },
        )
    }
}
