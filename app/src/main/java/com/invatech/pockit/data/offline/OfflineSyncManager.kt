package com.invatech.pockit.data.offline

import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableOptions
import com.invatech.pockit.PockitApplication
import com.invatech.pockit.data.network.NetworkMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class OfflineSyncState(val count: Int = 0, val busy: Boolean = false, val error: String? = null)

/** Durable batches survive process death and retries. Only the server can authorize sync. */
object OfflineSyncManager {
    private val mutable = MutableStateFlow(OfflineSyncState())
    val state = mutable.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private var owner: String? = null
    private var observation: Job? = null
    private var initialized = false
    private val prefs get() = PockitApplication.instance.getSharedPreferences("offline_sync_batches", 0)

    fun initialize() {
        if (initialized) return
        initialized = true
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid == owner) return@addAuthStateListener
            owner = uid; observation?.cancel(); mutable.value = OfflineSyncState()
            if (uid != null) observation = scope.launch {
                val dao = PockitApplication.instance.database.pendingWalletDao()
                combine(dao.observe(), NetworkMonitor.online) { rows, online -> rows.size to online }
                    .collect { (count, online) ->
                    if (owner != uid) return@collect
                    mutable.value = mutable.value.copy(count = count)
                    if (count > 0 && online) sync()
                }
            }
        }
    }

    suspend fun sync() = mutex.withLock {
        val uid = owner ?: return@withLock
        if (!NetworkMonitor.online.value || FirebaseAuth.getInstance().currentUser?.uid != uid) return@withLock
        val database = PockitApplication.instance.database
        val pending = database.pendingWalletDao()
        val rows = pending.all()
        if (rows.isEmpty()) { prefs.edit().remove(uid).commit(); return@withLock }
        mutable.value = mutable.value.copy(busy = true, error = null)
        try {
            val batch = prefs.getString(uid, null)?.let { JSONObject(it) } ?: run {
                JSONObject().put("requestId", UUID.randomUUID().toString())
                    .put("ids", JSONArray(rows.take(100).map { it.id })).also {
                        check(prefs.edit().putString(uid, it.toString()).commit()) { "Could not prepare sync. Your entries are safe." }
                    }
            }
            val ids = batch.getJSONArray("ids").let { array -> (0 until array.length()).map { array.getString(it) } }
            val entries = rows.filter { it.id in ids }
            check(entries.size == ids.size) { "The pending sync changed. Your entries have been kept." }
            val options = HttpsCallableOptions.Builder().setLimitedUseAppCheckTokens(true).build()
            val response = FirebaseFunctions.getInstance("asia-south2")
                .getHttpsCallable("syncOfflineTransactions", options)
                .call(mapOf("requestId" to batch.getString("requestId"), "entries" to entries.map { it.payload() }))
                .await()
            val result = response.data as? Map<*, *> ?: error("Invalid sync response")
            check(FirebaseAuth.getInstance().currentUser?.uid == uid && owner == uid)
            // Keep accepted rows visible before removing their pending overlay. Account-scoped DB stays captured.
            database.withTransaction {
                database.walletDao().insertTransactions(entries.map { it.cached() })
                pending.delete(ids)
            }
            val balance = (result["balancePaise"] as Number).toLong()
            com.invatech.pockit.data.repository.WalletRepository.acceptSyncedBalance(uid, balance)
            prefs.edit().remove(uid).commit()
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            if (owner == uid) {
                if ((e as? com.google.firebase.functions.FirebaseFunctionsException)?.code == com.google.firebase.functions.FirebaseFunctionsException.Code.FAILED_PRECONDITION)
                    prefs.edit().remove(uid).commit()
                mutable.value = mutable.value.copy(error = syncMessage(e))
            }
        } finally {
            if (owner == uid) mutable.value = mutable.value.copy(busy = false, count = pending.all().size)
        }
    }

    private fun syncMessage(error: Exception): String {
        if (!NetworkMonitor.online.value) return "You’re offline. Your entries are safe on this phone."
        return when ((error as? FirebaseFunctionsException)?.code) {
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Sign in again to sync your saved entries."
            FirebaseFunctionsException.Code.UNAVAILABLE, FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                "Cloud sync is temporarily unavailable. Your entries are safe and will retry automatically."
            FirebaseFunctionsException.Code.ALREADY_EXISTS -> "A saved entry conflicts with a cloud record. Review it before retrying."
            else -> "Could not sync right now. Your entries are safe and will retry automatically."
        }
    }
}
