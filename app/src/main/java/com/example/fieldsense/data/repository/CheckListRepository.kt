package com.example.fieldsense.data.repository

import android.util.Log
import com.example.fieldsense.data.local.ChecklistDao
import com.example.fieldsense.data.model.Answer
import com.example.fieldsense.data.model.VisitChecklist
import com.example.fieldsense.data.remote.FirestoreService
import kotlinx.coroutines.flow.Flow

class ChecklistRepository(
    private val checklistDao: ChecklistDao,
    private val firestoreService: FirestoreService
) {
    fun getChecklistsForVisit(visitId: Int): Flow<List<VisitChecklist>> =
        checklistDao.getChecklistsForVisit(visitId)

    fun getAnswersForChecklist(checklistId: Int): Flow<List<Answer>> =
        checklistDao.getAnswersForChecklist(checklistId)

    suspend fun insertChecklistWithAnswers(checklist: VisitChecklist, answers: List<Answer>) {
        val generatedId = checklistDao.insertChecklist(checklist).toInt()
        val savedChecklist = checklist.copy(id = generatedId)

        val answersWithChecklistId = answers.map { it.copy(checklistId = generatedId) }
        checklistDao.insertAnswers(answersWithChecklistId)

        try {
            firestoreService.uploadChecklistWithAnswers(savedChecklist, answersWithChecklistId)
            checklistDao.updateChecklist(savedChecklist.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e("ChecklistRepository", "Cloud sync failed, stored locally only", e)
        }
    }

    suspend fun deleteChecklist(checklist: VisitChecklist) {
        checklistDao.deleteAnswersForChecklist(checklist.id)
        checklistDao.deleteChecklist(checklist)
        try {
            firestoreService.deleteChecklist(checklist)
        } catch (e: Exception) {
            Log.e("ChecklistRepository", "Cloud delete failed", e)
        }
    }

    suspend fun pullChecklistsFromServer(visitId: Int) {
        try {
            val remoteChecklists = firestoreService.getChecklistsForVisit(visitId)
            remoteChecklists.forEach { checklist ->
                checklistDao.insertChecklist(checklist.copy(isSynced = true))
                val remoteAnswers = firestoreService.getAnswersForChecklist(visitId, checklist.id)
                checklistDao.insertAnswers(remoteAnswers)
            }
        } catch (e: Exception) {
            Log.e("Sync", "Failed to pull checklists for visit $visitId", e)
        }
    }

    suspend fun syncPendingChecklists() {
        val pending = checklistDao.getUnsyncedChecklists()
        pending.forEach { checklist ->
            try {
                val syncedChecklist = checklist.copy(isSynced = true)
                checklistDao.updateChecklist(syncedChecklist)
                Log.d("Sync", "Checklist ${checklist.id} sincronizada!")
            } catch (e: Exception) {
                Log.e("Sync", "Sync falhou para checklist ${checklist.id}")
            }
        }
    }
}
