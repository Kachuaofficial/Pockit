package com.invatech.pockit.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.common.motion.rememberPressScale
import com.invatech.pockit.ui.home.model.Category
import com.invatech.pockit.ui.home.model.Transaction
import com.invatech.pockit.ui.theme.CocoaEspresso
import com.invatech.pockit.ui.theme.CocoaMuted
import com.invatech.pockit.ui.theme.PastelPeach
import com.invatech.pockit.ui.theme.PastelPink
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// UI models (map your real transaction model to these in the ViewModel / mapper)
// amount < 0 = expense, amount > 0 = income
// ─────────────────────────────────────────────────────────────────────────────

enum class DoodleCategory(val avatarColor: Color, val cardColor: Color) {
    FOOD(Color(0xFFFFB27A), Color(0xFFFFE7D6)),
    GREENS(Color(0xFF9EDDA5), Color(0xFFDFF5E0)),
    DESSERT(Color(0xFFFFA690), Color(0xFFFFDDD3)),
    RIDE(Color(0xFF93CBF2), Color(0xFFDBEFFB)),
    GAME(Color(0xFFC9B8F3), Color(0xFFEDE6FB)),
    BEAUTY(Color(0xFFFF9FC6), Color(0xFFFFE1EF)),
    SPORT(Color(0xFFFFD66E), Color(0xFFFFF3CF))
}

data class RecordEntryUi(
    val id: String,
    val category: DoodleCategory,
    val title: String,
    val time: String,
    val amount: Double
)

data class RecordHourUi(val label: String, val entries: List<RecordEntryUi>)

data class RecordDayUi(
    val dateLabel: String,
    val weekday: String,
    val income: Double,
    val expense: Double,
    val hours: List<RecordHourUi>
)

fun Category.toDoodleCategory(): DoodleCategory = when (this) {
    Category.Food -> DoodleCategory.FOOD
    Category.Groceries, Category.Health, Category.Income, Category.Greens -> DoodleCategory.GREENS
    Category.Dessert -> DoodleCategory.DESSERT
    Category.Car, Category.Travel -> DoodleCategory.RIDE
    Category.Game, Category.Entertainment -> DoodleCategory.GAME
    Category.Cosmetic, Category.Shopping -> DoodleCategory.BEAUTY
    Category.Sport -> DoodleCategory.SPORT
    Category.Salary -> DoodleCategory.GREENS
    Category.Bills, Category.Other -> DoodleCategory.FOOD
}

fun Transaction.toRecordEntryUi(): RecordEntryUi {
    return RecordEntryUi(
        id = stringId.ifEmpty { id.toString() },
        category = category.toDoodleCategory(),
        title = title.ifBlank { category.label },
        time = if (timeLabel.isNotBlank()) timeLabel else dateLabel,
        amount = if (isExpense) -amount else amount
    )
}

fun groupTransactionsToRecordDays(transactions: List<Transaction>): List<RecordDayUi> {
    val dateMap = LinkedHashMap<String, MutableList<Transaction>>()
    transactions.forEach { tx ->
        val key = if (tx.dateKey.isNotBlank()) tx.dateKey else tx.dateLabel
        dateMap.getOrPut(key) { mutableListOf() }.add(tx)
    }

    return dateMap.map { (dateKey, txList) ->
        val first = txList.first()
        val dateLabel = if (first.dateMonthDay.isNotBlank()) first.dateMonthDay else dateKey
        val weekday = if (first.dayOfWeek.isNotBlank()) first.dayOfWeek else ""

        var dayIncome = 0.0
        var dayExpense = 0.0
        txList.forEach {
            if (it.isExpense) dayExpense += it.amount else dayIncome += it.amount
        }

        val hourMap = LinkedHashMap<String, MutableList<RecordEntryUi>>()
        txList.forEach { tx ->
            val hourLabel = if (tx.timeLabel.length >= 2) "${tx.timeLabel.take(2)}:00" else "12:00"
            hourMap.getOrPut(hourLabel) { mutableListOf() }.add(tx.toRecordEntryUi())
        }

        val hours = hourMap.map { (hLabel, entries) ->
            RecordHourUi(label = hLabel, entries = entries)
        }

        RecordDayUi(
            dateLabel = dateLabel,
            weekday = weekday,
            income = dayIncome,
            expense = dayExpense,
            hours = hours
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen pieces
// ─────────────────────────────────────────────────────────────────────────────

/** Warm cream page background with soft peach / pink blobs in the corners. */
@Composable
fun RecordDoodleBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.background(Color(0xFFFFFBEF))) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = PastelPeach.copy(alpha = 0.55f),
                radius = size.width * 0.38f,
                center = Offset(size.width, size.height * 0.02f)
            )
            drawCircle(
                color = PastelPink.copy(alpha = 0.40f),
                radius = size.width * 0.30f,
                center = Offset(-size.width * 0.02f, size.height * 0.22f)
            )
        }
        content()
    }
}

/** One day: dark date chip + weekday chip on the left, income / expense totals on the right. */
@Composable
fun RecordDayGroup(
    day: RecordDayUi,
    modifier: Modifier = Modifier,
    onEntryClick: (RecordEntryUi) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RecordDayHeader(day)
        day.hours.forEach { hour ->
            RecordHourLabel(hour.label)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                hour.entries.forEach { entry ->
                    RecordEntryRow(entry = entry, onClick = { onEntryClick(entry) })
                }
            }
        }
    }
}

@Composable
fun RecordDayHeader(day: RecordDayUi, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = day.dateLabel,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(CocoaEspresso)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = day.weekday,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = CocoaEspresso,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(PastelPeach)
                .padding(horizontal = 7.dp, vertical = 3.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = String.format(Locale.US, "+%.2f", day.income),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            color = CocoaMuted
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "−" + groupedInt(day.expense),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            color = CocoaEspresso
        )
    }
}

@Composable
fun RecordHourLabel(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(start = 2.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            color = CocoaMuted
        )
        Text(
            text = "▸",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = CocoaMuted
        )
    }
}

/** Pastel card: doodle avatar, title + time, signed amount. */
@Composable
fun RecordEntryRow(
    entry: RecordEntryUi,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val source = remember { MutableInteractionSource() }
    val scale by rememberPressScale(source, pressedScale = 0.97f)
    val isIncome = entry.amount > 0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(entry.category.cardColor)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryDoodleAvatar(category = entry.category, modifier = Modifier.size(44.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = CocoaEspresso
            )
            Text(
                text = entry.time,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = CocoaMuted
            )
        }
        Text(
            text = signedInt(entry.amount),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (isIncome) Color(0xFF3E9B5A) else CocoaEspresso
        )
    }
}

/** Dark squircle pencil button (bottom-right "add record"). */
@Composable
fun RecordPencilFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val scale by rememberPressScale(source, pressedScale = 0.92f)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CocoaEspresso)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = "Add record",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Doodle category avatars — hand-drawn style: cream fills, espresso outline,
// round caps, tiny kawaii faces with blush cheeks.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CategoryDoodleAvatar(category: DoodleCategory, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(category.avatarColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)) {
            when (category) {
                DoodleCategory.FOOD -> drawBowlDoodle()
                DoodleCategory.GREENS -> drawBroccoliDoodle()
                DoodleCategory.DESSERT -> drawCupcakeDoodle()
                DoodleCategory.RIDE -> drawCarDoodle()
                DoodleCategory.GAME -> drawGamepadDoodle()
                DoodleCategory.BEAUTY -> drawLipstickDoodle()
                DoodleCategory.SPORT -> drawBallDoodle()
            }
        }
    }
}

private val DoodleCream = Color(0xFFFFFDF9)
private val DoodleBlush = Color(0xFFFFB3C1)

private fun DrawScope.inkStroke() =
    Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.fillAndOutline(path: Path, fill: Color) {
    drawPath(path = path, color = fill)
    drawPath(path = path, color = CocoaEspresso, style = inkStroke())
}

private fun DrawScope.fillAndOutlineRound(
    topLeft: Offset,
    size: Size,
    radius: Float,
    fill: Color
) {
    val corner = CornerRadius(radius, radius)
    drawRoundRect(color = fill, topLeft = topLeft, size = size, cornerRadius = corner)
    drawRoundRect(
        color = CocoaEspresso,
        topLeft = topLeft,
        size = size,
        cornerRadius = corner,
        style = inkStroke()
    )
}

private fun DrawScope.fillAndOutlineCircle(center: Offset, radius: Float, fill: Color) {
    drawCircle(color = fill, radius = radius, center = center)
    drawCircle(color = CocoaEspresso, radius = radius, center = center, style = inkStroke())
}

/** Two dot eyes, blush cheeks and a little smile. Everything scales with [spread]. */
private fun DrawScope.kawaiiFace(cx: Float, cy: Float, spread: Float) {
    val eye = spread * 0.42f
    val cheekY = cy + eye * 1.7f
    drawCircle(DoodleBlush, eye * 1.5f, Offset(cx - spread * 1.9f, cheekY))
    drawCircle(DoodleBlush, eye * 1.5f, Offset(cx + spread * 1.9f, cheekY))
    drawCircle(CocoaEspresso, eye, Offset(cx - spread, cy))
    drawCircle(CocoaEspresso, eye, Offset(cx + spread, cy))
    val smile = Path().apply {
        moveTo(cx - spread * 0.5f, cy + eye * 1.7f)
        quadraticTo(cx, cy + eye * 3.5f, cx + spread * 0.5f, cy + eye * 1.7f)
    }
    drawPath(
        path = smile,
        color = CocoaEspresso,
        style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.sparkle(cx: Float, cy: Float, r: Float) {
    val sw = 1.6.dp.toPx()
    drawLine(CocoaEspresso, Offset(cx - r, cy), Offset(cx + r, cy), sw, StrokeCap.Round)
    drawLine(CocoaEspresso, Offset(cx, cy - r), Offset(cx, cy + r), sw, StrokeCap.Round)
}

// FOOD — smiling bowl with a mound of rice / noodles
private fun DrawScope.drawBowlDoodle() {
    val w = size.width
    val h = size.height
    val mound = Path().apply {
        moveTo(w * 0.22f, h * 0.50f)
        quadraticTo(w * 0.50f, h * 0.10f, w * 0.78f, h * 0.50f)
        close()
    }
    fillAndOutline(mound, Color(0xFFFFF1B8))
    val bowl = Path().apply {
        moveTo(w * 0.12f, h * 0.48f)
        lineTo(w * 0.88f, h * 0.48f)
        cubicTo(w * 0.86f, h * 0.95f, w * 0.14f, h * 0.95f, w * 0.12f, h * 0.48f)
        close()
    }
    fillAndOutline(bowl, DoodleCream)
    kawaiiFace(w * 0.50f, h * 0.65f, w * 0.10f)
}

// GREENS — broccoli with a face on its stem
private fun DrawScope.drawBroccoliDoodle() {
    val w = size.width
    val h = size.height
    val stem = Path().apply {
        moveTo(w * 0.38f, h * 0.50f)
        lineTo(w * 0.62f, h * 0.50f)
        lineTo(w * 0.67f, h * 0.90f)
        lineTo(w * 0.33f, h * 0.90f)
        close()
    }
    fillAndOutline(stem, Color(0xFFEAF7C9))
    val green = Color(0xFF6FC77E)
    fillAndOutlineCircle(Offset(w * 0.28f, h * 0.42f), w * 0.19f, green)
    fillAndOutlineCircle(Offset(w * 0.72f, h * 0.42f), w * 0.19f, green)
    fillAndOutlineCircle(Offset(w * 0.50f, h * 0.28f), w * 0.22f, green)
    kawaiiFace(w * 0.50f, h * 0.68f, w * 0.065f)
}

// DESSERT — cupcake with cherry
private fun DrawScope.drawCupcakeDoodle() {
    val w = size.width
    val h = size.height
    val wrapper = Path().apply {
        moveTo(w * 0.24f, h * 0.56f)
        lineTo(w * 0.76f, h * 0.56f)
        lineTo(w * 0.68f, h * 0.92f)
        lineTo(w * 0.32f, h * 0.92f)
        close()
    }
    fillAndOutline(wrapper, Color(0xFFFFE0B5))
    val fold = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
    drawLine(CocoaEspresso, Offset(w * 0.41f, h * 0.64f), Offset(w * 0.43f, h * 0.84f), fold.width, StrokeCap.Round)
    drawLine(CocoaEspresso, Offset(w * 0.59f, h * 0.64f), Offset(w * 0.57f, h * 0.84f), fold.width, StrokeCap.Round)

    val pink = Color(0xFFFFC2D6)
    fillAndOutlineRound(Offset(w * 0.14f, h * 0.32f), Size(w * 0.72f, h * 0.32f), h * 0.16f, pink)
    fillAndOutlineRound(Offset(w * 0.30f, h * 0.14f), Size(w * 0.40f, h * 0.24f), h * 0.12f, pink)
    fillAndOutlineCircle(Offset(w * 0.50f, h * 0.10f), w * 0.06f, Color(0xFFFF6B6B))
    kawaiiFace(w * 0.50f, h * 0.45f, w * 0.085f)
}

// RIDE — little car with wheels
private fun DrawScope.drawCarDoodle() {
    val w = size.width
    val h = size.height
    val cabin = Path().apply {
        moveTo(w * 0.28f, h * 0.52f)
        lineTo(w * 0.36f, h * 0.26f)
        lineTo(w * 0.64f, h * 0.26f)
        lineTo(w * 0.72f, h * 0.52f)
        close()
    }
    fillAndOutline(cabin, Color(0xFFEAF6FF))
    fillAndOutlineRound(Offset(w * 0.08f, h * 0.46f), Size(w * 0.84f, h * 0.30f), h * 0.10f, DoodleCream)
    fillAndOutlineCircle(Offset(w * 0.30f, h * 0.78f), w * 0.11f, CocoaEspresso)
    fillAndOutlineCircle(Offset(w * 0.70f, h * 0.78f), w * 0.11f, CocoaEspresso)
    drawCircle(DoodleCream, w * 0.04f, Offset(w * 0.30f, h * 0.78f))
    drawCircle(DoodleCream, w * 0.04f, Offset(w * 0.70f, h * 0.78f))
    kawaiiFace(w * 0.50f, h * 0.58f, w * 0.075f)
}

// GAME — controller with d-pad and two buttons
private fun DrawScope.drawGamepadDoodle() {
    val w = size.width
    val h = size.height
    fillAndOutlineRound(Offset(w * 0.06f, h * 0.30f), Size(w * 0.88f, h * 0.46f), h * 0.23f, DoodleCream)
    val pad = 2.dp.toPx()
    drawLine(CocoaEspresso, Offset(w * 0.22f, h * 0.53f), Offset(w * 0.38f, h * 0.53f), pad, StrokeCap.Round)
    drawLine(CocoaEspresso, Offset(w * 0.30f, h * 0.44f), Offset(w * 0.30f, h * 0.62f), pad, StrokeCap.Round)
    fillAndOutlineCircle(Offset(w * 0.64f, h * 0.47f), w * 0.055f, Color(0xFFFF8FB1))
    fillAndOutlineCircle(Offset(w * 0.77f, h * 0.58f), w * 0.055f, Color(0xFF8EC9F0))
    sparkle(w * 0.84f, h * 0.18f, w * 0.05f)
}

// BEAUTY — smiling lipstick with a sparkle
private fun DrawScope.drawLipstickDoodle() {
    val w = size.width
    val h = size.height
    fillAndOutlineRound(Offset(w * 0.42f, h * 0.12f), Size(w * 0.16f, h * 0.34f), w * 0.06f, Color(0xFFFF6F91))
    fillAndOutlineRound(Offset(w * 0.36f, h * 0.44f), Size(w * 0.28f, h * 0.11f), w * 0.03f, Color(0xFFFFD66E))
    fillAndOutlineRound(Offset(w * 0.32f, h * 0.53f), Size(w * 0.36f, h * 0.38f), w * 0.05f, Color(0xFFFFC2D6))
    kawaiiFace(w * 0.50f, h * 0.70f, w * 0.07f)
    sparkle(w * 0.80f, h * 0.24f, w * 0.06f)
    sparkle(w * 0.20f, h * 0.44f, w * 0.04f)
}

// SPORT — ball with seams
private fun DrawScope.drawBallDoodle() {
    val w = size.width
    val h = size.height
    val center = Offset(w * 0.5f, h * 0.5f)
    val radius = w * 0.40f
    fillAndOutlineCircle(center, radius, Color(0xFFFF9F5A))
    val seam = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    drawLine(CocoaEspresso, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), seam.width, StrokeCap.Round)
    drawLine(CocoaEspresso, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), seam.width, StrokeCap.Round)
    val left = Path().apply {
        moveTo(w * 0.20f, h * 0.30f)
        quadraticTo(w * 0.38f, h * 0.50f, w * 0.20f, h * 0.70f)
    }
    val right = Path().apply {
        moveTo(w * 0.80f, h * 0.30f)
        quadraticTo(w * 0.62f, h * 0.50f, w * 0.80f, h * 0.70f)
    }
    drawPath(left, CocoaEspresso, style = seam)
    drawPath(right, CocoaEspresso, style = seam)
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun groupedInt(value: Double): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN")).format(abs(value).toLong())

private fun signedInt(value: Double): String =
    (if (value < 0) "−" else "+") + groupedInt(value)

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun RecordDoodleUiPreview() {
    val day = RecordDayUi(
        dateLabel = "12 29",
        weekday = "Thu",
        income = 0.0,
        expense = 261.0,
        hours = listOf(
            RecordHourUi(
                label = "14:00",
                entries = listOf(
                    RecordEntryUi("1", DoodleCategory.FOOD, "Food", "14:59", -50.0),
                    RecordEntryUi("2", DoodleCategory.GREENS, "Greens", "14:41", -58.0)
                )
            ),
            RecordHourUi(
                label = "13:00",
                entries = listOf(
                    RecordEntryUi("3", DoodleCategory.DESSERT, "Dessert", "13:20", -65.0),
                    RecordEntryUi("4", DoodleCategory.RIDE, "Car", "13:57", -68.0),
                    RecordEntryUi("5", DoodleCategory.GAME, "Game", "13:05", -41.0),
                    RecordEntryUi("6", DoodleCategory.BEAUTY, "Beauty", "13:02", -33.0),
                    RecordEntryUi("7", DoodleCategory.SPORT, "Sport", "13:01", 90.0)
                )
            )
        )
    )
    RecordDoodleBackdrop(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            RecordHeaderSection(totalExpenses = 572.0, totalIncome = 5194.0, balance = 4622.0)
            Spacer(modifier = Modifier.height(12.dp))
            RecordDayGroup(day = day)
        }
        RecordPencilFab(
            onClick = {},
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        )
    }
}
