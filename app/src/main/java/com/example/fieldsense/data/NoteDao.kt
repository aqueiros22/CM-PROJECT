package com.example.fieldsense.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE visitId = :visitId ORDER BY id DESC")
    fun getNotesForVisit(visitId: Int): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE visitId = :visitId")
    suspend fun getNotesForVisitSync(visitId: Int): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Delete
    suspend fun deleteNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Query("SELECT * FROM notes WHERE isSynced = 0")
    suspend fun getUnsyncedNotes(): List<Note>
}
