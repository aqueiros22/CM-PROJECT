package com.example.fieldsense.data.repository

import android.util.Log
import com.example.fieldsense.data.local.QuestionDao
import com.example.fieldsense.data.local.TemplateDao
import com.example.fieldsense.data.model.Question
import com.example.fieldsense.data.model.Template
import com.example.fieldsense.data.remote.FirestoreService
import kotlinx.coroutines.flow.Flow

class TemplateRepository  (
    private val templateDao: TemplateDao,
    private val questionDao: QuestionDao,
    private val firestoreService: FirestoreService
) {
    fun getTemplates(): Flow<List<Template>> =
        templateDao.getTemplates()

    suspend fun insertTemplate(template: Template) {
        val generatedId = templateDao.insertTemplate(template)
        val syncedTemplate = template.copy(id = generatedId.toInt(), isSynced = true)

        try {
            firestoreService.uploadTemplate(syncedTemplate)
            templateDao.updateTemplate(syncedTemplate)
        } catch (e: Exception) {
            Log.e("TemplateRepository", "Cloud sync failed, stored locally only", e)
        }
    }
    suspend fun deleteTemplate(template: Template) {
        templateDao.deleteTemplate(template)
        try {
            firestoreService.deleteTemplate(template)
        } catch (e: Exception) {
            Log.e("TemplateRepository", "Cloud delete failed", e)
        }
    }

    suspend fun updateTemplate(template: Template) {
        val updatedTemplate = template.copy(isSynced = false)
        templateDao.updateTemplate(updatedTemplate)
        try {
            val syncedTemplate = updatedTemplate.copy(isSynced = true)
            firestoreService.uploadTemplate(syncedTemplate)
            templateDao.updateTemplate(syncedTemplate)
        } catch (e: Exception) {
            Log.e("TemplateRepository", "Cloud update failed, stored locally only", e)
        }
    }

    suspend fun pullTemplatesFromServer() {
        try {
            val remoteTemplates = firestoreService.getAllTemplates()
            remoteTemplates.forEach { template ->
                templateDao.insertTemplate(template.copy(isSynced = true))
                val remoteQuestions = firestoreService.getQuestionsForTemplate(template.id)
                questionDao.deleteQuestionsForTemplate(template.id)
                questionDao.insertQuestions(remoteQuestions)
            }
        } catch (e: Exception) {
            Log.e("Sync", "Failed to pull templates", e)
        }
    }

    suspend fun syncPendingTemplates() {
        val pendingTemplates = templateDao.getUnsyncedTemplates()

        pendingTemplates.forEach { template ->
            try {
                val syncedTemplate = template.copy(isSynced = true)
                firestoreService.uploadTemplate(syncedTemplate)
                templateDao.updateTemplate(syncedTemplate)
                Log.d("Sync", "Template ${template.id} sincronizada com sucesso!")
            } catch (e: Exception) {
                Log.e("Sync", "Ainda sem ligação para o template ${template.id}")
            }
        }
    }
    suspend fun insertTemplateWithQuestions(template: Template, questions: List<Question>) {
        val templateId = templateDao.insertTemplate(template).toInt()
        Log.d("TemplateRepo", "Template inserida com id: $templateId")

        val questionsWithTemplateId = questions.mapIndexed { index, q ->
            q.copy(templateId = templateId, order = index)
        }
        questionDao.insertQuestions(questionsWithTemplateId)
        val persistedQuestions = questionDao.getQuestionsForTemplateOnce(templateId)
        Log.d("TemplateRepo", "Perguntas inseridas: ${questionsWithTemplateId.size}")

        val syncedTemplate = template.copy(id = templateId, isSynced = true)
        try {
            firestoreService.uploadTemplateWithQuestions(syncedTemplate, persistedQuestions)
            templateDao.updateTemplate(syncedTemplate)
            Log.d("TemplateRepo", "Sync com Firestore ok")
        } catch (e: Exception) {
            Log.e("TemplateRepository", "Cloud sync failed", e)
        }
    }

    suspend fun updateTemplateWithQuestions(template: Template, questions: List<Question>) {
        val updatedTemplate = template.copy(isSynced = false)

        templateDao.updateTemplate(updatedTemplate)
        questionDao.deleteQuestionsForTemplate(template.id)

        val updatedQuestions = questions.mapIndexed { index, q ->
            q.copy(
                templateId = template.id,
                order = index
            )
        }
        questionDao.insertQuestions(updatedQuestions)
        val persistedQuestions = questionDao.getQuestionsForTemplateOnce(template.id)
        try {
            val syncedTemplate = updatedTemplate.copy(isSynced = true)
            firestoreService.uploadTemplateWithQuestions(
                syncedTemplate,
                persistedQuestions
            )
            templateDao.updateTemplate(syncedTemplate)
        } catch (e: Exception) {
            Log.e("TemplateRepository", "Cloud update failed", e)
        }
    }
    fun getQuestionsForTemplate(templateId: Int): Flow<List<Question>> =
        questionDao.getQuestionsForTemplate(templateId)
}
