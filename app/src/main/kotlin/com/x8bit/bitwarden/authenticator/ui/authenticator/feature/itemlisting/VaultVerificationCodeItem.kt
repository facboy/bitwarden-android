package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.ui.platform.components.icon.BitwardenIcon
import com.x8bit.bitwarden.authenticator.ui.platform.components.indicator.BitwardenCircularCountdownIndicator
import com.x8bit.bitwarden.authenticator.ui.platform.components.model.IconData
import com.x8bit.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme

/**
 * The verification code item displayed to the user.
 *
 * @param authCode The code for the item.
 * @param issuer The label for the item.
 * @param periodSeconds The times span where the code is valid.
 * @param timeLeftSeconds The seconds remaining until a new code is needed.
 * @param startIcon The leading icon for the item.
 * @param onItemClick The lambda function to be invoked when the item is clicked.
 * @param modifier The modifier for the item.
 * @param supportingLabel The supporting label for the item.
 */
@OptIn(ExperimentalFoundationApi::class)
@Suppress("LongMethod", "MagicNumber")
@Composable
fun VaultVerificationCodeItem(
    authCode: String,
    issuer: String?,
    periodSeconds: Int,
    timeLeftSeconds: Int,
    alertThresholdSeconds: Int,
    startIcon: IconData,
    onItemClick: () -> Unit,
    onEditItemClick: () -> Unit,
    onDeleteItemClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingLabel: String? = null,
) {
    var shouldShowDropdownMenu by remember { mutableStateOf(value = false) }
    Row(
        modifier = Modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = MaterialTheme.colorScheme.primary),
                onClick = onItemClick,
                onLongClick = {
                    shouldShowDropdownMenu = true
                }
            )
            .defaultMinSize(minHeight = 72.dp)
            .padding(vertical = 8.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BitwardenIcon(
            iconData = startIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.weight(1f),
        ) {
            issuer?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            supportingLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        BitwardenCircularCountdownIndicator(
            timeLeftSeconds = timeLeftSeconds,
            periodSeconds = periodSeconds,
            alertThresholdSeconds = alertThresholdSeconds
        )

        Text(
            text = authCode.chunked(3).joinToString(" "),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    DropdownMenu(
        expanded = shouldShowDropdownMenu,
        onDismissRequest = { shouldShowDropdownMenu = false },
    ) {
        DropdownMenuItem(
            text = {
                Text(text = stringResource(id = R.string.edit_item))
            },
            onClick = {
                shouldShowDropdownMenu = false
                onEditItemClick()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit_item),
                    contentDescription = stringResource(R.string.edit_item)
                )
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = {
                Text(text = stringResource(id = R.string.delete_item))
            },
            onClick = {
                shouldShowDropdownMenu = false
                onDeleteItemClick()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete_item),
                    contentDescription = stringResource(id = R.string.delete_item),
                )
            }
        )
    }
}

@Suppress("MagicNumber")
@Preview(showBackground = true)
@Composable
private fun VerificationCodeItem_preview() {
    AuthenticatorTheme {
        VaultVerificationCodeItem(
            authCode = "1234567890".chunked(3).joinToString(" "),
            issuer = "Sample Label",
            periodSeconds = 30,
            timeLeftSeconds = 15,
            alertThresholdSeconds = 7,
            startIcon = IconData.Local(R.drawable.ic_login_item),
            onItemClick = {},
            onEditItemClick = {},
            onDeleteItemClick = {},
            modifier = Modifier.padding(horizontal = 16.dp),
            supportingLabel = "Supporting Label",
        )
    }
}
