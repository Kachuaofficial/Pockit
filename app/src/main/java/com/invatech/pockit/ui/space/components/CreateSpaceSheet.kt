package com.invatech.pockit.ui.space.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.common.loading.ExpressiveLoadingIndicator
import com.invatech.pockit.ui.space.model.SpaceAccents
import com.invatech.pockit.ui.space.model.SpaceEmojis
import com.invatech.pockit.ui.theme.Ink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSpaceSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, emoji: String, themeIndex: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(SpaceEmojis.first()) }
    var themeIndex by remember { mutableStateOf(0) }
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
                text = "A shared place to track and split expenses with your people.",
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
                Text(text = emoji, fontSize = 42.sp)
            }

            SoftTextField(
                value = name,
                onValueChange = { if (it.length <= 24) name = it },
                placeholder = "Space name (e.g. Goa Trip)"
            )

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
                                if (isSelected) Ink else MaterialTheme.colorScheme.surface
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { emoji = item },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = item, fontSize = 24.sp)
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
                                color = Ink,
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
                    onCreate(name, emoji, themeIndex)
                }
            )
        }
    }
}
