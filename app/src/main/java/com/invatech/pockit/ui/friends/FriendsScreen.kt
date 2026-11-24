package com.invatech.pockit.ui.friends

/*
 * Friends area combining the personal ledger and connected-friend views.
 * Coordinates QR/manual connection, expense forms, search and friend-detail navigation.
 */

import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.invatech.pockit.ui.common.ads.PockitBannerAd
import com.invatech.pockit.ui.common.ads.PockitNativeAdCard
import com.invatech.pockit.ui.common.motion.M3NavMotion
import com.invatech.pockit.ui.friends.components.AddFriendExpenseSheet
import com.invatech.pockit.ui.friends.components.FriendItemCard
import com.invatech.pockit.ui.friends.components.FriendsExpressiveTabLayout
import com.invatech.pockit.ui.friends.components.FriendsSummaryCards
import com.invatech.pockit.ui.friends.components.FriendsTabType
import com.invatech.pockit.ui.friends.components.MyQrCodeSheet
import com.invatech.pockit.ui.friends.components.ScanFriendQrSheet
import com.invatech.pockit.ui.friends.model.ExpenseDirection
import com.invatech.pockit.ui.friends.model.FriendWithBalance
import com.invatech.pockit.ui.home.components.RecordDoodleBackdrop
import com.invatech.pockit.ui.theme.CloudDeep
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.Mint
import kotlinx.coroutines.CancellationException

@Composable
fun FriendsScreen(modifier: Modifier = Modifier) {
    val financeVm: com.invatech.pockit.ui.finance.FinanceViewModel = viewModel()
    val friendsVm: FriendsViewModel = viewModel()
    val personalRecords by financeVm.ledger.collectAsState()
    val connectedFriends by friendsVm.friendsWithBalances.collectAsState()
    val personalGroups = remember(personalRecords) { personalRecords.groupBy { it.name } }
    val personalBalances = remember(personalGroups) {
        personalGroups.mapValues { com.invatech.pockit.ui.finance.udharBalance(it.value) }
    }
    var showAddOptions by rememberSaveable { mutableStateOf(false) }
    var showMyQr by rememberSaveable { mutableStateOf(false) }
    var showScanQr by rememberSaveable { mutableStateOf(false) }
    var showPersonalEntry by rememberSaveable { mutableStateOf(false) }
    var selectedPersonalName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedConnectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showConnectedExpense by rememberSaveable { mutableStateOf(false) }
    var expenseDirection by remember { mutableStateOf(ExpenseDirection.GAVE_UDHAR) }

    val selectedConnected = remember(connectedFriends, selectedConnectedId) {
        connectedFriends.find { it.friend.id == selectedConnectedId }
    }

    Box(modifier.fillMaxSize()) {
        UnifiedFriendsOverview(
            personalBalances = personalBalances,
            connectedFriends = connectedFriends,
            onPersonalClick = { selectedPersonalName = it },
            onConnectedClick = { selectedConnectedId = it },
            onAddFriend = { showAddOptions = true }
        )

        AnimatedVisibility(
            visible = selectedConnected != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            selectedConnected?.let { detail ->
                FriendDetailScreen(
                    friendWithBalance = detail,
                    currentUserId = friendsVm.myUserId,
                    onDismiss = { selectedConnectedId = null },
                    onAddExpense = { direction ->
                        expenseDirection = direction
                        showConnectedExpense = true
                    },
                    onSettleUp = { friendsVm.settleBalance(detail.friend.id) },
                    onConfirmTransaction = friendsVm::confirmTransaction,
                    onRejectTransaction = friendsVm::rejectTransaction,
                    onDeleteTransaction = friendsVm::deleteTransaction,
                    onRemoveFriend = {
                        friendsVm.removeFriend(detail.friend.id, detail.friend.friendshipId)
                        selectedConnectedId = null
                    }
                )
            }
        }
    }

    if (showAddOptions) FriendAddOptionsSheet(
        onDismiss = { showAddOptions = false },
        onAddPersonal = { showAddOptions = false; selectedPersonalName = null; showPersonalEntry = true },
        onScanFriend = { showAddOptions = false; showScanQr = true },
        onShareCode = { showAddOptions = false; showMyQr = true }
    )
    selectedPersonalName?.let { name ->
        PersonalFriendSheet(
            name = name,
            records = personalGroups[name].orEmpty(),
            balance = personalBalances[name] ?: 0L,
            busy = financeVm.busy.collectAsState().value,
            onDismiss = { selectedPersonalName = null },
            onAddEntry = { showPersonalEntry = true },
            onDelete = financeVm::deleteEntry
        )
    }
    if (showPersonalEntry) RecordUdharDialog(
        initialName = selectedPersonalName.orEmpty(),
        busy = financeVm.busy.collectAsState().value,
        error = financeVm.error.collectAsState().value,
        onDismiss = { showPersonalEntry = false },
        onSave = { name, amount, direction, note ->
            financeVm.addUdhar(name, amount, direction, note) { showPersonalEntry = false }
        }
    )
    if (showMyQr) MyQrCodeSheet(
        qrContent = friendsVm.myQrPayload,
        userName = friendsVm.myUserName,
        userEmail = friendsVm.myUserEmail,
        onDismiss = { showMyQr = false }
    )
    if (showScanQr) ScanFriendQrSheet(
        onDismiss = { showScanQr = false },
        onCodeScanned = friendsVm::connectFriendViaBarcode,
        onManualConnect = friendsVm::connectFriendViaManualInput
    )
    if (showConnectedExpense && selectedConnected != null) AddFriendExpenseSheet(
        friends = connectedFriends.map { it.friend },
        initialFriendId = selectedConnected.friend.id,
        initialDirection = expenseDirection,
        onDismiss = { showConnectedExpense = false },
        onSaveExpense = friendsVm::addExpense
    )
}

@Composable
private fun LegacyUnifiedFriendsOverview(
    personalBalances: Map<String, Long>,
    connectedFriends: List<FriendWithBalance>,
    onOpenPersonal: () -> Unit,
    onOpenConnected: () -> Unit
) {
    RecordDoodleBackdrop(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Friends", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Connected friends and personal records, together.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.invatech.pockit.ui.finance.FinanceCard(
                        modifier = Modifier.weight(1f).clickable(onClick = onOpenPersonal),
                        color = com.invatech.pockit.ui.theme.PastelPeach
                    ) {
                        Text("Personal udhar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${personalBalances.size} contacts", style = MaterialTheme.typography.bodySmall)
                        Text("Add or manage", style = MaterialTheme.typography.labelLarge, color = com.invatech.pockit.ui.theme.PockitOrangeDeep)
                    }
                    com.invatech.pockit.ui.finance.FinanceCard(
                        modifier = Modifier.weight(1f).clickable(onClick = onOpenConnected),
                        color = com.invatech.pockit.ui.theme.PastelMint
                    ) {
                        Text("Connected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${connectedFriends.size} friends", style = MaterialTheme.typography.bodySmall)
                        Text("Connect or split", style = MaterialTheme.typography.labelLarge, color = com.invatech.pockit.ui.theme.PockitGreen)
                    }
                }
            }
            item {
                Text("Personal contacts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (personalBalances.isEmpty()) {
                item {
                    com.invatech.pockit.ui.finance.FinanceCard(Modifier.clickable(onClick = onOpenPersonal)) {
                        Text("No personal udhar yet", style = MaterialTheme.typography.titleMedium)
                        Text("Add someone by name—no Pockit connection required.")
                    }
                }
            } else {
                itemsIndexed(personalBalances.entries.toList(), key = { _, item -> item.key }) { _, item ->
                    com.invatech.pockit.ui.finance.FinanceCard(Modifier.clickable(onClick = onOpenPersonal)) {
                        Text(item.key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                item.value > 0 -> "You'll receive ${com.invatech.pockit.domain.Money.format(item.value)}"
                                item.value < 0 -> "You owe ${com.invatech.pockit.domain.Money.format(-item.value)}"
                                else -> "Settled up"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Text("Connected friends", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (connectedFriends.isEmpty()) {
                item {
                    com.invatech.pockit.ui.finance.FinanceCard(Modifier.clickable(onClick = onOpenConnected)) {
                        Text("No connected friends yet", style = MaterialTheme.typography.titleMedium)
                        Text("Scan a friend's code or share yours to start splitting together.")
                    }
                }
            } else {
                itemsIndexed(connectedFriends, key = { _, item -> item.friend.id }) { _, item ->
                    FriendItemCard(
                        friendWithBalance = item,
                        onClick = onOpenConnected,
                        modifier = Modifier.animateItem()
                    )
                }
            }
            if (personalBalances.isNotEmpty() || connectedFriends.isNotEmpty()) {
                item { PockitBannerAd(placementKey = "FriendsScreen.unified.banner") }
            }
        }
    }
}

@Composable
fun ConnectedFriendsContent(
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = viewModel()
) {
    val context = LocalContext.current
    val friendsList by viewModel.friendsWithBalances.collectAsState()
    val totalYoullGet by viewModel.totalYoullGet.collectAsState()
    val totalYoullGive by viewModel.totalYoullGive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var showMyQrSheet by remember { mutableStateOf(false) }
    var showScanQrSheet by remember { mutableStateOf(false) }
    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var selectedFriendForDetail by remember { mutableStateOf<FriendWithBalance?>(null) }
    var expenseInitialFriendId by remember { mutableStateOf<String?>(null) }
    var expenseInitialDirection by remember { mutableStateOf(ExpenseDirection.GAVE_UDHAR) }

    // Listen for toast messages from ViewModel
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Keep detail view in sync when transactions update
    val currentDetailFriend = remember(friendsList, selectedFriendForDetail) {
        selectedFriendForDetail?.let { detail ->
            friendsList.find { it.friend.id == detail.friend.id }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        RecordDoodleBackdrop(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FriendsSummaryCards(
                        totalYoullGet = totalYoullGet,
                        totalYoullGive = totalYoullGive,
                        selectedFilter = selectedFilter,
                        onSelectFilter = { viewModel.setFilter(it) },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onMyQrClick = { showMyQrSheet = true },
                        onScanQrClick = { showScanQrSheet = true },
                        onAddExpenseClick = {
                            if (friendsList.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "Scan a friend's barcode first to start recording expenses!",
                                    Toast.LENGTH_LONG
                                ).show()
                                showScanQrSheet = true
                            } else {
                                expenseInitialFriendId = null
                                expenseInitialDirection = ExpenseDirection.GAVE_UDHAR
                                showAddExpenseSheet = true
                            }
                        }
                    )
                }

                // Friends list header
                item {
                    RowHeader(count = friendsList.size)
                }

                if (friendsList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {

                                Text(
                                    text = if (searchQuery.isNotBlank()) "No friends match '$searchQuery'" else "No friends connected yet",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Connect in 1 second! Show your barcode or scan your friend's code to start tracking expenses together.",
                                    fontSize = 13.sp,
                                    color = InkMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(CloudDeep)
                                            .clickable { showMyQrSheet = true }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.QrCode2,
                                                contentDescription = null,
                                                tint = Ink,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "My Barcode",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Ink
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(com.invatech.pockit.ui.theme.PockitOrange)
                                            .clickable { showScanQrSheet = true }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.QrCodeScanner,
                                                contentDescription = null,
                                                tint = com.invatech.pockit.ui.theme.PockitBackground,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Scan Friend",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = com.invatech.pockit.ui.theme.PockitBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(friendsList, key = { _, item -> item.friend.id }) { index, item ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FriendItemCard(
                                friendWithBalance = item,
                                onClick = { selectedFriendForDetail = item },
                                modifier = Modifier.animateItem()
                            )
                            if (com.invatech.pockit.ui.common.ads.NativePlacementPolicy.afterRow(index, friendsList.size)) {
                                PockitNativeAdCard(placementKey = "FriendsScreen.native.$index", modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }

                item {
                    if (friendsList.isNotEmpty()) PockitBannerAd(placementKey = "FriendsScreen.banner.1", modifier = Modifier.padding(vertical = 4.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Dedicated Full Screen: Friend Ledger Detail
        AnimatedVisibility(
            visible = currentDetailFriend != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            currentDetailFriend?.let { detailFriend ->
                FriendDetailScreen(
                    friendWithBalance = detailFriend,
                    currentUserId = viewModel.myUserId,
                    onDismiss = { selectedFriendForDetail = null },
                    onAddExpense = { direction ->
                        expenseInitialFriendId = detailFriend.friend.id
                        expenseInitialDirection = direction
                        showAddExpenseSheet = true
                    },
                    onSettleUp = {
                        viewModel.settleBalance(detailFriend.friend.id)
                    },
                    onConfirmTransaction = { txId ->
                        viewModel.confirmTransaction(txId)
                    },
                    onRejectTransaction = { txId ->
                        viewModel.rejectTransaction(txId)
                    },
                    onDeleteTransaction = { txId ->
                        viewModel.deleteTransaction(txId)
                    },
                    onRemoveFriend = {
                        viewModel.removeFriend(detailFriend.friend.id, detailFriend.friend.friendshipId)
                        selectedFriendForDetail = null
                    }
                )
            }
        }
    }

    // Sheet: My QR Code
    if (showMyQrSheet) {
        MyQrCodeSheet(
            qrContent = viewModel.myQrPayload,
            userName = viewModel.myUserName,
            userEmail = viewModel.myUserEmail,
            onDismiss = { showMyQrSheet = false }
        )
    }

    // Sheet: Scan Friend QR / Barcode
    if (showScanQrSheet) {
        ScanFriendQrSheet(
            onDismiss = { showScanQrSheet = false },
            onCodeScanned = { rawCode ->
                viewModel.connectFriendViaBarcode(rawCode)
            },
            onManualConnect = { manualInput ->
                viewModel.connectFriendViaManualInput(manualInput)
            }
        )
    }

    // Sheet: Add Udhar Expense (Only for connected friends)
    if (showAddExpenseSheet) {
        AddFriendExpenseSheet(
            friends = friendsList.map { it.friend },
            initialFriendId = expenseInitialFriendId,
            initialDirection = expenseInitialDirection,
            onDismiss = { showAddExpenseSheet = false },
            onSaveExpense = { friendId, dir, amount, desc ->
                viewModel.addExpense(friendId, dir, amount, desc)
            }
        )
    }
}

@Composable
private fun RowHeader(count: Int) {
    Text(
        text = "Connected Friends ($count)",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}
