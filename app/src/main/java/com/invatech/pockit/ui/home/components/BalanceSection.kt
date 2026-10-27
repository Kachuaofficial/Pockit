package com.invatech.pockit.ui.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.Lavender
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun BalanceSection(
    balance: Double,
    changePercent: Double,
    modifier: Modifier = Modifier
) {
    // Smooth fluid rolling number counter
    val animatedBalance by animateFloatAsState(
        targetValue = balance.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "animated_home_balance"
    )

    val whole = animatedBalance.toLong()
    val cents = ((animatedBalance - whole) * 100).roundToInt().coerceIn(0, 99)
    val wholeFormatted = NumberFormat
        .getIntegerInstance(Locale.forLanguageTag("en-IN"))
        .format(whole)
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        Text(
            text = "Current balance",
            style = MaterialTheme.typography.bodyMedium,
            color = mutedColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = buildAnnotatedString {
                append("₹$wholeFormatted")
                withStyle(SpanStyle(color = mutedColor)) {
                    append(".${cents.toString().padStart(2, '0')}")
                }
            },
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(color = Lavender, shape = CircleShape) {
                Text(
                    text = "${if (changePercent >= 0) "+" else ""}$changePercent%",
                    color = Ink,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Text(
                text = "vs last month",
                style = MaterialTheme.typography.labelMedium,
                color = mutedColor
            )
        }
    }
}
