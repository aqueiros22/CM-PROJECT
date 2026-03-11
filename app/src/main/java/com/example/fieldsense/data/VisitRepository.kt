package com.example.fieldsense.data

import android.util.Log
import kotlinx.coroutines.flow.Flow

class VisitRepository(
    private val visitDao: VisitDao,
    private val firestoreService: FirestoreService
) {
    val allVisits: Flow<List<Visit>> = visitDao.getAllVisits()

    suspend fun insert(visit: Visit) {

        val generatedId = visitDao.insertVisit(visit)
        val syncedVisit = visit.copy(id = generatedId.toInt(),
            isSynced = true
        )

        try {
            firestoreService.uploadVisit(syncedVisit)
            visitDao.insertVisit(syncedVisit)
        } catch (e: Exception) {
            Log.e("Repository", "Cloud sync failed, stored locally only", e)
        }
    }

    suspend fun delete(visitId: Int) {
        visitDao.deleteVisitById(visitId)
        try {
            firestoreService.deleteVisit(visitId)
        } catch (e: Exception) {
            Log.e("Repository", "Cloud delete failed", e)
        }
    }
}