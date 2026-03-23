package com.example.fieldsense


import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MapTilerResponse(
    val features: List<MapTilerFeature>
)

@Serializable
data class MapTilerFeature(
    @SerialName("place_name") val placeName: String,
    val center: List<Double>  // [longitude, latitude]
)

class MapTilerGeocodingService {
    private val apiKey = BuildConfig.MAPTILER_API_KEY
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun search(query: String): List<MapTilerFeature> {
        return try {
            val response: MapTilerResponse = client.get(
                "https://api.maptiler.com/geocoding/${query}.json"
            ) {
                parameter("key", apiKey)
                parameter("limit", 5)
                parameter("language", "pt") // change to your language
            }.body()
            Log.d("MapTilerQ", "Query: $query")
            Log.d("MapTiler", "Response: $response")
            response.features
        } catch (e: Exception) {
            emptyList()
        }
    }
}