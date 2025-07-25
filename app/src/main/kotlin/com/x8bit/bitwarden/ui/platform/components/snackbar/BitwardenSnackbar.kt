package com.x8bit.bitwarden.ui.platform.components.snackbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.button.color.bitwardenOutlinedButtonColors
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText

/**
 * Custom snackbar for Bitwarden.
 *
 * @param bitwardenSnackbarData The data required to display the Snackbar.
 * @param modifier The [Modifier] to be applied to the button.
 * @param windowInsets The insets to be applied to this composable. By default this will account for
 * the insets that are on the sides and bottom of the screen (Display Cutout and Navigation bars).
 * @param onDismiss The callback invoked when the Snackbar is dismissed.
 * @param onActionClick The callback invoked when the Snackbar action occurs.
 */
@Suppress("LongMethod")
@Composable
fun BitwardenSnackbar(
    bitwardenSnackbarData: BitwardenSnackbarData,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.displayCutout
        .only(sides = WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        .union(insets = WindowInsets.navigationBars),
    onDismiss: () -> Unit = {},
    onActionClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .windowInsetsPadding(insets = windowInsets)
            .consumeWindowInsets(insets = windowInsets)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BitwardenTheme.colorScheme.background.alert,
                    shape = BitwardenTheme.shapes.snackbar,
                )
                // I there is no explicit dismiss action, the Snackbar can be dismissed by clicking
                // anywhere on the Snackbar.
                .clickable(
                    enabled = !bitwardenSnackbarData.withDismissAction,
                    onClick = onDismiss,
                )
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.weight(weight = 1f)) {
                bitwardenSnackbarData.messageHeader?.let {
                    Text(
                        text = it(),
                        color = BitwardenTheme.colorScheme.text.reversed,
                        style = BitwardenTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = bitwardenSnackbarData.message(),
                    color = BitwardenTheme.colorScheme.text.reversed,
                    style = BitwardenTheme.typography.bodyMedium,
                )
                bitwardenSnackbarData.actionLabel?.let {
                    Spacer(Modifier.height(12.dp))
                    BitwardenOutlinedButton(
                        label = it(),
                        onClick = onActionClick,
                        colors = bitwardenOutlinedButtonColors(
                            contentColor = BitwardenTheme.colorScheme.text.reversed,
                            outlineColor = BitwardenTheme
                                .colorScheme
                                .outlineButton
                                .borderReversed,
                        ),
                    )
                }
            }
            if (bitwardenSnackbarData.withDismissAction) {
                BitwardenStandardIconButton(
                    onClick = onDismiss,
                    vectorIconRes = BitwardenDrawable.ic_close,
                    contentDescription = stringResource(BitwardenString.close),
                    contentColor = BitwardenTheme.colorScheme.icon.reversed,
                    modifier = Modifier.offset(x = 12.dp, y = (-12).dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun BitwardenCustomSnackbar_preview() {
    BitwardenTheme {
        Surface {
            BitwardenSnackbar(
                BitwardenSnackbarData(
                    messageHeader = "Header".asText(),
                    message = "Message".asText(),
                    actionLabel = "Action".asText(),
                    withDismissAction = true,
                ),
            )
        }
    }
}
