package com.invatech.pockit.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.invatech.pockit.PockitApplication
import com.invatech.pockit.data.local.database.*
import com.invatech.pockit.data.repository.WalletRepository
import com.invatech.pockit.domain.Money
import com.invatech.pockit.ui.home.model.WalletTransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class FinanceViewModel : ViewModel() {
    private val owner = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val dao = FinanceDatabase.get(PockitApplication.instance).records()
    private val repo = com.invatech.pockit.data.repository.FinanceRepository(owner)
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val syncGeneration = MutableStateFlow(0)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val records = syncGeneration.flatMapLatest { repo.observe().catch { _error.value = "Cloud sync failed: ${it.message}" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pending = records.map { it.filter { row -> row.kind == "UPI" && row.status == "PENDING" } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val ledger = records.map { it.filter { row -> row.kind == "UDHAR" } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val goals = records.map { it.filter { row -> row.kind == "GOAL" } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun retrySync() { _error.value = null; syncGeneration.value++ }
    fun clearError() { _error.value = null }
    private fun perform(action: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                check(owner.isNotBlank() && FirebaseAuth.getInstance().currentUser?.uid == owner) { "Please sign in again." }
                action()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _error.value = e.message ?: "Unable to save. Please try again."
            } finally { _busy.value = false }
        }
    }
    fun approve(record: FinanceRecord, name: String, amount: Long, direction: String, category: String) = perform {
        require(record.owner == owner && amount in 1..Money.MAX_AMOUNT && name.isNotBlank())
        WalletRepository().addTransaction(name, amount, WalletTransactionType.valueOf(direction), category, "upi_${record.id}", record.occurredAt)
        repo.status(record, "APPROVED")
    }
    fun reject(record: FinanceRecord) = perform { repo.status(record, "REJECTED") }
    fun addUdhar(name: String, amount: Long, direction: String, note: String, onSaved: () -> Unit) = perform {
        require(name.isNotBlank() && amount in 1..Money.MAX_AMOUNT)
        require(direction in listOf("LENT", "BORROWED", "RECEIVED", "REPAID"))
        val canonicalName = name.trim().replace(Regex("\\s+"), " ")
        val existing = records.value.filter { it.kind == "UDHAR" }.filter { it.name.equals(canonicalName, true) }
        val balance = udharBalance(existing)
        if (direction == "RECEIVED") require(balance > 0 && amount <= balance) { "Repayment exceeds the amount owed to you." }
        if (direction == "REPAID") require(balance < 0 && amount <= -balance) { "Repayment exceeds what you owe." }
        repo.save(FinanceRecord(UUID.randomUUID().toString(), owner, "UDHAR", existing.firstOrNull()?.name ?: canonicalName,
            amount, direction, note = note.trim(), status = "SAVED"))
        onSaved()
    }
    fun deleteEntry(record: FinanceRecord) = perform { require(record.owner == owner); repo.status(record, "DELETED") }
    fun addGoal(name: String, target: Long, onSaved: () -> Unit) = perform {
        require(name.isNotBlank() && target in 1..Money.MAX_AMOUNT)
        repo.save(FinanceRecord(UUID.randomUUID().toString(), owner, "GOAL", name.trim(), target, "0", status = "SAVED"))
        onSaved()
    }
    fun contribute(goal: FinanceRecord, amount: Long, onSaved: () -> Unit) = perform {
        require(amount > 0 && goal.owner == owner)
        repo.contribute(goal, amount)
        onSaved()
    }
}

fun udharBalance(rows: List<FinanceRecord>): Long = Money.sum(rows.map {
    if (it.direction == "LENT" || it.direction == "REPAID") it.amountPaise else -it.amountPaise
})
