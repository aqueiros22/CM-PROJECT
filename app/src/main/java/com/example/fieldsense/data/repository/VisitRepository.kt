package com.example.fieldsense.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.fieldsense.data.local.*
import com.example.fieldsense.data.model.Visit
import com.example.fieldsense.data.remote.FirestoreService
import com.example.fieldsense.location.getAddressFromLocation
import kotlinx.coroutines.flow.Flow

class VisitRepository(
    private val visitDao: VisitDao,
    private val noteDao: NoteDao,
    private val areaDao: AreaDao,
    private val attachmentDao: AttachmentDao,
    private val checklistDao: ChecklistDao,
    private val templateDao: TemplateDao,
    private val questionDao: QuestionDao,
    private val firestoreService: FirestoreService,
    private val context: Context
) {
    val allVisits: Flow<List<Visit>> = visitDao.getAllVisits()

    fun getVisitsByUser(userId: String): Flow<List<Visit>> = visitDao.getActiveVisitsByUser(userId)

    fun getArchivedVisits(userId: String): Flow<List<Visit>> = visitDao.getArchivedVisitsByUser(userId)

    suspend fun insert(visit: Visit) {
        val generatedId = visitDao.insertVisit(visit)
        val localVisit = visit.copy(id = generatedId.toInt(), isSynced = false)
        if (isInternetAvailable()) {
            try {
                val syncedVisit = buildSyncedVisit(localVisit)
                firestoreService.uploadVisit(syncedVisit)
                visitDao.updateVisit(syncedVisit)
            } catch (e: Exception) {
                Log.e("Repository", "Cloud sync failed", e)
            }
        }
    }

    suspend fun update(visit: Visit) {
        visitDao.updateVisit(visit.copy(isSynced = false))
        if (isInternetAvailable()) {
            try {
                val syncedVisit = buildSyncedVisit(visit)
                firestoreService.uploadVisit(syncedVisit)
                visitDao.updateVisit(syncedVisit)
            } catch (e: Exception) {
                Log.e("Repository", "Cloud update failed", e)
            }
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

    suspend fun pullEverythingFromServer() {
        if (!isInternetAvailable()) {
            Log.d("Sync", "No internet available for sync")
            return
        }
        try {
            Log.d("Sync", "Starting full pull from Firestore...")
            
            // 1. Templates e Perguntas
            val remoteTemplates = firestoreService.getAllTemplates()
            Log.d("Sync", "Found ${remoteTemplates.size} templates")
            remoteTemplates.forEach { template ->
                val syncedTemplate = template.copy(isSynced = true)
                if (templateDao.existsById(syncedTemplate.id)) {
                    templateDao.updateTemplate(syncedTemplate)
                } else {
                    templateDao.insertTemplate(syncedTemplate)
                }

                val questions = firestoreService.getQuestionsForTemplate(template.id)
                questionDao.deleteQuestionsForTemplate(template.id)
                questionDao.insertQuestions(questions)
            }

            // 2. Visitas e dependentes
            val remoteVisits = firestoreService.getAllVisits()
            Log.d("Sync", "Found ${remoteVisits.size} visits")
            remoteVisits.forEach { visit ->
                val syncedVisit = visit.copy(isSynced = true)
                if (visitDao.existsById(syncedVisit.id)) {
                    visitDao.updateVisit(syncedVisit)
                } else {
                    visitDao.insertVisit(syncedVisit)
                }
                val vId = visit.id

                // Notas
                val notes = firestoreService.getNotesForVisit(vId)
                notes.forEach {
                    val syncedNote = it.copy(isSynced = true)
                    if (noteDao.existsById(syncedNote.id)) {
                        noteDao.updateNote(syncedNote)
                    } else {
                        noteDao.insertNote(syncedNote)
                    }
                }
                
                // Áreas
                val areas = firestoreService.getAreasForVisit(vId)
                areas.forEach {
                    val syncedArea = it.copy(isSynced = true)
                    if (areaDao.existsById(syncedArea.id)) {
                        areaDao.updateArea(syncedArea)
                    } else {
                        areaDao.insertArea(syncedArea)
                    }
                }
                
                // Attachments
                val attachments = firestoreService.getAttachmentsForVisit(vId)
                attachments.forEach {
                    val syncedAttachment = it.copy(isSynced = true)
                    if (attachmentDao.existsById(syncedAttachment.id)) {
                        attachmentDao.updateAttachment(syncedAttachment)
                    } else {
                        attachmentDao.insertAttachment(syncedAttachment)
                    }
                }
                
                // Checklists e Respostas
                val checklists = firestoreService.getChecklistsForVisit(vId)
                checklists.forEach { checklist ->
                    val syncedChecklist = checklist.copy(isSynced = true)
                    if (checklistDao.existsChecklistById(syncedChecklist.id)) {
                        checklistDao.updateChecklist(syncedChecklist)
                    } else {
                        checklistDao.insertChecklist(syncedChecklist)
                    }

                    val answers = firestoreService.getAnswersForChecklist(vId, checklist.id)
                    checklistDao.deleteAnswersForChecklist(checklist.id)
                    checklistDao.insertAnswers(answers)
                }
            }
            Log.d("Sync", "Full pull finished successfully")
        } catch (e: Exception) {
            Log.e("Sync", "Error pulling all data", e)
        }
    }

    suspend fun syncPendingVisits(userId: String) {
        val pendingVisits = visitDao.getUnsyncedVisits(userId)
        pendingVisits.forEach { visit ->
            try {
                val syncedVisit = buildSyncedVisit(visit)
                firestoreService.uploadVisit(syncedVisit)
                visitDao.updateVisit(syncedVisit)
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
            if (!translatedAddress.any { it.isDigit() } || translatedAddress.contains(",")) {
                locationToSave = translatedAddress
            }
        }
        return visit.copy(location = locationToSave, isSynced = true)
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun extractCoordinates(locationText: String): Pair<Double, Double>? {
        return try {
            val parts = locationText.replace(" (Last known)", "").trim().split(",")
            if (parts.size == 2) Pair(parts[0].trim().toDouble(), parts[1].trim().toDouble()) else null
        } catch (e: Exception) { null }
    }
}
