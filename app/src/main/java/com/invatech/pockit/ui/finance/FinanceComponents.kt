package com.invatech.pockit.ui.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun PageHeading(title: String, subtitle: String = "", onBack: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        if (onBack != null) IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                "Back"
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle.isNotBlank()) Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FinanceCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val motion by com.invatech.pockit.ui.common.motion.MotionSettings.enabled.collectAsState()
    val reveal = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(motion) {
        if (motion) reveal.animateTo(
            1f,
            spring(dampingRatio = .76f, stiffness = 240f)
        ) else reveal.snapTo(1f)
    }
    Card(
        modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = if (motion) spring(dampingRatio = 1f, stiffness = 500f) else snap())
            .graphicsLayer {
                alpha = reveal.value.coerceIn(0f, 1f)
                translationY = (1f - reveal.value) * 16.dp.toPx()
                scaleX = .98f + .02f * reveal.value
                scaleY = .98f + .02f * reveal.value
            },
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, if (color == MaterialTheme.colorScheme.surface) MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f) else Color.Transparent),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun AmountField(value: String, onChange: (String) -> Unit, label: String = "Amount (₹)") {
    OutlinedTextField(
        value,
        onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ErrorNotice(message: String?) {
    if (message != null) Text(
        message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}
