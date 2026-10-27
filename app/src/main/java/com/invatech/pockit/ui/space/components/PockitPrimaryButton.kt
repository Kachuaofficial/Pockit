package com.invatech.pockit.ui.space.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted

@Composable
fun PockitPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Ink,
            contentColor = Color.White,
            disabledContainerColor = InkMuted.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.85f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
