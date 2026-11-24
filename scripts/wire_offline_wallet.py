from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/invatech/pockit'
p=JAVA/'data/repository/WalletRepository.kt'
s=p.read_text()
s=s.replace('import kotlinx.coroutines.flow.map','import kotlinx.coroutines.flow.map\nimport kotlinx.coroutines.flow.combine\nimport com.invatech.pockit.data.network.NetworkMonitor\nimport com.invatech.pockit.data.local.entity.PendingWalletEntry')
s=s.replace('private val balances =', '''fun acceptSyncedBalance(uid: String, balance: Long) {
            balances.getOrPut(uid) { kotlinx.coroutines.flow.MutableStateFlow(balance) }.value = balance
            PockitApplication.instance.getSharedPreferences("wallet_balance", 0).edit().putLong(uid, balance).apply()
        }
        private val balances =''')
s=s.replace('fun observeBalance(): kotlinx.coroutines.flow.StateFlow<Long> = balanceState()', '''fun observeBalance(): Flow<Long> = combine(balanceState(), pendingDao.observe()) { balance, rows ->
        rows.fold(balance) { total, row -> Math.addExact(total, if (row.type == WalletTransactionType.INCOME) row.amountPaise else -row.amountPaise) }
    }
    suspend fun currentBalance(): Long = pendingDao.all().fold(balanceState().value) { total, row ->
        Math.addExact(total, if (row.type == WalletTransactionType.INCOME) row.amountPaise else -row.amountPaise)
    }''')
s=s.replace('private val localDao: WalletDao?', 'private val pendingDao = PockitApplication.instance.database.pendingWalletDao()\n    private val localDao: WalletDao?')
start=s.index('        return localDao?.observeTransactions()?.map');end=s.index('\n    /**',start)
s=s[:start]+'''        return combine(localDao?.observeTransactions() ?: kotlinx.coroutines.flow.flowOf(emptyList()), pendingDao.observe()) { cached, pending ->
            (cached.map { it.toWalletTransaction() } + pending.map { it.cached().toWalletTransaction() })
                .associateBy { it.id }.values.sortedByDescending { it.createdAt?.toDate()?.time ?: 0 }
        }
    }

    suspend fun getAllLocalTransactions(): List<WalletTransaction> {
        return (localDao?.getAllTransactions().orEmpty() + pendingDao.all().map { it.cached() })
            .associateBy { it.id }.values.sortedByDescending { it.createdAtMillis }.map { it.toWalletTransaction() }
    }
''' + s[end:]
s=s.replace('else snapshot?.getLong("balancePaise")?.let(::publishBalance)', 'else if (!com.invatech.pockit.data.offline.OfflineSyncManager.state.value.busy) snapshot?.getLong("balancePaise")?.let(::publishBalance)')
s=s.replace('val snapshot = walletRef(uid).get().await()', 'if (!NetworkMonitor.online.value) return@withLock currentBalance()\n        val snapshot = walletRef(uid).get().await()')
s=s.replace('        return try {\n            val query =', '        if (!NetworkMonitor.online.value) return@withLock getAllLocalTransactions()\n        return try {\n            val query =',1)
s=s.replace('''            list
        } catch (e: Exception)''','''            getAllLocalTransactions()
        } catch (e: Exception)''',1)
needle='''        val uid = userId()
        val wallet = walletRef(uid)
        val transactionRef = firestore'''
start=s.index(needle,s.index('suspend fun addTransaction('))
s=s[:start]+s[start:].replace(needle,'''        val uid = userId()
        if (!NetworkMonitor.online.value || pendingDao.all().isNotEmpty()) {
            check(!com.invatech.pockit.data.offline.OfflineSyncManager.state.value.busy) { "Your entries are syncing. Please try again in a moment." }
            val rows = pendingDao.all()
            val id = requestId ?: java.util.UUID.randomUUID().toString()
            val existing = rows.firstOrNull { it.id == id }
            if (existing == null) {
                check(rows.size < 100) { "You have 100 pending entries. Connect and sync before adding more." }
                val entry = PendingWalletEntry(id, title.trim(), amountPaise, type, category, occurredAtMillis)
                val projected = Math.addExact(currentBalance(), if (type == WalletTransactionType.INCOME) amountPaise else -amountPaise)
                check(projected in -com.invatech.pockit.domain.Money.MAX_AMOUNT..com.invatech.pockit.domain.Money.MAX_AMOUNT) { "The resulting balance is outside the supported range." }
                pendingDao.insert(entry)
            } else check(existing.title == title.trim() && existing.amountPaise == amountPaise && existing.type == type && existing.category == category)
            return@withLock currentBalance() to id
        }
        val wallet = walletRef(uid)
        val transactionRef = firestore''',1)
# Cloud mutations are explicit online operations; pending entries remain immutable until synced.
for signature in ['suspend fun setRecordedBalance(', 'suspend fun deleteTransaction(', 'suspend fun restoreTransaction(', 'suspend fun updateTransaction(']:
    start=s.index(signature); body=s.index('val uid = userId()',start)
    s=s[:body]+'''NetworkMonitor.requireOnline()
        check(pendingDao.all().isEmpty()) { "Sync your pending entries before editing saved records or the balance." }
        '''+s[body:]
p.write_text(s)

p=JAVA/'ui/home/HomeViewModel.kt';s=p.read_text().replace('walletRepository.observeBalance().value','walletRepository.currentBalance()');p.write_text(s)
p=JAVA/'data/offline/OfflineSyncManager.kt';s=p.read_text().replace('import com.google.firebase.auth.FirebaseAuth','import androidx.room.withTransaction\nimport com.google.firebase.auth.FirebaseAuth').replace('androidx.room.withTransaction(database)', 'database.withTransaction')
s=s.replace('if (owner == uid) mutable.value = mutable.value.copy(error = CommerceRepository.message(e))', '''if (owner == uid) {
                if ((e as? com.google.firebase.functions.FirebaseFunctionsException)?.code == com.google.firebase.functions.FirebaseFunctionsException.Code.FAILED_PRECONDITION)
                    prefs.edit().remove(uid).commit()
                mutable.value = mutable.value.copy(error = CommerceRepository.message(e))
            }''');p.write_text(s)

# Existing personal financial writes require internet; offline recording uses only the new wallet outbox.
for filename, signatures in {
 'FinanceRepository.kt':['suspend fun save(row: FinanceRecord) {','suspend fun contribute(row: FinanceRecord, amount: Long) {'],
 'BudgetRepository.kt':['        require(limitPaise in 1..Money.MAX_AMOUNT)','suspend fun deleteBudget(category: String) {'],
 'SubscriptionRepository.kt':['suspend fun addOrUpdateSubscription(subscription: SubscriptionEntity) {','suspend fun deleteSubscription(id: String) {']
}.items():
    p=JAVA/'data/repository'/filename;s=p.read_text()
    for target in signatures:
        assert target in s, target
        if target.startswith('        require'):
            s=s.replace(target,'        com.invatech.pockit.data.network.NetworkMonitor.requireOnline()\n'+target,1)
        else:s=s.replace(target,target+'\n        com.invatech.pockit.data.network.NetworkMonitor.requireOnline()',1)
    p.write_text(s)
print('Offline wallet outbox and online mutation gates wired.')
