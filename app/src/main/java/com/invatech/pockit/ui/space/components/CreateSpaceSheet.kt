package com.invatech.pockit.ui.space.components

/*
 * Collects space name, visual choices and bill-split/household type, then delegates creation to its callback.
 */

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.space.model.SpaceAccents
import com.invatech.pockit.ui.space.model.SpaceEmojis
import com.invatech.pockit.ui.space.model.SpaceType
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.Mint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSpaceSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, emoji: String, themeIndex: Int, type: SpaceType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(SpaceEmojis.first()) }
    var themeIndex by remember { mutableStateOf(0) }
    var spaceType by remember { mutableStateOf(SpaceType.BILL_SPLIT) }
    var isCreating by remember { mutableStateOf(false) }

    val heroAccent by animateColorAsState(
        targetValue = SpaceAccents[themeIndex],
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "hero_accent_color"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Create a space",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "A shared place to track expenses and manage budgets together.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Animated Hero Emoji Avatar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(heroAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, style = MaterialTheme.typography.displayLarge)
            }

            SoftTextField(
                value = name,
                onValueChange = { if (it.length <= 24) name = it },
                placeholder = if (spaceType == SpaceType.HOUSEHOLD) "Space name (e.g. Our Home)" else "Space name (e.g. Goa Trip)"
            )

            // Space Mode Selection
            Text(
                text = "Space Mode",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bill Split Card
                Surface(
                    onClick = { spaceType = SpaceType.BILL_SPLIT },
                    shape = RoundedCornerShape(20.dp),
                    color = if (spaceType == SpaceType.BILL_SPLIT) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (spaceType == SpaceType.BILL_SPLIT) 2.dp else 1.dp,
                        color = if (spaceType == SpaceType.BILL_SPLIT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "👥", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.size(6.dp))
                            Text(
                                text = "Bill Split",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Split costs & settle debts (Trips, roommates)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Household Card
                Surface(
                    onClick = { spaceType = SpaceType.HOUSEHOLD },
                    shape = RoundedCornerShape(20.dp),
                    color = if (spaceType == SpaceType.HOUSEHOLD) Mint.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (spaceType == SpaceType.HOUSEHOLD) 2.dp else 1.dp,
                        color = if (spaceType == SpaceType.HOUSEHOLD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏡", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.size(6.dp))
                            Text(
                                text = "Household",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Joint tracking, no debt splits (Family, couple)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "Pick an icon",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpaceEmojis.forEach { item ->
                    val isSelected = item == emoji
                    val emojiScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "emoji_selection_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .scale(emojiScale)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { emoji = item },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = item, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }

            Text(
                text = "Pick a color",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpaceAccents.forEachIndexed { index, color ->
                    val isSelected = index == themeIndex
                    val borderWidth by animateDpAsState(
                        targetValue = if (isSelected) 3.5.dp else 0.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "color_border_width"
                    )
                    val colorScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "color_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .scale(colorScale)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = borderWidth,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { themeIndex = index }
                    )
                }
            }

            PockitPrimaryButton(
                text = if (isCreating) "Creating space..." else "Create space",
                enabled = name.isNotBlank() && !isCreating,
                onClick = {
                    isCreating = true
                    onCreate(name, emoji, themeIndex, spaceType)
                }
            )
        }
    }
}
