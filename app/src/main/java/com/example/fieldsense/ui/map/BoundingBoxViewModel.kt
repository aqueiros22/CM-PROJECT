package com.example.fieldsense.ui.map

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.maplibre.android.geometry.LatLng
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position


data class BoundingBoxState(
    val point1: LatLng? = null,
    val point2: LatLng? = null,
) {
    val isComplete get() = point1 != null && point2 != null

    fun toPolygonCoords(): List<List<Double>>? {
        val p1 = point1 ?: return null
        val p2 = point2 ?: return null
        val minLat = minOf(p1.latitude, p2.latitude)
        val maxLat = maxOf(p1.latitude, p2.latitude)
        val minLng = minOf(p1.longitude, p2.longitude)
        val maxLng = maxOf(p1.longitude, p2.longitude)
        return listOf(
            listOf(minLng, minLat),
            listOf(maxLng, minLat),
            listOf(maxLng, maxLat),
            listOf(minLng, maxLat),
            listOf(minLng, minLat) // close ring
        )
    }

    fun toBoundingBox(): BoundingBox? {
        val p1 = point1 ?: return null
        val p2 = point2 ?: return null
        return BoundingBox(
            southwest = Position(minOf(p1.longitude, p2.longitude), minOf(p1.latitude, p2.latitude)),
            northeast = Position(maxOf(p1.longitude, p2.longitude), maxOf(p1.latitude, p2.latitude))
        )
    }

    fun getPointsGeoJson(): GeoJsonData {
        val points = listOfNotNull(point1, point2)
        val features = points.joinToString(",") { p ->
            """{"type":"Feature","geometry":{"type":"Point","coordinates":[${p.longitude},${p.latitude}]}}"""
        }
        return GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[$features]}""")
    }
}


class BoundingBoxViewModel : ViewModel() {
    var state by mutableStateOf(BoundingBoxState())
        private set
    fun onMapTap(latLng: LatLng) {
        state = when {
            state.point1 == null -> state.copy(point1 = latLng)
            state.point2 == null -> state.copy(point2 = latLng)
            else -> BoundingBoxState() // reset and start over
        }
        Log.d("PolygonState", "state: P1 ${state.point1}, P2 ${state.point2} ")
    }

    fun reset() { state = BoundingBoxState() }

}