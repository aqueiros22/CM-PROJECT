package com.example.fieldsense.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VisitViewModel(private val repository: VisitRepository) : ViewModel() {

    val visits = repository.allVisits.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun insertVisit(code: String, name: String, date: String, location: String) {
        viewModelScope.launch {
            val visit = Visit(code = code, name = name, date = date, location = location)
            repository.insert(visit)
        }
    }

    fun updateVisit(visit: Visit) {
        viewModelScope.launch {
            repository.update(visit)
        }
    }

    fun deleteVisit(visitId: Int) {
        viewModelScope.launch {
            repository.delete(visitId)
        }
    }

    fun syncPendingVisits() {
        viewModelScope.launch {
            repository.syncPendingVisits()
        }
    }

    fun onNetworkRestored() {
        viewModelScope.launch {
            repository.syncPendingVisits()
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
