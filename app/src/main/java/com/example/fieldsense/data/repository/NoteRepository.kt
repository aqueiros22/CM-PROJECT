package com.example.fieldsense.data.repository

import android.util.Log
import com.example.fieldsense.data.local.NoteDao
import com.example.fieldsense.data.model.Note
import com.example.fieldsense.data.remote.FirestoreService
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val firestoreService: FirestoreService
) {

    fun getNotesForVisit(visitId: Int): Flow<List<Note>> =
        noteDao.getNotesForVisit(visitId)

    suspend fun insertNote(note: Note) {
        val generatedId = noteDao.insertNote(note)
        val syncedNote = note.copy(id = generatedId.toInt(), isSynced = true)

        try {
            firestoreService.uploadNote(syncedNote)
            noteDao.insertNote(syncedNote)
        } catch (e: Exception) {
            Log.e("NoteRepository", "Cloud sync failed, stored locally only", e)
        }
    }
    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
        try {
            firestoreService.deleteNote(note)
        } catch (e: Exception) {
            Log.e("NoteRepository", "Cloud delete failed", e)
        }
    }

    suspend fun updateNote(note: Note) {
        val updatedNote = note.copy(isSynced = false)
        noteDao.updateNote(updatedNote)
        try {
            val syncedNote = updatedNote.copy(isSynced = true)
            firestoreService.uploadNote(syncedNote)
            noteDao.updateNote(syncedNote)
        } catch (e: Exception) {
            Log.e("NoteRepository", "Cloud update failed, stored locally only", e)
        }
    }

    suspend fun pullNotesFromServer(visitId: Int) {
        try {
            val remoteNotes = firestoreService.getNotesForVisit(visitId)
            remoteNotes.forEach { note ->
                noteDao.insertNote(note.copy(isSynced = true))
            }
        } catch (e: Exception) {
            Log.e("Sync", "Failed to pull notes for visit $visitId", e)
        }
    }

    suspend fun syncPendingNotes(userId: String) {
        val pendingNotes = noteDao.getUnsyncedNotes(userId)

        pendingNotes.forEach { note ->
            try {
                val syncedNote = note.copy(isSynced = true)
                firestoreService.uploadNote(syncedNote)
                noteDao.insertNote(syncedNote)
                Log.d("Sync", "Nota ${note.id} sincronizada com sucesso!")
            } catch (e: Exception) {
                Log.e("Sync", "Ainda sem ligação para a nota ${note.id}")
            }
        }
    }
}
