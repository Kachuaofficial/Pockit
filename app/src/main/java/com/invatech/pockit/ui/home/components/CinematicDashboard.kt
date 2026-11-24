package com.invatech.pockit.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.South
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.domain.Money
import com.invatech.pockit.ui.home.HomeTransactionUi
import com.invatech.pockit.ui.home.model.WalletTransactionType
import com.invatech.pockit.ui.theme.ExpenseRed
import com.invatech.pockit.ui.theme.PastelLilac
import com.invatech.pockit.ui.theme.PastelPeach
import com.invatech.pockit.ui.theme.PastelPink
import com.invatech.pockit.ui.theme.PastelSky
import com.invatech.pockit.ui.theme.PockitBackground
import com.invatech.pockit.ui.theme.PockitBlue
import com.invatech.pockit.ui.theme.PockitBorder
import com.invatech.pockit.ui.theme.PockitElevated
import com.invatech.pockit.ui.theme.PockitGreen
import com.invatech.pockit.ui.theme.PockitMutedText
import com.invatech.pockit.ui.theme.PockitOrange
import com.invatech.pockit.ui.theme.PockitOrangeDeep
import com.invatech.pockit.ui.theme.PockitOrangeLight
import com.invatech.pockit.ui.theme.PockitPurple
import com.invatech.pockit.ui.theme.PockitSecondaryText
import com.invatech.pockit.ui.theme.PockitSurface
import com.invatech.pockit.ui.theme.PockitText
import java.util.Calendar

private fun dashboardMoney(paise: Long): String = Money.format(paise).let {
    if (paise % 100L == 0L) it.removeSuffix(".00") else it
}

/** Official supplied Pockit artwork, shared across app surfaces. */
@Composable
fun PockitMark(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(com.invatech.pockit.R.drawable.pockit_brand),
        contentDescription = "Pockit official logo",
        modifier = modifier.clip(RoundedCornerShape(20)),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}

@Composable
fun CinematicCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = PockitSurface, contentColor = PockitText,
        border = BorderStroke(1.dp, PockitBorder)
    ) {
        Column(
            Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            PastelPink.copy(alpha = .48f),
                            PockitSurface,
                            PastelLilac.copy(alpha = .38f)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

/** Procedural sunset artwork avoids network/image loading on the dashboard. */
@Composable
fun SunsetMountains(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sun = Offset(w * .70f, h * .46f)
        drawCircle(
            Brush.radialGradient(
                listOf(PockitOrange.copy(alpha = .30f), Color.Transparent),
                sun,
                w * .55f
            ), w * .55f, sun
        )
        drawCircle(
            Brush.verticalGradient(listOf(PockitOrangeLight, PockitOrangeDeep)),
            h * .28f,
            sun
        )
        fun mountain(points: List<Pair<Float, Float>>, color: Color) {
            val p = Path().apply {
                moveTo(0f, h); points.forEach { (x, y) ->
                lineTo(
                    w * x,
                    h * y
                )
            }; lineTo(w, h); close()
            }
            drawPath(p, color)
        }
        mountain(
            listOf(
                0f to .85f,
                .16f to .63f,
                .28f to .73f,
                .43f to .48f,
                .51f to .58f,
                .68f to .39f,
                .77f to .59f,
                .91f to .48f,
                1f to .63f
            ), PastelLilac
        )
        mountain(
            listOf(
                0f to .95f,
                .20f to .76f,
                .35f to .85f,
                .50f to .67f,
                .68f to .41f,
                .61f to .72f,
                .76f to .65f,
                .90f to .84f,
                1f to .69f
            ), PastelPeach
        )
        val ridge = Path().apply {
            moveTo(w * .48f, h * .72f); lineTo(w * .68f, h * .41f); lineTo(
            w * .60f,
            h * .64f
        ); lineTo(w * .63f, h * .66f); lineTo(w * .49f, h * .80f)
        }
        drawPath(ridge, PockitOrange.copy(alpha = .38f), style = Stroke(1.1.dp.toPx()))
        mountain(
            listOf(
                0f to 1f,
                .22f to .88f,
                .39f to .96f,
                .59f to .81f,
                .79f to .95f,
                1f to .88f
            ), PockitBackground
        )
        drawRect(
            Brush.horizontalGradient(
                listOf(
                    PockitBackground,
                    Color.Transparent,
                    Color.Transparent
                )
            )
        )
    }
}

@Composable
fun DashboardGreeting(
    userName: String, unread: Int, onNotifications: () -> Unit, onSettings: () -> Unit,
    onRewards: () -> Unit = {}, userId: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PockitMark(Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Pockit",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-.8).sp,
                modifier = Modifier.weight(1f)
            )
            Surface(
                onClick = onRewards,
                shape = CircleShape,
                color = PastelLilac,
                border = BorderStroke(1.dp, PockitPurple.copy(alpha = .22f))
            ) {
                Icon(
                    Icons.Rounded.ShoppingCart,
                    "Open feature store",
                    Modifier.padding(10.dp).size(20.dp),
                    tint = PockitPurple
                )
            }
            IconButton(onClick = onNotifications) {
                BadgedBox(badge = {
                    if (unread > 0) Badge(
                        containerColor = PockitOrange,
                        contentColor = PockitBackground
                    ) { Text(if (unread > 99) "99+" else "$unread") }
                }) {
                    Icon(Icons.Rounded.NotificationsNone, "Notifications", tint = PockitText)
                }
            }
            Surface(
                onClick = onSettings,
                shape = CircleShape,
                border = BorderStroke(1.dp, PockitOrange.copy(alpha = .7f)),
                color = PockitElevated
            ) {
                com.invatech.pockit.ui.common.UserAvatar(
                    userId = userId,
                    name = userName,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 105.dp)
        ) {
            SunsetMountains(
                Modifier
                    .align(Alignment.BottomEnd)
                    .width(205.dp)
                    .height(112.dp)
            )
            Column(
                Modifier.padding(top = 10.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                Text(
                    when (hour) {
                        in 5..11 -> "Good Morning,"; in 12..16 -> "Good Afternoon,"; else -> "Good Evening,"
                    },
                    fontSize = 22.sp, fontWeight = FontWeight.Medium
                )
                Text(
                    "${userName.substringBefore(' ').ifBlank { "there" }} 👋",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-.7).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(.70f)
                )
                Text(
                    "Track · Spend · Save",
                    color = PockitSecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SpendingSparkline(values: List<Long>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2 || values.maxOrNull() == 0L) {
            drawLine(
                PockitBorder,
                Offset(0f, size.height * .85f),
                Offset(size.width, size.height * .85f),
                2.dp.toPx()
            )
            return@Canvas
        }
        val max = (values.maxOrNull() ?: 1L).coerceAtLeast(1)
        val pts = values.mapIndexed { i, v ->
            Offset(
                i * size.width / (values.size - 1),
                size.height * (.9f - .78f * (v.toDouble() / max).toFloat())
            )
        }
        val line = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val a = pts[i - 1]
                val b = pts[i]
                val mid = (a.x + b.x) / 2
                cubicTo(mid, a.y, mid, b.y, b.x, b.y)
            }
        }
        val fill = Path().apply {
            addPath(line); lineTo(size.width, size.height); lineTo(
            0f,
            size.height
        ); close()
        }
        drawPath(
            fill,
            Brush.verticalGradient(listOf(PockitOrange.copy(alpha = .50f), Color.Transparent))
        )
        drawPath(line, PockitOrange, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun SpendingSummary(
    spent: Long, budget: Long?, trend: List<Long>, period: String,
    previousMonth: Boolean, onPeriodChange: (Boolean) -> Unit, onBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menu by remember { mutableStateOf(false) }
    CinematicCard(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box {
                Surface(
                    onClick = { menu = true },
                    shape = RoundedCornerShape(11.dp),
                    color = PockitElevated
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (previousMonth) "Last Month" else "This Month",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            null,
                            Modifier
                                .size(20.dp)
                                .padding(start = 4.dp),
                            tint = PockitSecondaryText
                        )
                    }
                }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("This Month") },
                        onClick = { onPeriodChange(false); menu = false })
                    DropdownMenuItem(
                        text = { Text("Last Month") },
                        onClick = { onPeriodChange(true); menu = false })
                }
            }
            Text(period, style = MaterialTheme.typography.bodySmall, color = PockitSecondaryText)
        }
        if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f || dashboardMoney(
                spent
            ).length > 11
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    dashboardMoney(spent),
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )
                Text(
                    "Total Spent",
                    color = PockitSecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
                SpendingSparkline(
                    trend, Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
            }
        } else Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 66.dp)
        ) {
            SpendingSparkline(
                trend,
                Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(.47f)
                    .height(62.dp)
            )
            Column(Modifier.fillMaxWidth(.70f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    dashboardMoney(spent),
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )
                Text(
                    "Total Spent",
                    color = PockitSecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        val fraction = if (budget != null && budget > 0) (spent.toDouble() / budget).toFloat()
            .coerceIn(0f, 1f) else 0f
        Box(
            Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(CircleShape)
                .background(PockitElevated)
        ) {
            if (fraction > 0) Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                PockitOrangeLight,
                                PockitOrangeDeep
                            )
                        )
                    )
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBudget)
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.TrackChanges,
                    null,
                    Modifier.size(24.dp),
                    tint = PockitSecondaryText
                )
                Column {
                    Text(budget?.let { dashboardMoney(it) } ?: "Set budget",
                        style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = PockitSecondaryText
                    )
                }
            }
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val over = budget != null && spent > budget
                Canvas(Modifier.size(23.dp)) {
                    drawArc(PockitBorder, -90f, 360f, false, style = Stroke(3.dp.toPx()))
                    if (budget != null) drawArc(
                        if (over) ExpenseRed else PockitGreen,
                        -90f,
                        if (over) 360f else 360f * (1f - fraction),
                        false,
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column {
                    Text(budget?.let { dashboardMoney(kotlin.math.abs(it - spent)) } ?: "—",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (over) ExpenseRed else PockitText)
                    Text(
                        if (over) "Over budget" else "Remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = PockitSecondaryText
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardActions(
    onExpense: () -> Unit,
    onIncome: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple("Add\nExpense", Icons.Rounded.Add, onExpense),
            Triple("Add\nIncome", Icons.Rounded.South, onIncome),
            Triple("Reports", Icons.Rounded.BarChart, onReports),
            Triple("Settings", Icons.Rounded.Settings, onSettings)
        ).forEachIndexed { i, (label, icon, click) ->
            Surface(
                onClick = click, modifier = Modifier.weight(1f), shape = RoundedCornerShape(23.dp),
                color = if (i == 0) Color(0xFF24180F) else PockitSurface,
                border = BorderStroke(
                    1.dp,
                    if (i == 0) PockitOrange.copy(alpha = .24f) else Color(0xFF202125)
                )
            ) {
                Column(
                    Modifier.padding(horizontal = 3.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier
                            .size(39.dp)
                            .then(
                                if (i == 0) Modifier.background(
                                    Brush.linearGradient(
                                        listOf(
                                            PockitOrangeLight,
                                            PockitOrangeDeep
                                        )
                                    ), RoundedCornerShape(12.dp)
                                ) else Modifier
                            ), contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            null,
                            Modifier.size(26.dp),
                            tint = if (i == 0) PockitBackground else PockitSecondaryText
                        )
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (i == 0) Color.White else PockitText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        minLines = 2
                    )
                }
            }
        }
    }
}

private data class Slice(
    val label: String,
    val amount: Long,
    val color: Color,
    val icon: ImageVector
)

private fun categoryStyle(name: String): Pair<Color, ImageVector> = when {
    name.contains("food", true) || name.contains(
        "dessert",
        true
    ) -> PockitOrange to Icons.Rounded.Restaurant

    name.contains("car", true) || name.contains("travel", true) || name.contains(
        "transport",
        true
    ) -> PockitPurple to Icons.Rounded.DirectionsCar

    name.contains("shop", true) || name.contains(
        "cosmetic",
        true
    ) -> PockitBlue to Icons.Rounded.ShoppingBag

    name.contains("bill", true) -> PockitGreen to Icons.Rounded.Home
    name.contains("health", true) || name.contains(
        "grocer",
        true
    ) -> PockitGreen to Icons.Rounded.Spa

    else -> PockitOrangeLight to Icons.Rounded.Category
}

@Composable
private fun LegacyCategorySpending(rows: List<HomeTransactionUi>, onSeeAll: () -> Unit) {
    val slices = remember(rows) {
        rows.filter { it.type == WalletTransactionType.EXPENSE }.groupBy { it.category }
            .map { (name, tx) ->
                val (color, icon) = categoryStyle(name); Slice(
                name,
                Money.sum(tx.map { it.amountPaise }),
                color,
                icon
            )
            }.filter { it.amount > 0 }.sortedByDescending { it.amount }
    }
    val total = Money.sum(slices.map { it.amount })
    CinematicCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Spending by Category",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onSeeAll,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(start = 8.dp)
            ) { Text("See All →", style = MaterialTheme.typography.labelMedium) }
        }
        @Composable
        fun chart(modifier: Modifier) {
            Box(
                modifier
                    .aspectRatio(1f)
                    .semantics { contentDescription = "Category spending: ${Money.format(total)}" },
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .padding(9.dp)
                ) {
                    val stroke = size.width * .17f
                    val inset = stroke / 2
                    val diameter = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        PockitElevated,
                        -90f,
                        360f,
                        false,
                        Offset(inset, inset),
                        diameter,
                        style = Stroke(stroke)
                    )
                    var start = -90f
                    slices.forEach { slice ->
                        val sweep =
                            360f * (slice.amount.toDouble() / total.coerceAtLeast(1)).toFloat()
                        drawArc(
                            slice.color,
                            start,
                            sweep,
                            false,
                            Offset(inset, inset),
                            diameter,
                            style = Stroke(stroke)
                        ); start += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        dashboardMoney(total),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Spent",
                        style = MaterialTheme.typography.bodySmall,
                        color = PockitSecondaryText
                    )
                }
            }
        }

        @Composable
        fun legend(modifier: Modifier) {
            Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (slices.isEmpty()) {
                    Text("Your picture starts here", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Add an expense to see where your money goes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PockitSecondaryText
                    )
                }
                slices.take(4).forEach { slice ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PockitElevated)
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(slice.icon, null, Modifier.size(18.dp), tint = slice.color)
                        Text(
                            slice.label,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${
                                kotlin.math.round(slice.amount * 100.0 / total.coerceAtLeast(1))
                                    .toInt()
                            }%", style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 300.dp || scale > 1.3f) Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                chart(Modifier.width(170.dp)); legend(Modifier.fillMaxWidth())
            } else Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                chart(Modifier.weight(.43f)); legend(Modifier.weight(.57f))
            }
        }
    }
}

@Composable
fun CategorySpending(rows: List<HomeTransactionUi>, onSeeAll: () -> Unit) {
    val slices = remember(rows) {
        rows.filter { it.type == WalletTransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (name, transactions) ->
                val (_, icon) = categoryStyle(name)
                val color = when {
                    name.contains("bill", true) || name.contains("home", true) -> PockitGreen
                    name.contains("other", true) -> PockitOrange
                    name.contains("food", true) || name.contains("dining", true) -> Color(0xFFF3A000)
                    name.contains("travel", true) || name.contains("transport", true) -> PockitPurple
                    name.contains("shop", true) -> PockitBlue
                    else -> PockitOrangeLight
                }
                Slice(name, Money.sum(transactions.map { it.amountPaise }), color, icon)
            }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }
    }
    val total = Money.sum(slices.map { it.amount })

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PockitSurface,
        contentColor = PockitText,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, PockitOrange.copy(alpha = .18f)),
        shadowElevation = 4.dp
    ) {
        Box(
            Modifier.background(
                Brush.verticalGradient(
                    listOf(Color.White, PastelPink.copy(alpha = .28f), PastelPeach.copy(alpha = .22f))
                )
            )
        ) {
            Canvas(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(38.dp)) {
                val backWave = Path().apply {
                    moveTo(0f, size.height * .45f)
                    cubicTo(size.width * .16f, size.height * .1f, size.width * .27f, size.height * .95f, size.width * .44f, size.height * .52f)
                    cubicTo(size.width * .62f, size.height * .08f, size.width * .76f, size.height * .9f, size.width, size.height * .28f)
                    lineTo(size.width, size.height); lineTo(0f, size.height); close()
                }
                drawPath(backWave, PastelPink.copy(alpha = .62f))
                val frontWave = Path().apply {
                    moveTo(0f, size.height * .72f)
                    cubicTo(size.width * .18f, size.height * .34f, size.width * .31f, size.height * 1.05f, size.width * .52f, size.height * .67f)
                    cubicTo(size.width * .72f, size.height * .3f, size.width * .84f, size.height * .92f, size.width, size.height * .54f)
                    lineTo(size.width, size.height); lineTo(0f, size.height); close()
                }
                drawPath(frontWave, Color.White.copy(alpha = .72f))
            }

            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp).padding(bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = PastelPink,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 3.dp
                    ) {
                        Icon(
                            Icons.Rounded.PieChart,
                            contentDescription = null,
                            tint = PockitOrangeDeep,
                            modifier = Modifier.padding(9.dp).size(22.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Spending by Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Where your money went this month", style = MaterialTheme.typography.bodySmall, color = PockitSecondaryText)
                    }
                    Surface(
                        onClick = onSeeAll,
                        color = PastelPink.copy(alpha = .52f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "See All →",
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = PockitOrangeDeep,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (slices.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(color = PastelPink, shape = CircleShape) {
                            Icon(Icons.Rounded.DonutLarge, null, Modifier.padding(12.dp).size(28.dp), tint = PockitOrangeDeep)
                        }
                        Text("Your picture starts here", style = MaterialTheme.typography.titleSmall)
                        Text("Add an expense to see where your money goes.", style = MaterialTheme.typography.bodySmall, color = PockitSecondaryText)
                    }
                } else {
                    @Composable
                    fun CategoryBarChart(modifier: Modifier) {
                        Surface(
                            modifier = modifier.height(190.dp)
                                .semantics { contentDescription = "Category spending graph: ${Money.format(total)}" },
                            shape = RoundedCornerShape(22.dp),
                            color = PastelLilac.copy(alpha = .38f),
                            border = BorderStroke(1.dp, PockitPurple.copy(alpha = .10f))
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                Column(Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 10.dp)) {
                                    Text("TOTAL SPENT", style = MaterialTheme.typography.labelSmall, color = PockitPurple, letterSpacing = .7.sp)
                                    Text(dashboardMoney(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                }
                                Canvas(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 46.dp, bottom = 14.dp)) {
                                    val values = slices.take(5)
                                    val maxValue = values.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L
                                    val gridColor = Color.White.copy(alpha = .72f)
                                    repeat(3) { index ->
                                        val y = size.height * index / 2f
                                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                                    }
                                    if (values.isNotEmpty()) {
                                        val sidePadding = 8.dp.toPx()
                                        val gap = 14.dp.toPx()
                                        val available = size.width - sidePadding * 2 - gap * (values.size - 1)
                                        val barWidth = (available / values.size).coerceAtLeast(14.dp.toPx())
                                        values.forEachIndexed { index, slice ->
                                            val fraction = (slice.amount.toFloat() / maxValue).coerceIn(.08f, 1f)
                                            val barHeight = size.height * .84f * fraction
                                            val x = sidePadding + index * (barWidth + gap)
                                            val y = size.height - barHeight
                                            drawRoundRect(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(slice.color.copy(alpha = .68f), slice.color),
                                                    startY = y,
                                                    endY = size.height
                                                ),
                                                topLeft = Offset(x, y),
                                                size = Size(barWidth, barHeight),
                                                cornerRadius = CornerRadius(barWidth * .28f, barWidth * .28f)
                                            )
                                            drawCircle(Color.White.copy(alpha = .92f), 3.dp.toPx(), Offset(x + barWidth / 2f, y + 8.dp.toPx()))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    @Composable
                    fun CategoryRows(modifier: Modifier) {
                        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            slices.take(4).forEach { slice ->
                                val percentage = kotlin.math.round(slice.amount * 100.0 / total.coerceAtLeast(1)).toInt()
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.White.copy(alpha = .94f),
                                    shape = RoundedCornerShape(18.dp),
                                    shadowElevation = 2.dp
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                                    ) {
                                        Surface(color = slice.color, shape = RoundedCornerShape(9.dp)) {
                                            Icon(slice.icon, null, Modifier.padding(6.dp).size(16.dp), tint = Color.White)
                                        }
                                        Text(
                                            slice.label,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text("$percentage%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryBarChart(Modifier.fillMaxWidth())
                        CategoryRows(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedCategorySpending(rows: List<HomeTransactionUi>, onSeeAll: () -> Unit) {
    val slices = remember(rows) {
        rows.filter { it.type == WalletTransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (name, transactions) ->
                val (color, icon) = categoryStyle(name)
                Slice(name, Money.sum(transactions.map { it.amountPaise }), color, icon)
            }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }
    }
    val total = Money.sum(slices.map { it.amount })
    val leading = slices.firstOrNull()

    CinematicCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(color = PastelPink, shape = RoundedCornerShape(14.dp)) {
                Icon(
                    Icons.Rounded.DonutLarge,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp).size(21.dp),
                    tint = PockitOrangeDeep
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "SPENDING MIX",
                    style = MaterialTheme.typography.labelSmall,
                    color = PockitPurple,
                    letterSpacing = 1.2.sp
                )
                Text("Spending by Category", style = MaterialTheme.typography.titleLarge)
            }
            TextButton(
                onClick = onSeeAll,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = PockitOrangeDeep),
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)
            ) { Text("See All →", style = MaterialTheme.typography.labelMedium) }
        }

        if (leading != null) {
            Surface(
                color = PastelPeach.copy(alpha = .72f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, PockitOrange.copy(alpha = .16f))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(color = Color.White.copy(alpha = .82f), shape = CircleShape) {
                        Icon(
                            leading.icon,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp).size(18.dp),
                            tint = leading.color
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Largest category", style = MaterialTheme.typography.bodySmall, color = PockitSecondaryText)
                        Text(leading.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(dashboardMoney(leading.amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${kotlin.math.round(leading.amount * 100.0 / total.coerceAtLeast(1)).toInt()}% of spending",
                            style = MaterialTheme.typography.labelSmall,
                            color = PockitOrangeDeep
                        )
                    }
                }
            }
        }

        @Composable
        fun CategoryDonut(modifier: Modifier) {
            Surface(
                modifier = modifier,
                color = PastelLilac.copy(alpha = .52f),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, PockitPurple.copy(alpha = .12f))
            ) {
                Box(
                    Modifier.fillMaxSize().padding(13.dp).aspectRatio(1f)
                        .semantics { contentDescription = "Category spending: ${Money.format(total)}" },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val strokeWidth = size.width * .15f
                        val inset = strokeWidth / 2
                        val diameter = Size(size.width - strokeWidth, size.height - strokeWidth)
                        drawArc(
                            Color.White.copy(alpha = .82f), -90f, 360f, false,
                            Offset(inset, inset), diameter,
                            style = Stroke(strokeWidth, cap = StrokeCap.Round)
                        )
                        var start = -90f
                        slices.forEach { slice ->
                            val fullSweep = 360f * (slice.amount.toDouble() / total.coerceAtLeast(1)).toFloat()
                            val gap = minOf(3f, fullSweep * .18f)
                            drawArc(
                                slice.color,
                                start + gap / 2,
                                (fullSweep - gap).coerceAtLeast(.8f),
                                false,
                                Offset(inset, inset),
                                diameter,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                            start += fullSweep
                        }
                    }
                    Surface(color = Color.White.copy(alpha = .94f), shape = CircleShape, shadowElevation = 2.dp) {
                        Column(
                            Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TOTAL", style = MaterialTheme.typography.labelSmall, color = PockitMutedText, letterSpacing = .8.sp)
                            Text(dashboardMoney(total), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        @Composable
        fun CategoryLegend(modifier: Modifier) {
            Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (slices.isEmpty()) {
                    Surface(color = PastelSky.copy(alpha = .55f), shape = RoundedCornerShape(22.dp)) {
                        Column(
                            Modifier.fillMaxWidth().padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(color = Color.White.copy(alpha = .82f), shape = CircleShape) {
                                Icon(Icons.Rounded.PieChart, null, Modifier.padding(11.dp).size(25.dp), tint = PockitBlue)
                            }
                            Text("Your picture starts here", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Add an expense to see where your money goes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PockitSecondaryText,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                slices.take(4).forEach { slice ->
                    val fraction = (slice.amount.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = slice.color.copy(alpha = .08f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, slice.color.copy(alpha = .12f))
                    ) {
                        Column(
                            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(color = Color.White.copy(alpha = .86f), shape = RoundedCornerShape(10.dp)) {
                                    Icon(slice.icon, null, Modifier.padding(6.dp).size(16.dp), tint = slice.color)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(slice.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(dashboardMoney(slice.amount), style = MaterialTheme.typography.labelSmall, color = PockitSecondaryText)
                                }
                                Surface(color = Color.White.copy(alpha = .82f), shape = CircleShape) {
                                    Text(
                                        "${kotlin.math.round(slice.amount * 100.0 / total.coerceAtLeast(1)).toInt()}%",
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = slice.color,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = .82f))) {
                                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(slice.color, CircleShape))
                            }
                        }
                    }
                }
                if (slices.size > 4) {
                    Text(
                        "+${slices.size - 4} more categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = PockitMutedText,
                        modifier = Modifier.align(Alignment.End).padding(end = 4.dp)
                    )
                }
            }
        }

        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 300.dp || fontScale > 1.3f) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (slices.isNotEmpty()) CategoryDonut(Modifier.width(190.dp))
                    CategoryLegend(Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (slices.isNotEmpty()) CategoryDonut(Modifier.weight(.44f))
                    CategoryLegend(Modifier.weight(.56f))
                }
            }
        }
    }
}
