package com.x8bit.bitwarden.ui.vault.feature.item.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.cardStyle
import com.x8bit.bitwarden.ui.platform.base.util.nullableTestTag
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenExpandingHeader
import com.x8bit.bitwarden.ui.platform.components.icon.BitwardenIcon
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.item.model.VaultItemLocation
import kotlinx.collections.immutable.ImmutableList

/**
 * The max number of items that can be displayed before the "show more" text is visible.
 */
private const val EXPANDABLE_THRESHOLD = 2

/**
 * Reusable composable for displaying the cipher name, favorite status, and related locations.
 */
@Suppress("LongMethod", "LongParameterList")
fun LazyListScope.itemHeader(
    value: String,
    isFavorite: Boolean,
    relatedLocations: ImmutableList<VaultItemLocation>,
    iconData: IconData,
    isExpanded: Boolean,
    iconTestTag: String? = null,
    textFieldTestTag: String? = null,
    onExpandClick: () -> Unit,
) {
    item {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .defaultMinSize(minHeight = 60.dp)
                .cardStyle(
                    cardStyle = CardStyle.Top(),
                    paddingVertical = 0.dp,
                )
                .padding(start = 16.dp),
        ) {
            ItemHeaderIcon(
                iconData = iconData,
                testTag = iconTestTag,
                modifier = Modifier.size(36.dp),
            )
            BitwardenTextField(
                label = null,
                value = value,
                onValueChange = { },
                readOnly = true,
                singleLine = false,
                actions = {
                    Icon(
                        painter = painterResource(
                            id = if (isFavorite) {
                                R.drawable.ic_favorite_full
                            } else {
                                R.drawable.ic_favorite_empty
                            },
                        ),
                        contentDescription = stringResource(
                            id = if (isFavorite) R.string.favorite else R.string.unfavorite,
                        ),
                        modifier = Modifier.padding(all = 12.dp),
                    )
                },
                textFieldTestTag = textFieldTestTag,
                cardStyle = null,
                textStyle = BitwardenTheme.typography.titleMedium,
            )
        }
    }

    if (relatedLocations.isEmpty()) {
        item {
            ItemLocationListItem(
                vectorPainter = rememberVectorPainter(R.drawable.ic_folder),
                text = stringResource(R.string.no_folder),
                iconTestTag = "NoFolderIcon",
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth()
                    .cardStyle(
                        cardStyle = CardStyle.Bottom,
                        paddingVertical = 0.dp,
                    ),
            )
        }
        return
    }

    items(
        key = { "locations_$it" },
        items = relatedLocations.take(EXPANDABLE_THRESHOLD),
    ) {
        ItemLocationListItem(
            vectorPainter = rememberVectorPainter(it.icon),
            iconTestTag = "ItemLocationIcon",
            text = it.name,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .animateItem()
                .cardStyle(
                    cardStyle = if (relatedLocations.size > EXPANDABLE_THRESHOLD) {
                        CardStyle.Middle(hasDivider = false)
                    } else {
                        CardStyle.Bottom
                    },
                    paddingVertical = 0.dp,
                    paddingHorizontal = 16.dp,
                ),
        )
    }

    if (isExpanded) {
        items(
            key = { "expandableLocations_$it" },
            items = relatedLocations.drop(EXPANDABLE_THRESHOLD),
        ) {
            ItemLocationListItem(
                vectorPainter = rememberVectorPainter(it.icon),
                text = it.name,
                iconTestTag = "ItemLocationIcon",
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .animateItem()
                    .cardStyle(
                        cardStyle = CardStyle.Middle(hasDivider = false),
                        paddingVertical = 0.dp,
                        paddingHorizontal = 16.dp,
                    ),
            )
        }
    }

    if (relatedLocations.size > EXPANDABLE_THRESHOLD) {
        item(key = "expandableLocationsShowMore") {
            BitwardenExpandingHeader(
                collapsedText = stringResource(R.string.show_more),
                expandedText = stringResource(R.string.show_less),
                isExpanded = isExpanded,
                onClick = onExpandClick,
                showExpansionIndicator = false,
                shape = RectangleShape,
                insets = PaddingValues(),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .animateItem()
                    .cardStyle(
                        cardStyle = CardStyle.Bottom,
                        paddingVertical = 0.dp,
                    ),
            )
        }
    }
}

@Composable
private fun ItemHeaderIcon(
    iconData: IconData,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val isLocalIcon = iconData is IconData.Local
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.then(
            if (isLocalIcon) {
                Modifier.background(
                    color = BitwardenTheme.colorScheme.illustration.backgroundPrimary,
                    shape = BitwardenTheme.shapes.favicon,
                )
            } else {
                Modifier
            },
        ),
    ) {
        BitwardenIcon(
            iconData = iconData,
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.illustration.outline,
            modifier = Modifier
                .nullableTestTag(testTag)
                .then(
                    if (!isLocalIcon) Modifier.fillMaxSize() else Modifier,
                ),
        )
    }
}

@Composable
private fun LazyItemScope.ItemLocationListItem(
    vectorPainter: VectorPainter,
    iconTestTag: String?,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .padding(8.dp),
    ) {
        Icon(
            painter = vectorPainter,
            tint = BitwardenTheme.colorScheme.icon.primary,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .nullableTestTag(iconTestTag),
        )
        Text(
            text = text,
            style = BitwardenTheme.typography.bodyLarge,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(start = 16.dp)
                .testTag("ItemLocationText"),
        )
    }
}

//region Previews
// @Composable
// @Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
// private fun ItemHeader_LocalIcon_Preview() {
//    BitwardenTheme {
//        ItemHeader(
//            value = "Login without favicon",
//            isFavorite = true,
//            iconData = IconData.Local(
//                iconRes = R.drawable.ic_globe,
//            ),
//            relatedLocations = persistentListOf(),
//        )
//    }
// }
//
// @Composable
// @Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
// private fun ItemHeader_NetworkIcon_Preview() {
//    BitwardenTheme {
//        ItemHeader(
//            value = "Login with favicon",
//            isFavorite = true,
//            iconData = IconData.Network(
//                uri = "mockuri",
//                fallbackIconRes = R.drawable.ic_globe,
//            ),
//            relatedLocations = persistentListOf(),
//        )
//    }
// }
//
// @Composable
// @Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
// private fun ItemHeader_Organization_Preview() {
//    BitwardenTheme {
//        ItemHeader(
//            value = "Login without favicon",
//            isFavorite = true,
//            iconData = IconData.Local(
//                iconRes = R.drawable.ic_globe,
//            ),
//            relatedLocations = persistentListOf(
//                VaultItemLocation.Organization("Stark Industries"),
//            ),
//        )
//    }
// }
//
// @Composable
// @Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
// private fun ItemNameField_Org_SingleCollection_Preview() {
//    BitwardenTheme {
//        ItemHeader(
//            value = "Login without favicon",
//            isFavorite = true,
//            iconData = IconData.Local(
//                iconRes = R.drawable.ic_globe,
//            ),
//            relatedLocations = persistentListOf(
//                VaultItemLocation.Organization("Stark Industries"),
//                VaultItemLocation.Collection("Marketing"),
//            ),
//        )
//    }
// }
//
// @Composable
// @Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
// private fun ItemNameField_Org_MultiCollection_Preview() {
//    BitwardenTheme {
//        ItemHeader(
//            value = "Login without favicon",
//            isFavorite = true,
//            iconData = IconData.Local(
//                iconRes = R.drawable.ic_globe,
//            ),
//            relatedLocations = persistentListOf(
//                VaultItemLocation.Organization("Stark Industries"),
//                VaultItemLocation.Collection("Marketing"),
//                VaultItemLocation.Collection("Product"),
//            ),
//        )
//    }
// }
//
// @Composable
// @Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
// private fun ItemNameField_Org_SingleCollection_Folder_Preview() {
//    BitwardenTheme {
//        ItemHeader(
//            value = "Note without favicon",
//            isFavorite = true,
//            iconData = IconData.Local(
//                iconRes = R.drawable.ic_note,
//            ),
//            relatedLocations = persistentListOf(
//                VaultItemLocation.Organization("Stark Industries"),
//                VaultItemLocation.Collection("Marketing"),
//                VaultItemLocation.Folder("Competition"),
//            ),
//        )
//    }
// }
//endregion Previews
