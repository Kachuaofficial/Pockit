package com.invatech.pockit.ui.home.components

/*
 * Personal transaction row with category visuals and caller-provided edit/delete actions.
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.domain.Money
import com.invatech.pockit.ui.common.motion.rememberPressScale
import com.invatech.pockit.ui.home.HomeTransactionUi
import com.invatech.pockit.ui.home.model.WalletTransactionType
import com.invatech.pockit.ui.theme.CocoaEspresso
import com.invatech.pockit.ui.theme.CocoaMuted
import com.invatech.pockit.ui.theme.CocoaSoft
import com.invatech.pockit.ui.theme.ExpenseRed
import com.invatech.pockit.ui.theme.IncomeGreen
import com.invatech.pockit.ui.theme.PastelApricot
import com.invatech.pockit.ui.theme.PastelLilac
import com.invatech.pockit.ui.theme.PastelMint
import com.invatech.pockit.ui.theme.PastelPeach
import com.invatech.pockit.ui.theme.PastelPink
import com.invatech.pockit.ui.theme.PastelSky
import com.invatech.pockit.ui.theme.PastelYellow
import com.invatech.pockit.ui.theme.WarmBorder
import java.text.SimpleDateFormat
import java.util.Locale

private data class TransactionCategoryTheme(
    val icon: ImageVector,
    val backgroundColor: Color,
    val accentColor: Color,
    val secondaryAccent: Color
)

private fun resolveCategoryTheme(category: String): TransactionCategoryTheme {
    val lower = category.lowercase().trim()
    return when {
        lower.contains("food") || lower.contains("eat") || lower.contains("rest") || lower.contains("chai") || lower.contains("cafe") ->
            TransactionCategoryTheme(Icons.Rounded.Fastfood, PastelPeach, Color(0xFFFF8A3D), Color(0xFFFFC56E))
        lower.contains("grocer") || lower.contains("kirana") || lower.contains("market") ->
            TransactionCategoryTheme(Icons.Rounded.Eco, PastelMint, Color(0xFF28B983), Color(0xFF8BE6BF))
        lower.contains("bill") || lower.contains("util") || lower.contains("recharge") || lower.contains("elect") ->
            TransactionCategoryTheme(Icons.Rounded.ReceiptLong, PastelPink, Color(0xFFEF6F9D), Color(0xFFFFB7CE))
        lower.contains("educ") || lower.contains("school") || lower.contains("college") || lower.contains("exam") || lower.contains("fee") ->
            TransactionCategoryTheme(Icons.Rounded.School, PastelLilac, Color(0xFF9467D8), Color(0xFFFFCD62))
        lower.contains("cab") || lower.contains("auto") || lower.contains("travel") || lower.contains("car") || lower.contains("fuel") ->
            TransactionCategoryTheme(Icons.Rounded.Flight, PastelSky, Color(0xFF3F9FD8), Color(0xFF8ED6F6))
        lower.contains("shop") || lower.contains("cloth") || lower.contains("store") ->
            TransactionCategoryTheme(Icons.Rounded.ShoppingBag, PastelLilac, Color(0xFF9A69DA), Color(0xFFD5B8FA))
        lower.contains("movie") || lower.contains("game") || lower.contains("fun") || lower.contains("ent") ->
            TransactionCategoryTheme(Icons.Rounded.Headphones, PastelLilac, Color(0xFF986DCE), Color(0xFFCDB3ED))
        lower.contains("health") || lower.contains("med") || lower.contains("pharm") ->
            TransactionCategoryTheme(Icons.Rounded.LocalHospital, PastelMint, Color(0xFF20A995), Color(0xFF7CDACB))
        lower.contains("sweet") || lower.contains("dessert") || lower.contains("cake") ->
            TransactionCategoryTheme(Icons.Rounded.Cake, PastelPink, Color(0xFFEF719F), Color(0xFFFFB7CE))
        lower.isBlank() || lower.contains("income") || lower.contains("salary") ->
            TransactionCategoryTheme(Icons.Rounded.AccountBalanceWallet, PastelMint, Color(0xFF28B983), Color(0xFF8BE6BF))
        else ->
            TransactionCategoryTheme(Icons.Rounded.Paid, PastelLilac, Color(0xFF8D72B8), Color(0xFFC7B3E4))
    }
}

@Composable
private fun OrganicWavePainter(
    theme: TransactionCategoryTheme,
    modifier: Modifier = Modifier
) {
    val surface = MaterialTheme.colorScheme.surface
    Canvas(modifier) {
        val backLayer = Path().apply {
            moveTo(size.width * .02f, size.height)
            cubicTo(
                size.width * .18f, size.height * .78f,
                size.width * .22f, size.height * .34f,
                size.width * .48f, size.height * .38f
            )
            cubicTo(
                size.width * .7f, size.height * .42f,
                size.width * .76f, size.height * .08f,
                size.width, size.height * .16f
            )
            lineTo(size.width, size.height)
            close()
        }
        drawPath(backLayer, lerp(surface, theme.secondaryAccent, .72f).copy(alpha = .08f))

        val middleLayer = Path().apply {
            moveTo(size.width * .16f, size.height)
            cubicTo(
                size.width * .32f, size.height * .7f,
                size.width * .42f, size.height * .66f,
                size.width * .56f, size.height * .5f
            )
            cubicTo(
                size.width * .7f, size.height * .32f,
                size.width * .84f, size.height * .5f,
                size.width, size.height * .28f
            )
            lineTo(size.width, size.height)
            close()
        }
        drawPath(middleLayer, lerp(surface, theme.backgroundColor, .82f).copy(alpha = .12f))

        val frontLayer = Path().apply {
            moveTo(size.width * .34f, size.height)
            cubicTo(
                size.width * .5f, size.height * .76f,
                size.width * .62f, size.height * .9f,
                size.width * .72f, size.height * .66f
            )
            cubicTo(
                size.width * .82f, size.height * .44f,
                size.width * .92f, size.height * .64f,
                size.width, size.height * .46f
            )
            lineTo(size.width, size.height)
            close()
        }
        drawPath(frontLayer, lerp(surface, theme.accentColor, .78f).copy(alpha = .2f))
    }
}

@Composable
private fun CategoryIllustration(
    theme: TransactionCategoryTheme,
    modifier: Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.5.dp.toPx()
            drawLine(
                theme.secondaryAccent.copy(alpha = .72f),
                start = androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .3f),
                end = androidx.compose.ui.geometry.Offset(size.width * .08f, size.height * .2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                theme.accentColor.copy(alpha = .66f),
                start = androidx.compose.ui.geometry.Offset(size.width * .27f, size.height * .18f),
                end = androidx.compose.ui.geometry.Offset(size.width * .24f, size.height * .04f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                theme.secondaryAccent.copy(alpha = .72f),
                start = androidx.compose.ui.geometry.Offset(size.width * .38f, size.height * .22f),
                end = androidx.compose.ui.geometry.Offset(size.width * .46f, size.height * .1f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
        Icon(
            theme.icon,
            contentDescription = null,
            tint = Color.Black.copy(alpha = .1f),
            modifier = Modifier.offset(x = 2.dp, y = 4.dp).size(52.dp)
        )
        Icon(
            theme.icon,
            contentDescription = null,
            tint = theme.accentColor,
            modifier = Modifier.size(52.dp)
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 13.dp, end = 9.dp)
                .size(8.dp)
                .background(Color.White.copy(alpha = .7f), CircleShape)
        )
    }
}

@Composable
private fun TransactionAmountBadge(
    amountPaise: Long,
    isExpense: Boolean,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val amountColor = if (isExpense) {
        if (dark) Color(0xFFFF91A5) else ExpenseRed
    } else {
        if (dark) Color(0xFF63DDAE) else IncomeGreen
    }
    val tint = if (isExpense) PastelPink else PastelMint
    val container = if (dark) {
        lerp(MaterialTheme.colorScheme.surface, amountColor, .18f)
    } else {
        tint.copy(alpha = .88f)
    }
    Surface(color = container, shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Text(
            (if (isExpense) "− " else "+ ") + Money.format(amountPaise),
            modifier = textModifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = amountColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Beautiful Material 3 Expressive Transaction Card for Money Deduct (Expense) & Money Add (Income).
 * Features rich category avatars, subtle colored amount pills, clean typography, and a polished 3-dot menu.
 */
@Composable
fun ExpressiveWalletTransactionCard(
    transaction: HomeTransactionUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    titleModifier: Modifier = Modifier,
    amountModifier: Modifier = Modifier
) {
    val isExpense = transaction.type == WalletTransactionType.EXPENSE
    val categoryTheme = remember(transaction.category) { resolveCategoryTheme(transaction.category) }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val date = transaction.createdAt?.toDate()?.let { SimpleDateFormat("d MMM · h:mm a", locale).format(it) } ?: "Just now"
    var menu by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val scale by rememberPressScale(source, pressedScale = .98f)
    val motion by com.invatech.pockit.ui.common.motion.MotionSettings.enabled.collectAsState()
    val moneyColor = if (isExpense) ExpenseRed else IncomeGreen
    val moneyBackground = if (isExpense) PastelPink else PastelMint
    val cardShape = RoundedCornerShape(28.dp)
    val cardSurface = MaterialTheme.colorScheme.surface
    val onCard = MaterialTheme.colorScheme.onSurface
    val isDark = isSystemInDarkTheme()
    val categoryContainer = if (isDark) {
        lerp(cardSurface, categoryTheme.accentColor, .2f)
    } else {
        categoryTheme.backgroundColor
    }
    val typeContainer = if (isDark) lerp(cardSurface, moneyColor, .18f) else moneyBackground.copy(alpha = .78f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = if (motion) scale else 1f
                scaleY = if (motion) scale else 1f
            }
            .shadow(
                7.dp,
                cardShape,
                ambientColor = categoryTheme.accentColor.copy(alpha = .08f),
                spotColor = categoryTheme.accentColor.copy(alpha = .1f)
            )
            .clip(cardShape)
            .clickable(
                interactionSource = source,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick ?: onEdit
            ),
        shape = cardShape,
        color = cardSurface,
        border = BorderStroke(1.dp, categoryTheme.accentColor.copy(alpha = .1f))
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(
                    listOf(
                        cardSurface,
                        lerp(cardSurface, categoryTheme.backgroundColor, .18f),
                        cardSurface.copy(alpha = .98f)
                    )
                )
            )
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(.52f)
                    .height(104.dp),
                contentAlignment = Alignment.Center
            ) {
                OrganicWavePainter(categoryTheme, Modifier.fillMaxSize())
                CategoryIllustration(
                    categoryTheme,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp, bottom = 4.dp)
                        .size(70.dp)
                )
            }

            Column(
                Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(50.dp).background(categoryContainer, RoundedCornerShape(17.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(categoryTheme.icon, null, Modifier.size(25.dp), tint = categoryTheme.accentColor)
                    }
                    Box(Modifier.width(2.dp).height(38.dp).background(CocoaMuted.copy(alpha = .18f), CircleShape))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            transaction.title.ifBlank { transaction.category },
                            modifier = titleModifier,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = onCard,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box {
                        IconButton(
                            onClick = { menu = true },
                            modifier = Modifier.background(cardSurface.copy(alpha = .72f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.MoreVert, "Actions for ${transaction.title}", tint = CocoaSoft)
                        }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Edit entry") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; onEdit() })
                            DropdownMenuItem(text = { Text("Delete entry", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onDelete() })
                        }
                    }
                }

                Row(Modifier.fillMaxWidth()) {
                    TransactionAmountBadge(
                        amountPaise = transaction.amountPaise,
                        isExpense = isExpense,
                        modifier = Modifier.weight(1f, fill = false),
                        textModifier = amountModifier
                    )
                    Spacer(Modifier.width(76.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 76.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f, fill = false),
                        color = categoryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(categoryTheme.icon, null, Modifier.size(16.dp), tint = categoryTheme.accentColor)
                            Text(
                                transaction.category,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = categoryTheme.accentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Box(Modifier.width(1.dp).height(24.dp).background(CocoaMuted.copy(alpha = .2f)))
                    Surface(color = typeContainer, shape = CircleShape) {
                        Icon(
                            if (isExpense) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            null,
                            Modifier.padding(6.dp).size(15.dp),
                            tint = moneyColor
                        )
                    }
                    Text(
                        if (isExpense) "Money out" else "Money in",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
