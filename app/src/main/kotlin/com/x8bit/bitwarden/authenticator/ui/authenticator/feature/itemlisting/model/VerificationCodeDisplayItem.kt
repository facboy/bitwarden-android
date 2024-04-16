package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model

import android.os.Parcelable
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.ui.platform.components.model.IconData
import kotlinx.parcelize.Parcelize

/**
 * The data for the verification code item to display.
 */
@Parcelize
data class VerificationCodeDisplayItem(
    val id: String,
    val label: String,
    val issuer: String?,
    val supportingLabel: String?,
    val timeLeftSeconds: Int,
    val periodSeconds: Int,
    val alertThresholdSeconds: Int,
    val authCode: String,
    val startIcon: IconData = IconData.Local(R.drawable.ic_login_item),
) : Parcelable
