package com.invatech.pockit.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.R
import com.invatech.pockit.ui.common.motion.MotionSettings
import java.text.SimpleDateFormat
import java.util.Date

@Suppress("UNUSED_PARAMETER")
@Composable
fun MonthlyPlanCard(
    color: Color,
    hasBudget: Boolean,
    spentText: String,
    limitText: String,
    usedFraction: Float,
    percentUsed: Int,
    amountLeftText: String,
    isOver: Boolean,
    onNavigateToBudgets: () -> Unit,
    onPlanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val month = SimpleDateFormat("MMMM yyyy", LocalConfiguration.current.locales[0]).format(Date())
    val motion by MotionSettings.enabled.collectAsState()
    val progress by animateFloatAsState(
        usedFraction.coerceIn(0f, 1f),
        animationSpec = if (motion) tween(450) else snap(),
        label = "monthly_budget_progress"
    )
    val coral = Color(0xFFFF3F78)
    val ink = Color(0xFF2A1C31)
    val muted = Color(0xFF716674)
    val stateColor = when {
        isOver -> Color(0xFFD93661)
        usedFraction >= .8f -> Color(0xFFD98217)
        else -> Color(0xFF078C62)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFFAFC), Color(0xFFFFF0F6))))
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFFE3EC), modifier = Modifier.size(43.dp)) {
                Icon(Icons.Rounded.CalendarMonth, null, tint = coral, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Monthly budget", fontSize = 19.sp, fontWeight = FontWeight.Black, color = ink)
                Text(month, fontSize = 12.sp, color = muted)
            }
            if (hasBudget) {
                Text(
                    when {
                        isOver -> "Over budget"
                        usedFraction >= .8f -> "Close to limit"
                        else -> "On track"
                    },
                    color = stateColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (hasBudget) {
            Box(Modifier.fillMaxWidth().height(102.dp)) {
                Row(Modifier.fillMaxWidth(.70f).align(Alignment.CenterStart)) {
                    BudgetValue("Spent", spentText, coral, Modifier.weight(1f))
                    BudgetValue("Budget", limitText, ink, Modifier.weight(1f))
                }
                BudgetPanda(Modifier.align(Alignment.BottomEnd).size(width = 118.dp, height = 106.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(10.dp).clip(CircleShape),
                    color = if (isOver) stateColor else coral,
                    trackColor = Color(0xFFE9DEE7),
                    strokeCap = StrokeCap.Round
                )
                Spacer(Modifier.width(9.dp))
                Text("${percentUsed.coerceAtLeast(0)}%", fontSize = 12.sp, fontWeight = FontWeight.Black, color = ink)
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(stateColor.copy(alpha = .09f)).padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Info, null, tint = stateColor, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    if (isOver) "$amountLeftText over budget" else "$amountLeftText left this month",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Box(Modifier.fillMaxWidth().height(112.dp)) {
                Column(Modifier.fillMaxWidth(.64f).align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Set a monthly spending limit", fontSize = 18.sp, lineHeight = 21.sp, fontWeight = FontWeight.Black, color = ink)
                    Text("See what is safe to spend throughout the month.", fontSize = 12.sp, lineHeight = 16.sp, color = muted)
                }
                BudgetPanda(Modifier.align(Alignment.BottomEnd).size(width = 118.dp, height = 106.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onNavigateToBudgets,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = coral, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 15.dp)
            ) {
                Text(if (hasBudget) "View budget" else "Set budget", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(19.dp))
            }
            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFFE6EF), shadowElevation = 0.dp) {
                IconButton(onClick = onPlanner, modifier = Modifier.size(50.dp)) {
                    Icon(Icons.Rounded.EditCalendar, "Open pocket planner", tint = Color(0xFF76509A))
                }
            }
        }
    }
}

@Composable
private fun BudgetPanda(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.matchParentSize()) {
            val back = Path().apply {
                moveTo(size.width * .08f, size.height * .88f)
                cubicTo(size.width * .03f, size.height * .48f, size.width * .28f, size.height * .15f, size.width * .67f, size.height * .20f)
                cubicTo(size.width * .98f, size.height * .24f, size.width * 1.03f, size.height * .60f, size.width * .90f, size.height * .88f)
                close()
            }
            val middle = Path().apply {
                moveTo(size.width * .20f, size.height * .96f)
                cubicTo(size.width * .05f, size.height * .67f, size.width * .30f, size.height * .37f, size.width * .60f, size.height * .40f)
                cubicTo(size.width * .91f, size.height * .43f, size.width * .97f, size.height * .72f, size.width * .82f, size.height * .96f)
                close()
            }
            drawPath(back, Color(0xFFFFD8E7).copy(alpha = .72f))
            drawPath(middle, Color(0xFFEBD9FF).copy(alpha = .62f))
            drawCircle(Color(0xFFFFBCD5).copy(alpha = .34f), size.minDimension * .22f, Offset(size.width * .80f, size.height * .28f))
        }
        Image(
            painter = painterResource(R.drawable.pockit_budget_panda),
            contentDescription = "Pockit panda with budget checklist",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(.88f).height(102.dp)
        )
    }
}

@Composable
private fun BudgetValue(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 11.sp, color = Color(0xFF756A78))
        Text(value, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Black, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
