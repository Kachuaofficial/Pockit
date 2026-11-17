package com.invatech.pockit.ui.friends.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.common.mascot.MascotPose
import com.invatech.pockit.ui.common.mascot.PockitMascot
import com.invatech.pockit.ui.common.motion.M3Spring
import com.invatech.pockit.ui.friends.model.FriendsFilter
import com.invatech.pockit.ui.space.util.formatMoney
import com.invatech.pockit.ui.theme.Cloud
import com.invatech.pockit.ui.theme.CloudDeep
import com.invatech.pockit.ui.theme.DarkBackground
import com.invatech.pockit.ui.theme.DarkTextPrimary
import com.invatech.pockit.ui.theme.DarkTextSecondary
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.MintContainerDark
import com.invatech.pockit.ui.theme.MintDark
import com.invatech.pockit.ui.theme.Peach
import com.invatech.pockit.ui.theme.PeachContainerDark
import com.invatech.pockit.ui.theme.PeachDark

@Composable
fun FriendsSummaryCards(
    totalYoullGet: Double,
    totalYoullGive: Double,
    selectedFilter: FriendsFilter,
    onSelectFilter: (FriendsFilter) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onMyQrClick: () -> Unit,
    onScanQrClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedYoullGet by animateFloatAsState(
        targetValue = totalYoullGet.toFloat(),
        animationSpec = M3Spring.bouncy(),
        label = "animated_youll_get"
    )

    val animatedYoullGive by animateFloatAsState(
        targetValue = totalYoullGive.toFloat(),
        animationSpec = M3Spring.bouncy(),
        label = "animated_youll_give"
    )

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Screen Title and QR Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Friends & Khata",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Scan barcode to connect & split",
                        fontSize = 12.sp,
                        color = InkMuted
                    )
                }
                PockitMascot(
                    pose = MascotPose.HAPPY_WAVING,
                    size = 46.dp
                )
            }

            // QR Actions (My Code & Scan)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // My QR
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onMyQrClick)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCode2,
                            contentDescription = "My QR",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "My QR",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Scan Barcode
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onScanQrClick)
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Scan",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Scan",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Two Cards: You'll Get vs You'll Give
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // You'll Get Card (Diya)
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = if (isDark) MintContainerDark else MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isDark) MintDark else MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                                tint = if (isDark) DarkBackground else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "YOU'LL GET",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) MintDark else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = formatMoney(animatedYoullGet.toDouble()),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) DarkTextPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "Lent",
                        fontSize = 11.sp,
                        color = if (isDark) DarkTextSecondary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                    )
                }
            }

            // You'll Give Card (Liya)
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = if (isDark) PeachContainerDark else MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isDark) PeachDark else MaterialTheme.colorScheme.tertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = null,
                                tint = if (isDark) DarkBackground else MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "YOU'LL GIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) PeachDark else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = formatMoney(animatedYoullGive.toDouble()),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) DarkTextPrimary else MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Text(
                        text = "Borrowed",
                        fontSize = 11.sp,
                        color = if (isDark) DarkTextSecondary else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.65f)
                    )
                }
            }
        }

        // Quick "+ Record Udhar" Action Pill with tactile spring
        val addInteraction = remember { MutableInteractionSource() }
        val isAddPressed by addInteraction.collectIsPressedAsState()
        val addScale by animateFloatAsState(
            targetValue = if (isAddPressed) 0.96f else 1f,
            animationSpec = M3Spring.touchFeedback(),
            label = "add_udhar_pill_scale"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(addScale)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(
                    interactionSource = addInteraction,
                    indication = null,
                    onClick = onAddExpenseClick
                )
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Record Transaction",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Gave money or borrowed from a friend",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Search bar
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Search friend by name or phone...",
                    fontSize = 14.sp,
                    color = InkMuted
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = InkMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = InkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FriendsFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                val bg by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    animationSpec = M3Spring.fastEffects(),
                    label = "filter_pill_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    animationSpec = M3Spring.fastEffects(),
                    label = "filter_pill_text"
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(bg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectFilter(filter) }
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
