package com.invatech.pockit.ui.notification

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invatech.pockit.data.local.entity.NotificationEntity
import com.invatech.pockit.data.repository.NotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

// ---------------------------------------------------------------------------------------------
// Public models
// ---------------------------------------------------------------------------------------------

enum class NotificationFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    EXPENSES("Expenses"),
    SPACES("Spaces")
}

/** Day buckets used to group the list under sticky-style section headers. */
enum class NotificationSection(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This week"),
    EARLIER("Earlier")
}

/** Flat list model so the UI can render headers + rows from a single LazyColumn. */
@Immutable
sealed interface NotificationListItem {
    val key: String

    @Immutable
    data class Header(val section: NotificationSection) : NotificationListItem {
        override val key: String get() = "header_${section.name}"
    }

    @Immutable
    data class Entry(val notification: NotificationEntity) : NotificationListItem {
        override val key: String get() = notification.id
    }
}

@Immutable
data class NotificationsUiState(
    /** Headers + rows for the currently selected filter, newest first. */
    val listItems: List<NotificationListItem> = emptyList(),
    /** Number of notifications matching the selected filter. */
    val visibleCount: Int = 0,
    /** Number of notifications regardless of filter (drives "clear all"). */
    val totalCount: Int = 0,
    val unreadCount: Int = 0,
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val isLoading: Boolean = true,
    val hasError: Boolean = false
)

/** One-shot UI events (snackbars). Delivered exactly once, never replayed on rotation. */
sealed interface NotificationEvent {
    data class ItemRemoved(val id: String) : NotificationEvent
    object AllMarkedRead : NotificationEvent
    object AllCleared : NotificationEvent
    data class Error(val message: String) : NotificationEvent
}

// ---------------------------------------------------------------------------------------------
// Type helpers (shared by the filters here and the card styling in the screen)
// ---------------------------------------------------------------------------------------------

internal fun NotificationEntity.hasType(vararg keys: String): Boolean =
    keys.any { type.contains(it, ignoreCase = true) }

internal fun NotificationEntity.isSpaceType(): Boolean = hasType("space")

private fun NotificationEntity.matches(filter: NotificationFilter): Boolean = when (filter) {
    NotificationFilter.ALL -> true
    NotificationFilter.UNREAD -> !isRead
    NotificationFilter.EXPENSES -> hasType("expense", "udhar", "settle")
    NotificationFilter.SPACES -> hasType("space")
}

/**
 * Groups an already newest-first list into Today / Yesterday / This week / Earlier.
 * Pure function so it is trivial to unit test with a fixed [nowMillis].
 */
internal fun groupByDay(
    sortedNewestFirst: List<NotificationEntity>,
    nowMillis: Long
): List<NotificationListItem> {
    if (sortedNewestFirst.isEmpty()) return emptyList()

    val cal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayStart = cal.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStart = cal.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, -6)
    val weekStart = cal.timeInMillis

    val result = ArrayList<NotificationListItem>(sortedNewestFirst.size + 4)
    var current: NotificationSection? = null
    for (notification in sortedNewestFirst) {
        val ts = notification.timestamp
        val section = when {
            ts >= todayStart -> NotificationSection.TODAY
            ts >= yesterdayStart -> NotificationSection.YESTERDAY
            ts >= weekStart -> NotificationSection.THIS_WEEK
            else -> NotificationSection.EARLIER
        }
        if (section != current) {
            result += NotificationListItem.Header(section)
            current = section
        }
        result += NotificationListItem.Entry(notification)
    }
    return result
}

// ---------------------------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------------------------

class NotificationViewModel(
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private sealed interface Source {
        object Loading : Source
        data class Ready(val items: List<NotificationEntity>) : Source
        object Failed : Source
    }

    private val _selectedFilter = MutableStateFlow(NotificationFilter.ALL)
    val selectedFilter: StateFlow<NotificationFilter> = _selectedFilter.asStateFlow()

    /** Ids hidden from the UI while their delete is waiting out the undo window. */
    private val pendingDeletes = MutableStateFlow<Set<String>>(emptySet())
    private val pendingDeleteJobs = mutableMapOf<String, Job>()

    private val retryTick = MutableStateFlow(0)

    private val _events = Channel<NotificationEvent>(Channel.BUFFERED)
    val events: Flow<NotificationEvent> = _events.receiveAsFlow()

    /** Kept for any other screen that shows a badge from this ViewModel. */
    val unreadCount: StateFlow<Int> = repository.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val source: Flow<Source> = retryTick.flatMapLatest {
        repository.observeNotifications()
            .map<List<NotificationEntity>, Source> { Source.Ready(it) }
            .onStart { emit(Source.Loading) }
            .catch { error ->
                Log.e(TAG, "Failed to observe notifications", error)
                emit(Source.Failed)
            }
    }

    val uiState: StateFlow<NotificationsUiState> = combine(
        source,
        pendingDeletes,
        _selectedFilter
    ) { src, pending, filter ->
        when (src) {
            is Source.Loading -> NotificationsUiState(selectedFilter = filter, isLoading = true)
            is Source.Failed -> NotificationsUiState(
                selectedFilter = filter,
                isLoading = false,
                hasError = true
            )
            is Source.Ready -> {
                val visible = src.items
                    .filter { it.id !in pending }
                    .sortedByDescending { it.timestamp }
                val filtered = if (filter == NotificationFilter.ALL) {
                    visible
                } else {
                    visible.filter { it.matches(filter) }
                }
                NotificationsUiState(
                    listItems = groupByDay(filtered, System.currentTimeMillis()),
                    visibleCount = filtered.size,
                    totalCount = visible.size,
                    unreadCount = visible.count { !it.isRead },
                    selectedFilter = filter,
                    isLoading = false
                )
            }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), NotificationsUiState())

    init {
        viewModelScope.launch {
            try {
                repository.ensureInitialWelcomeNotification()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Welcome notification is cosmetic; never surface this to the user.
                Log.w(TAG, "Could not create welcome notification", e)
            }
        }
    }

    fun setFilter(filter: NotificationFilter) {
        _selectedFilter.value = filter
    }

    fun retry() {
        retryTick.update { it + 1 }
    }

    fun markAsRead(id: String) {
        launchSafely("Couldn't mark as read. Please try again.") {
            repository.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        launchSafely(
            failureMessage = "Couldn't mark all as read. Please try again.",
            onSuccess = { _events.trySend(NotificationEvent.AllMarkedRead) }
        ) {
            repository.markAllAsRead()
        }
    }

    /**
     * Soft delete: the row disappears immediately and the real delete is committed after
     * [UNDO_WINDOW_MS] unless [undoDelete] is called. If the process dies inside that window
     * nothing was deleted, which is the safe failure mode.
     */
    fun deleteNotification(id: String) {
        if (pendingDeleteJobs.containsKey(id)) return

        pendingDeletes.update { it + id }
        pendingDeleteJobs[id] = viewModelScope.launch {
            try {
                delay(UNDO_WINDOW_MS)
                repository.deleteNotification(id)
                // The id intentionally stays in [pendingDeletes]: removing it before the database
                // emits would flash the row back for a frame.
                pendingDeleteJobs.remove(id)
            } catch (e: CancellationException) {
                // Cancelled by undoDelete(), which already cleaned up its own state.
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete notification $id", e)
                pendingDeleteJobs.remove(id)
                pendingDeletes.update { it - id }
                _events.trySend(NotificationEvent.Error("Couldn't delete notification. It has been restored."))
            }
        }
        _events.trySend(NotificationEvent.ItemRemoved(id))
    }

    fun undoDelete(id: String) {
        pendingDeleteJobs.remove(id)?.cancel()
        pendingDeletes.update { it - id }
    }

    fun clearAll() {
        launchSafely(
            failureMessage = "Couldn't clear notifications. Please try again.",
            onSuccess = { _events.trySend(NotificationEvent.AllCleared) }
        ) {
            repository.clearAll()
        }
    }

    /** Debug helper. The screen only exposes it when `showDebugActions` is true. */
    fun sendTestNotification() {
        launchSafely("Couldn't create test notification.") {
            val sampleTypes = listOf(
                Triple("\uD83D\uDCB8 New Udhar Added", "Aman added ₹450 for 'Evening Snacks'", "friend_expense"),
                Triple("\uD83D\uDE80 Flatmates: New Expense", "Rohan added ₹1,200 for 'Electricity Bill'", "space_expense"),
                Triple("✅ Settle Up Received", "Priya settled up ₹500 with you", "friend_expense")
            )
            val sample = sampleTypes.random()
            repository.insertNotification(
                title = sample.first,
                body = sample.second,
                type = sample.third
            )
        }
    }

    private fun launchSafely(
        failureMessage: String,
        onSuccess: (() -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
                onSuccess?.invoke()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, failureMessage, e)
                _events.trySend(NotificationEvent.Error(failureMessage))
            }
        }
    }

    private companion object {
        const val TAG = "NotificationViewModel"
        const val STOP_TIMEOUT_MS = 5_000L
        const val UNDO_WINDOW_MS = 5_000L
    }
}