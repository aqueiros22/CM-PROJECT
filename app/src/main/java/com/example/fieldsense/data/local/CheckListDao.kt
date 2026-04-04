package com.example.fieldsense.data.local

import androidx.room.*
import com.example.fieldsense.data.model.Answer
import com.example.fieldsense.data.model.VisitChecklist
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {

    // Checklists
    @Query("SELECT * FROM visit_checklists WHERE visitId = :visitId ORDER BY id DESC")
    fun getChecklistsForVisit(visitId: Int): Flow<List<VisitChecklist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: VisitChecklist): Long

    @Delete
    suspend fun deleteChecklist(checklist: VisitChecklist)

    @Query("SELECT * FROM visit_checklists WHERE isSynced = 0")
    suspend fun getUnsyncedChecklists(): List<VisitChecklist>

    @Update
    suspend fun updateChecklist(checklist: VisitChecklist)

    // Answers
    @Query("SELECT * FROM answers WHERE checklistId = :checklistId")
    fun getAnswersForChecklist(checklistId: Int): Flow<List<Answer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<Answer>)

    @Query("DELETE FROM answers WHERE checklistId = :checklistId")
    suspend fun deleteAnswersForChecklist(checklistId: Int)
}