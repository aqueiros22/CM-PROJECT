package com.example.fieldsense.data.local

import androidx.room.*
import com.example.fieldsense.data.model.Answer
import com.example.fieldsense.data.model.VisitChecklist
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {

    @Query("SELECT EXISTS(SELECT 1 FROM visit_checklists WHERE id = :checklistId)")
    suspend fun existsChecklistById(checklistId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM answers WHERE id = :answerId)")
    suspend fun existsAnswerById(answerId: Int): Boolean

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

    @Query("SELECT * FROM answers WHERE checklistId = :checklistId")
    suspend fun getAnswersForChecklistSync(checklistId: Int): List<Answer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<Answer>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: Answer): Long

    @Update
    suspend fun updateAnswer(answer: Answer)

    @Query("DELETE FROM answers WHERE checklistId = :checklistId")
    suspend fun deleteAnswersForChecklist(checklistId: Int)

        @Query(
                """
                DELETE FROM answers
                WHERE checklistId = :checklistId
                    AND id NOT IN (
                        SELECT MAX(id)
                        FROM answers
                        WHERE checklistId = :checklistId
                        GROUP BY questionId
                    )
                """
        )
        suspend fun removeDuplicateAnswersForChecklist(checklistId: Int)
}