package com.invatech.pockit.ui.friends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invatech.pockit.R
import com.invatech.pockit.domain.Money
import com.invatech.pockit.ui.common.UserAvatar
import com.invatech.pockit.ui.friends.model.ExpenseDirection
import com.invatech.pockit.ui.friends.model.FriendWithBalance
import kotlin.math.absoluteValue

private enum class OverviewFilter { ALL, GET, GIVE }

private data class OverviewFriend(
    val key: String,
    val name: String,
    val subtitle: String,
    val balancePaise: Long,
    val friendId: String? = null,
    val isConnected: Boolean,
    val onClick: () -> Unit
)

@Composable
internal fun UnifiedFriendsOverview(
    personalBalances: Map<String, Long>,
    connectedFriends: List<FriendWithBalance>,
    onPersonalClick: (String) -> Unit,
    onConnectedClick: (String) -> Unit,
    onAddFriend: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(OverviewFilter.ALL) }

    val rows = remember(personalBalances, connectedFriends, query, filter) {
        val personal = personalBalances.map { (name, balance) ->
            OverviewFriend(
                key = "personal:$name",
                name = name,
                subtitle = "Personal record · Tap to manage",
                balancePaise = balance,
                isConnected = false,
                onClick = { onPersonalClick(name) }
            )
        }
        val connected = connectedFriends.map { item ->
            val last = item.lastTransaction
            val activity = when (last?.direction) {
                ExpenseDirection.GAVE_UDHAR -> "You paid ${Money.format(Money.legacyPaise(last.amount))}"
                ExpenseDirection.TOOK_UDHAR -> "Took ${Money.format(Money.legacyPaise(last.amount))}"
                ExpenseDirection.SETTLE_UP -> "Settlement recorded"
                null -> "No transactions yet"
            }
            OverviewFriend(
                key = "connected:${item.friend.id}",
                name = item.friend.name,
                subtitle = activity,
                balancePaise = Money.legacyPaise(item.netBalance),
                friendId = item.friend.id,
                isConnected = true,
                onClick = { onConnectedClick(item.friend.id) }
            )
        }
        (connected + personal).filter { row ->
            row.name.contains(query.trim(), ignoreCase = true) && when (filter) {
                OverviewFilter.ALL -> true
                OverviewFilter.GET -> row.balancePaise > 0
                OverviewFilter.GIVE -> row.balancePaise < 0
            }
        }
    }
    val allBalances = remember(personalBalances, connectedFriends) {
        personalBalances.values + connectedFriends.map { Money.legacyPaise(it.netBalance) }
    }
    val totalGet = remember(allBalances) { Money.sum(allBalances.filter { it > 0 }) }
    val totalGive = remember(allBalances) { Money.sum(allBalances.filter { it < 0 }.map { it.absoluteValue }) }

    Box(Modifier.fillMaxSize().background(Color(0xFFFFFAF8))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { FriendsHero() }
            item { FriendSearch(query, onQueryChange = { query = it }) }
            item { FriendFilters(filter, onSelect = { filter = it }) }
            item { BalanceSummary(totalGet, totalGive) }
            item {
                Text(
                    text = "Friends (${rows.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF21182A)
                )
            }
            if (rows.isEmpty()) {
                item { EmptyFriends(query, onAddFriend) }
            } else {
                items(rows, key = { it.key }) { row ->
                    UnifiedFriendCard(row, Modifier.animateItem())
                }
            }
            item { AddFriendButton(onAddFriend) }
        }
    }
}

@Composable
private fun FriendsHero() {
    Box(Modifier.fillMaxWidth().height(152.dp)) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color(0xFFFFDDEA), radius = size.minDimension * .44f, center = Offset(size.width * .79f, size.height * .55f))
            drawCircle(Color(0xFFF4E5FF).copy(alpha = .88f), radius = size.minDimension * .29f, center = Offset(size.width * .91f, size.height * .31f))
            drawCircle(Color(0xFFFFEDF4), radius = size.minDimension * .24f, center = Offset(size.width * .64f, size.height * .71f))
        }
        Column(Modifier.align(Alignment.CenterStart)) {
            Text("Friends", fontSize = 38.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black, color = Color(0xFF21152B))
            Spacer(Modifier.height(3.dp))
            Text(
                "Send, receive or settle money\nwith your friends",
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = Color(0xFF695D70)
            )
        }
        Image(
            painter = painterResource(R.drawable.pockit_friends_panda),
            contentDescription = "Pockit panda holding a phone",
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.BottomEnd).size(width = 172.dp, height = 142.dp)
        )
    }
}

@Composable
private fun FriendSearch(value: String, onQueryChange: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = .94f),
        shadowElevation = 8.dp,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().height(58.dp)
    ) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = Color(0xFF5A4368), modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(13.dp))
            BasicTextField(
                value = value,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF2B2033), fontSize = 16.sp),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text("Search friend by name...", color = Color(0xFF776B7D), fontSize = 16.sp)
                    inner()
                }
            )
        }
    }
}

@Composable
private fun FriendFilters(selected: OverviewFilter, onSelect: (OverviewFilter) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip("All Friends", OverviewFilter.ALL, selected, Color.White, Icons.Rounded.Groups, onSelect, Modifier.weight(1.2f))
        FilterChip("You'll Get", OverviewFilter.GET, selected, Color(0xFF08B874), Icons.Rounded.ArrowDownward, onSelect, Modifier.weight(1f))
        FilterChip("You'll Give", OverviewFilter.GIVE, selected, Color(0xFFFF477C), Icons.Rounded.ArrowUpward, onSelect, Modifier.weight(1f))
        Surface(shape = CircleShape, color = Color.White, shadowElevation = 4.dp, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Tune, "Filter friends", tint = Color(0xFF4F345F), modifier = Modifier.padding(13.dp))
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    type: OverviewFilter,
    selected: OverviewFilter,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onSelect: (OverviewFilter) -> Unit,
    modifier: Modifier
) {
    val active = selected == type
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (active) Color(0xFFFF3F78) else Color.White,
        shadowElevation = if (active) 7.dp else 3.dp,
        modifier = modifier.height(48.dp).clickable { onSelect(type) }
    ) {
        Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = if (active) Color.White else accent, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color(0xFF2D2235))
        }
    }
}

@Composable
private fun BalanceSummary(totalGet: Long, totalGive: Long) {
    Box(
        Modifier.fillMaxWidth().height(124.dp).shadow(6.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp))
            .background(Brush.horizontalGradient(listOf(Color.White, Color(0xFFFFEDF4))))
    ) {
        Canvas(Modifier.matchParentSize()) { drawSummaryWaves() }
        Row(Modifier.fillMaxSize().padding(start = 18.dp, end = 94.dp), verticalAlignment = Alignment.CenterVertically) {
            BalanceMetric("You'll Get", totalGet, true, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(64.dp).background(Color(0xFFE8DCE9)))
            BalanceMetric("You'll Give", totalGive, false, Modifier.weight(1f).padding(start = 16.dp))
        }
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 17.dp).size(64.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFC1D6)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.AccountBalanceWallet, null, tint = Color(0xFFFF3F78), modifier = Modifier.size(38.dp))
        }
    }
}

@Composable
private fun BalanceMetric(label: String, amount: Long, get: Boolean, modifier: Modifier) {
    val color = if (get) Color(0xFF07B875) else Color(0xFFFF3F78)
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(27.dp).clip(CircleShape).background(color.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                Icon(if (get) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward, null, tint = color, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF33263A))
        }
        Text(Money.format(amount), fontSize = 20.sp, fontWeight = FontWeight.Black, color = color, maxLines = 1)
    }
}

@Composable
private fun UnifiedFriendCard(row: OverviewFriend, modifier: Modifier = Modifier) {
    val index = (row.name.hashCode().absoluteValue % 4)
    val palettes = listOf(
        listOf(Color(0xFFFDF9FF), Color(0xFFEAD9FF)),
        listOf(Color(0xFFFFFCF4), Color(0xFFFFE4A8)),
        listOf(Color(0xFFFFF8FA), Color(0xFFFFD5E3)),
        listOf(Color(0xFFF8FBFF), Color(0xFFDDE9FF))
    )
    Box(
        modifier.fillMaxWidth().height(104.dp).shadow(3.dp, RoundedCornerShape(25.dp)).clip(RoundedCornerShape(25.dp))
            .background(Brush.horizontalGradient(listOf(Color.White, palettes[index][0]))).clickable(onClick = row.onClick)
    ) {
        Canvas(Modifier.matchParentSize()) { drawCardWaves(palettes[index][1]) }
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (row.isConnected) UserAvatar(row.friendId, row.name, Modifier.size(58.dp)) else InitialAvatar(row.name, palettes[index][1])
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(row.name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF26172F), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(row.subtitle, fontSize = 12.sp, color = Color(0xFF706476), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text(if (row.isConnected) "Connected" else "Personal", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C45BE))
            }
            FriendBalance(row.balancePaise)
            Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF392643), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun InitialAvatar(name: String, color: Color) {
    Box(Modifier.size(58.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(name.trim().firstOrNull()?.uppercase() ?: "?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D318D))
    }
}

@Composable
private fun FriendBalance(balance: Long) {
    when {
        balance > 0 -> Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
            Text("You'll Get", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF078C5D))
            Text(Money.format(balance), fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color(0xFF08AE72), maxLines = 1)
        }
        balance < 0 -> Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
            Text("You'll Give", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB62B56))
            Text(Money.format(balance.absoluteValue), fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF3F78), maxLines = 1)
        }
        else -> Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFEFE3FF), modifier = Modifier.padding(end = 8.dp)) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF7640B5), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Settled", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF63379A))
            }
        }
    }
}

@Composable
private fun EmptyFriends(query: String, onAdd: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth().clickable(onClick = onAdd)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (query.isBlank()) "No friends yet" else "No matching friends", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("Add a friend or change your search and filter.", color = Color(0xFF746978), fontSize = 13.sp)
        }
    }
}

@Composable
private fun AddFriendButton(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(25.dp),
        color = Color(0xFFFF3F78),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().height(58.dp).clickable(onClick = onClick)
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("+", fontSize = 30.sp, color = Color.White, fontWeight = FontWeight.Light)
            Spacer(Modifier.width(10.dp))
            Text("Add New Friend", fontSize = 17.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

private fun DrawScope.drawSummaryWaves() {
    val p = Path().apply {
        moveTo(size.width * .53f, size.height)
        cubicTo(size.width * .68f, size.height * .68f, size.width * .76f, size.height * .92f, size.width, size.height * .56f)
        lineTo(size.width, size.height); close()
    }
    drawPath(p, Color(0xFFFF8FB7).copy(alpha = .22f))
}

private fun DrawScope.drawCardWaves(accent: Color) {
    val back = Path().apply {
        moveTo(size.width * .46f, size.height)
        cubicTo(size.width * .63f, size.height * .62f, size.width * .74f, size.height * .88f, size.width, size.height * .52f)
        lineTo(size.width, size.height); close()
    }
    val front = Path().apply {
        moveTo(size.width * .62f, size.height)
        cubicTo(size.width * .76f, size.height * .72f, size.width * .87f, size.height * .94f, size.width, size.height * .67f)
        lineTo(size.width, size.height); close()
    }
    drawPath(back, accent.copy(alpha = .34f))
    drawPath(front, accent.copy(alpha = .62f))
}
