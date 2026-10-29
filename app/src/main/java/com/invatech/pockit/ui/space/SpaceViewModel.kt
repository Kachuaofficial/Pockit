package com.invatech.pockit.ui.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invatech.pockit.data.repository.SpaceRepository
import com.invatech.pockit.ui.space.model.JoinResult
import com.invatech.pockit.ui.space.model.Space
import com.invatech.pockit.ui.space.model.SpaceExpense
import com.invatech.pockit.ui.space.util.Settlement
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Production SpaceViewModel connected to Firebase Firestore.
 * Provides real-time synchronization of spaces, members, expenses, and settlements across devices.
 */
class SpaceViewModel(
    private val repository: SpaceRepository = SpaceRepository()
) : ViewModel() {

    private val _spaces = MutableStateFlow<List<Space>>(emptyList())
    val spaces: StateFlow<List<Space>> = _spaces.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val expenseJobs = mutableMapOf<String, Job>()
    private val expensesBySpace = mutableMapOf<String, List<SpaceExpense>>()
    private var rawSpacesList: List<Space> = emptyList()

    init {
        observeSpaces()
    }

    private fun observeSpaces() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.observeSpaces().collectLatest { spacesList ->
                    rawSpacesList = spacesList

                    // Cancel listeners for deleted/removed spaces
                    val currentSpaceIds = spacesList.map { it.id }.toSet()
                    val removedIds = expenseJobs.keys - currentSpaceIds
                    removedIds.forEach { id ->
                        expenseJobs.remove(id)?.cancel()
                        expensesBySpace.remove(id)
                    }

                    // Attach real-time expense listener for each space
                    spacesList.forEach { space ->
                        if (!expenseJobs.containsKey(space.id)) {
                            expenseJobs[space.id] = viewModelScope.launch {
                                repository.observeExpenses(space.id).collectLatest { expenses ->
                                    expensesBySpace[space.id] = expenses
                                    updateMergedSpaces()
                                }
                            }
                        }
                    }

                    updateMergedSpaces()
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    private fun updateMergedSpaces() {
        _spaces.value = rawSpacesList.map { space ->
            val expenses = expensesBySpace[space.id] ?: space.expenses
            space.copy(expenses = expenses)
        }
    }

    fun createSpace(
        name: String,
        emoji: String,
        themeIndex: Int,
        type: com.invatech.pockit.ui.space.model.SpaceType = com.invatech.pockit.ui.space.model.SpaceType.BILL_SPLIT,
        onComplete: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val spaceId = repository.createSpace(name, emoji, themeIndex, type)
                onComplete(spaceId)
            } catch (e: Exception) {
                // Error handling fallback
            }
        }
    }

    suspend fun joinSpace(code: String): JoinResult {
        return repository.joinSpace(code)
    }

    fun addMember(spaceId: String, name: String) {
        viewModelScope.launch {
            repository.addMember(spaceId, name)
        }
    }

    fun addExpense(
        spaceId: String,
        title: String,
        amount: Double,
        paidById: String,
        shares: Map<String, Double>
    ) {
        viewModelScope.launch {
            repository.addExpense(spaceId, title, amount, paidById, shares)
        }
    }

    fun settle(spaceId: String, settlement: Settlement) {
        viewModelScope.launch {
            repository.settle(spaceId, settlement)
        }
    }

    /**
     * Confirms a received settlement payment with optimistic UI update.
     */
    fun confirmSettlement(spaceId: String, expenseId: String, onError: (String) -> Unit = {}) {
        val currentExpenses = expensesBySpace[spaceId] ?: emptyList()
        expensesBySpace[spaceId] = currentExpenses.map {
            if (it.id == expenseId) it.copy(isConfirmed = true) else it
        }
        updateMergedSpaces()

        viewModelScope.launch {
            val result = repository.confirmSettlement(spaceId, expenseId)
            result.onFailure { error ->
                expensesBySpace[spaceId] = currentExpenses
                updateMergedSpaces()
                onError(error.message ?: "Failed to confirm payment")
            }
        }
    }

    /**
     * Declines or rejects an unconfirmed settlement with optimistic UI update.
     */
    fun rejectSettlement(spaceId: String, expenseId: String, onError: (String) -> Unit = {}) {
        val currentExpenses = expensesBySpace[spaceId] ?: emptyList()
        expensesBySpace[spaceId] = currentExpenses.filterNot { it.id == expenseId }
        updateMergedSpaces()

        viewModelScope.launch {
            val result = repository.rejectSettlement(spaceId, expenseId)
            result.onFailure { error ->
                expensesBySpace[spaceId] = currentExpenses
                updateMergedSpaces()
                onError(error.message ?: "Failed to decline payment")
            }
        }
    }

    /**
     * Deletes an expense from the space in Firestore with optimistic UI update.
     */
    fun deleteExpense(spaceId: String, expenseId: String) {
        val currentExpenses = expensesBySpace[spaceId] ?: emptyList()
        // Optimistic update for instantaneous 60/120fps UI response
        expensesBySpace[spaceId] = currentExpenses.filter { it.id != expenseId }
        updateMergedSpaces()

        viewModelScope.launch {
            try {
                repository.deleteExpense(spaceId, expenseId)
            } catch (e: Exception) {
                // Rollback on failure
                expensesBySpace[spaceId] = currentExpenses
                updateMergedSpaces()
            }
        }
    }

    /**
     * Restores a deleted expense document back to Firestore with optimistic UI update.
     */
    fun restoreExpense(spaceId: String, expense: SpaceExpense) {
        val currentExpenses = expensesBySpace[spaceId] ?: emptyList()
        // Optimistic update
        expensesBySpace[spaceId] = (listOf(expense) + currentExpenses.filter { it.id != expense.id })
            .sortedByDescending { it.dateLabel }
        updateMergedSpaces()

        viewModelScope.launch {
            try {
                repository.restoreExpense(spaceId, expense)
            } catch (e: Exception) {
                // Rollback on failure
                expensesBySpace[spaceId] = currentExpenses
                updateMergedSpaces()
            }
        }
    }

    /**
     * Renames a space with optimistic UI update.
     */
    fun renameSpace(spaceId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return

        rawSpacesList = rawSpacesList.map { space ->
            if (space.id == spaceId) space.copy(name = trimmed) else space
        }
        updateMergedSpaces()

        viewModelScope.launch {
            try {
                repository.renameSpace(spaceId, trimmed)
            } catch (e: Exception) {
                // Real-time listener will resync state if error
            }
        }
    }

    /**
     * Removes a member from a space (only creator can do this) with optimistic UI update.
     */
    fun removeMember(spaceId: String, memberId: String, onError: (String) -> Unit = {}) {
        val originalList = rawSpacesList
        rawSpacesList = rawSpacesList.map { space ->
            if (space.id == spaceId) {
                space.copy(members = space.members.filterNot { it.id == memberId })
            } else space
        }
        updateMergedSpaces()

        viewModelScope.launch {
            val result = repository.removeMember(spaceId, memberId)
            result.onFailure { error ->
                rawSpacesList = originalList
                updateMergedSpaces()
                onError(error.message ?: "Failed to remove member")
            }
        }
    }

    /**
     * Deletes a space and all its expenses with optimistic UI update.
     */
    fun deleteSpace(spaceId: String, onDeleted: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val originalList = rawSpacesList
        val originalExpenses = expensesBySpace[spaceId]

        rawSpacesList = rawSpacesList.filterNot { it.id == spaceId }
        updateMergedSpaces()

        expenseJobs.remove(spaceId)?.cancel()
        expensesBySpace.remove(spaceId)

        viewModelScope.launch {
            val result = repository.deleteSpace(spaceId)
            result.onSuccess {
                onDeleted()
            }.onFailure { error ->
                rawSpacesList = originalList
                if (originalExpenses != null) {
                    expensesBySpace[spaceId] = originalExpenses
                }
                updateMergedSpaces()
                onError(error.message ?: "Failed to delete space")
            }
        }
    }
}
