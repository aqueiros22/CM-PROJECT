package com.example.fieldsense.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

import com.example.fieldsense.data.model.Question

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE templateId = :templateId ORDER BY id DESC")
    fun getQuestionsForTemplate(templateId: Int): Flow<List<Question>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<Question>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: Question): Long

    @Delete
    suspend fun deleteQuestion(question: Question)

    @Query("DELETE FROM questions WHERE templateId = :templateId")
    suspend fun deleteQuestionsForTemplate(templateId: Int)
}