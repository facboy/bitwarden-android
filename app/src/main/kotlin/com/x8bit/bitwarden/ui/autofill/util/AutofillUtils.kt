package com.x8bit.bitwarden.ui.autofill.util

import androidx.core.content.ContextCompat
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher

/**
 * Creates content description for an autofill suggestion given an [AutofillCipher] and
 * [AutofillAppInfo].
 */
fun getAutofillSuggestionContentDescription(
    autofillCipher: AutofillCipher,
    autofillAppInfo: AutofillAppInfo,
): String =
    String.format(
        "%s, %s, %s, %s",
        ContextCompat.getString(autofillAppInfo.context, BitwardenString.autofill_suggestion),
        getAutofillSuggestionCipherType(
            autofillCipher = autofillCipher,
            autofillAppInfo = autofillAppInfo,
        ),
        autofillCipher.name,
        autofillCipher.subtitle,
    )

private fun getAutofillSuggestionCipherType(
    autofillCipher: AutofillCipher,
    autofillAppInfo: AutofillAppInfo,
): String =
    when (autofillCipher) {
        is AutofillCipher.Card -> ContextCompat.getString(
            autofillAppInfo.context,
            BitwardenString.type_card,
        )

        is AutofillCipher.Login -> ContextCompat.getString(
            autofillAppInfo.context,
            BitwardenString.type_login,
        )
    }
