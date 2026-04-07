package com.example.fieldsense.ui.visits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fieldsense.data.model.Visit
import com.example.fieldsense.data.repository.VisitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class VisitViewModel(private val repository: VisitRepository) : ViewModel() {

    private val _userId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val visits = _userId.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(emptyList<Visit>())
        } else {
            repository.getVisitsByUser(uid)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val archivedVisits = _userId.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(emptyList<Visit>())
        } else {
            repository.getArchivedVisits(uid)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun setUserId(userId: String) {
        _userId.value = userId
    }

    fun insertVisit(userId: String, code: String, name: String, date: String, location: String) {
        viewModelScope.launch {
            val visit =
                Visit(userId = userId, code = code, name = name, date = date, location = location)
            repository.insert(visit)
        }
    }

    fun updateVisit(visit: Visit) {
        viewModelScope.launch {
            repository.update(visit)
        }
    }

    fun archiveVisit(visit: Visit) {
        viewModelScope.launch {
            repository.update(visit.copy(isArchived = true, isSynced = false))
        }
    }

    fun unarchiveVisit(visit: Visit) {
        viewModelScope.launch {
            repository.update(visit.copy(isArchived = false, isSynced = false))
        }
    }

    fun deleteVisit(visitId: Int) {
        viewModelScope.launch {
            repository.delete(visitId)
        }
    }

    fun syncPendingVisits() {
        viewModelScope.launch {
            _userId.value?.let { uid ->
                repository.syncPendingVisits(uid)
            }
        }
    }

    fun pullEverything() {
        viewModelScope.launch {
            repository.pullEverythingFromServer()
        }
    }

    fun onNetworkRestored() {
        viewModelScope.launch {
            _userId.value?.let { uid ->
                repository.syncPendingVisits(uid)
            }
            repository.pullEverythingFromServer()
        }
    }
}
class VisitViewModelFactory(private val repository: VisitRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VisitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VisitViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
