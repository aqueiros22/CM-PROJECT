package com.example.fieldsense.ui.areas

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fieldsense.data.model.Area
import com.example.fieldsense.data.repository.AreaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.maplibre.spatialk.geojson.Position

class AreaViewModel(private val repository: AreaRepository) : ViewModel() {

    private val _userId = MutableStateFlow<String?>(null)
    private val areasCache = mutableMapOf<Int, StateFlow<List<Area>>>()

    val currentPoints = mutableStateListOf<Position>()

    fun setUserId(userId: String) {
        _userId.value = userId
    }

    fun getAreasForVisit(visitId: Int): StateFlow<List<Area>> {
        return areasCache.getOrPut(visitId) {
            repository.getAreasForVisit(visitId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
                )
        }
    }

    fun insertArea(visitId: Int, points: String) {
        viewModelScope.launch {
            insertAreaSuspend(visitId, points)
        }
    }

    suspend fun insertAreaSuspend(visitId: Int, points: String) {
        val uid = _userId.value ?: ""
        repository.insertArea(
            Area(
                userId = uid,
                visitId = visitId,
                points = points,
                isSynced = false
            )
        )
    }

    fun deleteArea(area: Area) {
        viewModelScope.launch {
            repository.deleteArea(area)
        }
    }

    fun syncPendingAreas() {
        viewModelScope.launch {
            _userId.value?.let { uid ->
                repository.syncPendingAreas(uid)
            }
        }
    }

    fun onNetworkRestored() {
        syncPendingAreas()
    }

    fun updateArea(area: Area) {
        viewModelScope.launch {
            repository.updateArea(area)
        }
    }

}

class AreaViewModelFactory(private val repository: AreaRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AreaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AreaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
