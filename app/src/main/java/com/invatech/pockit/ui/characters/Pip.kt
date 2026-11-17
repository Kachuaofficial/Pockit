package com.invatech.pockit.ui.characters

/*
 * PIP — an original Compose-Canvas mascot with 10 moods.
 *
 * Usage:
 *   setContent { MaterialTheme { MascotShowcase() } }          // demo screen
 *   Mascot(mood = MascotMood.Happy, modifier = Modifier.size(120.dp))   // anywhere in your UI
 *
 * Needs Compose 1.5+ (Path.quadraticTo) and Material3. No image assets, everything is drawn in code.
 */

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// ───────────────────────────── palette & constants ─────────────────────────────

private val Ink = Color(0xFF1F2A44)
private val BaseBody = Color(0xFF5FD8C0)
private val Blush = Color(0xFFFF8FA8)
private val TAU = (2.0 * PI).toFloat()
private val PI_F = PI.toFloat()

private fun Color.fade(a: Float): Color = copy(alpha = a.coerceIn(0f, 1f))

// ───────────────────────────── moods ─────────────────────────────

enum class MascotMood(
    val label: String,
    val tagline: String,
    val bg: Color,
    internal val tint: Color,
    internal val glow: Color,
) {
    Happy("Happy", "Sab badhiya hai!", Color(0xFFFFF4C9), BaseBody, Color(0xFFFFD84D)),
    Excited("Excited", "Kuch bada hone wala hai!", Color(0xFFFFE9D6), Color(0xFFB8F26B), Color(0xFFFFE066)),
    Love("Love", "Dil se dil tak", Color(0xFFFFE0EC), Color(0xFFFF7FAF), Color(0xFFFF6FA5)),
    Wink("Wink", "Ye apna secret hai", Color(0xFFEAE2FF), BaseBody, Color(0xFFFFD84D)),
    Surprised("Surprised", "Arey wah! Sach mein?", Color(0xFFD9F4FF), Color(0xFF6FD6FF), Color(0xFF63E6FF)),
    Thinking("Thinking", "Hmm, ek minute sochne do...", Color(0xFFEDE8FF), Color(0xFFB4A7FF), Color(0xFFC0A8FF)),
    Sleepy("Sleepy", "Thodi der aur soja na...", Color(0xFFDDE3FF), Color(0xFF7C86E8), Color(0xFF8EA0FF)),
    Sad("Sad", "Aaj thoda dil bhaari hai", Color(0xFFDCE8F7), Color(0xFF6C8BC8), Color(0xFF7FA6E8)),
    Angry("Angry", "Bas, bohot ho gaya!", Color(0xFFFFDAD4), Color(0xFFFF5A4D), Color(0xFFFF4D4D)),
    Cool("Cool", "Apun toh swag mein hai", Color(0xFFD6ECFF), Color(0xFF4FA3FF), Color(0xFF4DB8FF)),
}

// ───────────────────────────── public composables ─────────────────────────────

@Composable
fun Mascot(mood: MascotMood, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pip")

    // 0..1 loop every 3s — drives breathing, sway, floating particles
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "phase",
    )

    // Blink: open most of the time, quick close near the end of each cycle
    val blink = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes<Float> {
                durationMillis = 3600
                1f at 0
                1f at 3300
                0.08f at 3420
                1f at 3540
            }
        ),
        label = "blink",
    )

    // Little squash-and-pop whenever the mood changes
    val pop = remember { Animatable(1f) }
    LaunchedEffect(mood) {
        pop.snapTo(0.86f)
        pop.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        )
    }

    // Body colour glides between moods
    val tint = animateColorAsState(mood.tint, tween(450), label = "tint")

    Canvas(modifier.aspectRatio(1f)) {
        drawMascot(mood, phase.value, blink.value, pop.value, tint.value)
    }
}

@Composable
fun Pip(mood: MascotMood, modifier: Modifier = Modifier) {
    Mascot(mood = mood, modifier = modifier)
}

@Composable
fun MascotShowcase(modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf(MascotMood.Happy) }
    val bg by animateColorAsState(selected.bg, tween(500), label = "bg")
    val moods = remember { MascotMood.values().toList() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Meet Pip", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("Ek mascot, dus moods", fontSize = 14.sp, color = Ink.fade(0.6f))
        Spacer(Modifier.height(12.dp))

        Mascot(selected, Modifier.size(260.dp))

        Crossfade(targetState = selected, label = "caption") { mood ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(mood.label, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(
                    mood.tagline,
                    fontSize = 15.sp,
                    color = Ink.fade(0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        moods.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { mood ->
                    MoodTile(
                        mood = mood,
                        selected = mood == selected,
                        modifier = Modifier.weight(1f),
                    ) { selected = mood }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MoodTile(
    mood: MascotMood,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(mood.bg)
            .border(
                width = if (selected) 2.5.dp else 0.dp,
                color = Ink.fade(0.85f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Mascot(mood, Modifier.fillMaxWidth())
        Text(mood.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun MascotShowcasePreview() {
    MaterialTheme { MascotShowcase() }
}

// ───────────────────────────── drawing ─────────────────────────────
// Everything below draws in a 100 x 100 design space; drawMascot scales it to the canvas.

internal fun DrawScope.drawMascot(
    mood: MascotMood,
    phase: Float,
    blink: Float,
    pop: Float,
    tint: Color,
) {
    val u = size.minDimension / 100f
    val t = phase * TAU
    val s = sin(t)

    // Whole-body motion per mood
    var dx = 0f
    var dy = s * 1.2f
    var rot = 0f
    var sx = 1f
    var sy = 1f + s * 0.012f
    when (mood) {
        MascotMood.Excited -> dy = -abs(sin(t * 2f)) * 7f
        MascotMood.Angry -> dx = sin(t * 12f) * 0.9f
        MascotMood.Sad -> {
            dy = 1.8f + s * 0.5f
            sy = 0.97f
        }
        MascotMood.Sleepy -> {
            rot = s * 3.5f
            dy = 1.2f + s * 1.2f
        }
        MascotMood.Love -> {
            val p = 1f + 0.03f * sin(t * 3f)
            sx = p
            sy = p
        }
        MascotMood.Surprised -> dy = -3f + sin(t * 10f) * 0.3f
        MascotMood.Thinking -> rot = 4f + s * 1.5f
        else -> Unit
    }

    val earTilt = when (mood) {
        MascotMood.Sad -> -30f
        MascotMood.Sleepy -> -18f
        MascotMood.Angry -> -22f
        MascotMood.Surprised -> 6f
        MascotMood.Excited -> sin(t * 4f) * 6f
        MascotMood.Love -> sin(t * 3f) * 4f
        MascotMood.Happy -> sin(t * 2f) * 2.5f
        MascotMood.Wink -> 3f
        MascotMood.Thinking -> 0f
        MascotMood.Cool -> 0f
    }

    val body = lerp(BaseBody, tint, 0.3f)
    val dark = lerp(body, Color(0xFF0B2F3A), 0.35f)
    val light = lerp(body, Color.White, 0.5f)
    val armFill = lerp(body, dark, 0.18f)
    val (leftArm, rightArm) = armPoints(mood, t)

    withTransform({
        translate((size.width - size.minDimension) / 2f, (size.height - size.minDimension) / 2f)
        scale(u, u, Offset.Zero)
    }) {
        // ground shadow shrinks while Pip hops
        val lift = maxOf(0f, -dy)
        val shadowW = 44f * (1f - lift * 0.03f)
        drawOval(
            Color.Black.fade(0.16f - lift * 0.01f),
            Offset(50f - shadowW / 2f, 91.5f),
            Size(shadowW, 6f),
        )

        withTransform({
            translate(dx, dy)
            rotate(rot, Offset(50f, 90f))
            scale(sx * pop, sy * pop, Offset(50f, 90f))
        }) {
            drawEars(earTilt, body, dark)
            drawAntenna(mood, t, dark)
            drawBody(body, dark, light)
            drawLimb(leftArm, armFill, dark)
            drawLimb(rightArm, armFill, dark)
            drawCheeks(mood, t)
            drawEyes(mood, phase, t, blink)
            drawBrows(mood, t)
            drawMouth(mood, t)
        }

        drawExtras(mood, phase, t)
    }
}

private fun DrawScope.drawEars(tilt: Float, body: Color, dark: Color) {
    val outer = Path().apply {
        moveTo(19f, 44f)
        quadraticTo(15f, 18f, 29f, 16f)
        quadraticTo(41f, 15f, 47f, 33f)
        close()
    }
    val inner = Path().apply {
        moveTo(25f, 40f)
        quadraticTo(23f, 24f, 30f, 23f)
        quadraticTo(37f, 23f, 41f, 33f)
        close()
    }
    for (side in 0..1) {
        withTransform({
            if (side == 1) scale(-1f, 1f, Offset(50f, 0f))
            rotate(tilt, Offset(33f, 42f))
        }) {
            drawPath(outer, body)
            drawPath(inner, Color(0xFFFFB6C8))
            drawPath(outer, dark, style = Stroke(width = 1.4f, join = StrokeJoin.Round))
        }
    }
}

private fun DrawScope.drawAntenna(mood: MascotMood, t: Float, dark: Color) {
    val droop = mood == MascotMood.Sad || mood == MascotMood.Sleepy
    val bulb = when (mood) {
        MascotMood.Sad -> Offset(60f, 25f)
        MascotMood.Sleepy -> Offset(58f, 23f)
        else -> Offset(50f + sin(t) * 3f, 13f)
    }
    val ctrl = if (droop) Offset(50f, 17f) else Offset(50f + sin(t) * 1.2f, 24f)
    val stalk = Path().apply {
        moveTo(50f, 36f)
        quadraticTo(ctrl.x, ctrl.y, bulb.x, bulb.y)
    }
    drawPath(stalk, dark, style = Stroke(width = 2.4f, cap = StrokeCap.Round))

    val dim = when (mood) {
        MascotMood.Sad -> 0.5f
        MascotMood.Sleepy -> 0.4f
        else -> 1f
    }
    val pulse = 1f + 0.12f * sin(t * 2f)
    drawCircle(mood.glow.fade(0.28f * dim), 8.5f * pulse, bulb)
    drawCircle(mood.glow.fade(dim), 4.6f, bulb)
    drawCircle(Color.White.fade(0.7f * dim), 1.5f, Offset(bulb.x - 1.3f, bulb.y - 1.4f))
    drawCircle(dark, 4.6f, bulb, style = Stroke(width = 1.2f))
}

private fun DrawScope.drawBody(body: Color, dark: Color, light: Color) {
    // feet
    drawOval(dark, Offset(27f, 84f), Size(19f, 9.5f))
    drawOval(dark, Offset(54f, 84f), Size(19f, 9.5f))

    // torso
    val brush = Brush.verticalGradient(
        colors = listOf(lerp(body, Color.White, 0.25f), body),
        startY = 30f,
        endY = 89f,
    )
    drawRoundRect(brush, Offset(17f, 30f), Size(66f, 59f), CornerRadius(30f, 30f))
    drawRoundRect(dark, Offset(17f, 30f), Size(66f, 59f), CornerRadius(30f, 30f), style = Stroke(width = 1.5f))

    // belly + gloss
    drawOval(light.fade(0.75f), Offset(33f, 71f), Size(34f, 16f))
    drawOval(Color.White.fade(0.4f), Offset(25f, 37f), Size(11f, 6f))
}

private fun armPoints(mood: MascotMood, t: Float): Pair<List<Offset>, List<Offset>> {
    val ls = Offset(21f, 64f)
    val rs = Offset(79f, 64f)
    val w = sin(t * 4f) * 3f
    return when (mood) {
        MascotMood.Happy -> listOf(ls, Offset(10f, 69f)) to listOf(rs, Offset(90f, 69f))
        MascotMood.Excited -> listOf(ls, Offset(9f, 43f + w)) to listOf(rs, Offset(91f, 43f - w))
        MascotMood.Love -> listOf(ls, Offset(43f, 74f)) to listOf(rs, Offset(57f, 74f))
        MascotMood.Wink -> listOf(ls, Offset(10f, 69f)) to listOf(rs, Offset(90f, 40f + w))
        MascotMood.Surprised -> listOf(ls, Offset(11f, 49f)) to listOf(rs, Offset(89f, 49f))
        MascotMood.Thinking -> listOf(ls, Offset(10f, 70f)) to listOf(rs, Offset(62f, 72f))
        MascotMood.Sleepy -> listOf(ls, Offset(14f, 74f)) to listOf(rs, Offset(86f, 74f))
        MascotMood.Sad -> listOf(ls, Offset(18f, 79f)) to listOf(rs, Offset(82f, 79f))
        MascotMood.Angry -> listOf(ls, Offset(62f, 75f)) to listOf(rs, Offset(38f, 75f))
        MascotMood.Cool ->
            listOf(ls, Offset(8f, 72f), Offset(22f, 80f)) to
                listOf(rs, Offset(92f, 72f), Offset(78f, 80f))
    }
}

private fun DrawScope.drawLimb(points: List<Offset>, fill: Color, dark: Color) {
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    drawPath(path, dark, style = Stroke(width = 10.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, fill, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawCheeks(mood: MascotMood, t: Float) {
    val color = if (mood == MascotMood.Angry) Color(0xFFFF4D4D) else Blush
    val alpha = when (mood) {
        MascotMood.Love -> 0.9f
        MascotMood.Angry -> 0.55f
        MascotMood.Cool -> 0.3f
        MascotMood.Sad -> 0.4f
        else -> 0.6f
    }
    val grow = if (mood == MascotMood.Love) 1.15f + 0.1f * sin(t * 3f) else 1f
    val w = 11f * grow
    val h = 7f * grow
    drawOval(color.fade(alpha), Offset(29f - w / 2f, 62.5f - h / 2f), Size(w, h))
    drawOval(color.fade(alpha), Offset(71f - w / 2f, 62.5f - h / 2f), Size(w, h))
}

// ───────────────────────────── eyes ─────────────────────────────

private fun DrawScope.openEye(
    cx: Float,
    cy: Float,
    rx: Float = 5.6f,
    ry: Float = 7f,
    blink: Float = 1f,
    look: Offset = Offset.Zero,
    shine: Float = 1f,
) {
    val x = cx + look.x
    val y = cy + look.y
    val h = ry * blink
    drawOval(Ink, Offset(x - rx, y - h), Size(rx * 2f, h * 2f))
    if (blink > 0.5f) {
        drawCircle(Color.White, 2.3f * shine, Offset(x - rx * 0.32f, y - ry * 0.38f))
        drawCircle(Color.White.fade(0.85f), 1.1f * shine, Offset(x + rx * 0.35f, y + ry * 0.42f))
    }
}

private fun DrawScope.arcEye(cx: Float, cy: Float, happy: Boolean) {
    val p = Path().apply {
        if (happy) {
            moveTo(cx - 6f, cy + 2.5f)
            quadraticTo(cx, cy - 6f, cx + 6f, cy + 2.5f)
        } else {
            moveTo(cx - 6f, cy - 1.5f)
            quadraticTo(cx, cy + 6f, cx + 6f, cy - 1.5f)
        }
    }
    drawPath(p, Ink, style = Stroke(width = 2.8f, cap = StrokeCap.Round))
}

private fun heartPath(cx: Float, cy: Float, s: Float): Path = Path().apply {
    moveTo(cx, cy + s * 0.9f)
    cubicTo(cx - s * 1.5f, cy - s * 0.1f, cx - s * 0.9f, cy - s * 1.1f, cx, cy - s * 0.35f)
    cubicTo(cx + s * 0.9f, cy - s * 1.1f, cx + s * 1.5f, cy - s * 0.1f, cx, cy + s * 0.9f)
    close()
}

private fun DrawScope.drawEyes(mood: MascotMood, phase: Float, t: Float, blink: Float) {
    val eyeL = 38f
    val eyeR = 62f
    val y = 52f

    when (mood) {
        MascotMood.Happy -> {
            openEye(eyeL, y, blink = blink)
            openEye(eyeR, y, blink = blink)
        }
        MascotMood.Excited -> {
            openEye(eyeL, y - 1f, rx = 6.6f, ry = 8.2f, shine = 1.35f)
            openEye(eyeR, y - 1f, rx = 6.6f, ry = 8.2f, shine = 1.35f)
        }
        MascotMood.Love -> {
            val p = 1f + 0.12f * sin(t * 3f)
            for (cx in listOf(eyeL, eyeR)) {
                drawPath(heartPath(cx, y, 5.6f * p), Color(0xFFFF3D77))
                drawCircle(Color.White.fade(0.9f), 1.4f, Offset(cx - 3f * p, y - 2.4f * p))
            }
        }
        MascotMood.Wink -> {
            openEye(eyeL, y, blink = blink)
            arcEye(eyeR, y, happy = true)
        }
        MascotMood.Surprised -> {
            for (cx in listOf(37f, 63f)) {
                drawCircle(Color.White, 8.4f, Offset(cx, 51f))
                drawCircle(Ink, 8.4f, Offset(cx, 51f), style = Stroke(width = 1.5f))
                drawCircle(Ink, 3.6f, Offset(cx, 51f + sin(t * 5f) * 0.4f))
                drawCircle(Color.White, 1.2f, Offset(cx - 1f, 50f))
            }
        }
        MascotMood.Thinking -> {
            val look = Offset(2.2f, -2.6f)
            openEye(eyeL, y, blink = blink, look = look)
            openEye(eyeR, y, blink = blink, look = look)
        }
        MascotMood.Sleepy -> {
            arcEye(eyeL, y, happy = false)
            arcEye(eyeR, y, happy = false)
        }
        MascotMood.Sad -> {
            for (cx in listOf(eyeL, eyeR)) {
                openEye(cx, y, rx = 5.8f, ry = 7.6f, blink = blink, look = Offset(0f, 0.8f), shine = 1.35f)
            }
            // falling tears
            for ((i, cx) in listOf(eyeL, eyeR).withIndex()) {
                val f = (phase * 2f + i * 0.5f) % 1f
                val a = minOf(f * 6f, 1f) * (1f - f)
                val tx = cx - 1.7f + (if (i == 0) -2f else 2f)
                drawOval(Color(0xFF8FD3FF).fade(a), Offset(tx, 58.5f + f * 14f), Size(3.4f, 5f))
            }
        }
        MascotMood.Angry -> {
            openEye(eyeL, y + 0.5f, rx = 5.4f, ry = 4.8f, blink = blink)
            openEye(eyeR, y + 0.5f, rx = 5.4f, ry = 4.8f, blink = blink)
        }
        MascotMood.Cool -> {
            val lens = Color(0xFF121826)
            drawRoundRect(lens, Offset(28f, 45f), Size(18f, 13f), CornerRadius(5f, 5f))
            drawRoundRect(lens, Offset(54f, 45f), Size(18f, 13f), CornerRadius(5f, 5f))
            drawLine(lens, Offset(46f, 49f), Offset(54f, 49f), 2.4f, StrokeCap.Round)
            drawLine(lens, Offset(28f, 48f), Offset(22.5f, 46.5f), 2f, StrokeCap.Round)
            drawLine(lens, Offset(72f, 48f), Offset(77.5f, 46.5f), 2f, StrokeCap.Round)
            for (x in listOf(28f, 54f)) {
                drawLine(Color.White.fade(0.45f), Offset(x + 4f, 55.5f), Offset(x + 8f, 47.5f), 1.6f, StrokeCap.Round)
                drawLine(Color.White.fade(0.3f), Offset(x + 8.5f, 55.5f), Offset(x + 11f, 50f), 1.4f, StrokeCap.Round)
            }
        }
    }
}

private fun DrawScope.drawBrows(mood: MascotMood, t: Float) {
    fun brow(x1: Float, y1: Float, cx: Float, cy: Float, x2: Float, y2: Float, w: Float = 2.4f) {
        val p = Path().apply {
            moveTo(x1, y1)
            quadraticTo(cx, cy, x2, y2)
        }
        drawPath(p, Ink, style = Stroke(width = w, cap = StrokeCap.Round))
    }

    when (mood) {
        MascotMood.Surprised -> {
            val up = sin(t * 5f) * 0.5f
            brow(31f, 39f + up, 37f, 34f + up, 43f, 38f + up)
            brow(57f, 38f + up, 63f, 34f + up, 69f, 39f + up)
        }
        MascotMood.Excited -> {
            brow(32f, 41f, 38f, 37.5f, 44f, 40f)
            brow(56f, 40f, 62f, 37.5f, 68f, 41f)
        }
        MascotMood.Thinking -> {
            brow(32f, 43f, 38f, 42f, 44f, 43f)
            brow(56f, 37f, 62f, 33.5f, 68f, 36.5f)
        }
        MascotMood.Sad -> {
            brow(31f, 45f, 37f, 42.5f, 44f, 40.5f)
            brow(56f, 40.5f, 63f, 42.5f, 69f, 45f)
        }
        MascotMood.Angry -> {
            brow(30f, 41f, 37f, 43f, 45f, 48f, 3.4f)
            brow(55f, 48f, 63f, 43f, 70f, 41f, 3.4f)
        }
        else -> Unit
    }
}

// ───────────────────────────── mouth ─────────────────────────────

private fun DrawScope.mouthArc(x1: Float, y1: Float, cx: Float, cy: Float, x2: Float, y2: Float) {
    val p = Path().apply {
        moveTo(x1, y1)
        quadraticTo(cx, cy, x2, y2)
    }
    drawPath(p, Ink, style = Stroke(width = 2.6f, cap = StrokeCap.Round))
}

private fun DrawScope.openMouth(cx: Float, top: Float, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(cx - w / 2f, top)
        lineTo(cx + w / 2f, top)
        cubicTo(cx + w / 2f, top + h * 1.3f, cx - w / 2f, top + h * 1.3f, cx - w / 2f, top)
        close()
    }
    drawPath(path, Color(0xFF5A1330))
    clipPath(path) {
        drawOval(Color(0xFFFF7A93), Offset(cx - w * 0.3f, top + h * 0.5f), Size(w * 0.6f, h * 0.8f))
        drawRect(Color.White, Offset(cx - w / 2f, top), Size(w, h * 0.2f))
    }
    drawPath(path, Ink, style = Stroke(width = 1.8f, join = StrokeJoin.Round))
}

private fun DrawScope.drawMouth(mood: MascotMood, t: Float) {
    when (mood) {
        MascotMood.Happy -> openMouth(50f, 61f, 15f, 9f)
        MascotMood.Excited -> openMouth(50f, 60f, 19f, 12f + sin(t * 4f) * 1.2f)
        MascotMood.Love -> {
            val p = Path().apply {
                moveTo(43.5f, 62f)
                quadraticTo(47f, 67.5f, 50f, 62f)
                quadraticTo(53f, 67.5f, 56.5f, 62f)
            }
            drawPath(p, Ink, style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        MascotMood.Wink -> mouthArc(43f, 61.5f, 50f, 70f, 57f, 61.5f)
        MascotMood.Surprised -> drawOval(Color(0xFF5A1330), Offset(46f, 61f), Size(8f, 11f))
        MascotMood.Thinking -> {
            val p = Path().apply {
                moveTo(44f, 65.5f)
                quadraticTo(47.5f, 61.5f, 51f, 65f)
                quadraticTo(54.5f, 68.5f, 58f, 64.5f)
            }
            drawPath(p, Ink, style = Stroke(width = 2.4f, cap = StrokeCap.Round))
        }
        MascotMood.Sleepy -> {
            val h = 4.4f + sin(t) * 1.2f
            drawOval(Color(0xFF5A1330), Offset(47.2f, 63f), Size(5.6f, h))
        }
        MascotMood.Sad -> {
            val q = sin(t * 9f) * 0.35f
            mouthArc(43f, 68f + q, 50f, 59.5f + q, 57f, 68f + q)
        }
        MascotMood.Angry -> {
            drawRoundRect(Color.White, Offset(42f, 62.5f), Size(16f, 8f), CornerRadius(3f, 3f))
            drawRoundRect(Ink, Offset(42f, 62.5f), Size(16f, 8f), CornerRadius(3f, 3f), style = Stroke(width = 1.8f))
            for (x in listOf(47.3f, 52.7f)) {
                drawLine(Ink, Offset(x, 62.5f), Offset(x, 70.5f), 1.2f)
            }
            drawLine(Ink, Offset(42f, 66.5f), Offset(58f, 66.5f), 1.2f)
        }
        MascotMood.Cool -> mouthArc(43f, 63f, 52f, 69f, 58f, 59.5f)
    }
}

// ───────────────────────────── floating extras ─────────────────────────────

private fun DrawScope.sparkle(cx: Float, cy: Float, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(cx, cy - r)
        quadraticTo(cx, cy, cx + r, cy)
        quadraticTo(cx, cy, cx, cy + r)
        quadraticTo(cx, cy, cx - r, cy)
        quadraticTo(cx, cy, cx, cy - r)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.drawZ(x: Float, y: Float, s: Float, alpha: Float) {
    val p = Path().apply {
        moveTo(x, y)
        lineTo(x + s, y)
        lineTo(x, y + s)
        lineTo(x + s, y + s)
    }
    drawPath(
        p,
        Color(0xFF6C77E0).fade(alpha),
        style = Stroke(width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun DrawScope.drawExtras(mood: MascotMood, phase: Float, t: Float) {
    val gold = Color(0xFFFFC93C)
    when (mood) {
        MascotMood.Happy -> {
            val k = 0.7f + 0.3f * sin(t * 2f)
            sparkle(90f, 40f, 3.4f * k, gold)
            sparkle(9f, 44f, 2.8f * (1.7f - k), gold)
        }
        MascotMood.Excited -> {
            val spots = listOf(Offset(8f, 32f), Offset(92f, 38f), Offset(80f, 10f), Offset(20f, 12f))
            spots.forEachIndexed { i, p ->
                val k = 0.55f + 0.45f * abs(sin(t * 2f + i * 0.9f))
                sparkle(p.x, p.y, 5.5f * k, gold)
            }
        }
        MascotMood.Love -> {
            val xs = listOf(9f, 91f, 14f)
            for (i in 0..2) {
                val f = (phase + i / 3f) % 1f
                val x = xs[i] + sin(f * TAU) * 2.5f
                val y = 46f - f * 30f
                drawPath(heartPath(x, y, 2.4f + f * 1.2f), Color(0xFFFF5C8A).fade(sin(f * PI_F)))
            }
        }
        MascotMood.Wink -> {
            sparkle(75f, 47f, 4f * (0.7f + 0.3f * sin(t * 3f)), gold)
        }
        MascotMood.Surprised -> {
            val j = sin(t * 6f) * 0.8f
            val red = Color(0xFFFF5A5F)
            drawLine(red, Offset(91f, 8f + j), Offset(91f, 20f + j), 4f, StrokeCap.Round)
            drawCircle(red, 2.4f, Offset(91f, 27f + j))
        }
        MascotMood.Thinking -> {
            val c = Offset(88f, 13f)
            drawCircle(Color.White, 8.5f, c)
            drawCircle(Ink.fade(0.5f), 8.5f, c, style = Stroke(width = 1.2f))
            for (i in 0..2) {
                val a = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * 2f - i * 1.3f))
                drawCircle(Ink.fade(a), 1.4f, Offset(83.5f + i * 4.5f, 13f))
            }
        }
        MascotMood.Sleepy -> {
            for (i in 0..1) {
                val f = (phase + i * 0.5f) % 1f
                drawZ(86f + f * 9f, 42f - f * 26f, 3f + f * 4.5f, sin(f * PI_F))
            }
        }
        MascotMood.Sad -> {
            val cloud = Color(0xFFB4C2DE)
            drawCircle(cloud, 4.5f, Offset(82f, 10f))
            drawCircle(cloud, 6f, Offset(89f, 8f))
            drawCircle(cloud, 4.3f, Offset(96f, 11f))
            drawRoundRect(cloud, Offset(79f, 10f), Size(19f, 6.5f), CornerRadius(3.2f, 3.2f))
            val drops = listOf(86.5f, 91f, 95.5f)
            for (i in 0..2) {
                val f = (phase * 2f + i / 3f) % 1f
                val y = 19f + f * 18f
                drawLine(
                    Color(0xFF6FA8F0).fade(1f - f),
                    Offset(drops[i], y),
                    Offset(drops[i] - 0.8f, y + 3.4f),
                    1.6f,
                    StrokeCap.Round,
                )
            }
        }
        MascotMood.Angry -> {
            val k = 1f + 0.18f * sin(t * 8f)
            val c = Offset(90f, 26f)
            for (sx in listOf(-1f, 1f)) {
                for (sy in listOf(-1f, 1f)) {
                    val p = Path().apply {
                        moveTo(c.x + sx * 5.5f * k, c.y + sy * 1.7f * k)
                        quadraticTo(c.x + sx * 5.5f * k, c.y + sy * 5.5f * k, c.x + sx * 1.7f * k, c.y + sy * 5.5f * k)
                    }
                    drawPath(p, Color(0xFFFF3B30), style = Stroke(width = 2.2f, cap = StrokeCap.Round))
                }
            }
        }
        MascotMood.Cool -> Unit
    }
}
