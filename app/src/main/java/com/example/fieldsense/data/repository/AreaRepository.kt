package com.example.fieldsense.data.repository

import android.util.Log
import com.example.fieldsense.data.local.AreaDao
import com.example.fieldsense.data.model.Area
import com.example.fieldsense.data.remote.FirestoreService
import kotlinx.coroutines.flow.Flow

class AreaRepository(
    private val areaDao: AreaDao,
    private val firestoreService: FirestoreService
) {
    fun getAreasForVisit(visitId: Int): Flow<List<Area>> = areaDao.getAreasForVisit(visitId)

    suspend fun insertArea(area: Area) {
        val generatedId = areaDao.insertArea(area)
        val syncedArea = area.copy(id = generatedId.toInt(), isSynced = true)

        try {
            firestoreService.uploadArea(syncedArea)
            areaDao.updateArea(syncedArea)
        } catch (e: Exception) {
            Log.e("AreaRepository", "Cloud sync failed, stored locally only", e)
        }
    }

    suspend fun deleteArea(area: Area){
        areaDao.deleteArea(area)
        try {
            firestoreService.deleteArea(area)
        } catch (e: Exception) {
            Log.e("AreaRepository", "Cloud delete failed", e)
        }
    }

    suspend fun updateArea(area: Area){
        val updatedArea = area.copy(isSynced = false)
        areaDao.updateArea(updatedArea)
        try {
            val syncedArea = updatedArea.copy(isSynced = true)
            firestoreService.uploadArea(syncedArea)
            areaDao.updateArea(syncedArea)
        } catch (e: Exception) {
            Log.e("AreaRepository", "Cloud update failed, stored locally only", e)
        }
    }

    suspend fun pullAreasFromServer(visitId: Int) {
        try {
            val remoteAreas = firestoreService.getAreasForVisit(visitId)
            remoteAreas.forEach { area ->
                areaDao.insertArea(area.copy(isSynced = true))
            }
        } catch (e: Exception) {
            Log.e("Sync", "Failed to pull areas for visit $visitId", e)
        }
    }

    suspend fun syncPendingAreas(userId: String) {
        val pendingAreas = areaDao.getUnsyncedAreas(userId)

        pendingAreas.forEach { area ->
            try {
                val syncedArea = area.copy(isSynced = true)
                firestoreService.uploadArea(syncedArea)
                areaDao.updateArea(syncedArea)
            }
            catch (e: Exception) {
                Log.e("Sync", "Ainda sem ligação para a área ${area.id}")
            }
        }
    }
}
