package com.example.fieldsense

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import org.maplibre.android.geometry.LatLng

class LocationViewModel: ViewModel() {
    private val _location = mutableStateOf<LatLng?>( null)
    val location: State<LatLng?> = _location

    fun updateLocation( newLocation: LatLng){
        _location.value = newLocation
    }

    fun search(query: String) {}

}