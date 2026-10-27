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

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadWallet()
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
                    balancePaise = balance,
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
                walletRepository.addTransaction(
                    title = title,
                    amountPaise = amountPaise,
                    type = type,
                    category = category
                )

                loadWallet()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                        ?: "Transaction save nahi ho paya."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
