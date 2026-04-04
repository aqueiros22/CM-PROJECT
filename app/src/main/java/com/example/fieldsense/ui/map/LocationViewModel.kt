package com.example.fieldsense.ui.map

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fieldsense.data.remote.MapTilerFeature
import com.example.fieldsense.data.remote.MapTilerGeocodingService
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng

class LocationViewModel: ViewModel() {
    private val _location = mutableStateOf<LatLng?>(null)

    private val geocodingService = MapTilerGeocodingService()
    val location: State<LatLng?> = _location
    
    var isSearching by mutableStateOf(false)
        private set

    var isFetchingLocation by mutableStateOf(false)

    var searchResults by mutableStateOf<List<MapTilerFeature>>(emptyList())
        private set

    fun updateLocation(newLocation: LatLng) {
        _location.value = newLocation
        isFetchingLocation = false
    }

    fun startFetchingLocation() {
        isFetchingLocation = true
    }

    fun search(query: String) {
        viewModelScope.launch {
            isSearching = true
            searchResults = geocodingService.search(query)
            isSearching = false
        }
    }

    fun clearResults() {
        searchResults = emptyList()
    }
}