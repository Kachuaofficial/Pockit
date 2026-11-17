package com.invatech.pockit.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

private data class CategoryVisual(
    val icon: ImageVector,
    val backgroundColor: Color,
    val iconColor: Color
)

private fun resolveCategoryVisual(category: String, isExpense: Boolean): CategoryVisual {
    if (!isExpense) {
        return CategoryVisual(
            icon = Icons.Rounded.AccountBalanceWallet,
            backgroundColor = PastelMint,
            iconColor = Color(0xFF1E7552)
        )
    }

    val lower = category.lowercase().trim()
    return when {
        lower.contains("food") || lower.contains("eat") || lower.contains("rest") || lower.contains("chai") || lower.contains("cafe") ->
            CategoryVisual(Icons.Rounded.Restaurant, PastelPeach, Color(0xFF944B1B))
        lower.contains("grocer") || lower.contains("kirana") || lower.contains("market") ->
            CategoryVisual(Icons.Rounded.ShoppingBag, PastelMint, Color(0xFF1E7552))
        lower.contains("bill") || lower.contains("util") || lower.contains("recharge") || lower.contains("elect") ->
            CategoryVisual(Icons.Rounded.ReceiptLong, PastelYellow, Color(0xFF8C6D14))
        lower.contains("cab") || lower.contains("auto") || lower.contains("travel") || lower.contains("car") || lower.contains("fuel") ->
            CategoryVisual(Icons.Rounded.DirectionsCar, PastelSky, Color(0xFF1B5885))
        lower.contains("shop") || lower.contains("cloth") || lower.contains("store") ->
            CategoryVisual(Icons.Rounded.ShoppingBag, PastelLilac, Color(0xFF75458C))
        lower.contains("movie") || lower.contains("game") || lower.contains("fun") || lower.contains("ent") ->
            CategoryVisual(Icons.Rounded.Movie, PastelApricot, Color(0xFF9E6517))
        lower.contains("health") || lower.contains("med") || lower.contains("pharm") ->
            CategoryVisual(Icons.Rounded.Spa, PastelMint, Color(0xFF1E7552))
        lower.contains("sweet") || lower.contains("dessert") || lower.contains("cake") ->
            CategoryVisual(Icons.Rounded.Cake, PastelPink, Color(0xFF9E3A52))
        else ->
            CategoryVisual(Icons.Rounded.Category, Color(0xFFF3ECE0), CocoaEspresso)
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
    val visual = remember(transaction.category, isExpense) { resolveCategoryVisual(transaction.category, isExpense) }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val date = transaction.createdAt?.toDate()?.let { SimpleDateFormat("d MMM · h:mm a", locale).format(it) } ?: "Just now"
    var menu by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val scale by rememberPressScale(source, pressedScale = .98f)
    val motion by com.invatech.pockit.ui.common.motion.MotionSettings.enabled.collectAsState()
    Surface(modifier.fillMaxWidth().graphicsLayer { scaleX = if (motion) scale else 1f; scaleY = if (motion) scale else 1f }
        .clip(RoundedCornerShape(26.dp)).clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), onClick = onClick ?: onEdit),
        shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, WarmBorder.copy(alpha = .65f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).background(visual.backgroundColor, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                    Icon(visual.icon, null, Modifier.size(22.dp), tint = visual.iconColor)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(transaction.title.ifBlank { transaction.category }, modifier = titleModifier,
                        style = MaterialTheme.typography.titleMedium, color = CocoaEspresso, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Actions for ${transaction.title}", tint = CocoaSoft) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Edit entry") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; onEdit() })
                        DropdownMenuItem(text = { Text("Delete entry", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onDelete() })
                    }
                }
            }
            Text((if (isExpense) "− " else "+ ") + Money.format(transaction.amountPaise), modifier = amountModifier,
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = if (isExpense) CocoaEspresso else Color(0xFF21664F))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = visual.backgroundColor, shape = CircleShape) {
                    Text(transaction.category, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = visual.iconColor)
                }
                Text(if (isExpense) "Money out" else "Money in", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
