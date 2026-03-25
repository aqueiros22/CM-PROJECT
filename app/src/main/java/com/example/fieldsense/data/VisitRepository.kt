package com.example.fieldsense.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.fieldsense.getAddressFromLocation
import kotlinx.coroutines.flow.Flow

class VisitRepository(
    private val visitDao: VisitDao,
    private val noteDao: NoteDao,
    private val firestoreService: FirestoreService,
    private val context: Context
) {
    val allVisits: Flow<List<Visit>> = visitDao.getAllVisits()

    fun getVisitsByUser(userId: String): Flow<List<Visit>> = visitDao.getVisitsByUser(userId)

    suspend fun insert(visit: Visit) {
        val generatedId = visitDao.insertVisit(visit)
        val localVisit = visit.copy(id = generatedId.toInt(), isSynced = false)

        if (!isInternetAvailable()) {
            return
        }

        try {
            val syncedVisit = buildSyncedVisit(localVisit)
            firestoreService.uploadVisit(syncedVisit)
            visitDao.updateVisit(syncedVisit)
        } catch (e: Exception) {
            Log.e("Repository", "Cloud sync failed, stored locally only", e)
        }
    }

    suspend fun update(visit: Visit) {
        visitDao.updateVisit(visit.copy(isSynced = false))

        if (!isInternetAvailable()) {
            return
        }

        try {
            val syncedVisit = buildSyncedVisit(visit)
            firestoreService.uploadVisit(syncedVisit)
            visitDao.updateVisit(syncedVisit)
        } catch (e: Exception) {
            Log.e("Repository", "Cloud update sync failed", e)
        }
    }

    suspend fun delete(visitId: Int) {
        // Obter todas as notas associadas a esta visita antes de as apagar localmente
        val notesToDelete = noteDao.getNotesForVisitSync(visitId)

        // Apagar visita localmente (o Cascade Delete no Room tratará das notas localmente)
        visitDao.deleteVisitById(visitId)

        try {
            // Apagar cada nota no Firestore
            notesToDelete.forEach { note ->
                firestoreService.deleteNote(note)
            }
            // Apagar a visita no Firestore
            firestoreService.deleteVisit(visitId)
        } catch (e: Exception) {
            Log.e("Repository", "Cloud delete failed", e)
        }
    }

    suspend fun syncPendingVisits(userId: String) {
        val pendingVisits = visitDao.getUnsyncedVisits(userId)

        pendingVisits.forEach { visit ->
            try {
                val syncedVisit = buildSyncedVisit(visit)

                firestoreService.uploadVisit(syncedVisit)
                visitDao.updateVisit(syncedVisit)

                Log.d("Sync", "Visit ${visit.id} synced and translated successfully!")
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync visit ${visit.id}", e)
            }
        }
    }

    private suspend fun buildSyncedVisit(visit: Visit): Visit {
        var locationToSave = visit.location

        val coords = extractCoordinates(visit.location)
        if (coords != null) {
            val translatedAddress = getAddressFromLocation(context, coords.first, coords.second)

            // Keep translated address when geocoder resolves coordinates.
            if (!translatedAddress.any { it.isDigit() } || translatedAddress.contains(",")) {
                locationToSave = translatedAddress
            }
        }

        return visit.copy(location = locationToSave, isSynced = true)
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun extractCoordinates(locationText: String): Pair<Double, Double>? {
        return try {
            val cleanText = locationText.replace(" (Last known)", "").trim()
            val parts = cleanText.split(",")

            if (parts.size == 2) {
                val lat = parts[0].trim().toDouble()
                val lon = parts[1].trim().toDouble()
                Pair(lat, lon)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
