package com.invatech.pockit.ui.friends

/*
 * Combines friend identities, transactions, search and filters into screen state.
 * Delegates cloud changes to FriendRepository and supplies the direction/status used by confirmation actions.
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invatech.pockit.data.repository.FriendRepository
import com.invatech.pockit.ui.friends.model.ExpenseDirection
import com.invatech.pockit.ui.friends.model.Friend
import com.invatech.pockit.ui.friends.model.FriendConnectionPayload
import com.invatech.pockit.ui.friends.model.FriendTransaction
import com.invatech.pockit.ui.friends.model.FriendWithBalance
import com.invatech.pockit.ui.friends.model.FriendsFilter
import com.invatech.pockit.ui.friends.model.TransactionStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class FriendsViewModel(
    private val repository: FriendRepository = FriendRepository()
) : ViewModel() {

    val myQrPayload: String
        get() = repository.currentUserQrPayload

    val myUserName: String
        get() = repository.currentUserName

    val myUserEmail: String
        get() = repository.currentUserEmail

    val myUserId: String
        get() = repository.currentUserId

    // In-memory friends list (connected via Barcode / QR)
    private val _localFriends = MutableStateFlow<List<Friend>>(emptyList())

    private val _localTransactions = MutableStateFlow<List<FriendTransaction>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(FriendsFilter.ALL)
    val selectedFilter: StateFlow<FriendsFilter> = _selectedFilter.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Realtime connected friends (Firestore + local)
    val connectedFriends: StateFlow<List<Friend>> = combine(
        repository.observeFriends(), _localFriends
    ) { remote, local ->
        (remote + local).distinctBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined Friends with Balances and Filters
    val friendsWithBalances: StateFlow<List<FriendWithBalance>> = combine(
        connectedFriends,
        combine(repository.observeTransactions(), _localTransactions) { remote, local ->
            val remoteIds = remote.map { it.id }.toSet()
            val remainingLocal = local.filterNot { it.id in remoteIds }
            (remote + remainingLocal).distinctBy { it.id }
        },
        _searchQuery,
        _selectedFilter
    ) { friends, transactions, query, filter ->
        val list = friends.map { friend ->
            val friendTxs =
                transactions.filter { it.friendId == friend.id }.sortedByDescending { it.timestamp }
            FriendWithBalance(friend = friend, transactions = friendTxs)
        }

        list.filter { item ->
            val matchesQuery = query.isBlank() || item.friend.name.contains(
                query,
                ignoreCase = true
            ) || item.friend.email.contains(query, ignoreCase = true) || item.friend.phone.contains(
                query,
                ignoreCase = true
            )

            val matchesFilter = when (filter) {
                FriendsFilter.ALL -> true
                FriendsFilter.YOULL_GET -> item.netBalance > 0
                FriendsFilter.YOULL_GIVE -> item.netBalance < 0
                FriendsFilter.SETTLED -> item.netBalance == 0.0
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalYoullGet: StateFlow<Double> = friendsWithBalances.combine(_selectedFilter) { list, _ ->
        list.map { it.netBalance }.filter { it > 0 }.sum()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalYoullGive: StateFlow<Double> =
        friendsWithBalances.combine(_selectedFilter) { list, _ ->
            list.map { it.netBalance }.filter { it < 0 }.map { kotlin.math.abs(it) }.sum()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: FriendsFilter) {
        _selectedFilter.value = filter
    }

    /**
     * Connect instantly with a friend by decoding their scanned QR code / Barcode.
     */
    fun connectFriendViaBarcode(rawBarcode: String) {
        viewModelScope.launch {
            val result = repository.connectFriendViaBarcode(rawBarcode)
            if (result.isSuccess) {
                val friend = result.getOrThrow()
                if (_localFriends.value.none { it.id == friend.id }) {
                    _localFriends.value = listOf(friend) + _localFriends.value
                }
                _toastEvent.emit("🎉 Connected with ${friend.name}! You can now add expenses.")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to read barcode"
                _toastEvent.emit(errorMsg)
            }
        }
    }

    /**
     * Accepts a Pockit payload directly. Other text currently becomes a derived placeholder user
     * ID and synthetic payload rather than a verified directory lookup by email. The QR-based
     * connection method handles the resulting payload.
     */
    fun connectFriendViaManualInput(input: String) {
        viewModelScope.launch {
            val trimmed = input.trim()
            if (trimmed.isBlank()) {
                _toastEvent.emit("Please enter a valid friend code or email")
                return@launch
            }

            val simulatedBarcode = if (trimmed.startsWith("pockit://")) {
                trimmed
            } else {
                FriendConnectionPayload(
                    userId = "user_${trimmed.hashCode()}",
                    name = trimmed.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = if (trimmed.contains("@")) trimmed else "$trimmed@pockit.app",
                    avatarEmoji = "🤝"
                ).toQrString()
            }

            connectFriendViaBarcode(simulatedBarcode)
        }
    }

    /**
     * Add expense for a connected friend.
     */
    fun addExpense(
        friendId: String,
        direction: ExpenseDirection,
        amount: Double,
        description: String,
        note: String? = null
    ) {
        viewModelScope.launch {
            val txId = UUID.randomUUID().toString()
            val myUid = repository.currentUserId
            repository.addExpense(
                friendId = friendId,
                direction = direction,
                amount = amount,
                description = description,
                note = note,
                txId = txId,
                status = TransactionStatus.CONFIRMED
            )

            val newTx = FriendTransaction(
                id = txId,
                friendId = friendId,
                amount = amount,
                direction = direction,
                description = description.trim().ifBlank {
                    when (direction) {
                        ExpenseDirection.GAVE_UDHAR -> "Gave Udhar"
                        ExpenseDirection.TOOK_UDHAR -> "Took Udhar"
                        ExpenseDirection.SETTLE_UP -> "Settlement"
                    }
                },
                timestamp = System.currentTimeMillis(),
                note = note?.trim(),
                creatorId = myUid,
                status = TransactionStatus.CONFIRMED
            )
            _localTransactions.value =
                listOf(newTx) + _localTransactions.value.filterNot { it.id == txId }
            _toastEvent.emit("Udhar transaction recorded!")
        }
    }

    /**
     * Settle up balance with friend.
     * Initiates a two-sided verification request (PENDING). The receiving friend/owner
     * can confirm or reject the settlement to guarantee truth for both sides.
     */
    fun settleBalance(friendId: String) {
        viewModelScope.launch {
            val friendWithBalance =
                friendsWithBalances.value.find { it.friend.id == friendId } ?: return@launch
            val currentBalance = friendWithBalance.netBalance

            if (kotlin.math.abs(currentBalance) < 0.01) {
                _toastEvent.emit("Balance is already settled!")
                return@launch
            }

            val myUid = repository.currentUserId
            val absAmount = kotlin.math.abs(currentBalance)
            // If currentBalance < 0, I owe money -> I am the payer
            // If currentBalance > 0, friend owes money -> friend is the payer
            val payerId = if (currentBalance < 0) myUid else friendId
            val desc = if (payerId == myUid) "Paid settlement" else "Marked settlement"
            val txId = UUID.randomUUID().toString()

            repository.addExpense(
                friendId = friendId,
                direction = ExpenseDirection.SETTLE_UP,
                amount = absAmount,
                description = desc,
                txId = txId,
                payerId = payerId,
                status = TransactionStatus.PENDING
            )

            // Local effective amount: +absAmount if I paid, -absAmount if I was paid
            val localEffectiveAmount = if (payerId == myUid) absAmount else -absAmount
            val settleTx = FriendTransaction(
                id = txId,
                friendId = friendId,
                amount = localEffectiveAmount,
                direction = ExpenseDirection.SETTLE_UP,
                description = desc,
                timestamp = System.currentTimeMillis(),
                payerId = payerId,
                creatorId = myUid,
                status = TransactionStatus.PENDING
            )
            _localTransactions.value =
                listOf(settleTx) + _localTransactions.value.filterNot { it.id == txId }
            _toastEvent.emit("Settlement requested! Waiting for ${friendWithBalance.friend.name} to confirm.")
        }
    }

    /**
     * Confirm a pending settlement or expense from the other party.
     */
    fun confirmTransaction(transactionId: String) {
        viewModelScope.launch {
            val result =
                repository.updateTransactionStatus(transactionId, TransactionStatus.CONFIRMED)
            if (result.isSuccess) {
                _localTransactions.value = _localTransactions.value.map {
                    if (it.id == transactionId) it.copy(status = TransactionStatus.CONFIRMED) else it
                }
                _toastEvent.emit("🎉 Settlement confirmed! Dues updated.")
            } else {
                _toastEvent.emit("Failed to confirm: ${result.exceptionOrNull()?.message ?: "Unknown error"}")
            }
        }
    }

    /**
     * Reject a disputed or incorrect settlement/expense from the other party.
     */
    fun rejectTransaction(transactionId: String) {
        viewModelScope.launch {
            val result =
                repository.updateTransactionStatus(transactionId, TransactionStatus.REJECTED)
            if (result.isSuccess) {
                _localTransactions.value = _localTransactions.value.map {
                    if (it.id == transactionId) it.copy(status = TransactionStatus.REJECTED) else it
                }
                _toastEvent.emit("Settlement rejected. Balance remains unchanged.")
            } else {
                _toastEvent.emit("Failed to reject: ${result.exceptionOrNull()?.message ?: "Unknown error"}")
            }
        }
    }

    /**
     * Remove a connected friend.
     */
    fun removeFriend(friendId: String, friendshipId: String = "") {
        viewModelScope.launch {
            val result = repository.removeFriend(friendId, friendshipId)
            if (result.isSuccess) {
                _localFriends.value = _localFriends.value.filterNot { it.id == friendId }
                _toastEvent.emit("Friend removed from your list")
            } else {
                _toastEvent.emit("Failed to remove friend: ${result.exceptionOrNull()?.message ?: "Unknown error"}")
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteExpense(transactionId)
            _localTransactions.value = _localTransactions.value.filterNot { it.id == transactionId }
        }
    }
}
