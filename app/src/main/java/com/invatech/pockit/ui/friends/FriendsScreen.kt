package com.invatech.pockit.ui.friends

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.invatech.pockit.ui.friends.components.AddFriendExpenseSheet
import com.invatech.pockit.ui.friends.components.FriendDetailSheet
import com.invatech.pockit.ui.friends.components.FriendItemCard
import com.invatech.pockit.ui.friends.components.FriendsSummaryCards
import com.invatech.pockit.ui.friends.components.MyQrCodeSheet
import com.invatech.pockit.ui.friends.components.ScanFriendQrSheet
import com.invatech.pockit.ui.friends.model.ExpenseDirection
import com.invatech.pockit.ui.friends.model.FriendWithBalance
import com.invatech.pockit.ui.theme.CloudDeep
import com.invatech.pockit.ui.theme.Ink
import com.invatech.pockit.ui.theme.InkMuted
import com.invatech.pockit.ui.theme.Mint

@Composable
fun FriendsScreen(
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
                            text = "📸",
                            fontSize = 42.sp
                        )
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
                                    .background(Ink)
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
                                        tint = Mint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Scan Friend",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            items(friendsList, key = { it.friend.id }) { item ->
                FriendItemCard(
                    friendWithBalance = item,
                    onClick = { selectedFriendForDetail = item },
                    modifier = Modifier.animateItem()
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
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

    // Sheet: Friend Ledger Detail
    currentDetailFriend?.let { detailFriend ->
        FriendDetailSheet(
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

@Composable
private fun RowHeader(count: Int) {
    Text(
        text = "Connected Friends ($count)",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}
