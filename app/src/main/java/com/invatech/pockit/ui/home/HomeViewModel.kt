package com.invatech.pockit.ui.home

/*
 * Observable dashboard state combining Room history, the recorded balance and cloud refresh callbacks.
 * Coordinates retries and optimistic record changes; errors restore the previous visible state where supported.
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.invatech.pockit.data.repository.WalletRepository
import com.invatech.pockit.ui.home.model.WalletTransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val balancePaise: Long = 0L,
    val transactions: List<HomeTransactionUi> = emptyList(),
    val isLoading: Boolean = true,
    val isInitialLoad: Boolean = true,
    val errorMessage: String? = null
)

data class HomeTransactionUi(
    val id: String,
    val title: String,
    val amountPaise: Long,
    val type: WalletTransactionType,
    val category: String,
    val createdAt: Timestamp? = null
)

class HomeViewModel(
    private val walletRepository: WalletRepository = WalletRepository()
) : ViewModel() {

    // Pair of input fingerprint and request ID. Keeping it after a failed save allows the same form to retry safely.
    private var pendingAddRequest: Pair<String, String>? = null
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var cloudListeners = emptyList<com.google.firebase.firestore.ListenerRegistration>()
    override fun onCleared() {
        cloudListeners.forEach { it.remove() }; super.onCleared()
    }

    init {
        cloudListeners = walletRepository.watchCloud(onTransactionsChanged = {
            viewModelScope.launch {
                try {
                    walletRepository.getTransactions()
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(errorMessage = e.message)
                }
            }
        }, onError = { _uiState.value = _uiState.value.copy(errorMessage = it) })
        viewModelScope.launch {
            walletRepository.observeBalance()
                .collect { balance -> _uiState.value = _uiState.value.copy(balancePaise = balance) }
        }
        observeLocalTransactions()
        loadWallet()
    }

    /**
     * Maps Room emissions into HomeTransactionUi so an accepted local insert, edit or snapshot
     * replacement refreshes the visible history without a screen-level network fetch.
     */
    private fun observeLocalTransactions() {
        viewModelScope.launch {
            walletRepository.observeTransactions().collect { roomList ->
                run {
                    _uiState.value = _uiState.value.copy(
                        transactions = roomList.map { transaction ->
                            HomeTransactionUi(
                                id = transaction.id,
                                title = transaction.title,
                                amountPaise = transaction.amountPaise,
                                type = transaction.type,
                                category = transaction.category,
                                createdAt = transaction.createdAt
                            )
                        }, isInitialLoad = false
                    )
                }
            }
        }
    }

    /**
     * Refreshes recorded balance and history while exposing loading/error state. A failed refresh
     * ends loading and leaves previously available screen data intact.
     */
    fun loadWallet() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, errorMessage = null
            )

            try {
                val balance = walletRepository.getBalancePaise()
                val transactions = walletRepository.getTransactions()

                _uiState.value = HomeUiState(
                    balancePaise = walletRepository.currentBalance(),
                    transactions = transactions.map { transaction ->
                        HomeTransactionUi(
                            id = transaction.id,
                            title = transaction.title,
                            amountPaise = transaction.amountPaise,
                            type = transaction.type,
                            category = transaction.category,
                            createdAt = transaction.createdAt
                        )
                    },
                    isLoading = false,
                    isInitialLoad = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInitialLoad = false,
                    errorMessage = e.message ?: "Wallet load nahi ho paya."
                )
            }
        }
    }

    fun addIncome(
        title: String, amountPaise: Long, category: String = "Income"
    ) {
        addTransaction(
            title = title,
            amountPaise = amountPaise,
            type = WalletTransactionType.INCOME,
            category = category
        )
    }

    fun addExpense(
        title: String, amountPaise: Long, category: String = "Other"
    ) {
        addTransaction(
            title = title,
            amountPaise = amountPaise,
            type = WalletTransactionType.EXPENSE,
            category = category
        )
    }

    /**
     * Reuses an in-memory request ID for an identical pending entry after a failed attempt. The
     * repository uses this ID for idempotency; after success the pending ID is cleared and the
     * accepted event is merged into UI state. This pending fingerprint is not durable across
     * process death.
     */
    private fun addTransaction(
        title: String, amountPaise: Long, type: WalletTransactionType, category: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, errorMessage = null
            )

            try {
                val fingerprint = "$title|$amountPaise|$type|$category"
                val request = pendingAddRequest?.takeIf { it.first == fingerprint }
                    ?: (fingerprint to java.util.UUID.randomUUID()
                        .toString()).also { pendingAddRequest = it }
                val (newBalance, txId) = walletRepository.addTransaction(
                    title = title,
                    amountPaise = amountPaise,
                    type = type,
                    category = category,
                    requestId = request.second
                )
                pendingAddRequest = null

                // Cost-saving: Add to state locally instead of refetching entire Firestore history
                val newTx = HomeTransactionUi(
                    id = txId,
                    title = title.trim(),
                    amountPaise = amountPaise,
                    type = type,
                    category = category,
                    createdAt = Timestamp.now()
                )

                _uiState.value = _uiState.value.copy(
                    balancePaise = newBalance,
                    transactions = listOf(newTx) + _uiState.value.transactions.filter { it.id != txId },
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = e.message ?: "Transaction save nahi ho paya."
                )
            }
        }
    }

    /**
     * Captures the currently displayed balance as an expected value and asks the repository for a
     * checked adjustment. Calls onSaved only after the server operation completes.
     */
    fun editBalance(value: Long, onSaved: () -> Unit) {
        val expected = _uiState.value.balancePaise
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                walletRepository.setRecordedBalance(value, expected)
                _uiState.value = _uiState.value.copy(balancePaise = value, isLoading = false)
                onSaved()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Removes the row optimistically for immediate feedback, then requests the atomic server
     * delete. Restores the previous list and balance if the request fails.
     */
    fun deleteTransaction(transactionId: String) {
        val previousTransactions = _uiState.value.transactions
        val previousBalance = _uiState.value.balancePaise
        // Optimistic UI update
        _uiState.value = _uiState.value.copy(
            transactions = previousTransactions.filter { it.id != transactionId })

        viewModelScope.launch {
            try {
                val newBalance = walletRepository.deleteTransaction(transactionId)
                _uiState.value = _uiState.value.copy(balancePaise = newBalance)
            } catch (e: Exception) {
                // Rollback on failure
                _uiState.value = _uiState.value.copy(
                    transactions = previousTransactions,
                    balancePaise = previousBalance,
                    errorMessage = e.message ?: "Transaction delete nahi ho paya."
                )
            }
        }
    }

    /**
     * Restores the row optimistically for Undo and asks the repository to restore its original ID
     * and balance effect. Rolls back the visible restoration if persistence fails.
     */
    fun restoreTransaction(transaction: HomeTransactionUi) {
        val previousTransactions = _uiState.value.transactions
        val previousBalance = _uiState.value.balancePaise
        // Optimistic UI update
        _uiState.value = _uiState.value.copy(
            transactions = (listOf(transaction) + previousTransactions.filter { it.id != transaction.id })
        )

        viewModelScope.launch {
            try {
                val newBalance = walletRepository.restoreTransaction(
                    transactionId = transaction.id,
                    title = transaction.title,
                    amountPaise = transaction.amountPaise,
                    type = transaction.type,
                    category = transaction.category,
                    createdAt = transaction.createdAt
                )
                _uiState.value = _uiState.value.copy(balancePaise = newBalance)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    transactions = previousTransactions,
                    balancePaise = previousBalance,
                    errorMessage = e.message ?: "Transaction restore nahi ho paya."
                )
            }
        }
    }

    /**
     * Shows the edited row while saving. On failure, restores the previous list and balance so a
     * rejected edit is not left looking accepted.
     */
    fun updateTransaction(
        transactionId: String,
        title: String,
        amountPaise: Long,
        type: WalletTransactionType,
        category: String
    ) {
        val previousTransactions = _uiState.value.transactions
        val previousBalance = _uiState.value.balancePaise

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        // Optimistic UI update
        _uiState.value = _uiState.value.copy(
            transactions = previousTransactions.map { tx ->
                if (tx.id == transactionId) {
                    tx.copy(
                        title = title.trim(),
                        amountPaise = amountPaise,
                        type = type,
                        category = category
                    )
                } else tx
            })

        viewModelScope.launch {
            try {
                val newBalance = walletRepository.updateTransaction(
                    transactionId = transactionId,
                    newTitle = title,
                    newAmountPaise = amountPaise,
                    newType = type,
                    newCategory = category
                )
                _uiState.value = _uiState.value.copy(balancePaise = newBalance, isLoading = false)
            } catch (e: Exception) {
                // Rollback on failure
                _uiState.value = _uiState.value.copy(
                    transactions = previousTransactions,
                    balancePaise = previousBalance,
                    isLoading = false,
                    errorMessage = e.message ?: "Transaction update nahi ho paya."
                )
            }
        }
    }
}
