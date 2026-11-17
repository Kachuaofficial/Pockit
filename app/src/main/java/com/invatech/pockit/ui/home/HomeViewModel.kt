package com.invatech.pockit.ui.home

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

    private var pendingAddRequest: Pair<String, String>? = null
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var cloudListeners = emptyList<com.google.firebase.firestore.ListenerRegistration>()
    override fun onCleared() { cloudListeners.forEach { it.remove() }; super.onCleared() }
    init {
        cloudListeners = walletRepository.watchCloud(onTransactionsChanged = {
            viewModelScope.launch {
                try { walletRepository.getTransactions() }
                catch (e: Exception) { _uiState.value = _uiState.value.copy(errorMessage = e.message) }
            }
        }, onError = { _uiState.value = _uiState.value.copy(errorMessage = it) })
        viewModelScope.launch { walletRepository.observeBalance().collect { balance -> _uiState.value = _uiState.value.copy(balancePaise = balance) } }
        observeLocalTransactions()
        loadWallet()
    }

    /**
     * Observes Room database continuously so any local additions, edits, or syncs
     * update the UI instantaneously with zero network latency.
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
                        },
                        isInitialLoad = false
                    )
                }
            }
        }
    }

    fun loadWallet() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val balance = walletRepository.getBalancePaise()
                val transactions = walletRepository.getTransactions()

                _uiState.value = HomeUiState(
                    balancePaise = walletRepository.observeBalance().value,
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
        title: String,
        amountPaise: Long,
        category: String = "Income"
    ) {
        addTransaction(
            title = title,
            amountPaise = amountPaise,
            type = WalletTransactionType.INCOME,
            category = category
        )
    }

    fun addExpense(
        title: String,
        amountPaise: Long,
        category: String = "Other"
    ) {
        addTransaction(
            title = title,
            amountPaise = amountPaise,
            type = WalletTransactionType.EXPENSE,
            category = category
        )
    }

    private fun addTransaction(
        title: String,
        amountPaise: Long,
        type: WalletTransactionType,
        category: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val fingerprint = "$title|$amountPaise|$type|$category"
                val request = pendingAddRequest?.takeIf { it.first == fingerprint }
                    ?: (fingerprint to java.util.UUID.randomUUID().toString()).also { pendingAddRequest = it }
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
                    isLoading = false,
                    errorMessage = e.message
                        ?: "Transaction save nahi ho paya."
                )
            }
        }
    }

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
     * Deletes a transaction and refunds or reverts wallet balance with optimistic UI update.
     */
    fun deleteTransaction(transactionId: String) {
        val previousTransactions = _uiState.value.transactions
        val previousBalance = _uiState.value.balancePaise
        // Optimistic UI update
        _uiState.value = _uiState.value.copy(
            transactions = previousTransactions.filter { it.id != transactionId }
        )

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
     * Restores a deleted transaction and re-applies wallet balance with optimistic UI update.
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
     * Updates an existing transaction with optimistic UI update.
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
            }
        )

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
