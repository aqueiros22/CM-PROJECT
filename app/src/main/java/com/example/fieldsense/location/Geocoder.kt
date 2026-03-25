package com.example.fieldsense.location

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

suspend fun getAddressFromLocation(context: Context, latitude: Double, longitude: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.getAddressLine(0) ?: "$latitude, $longitude"
            } else {

                "$latitude, $longitude"
            }
        } catch (e: Exception) {
            // Se falhar (ex: sem internet), devolve as coordenadas
            "$latitude, $longitude"
        }
    }
}