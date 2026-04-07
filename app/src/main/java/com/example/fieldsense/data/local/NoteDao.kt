package com.example.fieldsense.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fieldsense.data.model.Note
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

    @Query("SELECT * FROM notes WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedNotes(userId: String): List<Note>
}