package com.invatech.pockit.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val CardShape = RoundedCornerShape(20.dp)

/**
 * Horizontal strip of palette cards with a live mini-preview of each palette in the
 * brightness currently in effect. Drop it into the settings screen next to the
 * System / Light / Dark selector:
 *
 *     val palette by ThemeManager.palette.collectAsState()
 *     PalettePicker(selected = palette, onSelect = ThemeManager::setPalette)
 */
@Composable
fun PalettePicker(
    selected: PockitPalette,
    onSelect: (PockitPalette) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PockitPalette.entries.forEach { palette ->
            PaletteCard(
                palette = palette,
                isDark = isDark,
                selected = palette == selected,
                onClick = { onSelect(palette) }
            )
        }
    }
}

@Composable
private fun PaletteCard(
    palette: PockitPalette,
    isDark: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val preview = remember(palette, isDark) { palette.colorScheme(isDark) }
    val ui = MaterialTheme.colorScheme

    val borderColor by animateColorAsState(
        targetValue = if (selected) ui.primary else ui.outline,
        label = "palette_border_color"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        label = "palette_border_width"
    )

    Column(
        modifier = Modifier
            .width(132.dp)
            .clip(CardShape)
            .background(ui.surface)
            .border(borderWidth, borderColor, CardShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PalettePreview(
            scheme = preview,
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = palette.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = ui.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = palette.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = ui.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = ui.primary,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(20.dp)
                )
            }
        }
    }
}

/** A tiny fake screen drawn with the palette's own colours: card, text lines, button, accents. */
@Composable
private fun PalettePreview(
    scheme: ColorScheme,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.background)
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.surface)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(scheme.primary)
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(scheme.onSurface.copy(alpha = 0.7f))
                    )
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(scheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(14.dp)
                        .clip(CircleShape)
                        .background(scheme.primary)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(scheme.secondary)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(scheme.tertiary)
                )
            }
        }
    }
}
