package com.example.fieldsense

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.maplibre.android.geometry.LatLng

class LocationViewModel: ViewModel() {
    private val _location = mutableStateOf<LatLng?>( null)

    private val geocodingService = MapTilerGeocodingService()
    val location: State<LatLng?> = _location
    var isSearching by mutableStateOf(false)
        private set

    var searchResults by mutableStateOf<List<MapTilerFeature>>(emptyList())
        private set

    fun updateLocation( newLocation: LatLng){
        _location.value = newLocation
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