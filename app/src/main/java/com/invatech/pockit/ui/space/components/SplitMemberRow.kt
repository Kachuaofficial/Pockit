package com.invatech.pockit.ui.space.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.space.model.Member
import com.invatech.pockit.ui.space.model.SplitMode
import com.invatech.pockit.ui.space.util.formatMoney
import com.invatech.pockit.ui.theme.Cloud
import com.invatech.pockit.ui.theme.CloudDeep
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted

@Composable
fun SplitMemberRow(
    member: Member,
    mode: SplitMode,
    selected: Boolean,
    equalShare: Double,
    customText: String,
    onToggle: () -> Unit,
    onCustomChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rowShape = RoundedCornerShape(22.dp)

    // Animated checkbox check scale
    val checkScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "check_scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val checkBg by animateColorAsState(
        targetValue = if (selected) primaryColor else Color.Transparent,
        label = "check_bg_color"
    )

    val checkBorder by animateColorAsState(
        targetValue = if (selected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
        label = "check_border_color"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = rowShape,
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .then(if (mode == SplitMode.Equal) Modifier.clickable(onClick = onToggle) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MemberAvatar(member = member, size = 40.dp)

            Text(
                text = member.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "split_mode_content"
            ) { currentMode ->
                if (currentMode == SplitMode.Equal) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnimatedVisibility(
                            visible = selected,
                            enter = fadeIn() + scaleIn(spring(stiffness = Spring.StiffnessMedium))
                        ) {
                            Text(
                                text = formatMoney(equalShare),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .scale(checkScale)
                                .clip(CircleShape)
                                .background(checkBg)
                                .border(2.dp, checkBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = onPrimaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .width(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "₹",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        BasicTextField(
                            value = customText,
                            onValueChange = onCustomChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(primaryColor),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            decorationBox = { inner ->
                                Box {
                                    if (customText.isEmpty()) {
                                        Text(
                                            text = "0",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
