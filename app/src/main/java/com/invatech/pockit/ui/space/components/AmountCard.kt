package com.invatech.pockit.ui.space.components

/*
 * Shared-expense amount input. Keeps editable text in the caller and reports changes through a callback.
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.invatech.pockit.ui.theme.Ink

@Composable
fun AmountCard(
    amountText: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .30f)),
        shape = RoundedCornerShape(30.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelMedium,
                color = Ink.copy(alpha = 0.6f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.displayLarge.copy(color = Ink),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (amountText.isEmpty()) {
                                Text(
                                    text = "0",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = Ink.copy(alpha = 0.25f)
                                )
                            }
                            inner()
                        }
                    }
                )
            }
        }
    }
}
