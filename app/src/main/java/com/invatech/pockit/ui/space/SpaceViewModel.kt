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
        onComplete: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val spaceId = repository.createSpace(name, emoji, themeIndex)
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
}
