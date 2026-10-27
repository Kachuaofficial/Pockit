package com.invatech.pockit.ui.space.components

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.Mint

@Composable
fun AmountCard(
    amountText: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Mint,
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
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    ),
                    cursorBrush = SolidColor(Ink),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (amountText.isEmpty()) {
                                Text(
                                    text = "0",
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.ExtraBold,
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
