package com.invatech.pockit.ui.walkthrough

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.invatech.pockit.ui.common.mascot.MascotPose
import com.invatech.pockit.ui.common.mascot.PockitMascot
import com.invatech.pockit.ui.common.motion.M3Spring
import com.invatech.pockit.ui.theme.CocoaEspresso
import com.invatech.pockit.ui.theme.CocoaMuted
import com.invatech.pockit.ui.theme.PastelPeach
import com.invatech.pockit.ui.theme.PastelPink

data class WalkthroughPage(
    val title: String,
    val subtitle: String,
    val description: String,
    val pose: MascotPose,
    val badge: String
)

private val pages = listOf(
    WalkthroughPage(
        title = "Track with Joy! 🐾",
        subtitle = "Fast & Delightful Logging",
        description = "Add daily expenses and income in 2 seconds. Enjoy cute doodle categories and instant wallet updates.",
        pose = MascotPose.HAPPY_WAVING,
        badge = "Step 1 of 4"
    ),
    WalkthroughPage(
        title = "Friends & Khata 🤝",
        subtitle = "Instant QR Barcode Connect",
        description = "Show your personal barcode to connect in 1 second! Track 'Lent' and 'Borrowed' with mutual verification.",
        pose = MascotPose.COZY_PEEK,
        badge = "Step 2 of 4"
    ),
    WalkthroughPage(
        title = "Spaces for Groups 🏕️",
        subtitle = "Trips, Flats & Roommates",
        description = "Create shared spaces for your group trips or flats. Pockit automatically calculates who owes what with zero drama.",
        pose = MascotPose.THINKING,
        badge = "Step 3 of 4"
    ),
    WalkthroughPage(
        title = "Smart Insights 📈",
        subtitle = "Financial Peace of Mind",
        description = "Explore interactive charts, category breakdowns, savings rate, and cute smart financial advice from your mascot.",
        pose = MascotPose.SLEEPING,
        badge = "Step 4 of 4"
    )
)

@Composable
fun CuteWalkthroughDialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    var currentPageIndex by remember { mutableIntStateOf(0) }
    val page = pages[currentPageIndex]
    val isLastPage = currentPageIndex == pages.lastIndex

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFFFFF9F0),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row: Step badge and Skip button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PastelPeach.copy(alpha = 0.6f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = page.badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CocoaEspresso
                        )
                    }

                    if (!isLastPage) {
                        TextButton(onClick = onComplete) {
                            Text(
                                text = "Skip",
                                color = CocoaMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(40.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mascot & Content with smooth slide transition
                AnimatedContent(
                    targetState = currentPageIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width / 2 } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width / 2 } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width / 2 } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width / 2 } + fadeOut()
                        }
                    },
                    label = "walkthrough_slide"
                ) { index ->
                    val cur = pages[index]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Cute mascot illustration
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(PastelPink.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            PockitMascot(
                                pose = cur.pose,
                                size = 115.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = cur.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CocoaEspresso,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = cur.subtitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD27D2D),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = cur.description,
                            fontSize = 14.sp,
                            color = CocoaMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Interactive Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.indices.forEach { idx ->
                        val isSelected = idx == currentPageIndex
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) CocoaEspresso else Color(0xFFE2D6C6))
                                .clickable { currentPageIndex = idx }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button (Next or Get Started)
                Button(
                    onClick = {
                        if (isLastPage) {
                            onComplete()
                        } else {
                            currentPageIndex++
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CocoaEspresso,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isLastPage) "Let's Get Started! ✨" else "Next →",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
