package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Represents a Bitwarden-styled [Switch].
 *
 * @param label The label for the switch.
 * @param isChecked Whether or not the switch is currently checked.
 * @param onCheckedChange A callback for when the checked state changes.
 * @param modifier The [Modifier] to be applied to the button.
 */
@Composable
fun BitwardenSwitch(
    label: String,
    isChecked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = MaterialTheme.colorScheme.primary),
                onClick = { onCheckedChange?.invoke(!isChecked) },
            )
            .semantics(mergeDescendants = true) {
                toggleableState = ToggleableState(isChecked)
            }
            .then(modifier),
    ) {
        Switch(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .height(32.dp)
                .width(52.dp),
            checked = isChecked,
            onCheckedChange = null,
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        )
    }
}

@Preview
@Composable
private fun BitwardenSwitch_preview_isChecked() {
    BitwardenSwitch(
        label = "Label",
        isChecked = true,
        onCheckedChange = {},
    )
}

@Preview
@Composable
private fun BitwardenSwitch_preview_isNotChecked() {
    BitwardenSwitch(
        label = "Label",
        isChecked = false,
        onCheckedChange = {},
    )
}
