package com.example.fieldsense.data.remote

import com.example.fieldsense.data.model.Answer
import com.example.fieldsense.data.model.Area
import com.example.fieldsense.data.model.Note
import com.example.fieldsense.data.model.Visit
import com.example.fieldsense.data.model.Attachment
import com.example.fieldsense.data.model.Question
import com.example.fieldsense.data.model.Template
import com.example.fieldsense.data.model.VisitChecklist
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private fun getUserVisitsCollection() = auth.currentUser?.uid?.let { uid ->
        db.collection("users").document(uid).collection("visits")
    }

    suspend fun uploadVisit(visit: Visit) {
        getUserVisitsCollection()?.document(visit.id.toString())?.set(visit)?.await()
    }

    suspend fun deleteVisit(visitId: Int) {
        getUserVisitsCollection()?.document(visitId.toString())?.delete()?.await()
    }

    private fun getNotesCollection(visitId: Int) =
        getUserVisitsCollection()?.document(visitId.toString())?.collection("notes")

    suspend fun uploadNote(note: Note) {
        getNotesCollection(note.visitId)?.document(note.id.toString())?.set(note)?.await()
    }

    suspend fun deleteNote(note: Note) {
        getNotesCollection(note.visitId)?.document(note.id.toString())?.delete()?.await()
    }
    private fun getAttachmentsCollection(visitId: Int) =
        getUserVisitsCollection()?.document(visitId.toString())?.collection("attachments")

    private fun getTemplatesCollection() = auth.currentUser?.uid?.let { uid ->
        db.collection("users").document(uid).collection("templates")
    }

    suspend fun uploadAttachment(attachment: Attachment) {
        getAttachmentsCollection(attachment.visitId)
            ?.document(attachment.id.toString())
            ?.set(attachment)
            ?.await()
    }

    suspend fun deleteAttachment(attachment: Attachment) {
        getAttachmentsCollection(attachment.visitId)
            ?.document(attachment.id.toString())
            ?.delete()
            ?.await()
    }

    suspend fun uploadTemplate(template: Template) {
        getTemplatesCollection()
            ?.document(template.id.toString())
            ?.set(template)
            ?.await()
    }

    suspend fun deleteTemplate(template: Template) {
        getTemplatesCollection()
            ?.document(template.id.toString())
            ?.delete()
            ?.await()
    }
    suspend fun uploadTemplateWithQuestions(template: Template, questions: List<Question>) {
        val templateRef = getTemplatesCollection()?.document(template.id.toString())


        templateRef?.set(template)?.await()

        val questionsCollection = templateRef?.collection("questions")

        val existingQuestions = questionsCollection?.get()?.await()
        for (doc in existingQuestions?.documents!!) {
            doc.reference.delete().await()
        }

        for (question in questions) {
            questionsCollection
                .document(question.id.toString())
                .set(question)
                .await()
        }
    }
    private fun getChecklistsCollection(visitId: Int) =
        getUserVisitsCollection()?.document(visitId.toString())?.collection("checklists")

    suspend fun uploadChecklistWithAnswers(checklist: VisitChecklist, answers: List<Answer>) {
        val checklistRef = getChecklistsCollection(checklist.visitId)
            ?.document(checklist.id.toString())

        checklistRef?.set(checklist)?.await()

        val answersCollection = checklistRef?.collection("answers")
        answers.forEach { answer ->
            answersCollection?.document(answer.id.toString())?.set(answer)?.await()
        }
    }

    suspend fun deleteChecklist(checklist: VisitChecklist) {
        getChecklistsCollection(checklist.visitId)
            ?.document(checklist.id.toString())
            ?.delete()
            ?.await()
    }

    fun getAreasCollection(visitId: Int) =
        getUserVisitsCollection()?.document(visitId.toString())?.collection("areas")
    suspend fun uploadArea(area: Area) {
        getAreasCollection(area.visitId)?.document(area.id.toString())?.set(area)?.await()

    }
    suspend fun deleteArea(area: Area){
        getAreasCollection(area.visitId)?.document(area.id.toString())?.delete()?.await()
    }
}