package com.invatech.pockit.ui.finance

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.invatech.pockit.ui.common.motion.MotionSettings
import kotlinx.coroutines.launch
import kotlin.math.*

private val OnboardingInk = Color(0xFF101714)
private val OnboardingWhite = Color(0xFFF4F7EE)
private val OnboardingMuted = Color(0xFF62665D)
private data class IntroPage(val label: String, val title: String, val highlight: String, val description: String, val accent: Color)
private val IntroPages = listOf(
    IntroPage("THE BIG PICTURE", "Money, in", "full flow.", "From your first coffee to your next big plan. Give every rupee a place to belong.", Color(0xFFF1849C)),
    IntroPage("MAKE IT HAPPEN", "Little savings.", "Big possibilities.", "A new laptop. That long-awaited trip. Build a budget that makes room for your life.", Color(0xFFC4B8F2)),
    IntroPage("KEEP IT BETWEEN YOU", "Good friends.", "Clear balances.", "Lent a little? Borrowed some? Just add a name. Your friends don't need the app.", Color(0xFF91CAB7)),
    IntroPage("YOU'RE IN CONTROL", "Less typing.", "More living.", "Let UPI payments come to you. Review the details, make a quick edit, and you're done.", Color(0xFFF8CC66))
)

/** Draw-only liquid motion: no bitmaps, blur, shaders, or per-frame layout work. */
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pager = rememberPagerState(pageCount = { IntroPages.size })
    val scope = rememberCoroutineScope()
    val preference by MotionSettings.enabled.collectAsState()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val motion = preference && lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val phase = liquidPhase(motion)
    val accent by animateColorAsState(IntroPages[pager.currentPage].accent, tween(if (motion) 550 else 0), label = "intro_palette")
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val statusWasLight = controller?.isAppearanceLightStatusBars ?: true
        val navWasLight = controller?.isAppearanceLightNavigationBars ?: true
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true
        onDispose { controller?.isAppearanceLightStatusBars = statusWasLight; controller?.isAppearanceLightNavigationBars = navWasLight }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFFFFFBEF))) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .075f), Color.Transparent), Offset(size.width * .9f, size.height * .25f), size.width * .9f), radius = size.width * .9f, center = Offset(size.width * .9f, size.height * .25f))
            drawLine(Color.White.copy(alpha = .035f), Offset(size.width * .12f, 0f), Offset(size.width * .12f, size.height), 1f)
            drawLine(Color.White.copy(alpha = .035f), Offset(size.width * .88f, 0f), Offset(size.width * .88f, size.height), 1f)
        }
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(Modifier.size(29.dp).clip(RoundedCornerShape(9.dp)).background(accent), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.AccountBalanceWallet, null, Modifier.size(18.dp), tint = OnboardingInk)
                    }
                    Text("pockit", color = OnboardingInk, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                }
                TextButton(onClick = onComplete, colors = ButtonDefaults.textButtonColors(contentColor = OnboardingMuted)) { Text("Skip") }
            }
            BoxWithConstraints(Modifier.weight(1f)) {
                val heroHeight = (maxHeight * .53f).coerceIn(200.dp, 340.dp)
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().testTag("intro_pager"), beyondViewportPageCount = 0) { index ->
                    val page = IntroPages[index]
                    val scroll = (pager.currentPage - index) + pager.currentPageOffsetFraction
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp)) {
                        Box(Modifier.fillMaxWidth().height(heroHeight).graphicsLayer {
                            if (motion) {
                                translationX = scroll * size.width * .13f
                                rotationZ = scroll * -5f
                                alpha = 1f - abs(scroll).coerceIn(0f, 1f) * .45f
                            }
                        }, contentAlignment = Alignment.Center) {
                            LiquidArtwork(index, page.accent, phase, motion, if (motion) scroll else 0f)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(6.dp).background(page.accent, CircleShape))
                            Text(page.label, color = OnboardingInk, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(page.title, color = OnboardingInk, fontSize = 39.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.7).sp, lineHeight = 43.sp)
                        Text(page.highlight, color = Color(0xFF286B5E), fontSize = if (index == 1) 35.sp else 39.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.7).sp, lineHeight = 43.sp)
                        Spacer(Modifier.height(15.dp))
                        Text(page.description, color = OnboardingMuted, fontSize = 15.sp, lineHeight = 23.sp)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 26.dp).padding(top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(4) { index ->
                            val width by animateDpAsState(if (pager.currentPage == index) 30.dp else 7.dp, tween(if (motion) 300 else 0), label = "intro_dot")
                            Box(Modifier.width(width).height(5.dp).clip(CircleShape).background(if (pager.currentPage == index) accent else Color(0xFF3C4941)))
                        }
                    }
                    Text("${pager.currentPage + 1} of 4", color = OnboardingMuted, fontSize = 12.sp)
                }
                Button(onClick = {
                    if (!pager.isScrollInProgress) {
                        if (pager.currentPage == 3) onComplete()
                        else scope.launch { if (motion) pager.animateScrollToPage(pager.currentPage + 1, animationSpec = tween(650, easing = FastOutSlowInEasing)) else pager.scrollToPage(pager.currentPage + 1) }
                    }
                }, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = OnboardingInk)) {
                    Text(if (pager.currentPage == 3) "Get started" else "Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(21.dp))
                }
                Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Lock, null, Modifier.size(12.dp), tint = OnboardingMuted)
                    Text("Your money. Your rules.", color = OnboardingMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun liquidPhase(enabled: Boolean): State<Float> {
    if (!enabled) return rememberUpdatedState(0f)
    val transition = rememberInfiniteTransition(label = "liquid_idle")
    return transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "liquid_phase")
}

@Composable
private fun LiquidArtwork(page: Int, accent: Color, phase: State<Float>, motion: Boolean, drag: Float) {
    val artworkMuted = Color(0xFFB9C6BD)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val cardWidth = (maxWidth * .72f).coerceAtMost(265.dp)
        val artworkScale = (maxHeight.value / 310f).coerceIn(.6f, 1f)
        Canvas(Modifier.fillMaxSize()) {
            val t = phase.value
            val center = Offset(size.width * .5f, size.height * .49f)
            val radius = min(size.width * .44f, size.height * .40f)
            // Smooth quadratic lobes morph continuously; phase is read only during drawing.
            val points = List(64) { i ->
                val angle = i * 2.0 * PI / 64
                val r = radius * (1 + .105 * sin(angle * 3 + t) + .055 * cos(angle * 5 - t * 2))
                Offset(center.x + (cos(angle) * r * (1 + abs(drag).coerceAtMost(1f) * .24f)).toFloat(), center.y + (sin(angle) * r * (1 - abs(drag).coerceAtMost(1f) * .12f)).toFloat())
            }
            val shape = Path().apply {
                val first = (points.last() + points.first()) / 2f
                moveTo(first.x, first.y)
                points.forEachIndexed { index, point ->
                    val next = points[(index + 1) % points.size]
                    val mid = (point + next) / 2f
                    quadraticTo(point.x, point.y, mid.x, mid.y)
                }
                close()
            }
            drawPath(shape, Brush.linearGradient(listOf(accent, accent.copy(alpha = .60f), Color(0xFF285B43)), Offset(center.x - radius, center.y - radius), Offset(center.x + radius, center.y + radius)))
            drawPath(shape, Brush.linearGradient(listOf(Color.White.copy(alpha = .5f), Color.Transparent, accent.copy(alpha = .35f))), style = Stroke(1.3.dp.toPx()))
            // Small satellite drops and orbit lines add depth without expensive blur layers.
            drawCircle(accent.copy(alpha = .8f), radius * .075f, Offset(center.x - radius * .95f, center.y + radius * .72f + sin(t) * 8))
            drawCircle(accent.copy(alpha = .45f), radius * .035f, Offset(center.x + radius * .97f, center.y - radius * .66f))
            drawCircle(Color.White.copy(alpha = .09f), radius * 1.1f, center, style = Stroke(1.dp.toPx()))
            val glint = Offset(center.x + radius * .78f, center.y - radius * .74f)
            drawLine(OnboardingWhite.copy(alpha = .75f), glint - Offset(6.dp.toPx(), 0f), glint + Offset(6.dp.toPx(), 0f), 1.5.dp.toPx())
            drawLine(OnboardingWhite.copy(alpha = .75f), glint - Offset(0f, 6.dp.toPx()), glint + Offset(0f, 6.dp.toPx()), 1.5.dp.toPx())
        }
        Surface(modifier = Modifier.width(cardWidth).graphicsLayer {
            scaleX = artworkScale
            scaleY = artworkScale
            rotationZ = -9f + if (motion) sin(phase.value) * 2f else 0f
            translationY = if (motion) cos(phase.value) * 7.dp.toPx() else 0f
        }, color = Color(0xFF18271F).copy(alpha = .96f), shape = RoundedCornerShape(25.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .22f)), shadowElevation = 16.dp) {
            Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(when(page) { 0 -> "YOUR POCKIT"; 1 -> "THE NEXT BIG THING"; 2 -> "YOUR INNER CIRCLE"; else -> "PAYMENT DETECTED" }, color = artworkMuted, fontSize = 8.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                    Icon(if (page == 3) Icons.Rounded.VerifiedUser else Icons.Rounded.AccountBalanceWallet, null, Modifier.size(17.dp), tint = accent)
                }
                when(page) {
                    0 -> {
                        Text("₹12,450", color = OnboardingWhite, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                            listOf(.3f, .5f, .4f, .72f, .6f, 1f, .83f).forEachIndexed { i, value ->
                                Box(Modifier.weight(1f).height((value * 39).dp).clip(RoundedCornerShape(4.dp)).background(if (i > 4) accent else accent.copy(alpha = .2f)))
                            }
                        }
                        Text("A little clarity goes a long way.", color = artworkMuted, fontSize = 9.sp)
                    }
                    1 -> {
                        Icon(Icons.Rounded.LaptopMac, null, Modifier.size(36.dp), tint = accent)
                        Text("New laptop fund", color = OnboardingWhite, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color.White.copy(alpha = .1f))) { Box(Modifier.fillMaxWidth(.72f).fillMaxHeight().background(accent, CircleShape)) }
                        Text("₹18,000 saved  /  ₹25,000", color = artworkMuted, fontSize = 10.sp)
                    }
                    2 -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("A", "R", "You").forEachIndexed { i, label -> Box(Modifier.size(38.dp).background(if (i == 2) accent else Color(0xFF35473D), CircleShape), contentAlignment = Alignment.Center) { Text(label, color = if (i == 2) OnboardingInk else OnboardingWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
                        }
                        Text("All good. All clear.", color = OnboardingWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Rahul owes you", color = artworkMuted, fontSize = 11.sp)
                        Text("₹450", color = accent, fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
                    }
                    else -> {
                        Text("Coffee with friends", color = OnboardingWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("− ₹240", color = OnboardingWhite, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth().background(accent.copy(alpha = .12f), RoundedCornerShape(10.dp)).padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Icon(Icons.Rounded.CheckCircle, null, Modifier.size(15.dp), tint = accent)
                            Text("Ready for your approval", color = accent, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
        Surface(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 0.dp, bottom = 18.dp).graphicsLayer {
            scaleX = artworkScale.coerceAtLeast(.8f)
            scaleY = artworkScale.coerceAtLeast(.8f)
            rotationZ = 5f
            translationY = if (motion) sin(phase.value + 1f) * -8.dp.toPx() else 0f
        }, color = OnboardingWhite, shape = RoundedCornerShape(15.dp), shadowElevation = 8.dp) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(when(page) { 0 -> Icons.Rounded.TrendingUp; 1 -> Icons.Rounded.Savings; 2 -> Icons.Rounded.DoneAll; else -> Icons.Rounded.Lock }, null, Modifier.size(20.dp), tint = Color(0xFF38634B))
                Column {
                    Text(when(page) { 0 -> "Looking good"; 1 -> "72% of the way"; 2 -> "No invites needed"; else -> "You get the final say" }, color = OnboardingInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(when(page) { 0 -> "Your month, at a glance"; 1 -> "One step closer"; 2 -> "Just a name. That's it."; else -> "Always review first" }, color = Color(0xFF6C7970), fontSize = 9.sp)
                }
            }
        }
    }
}
