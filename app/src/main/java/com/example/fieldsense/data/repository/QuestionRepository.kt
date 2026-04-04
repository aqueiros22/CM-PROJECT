package com.example.fieldsense.data.repository

import android.util.Log
import com.example.fieldsense.data.local.QuestionDao
import com.example.fieldsense.data.model.Note
import com.example.fieldsense.data.model.Question
import com.example.fieldsense.data.remote.FirestoreService
import kotlinx.coroutines.flow.Flow

class QuestionRepository (
    private val questionDao: QuestionDao,
) {
    fun getQuestionsForTemplate(templateId: Int): Flow<List<Question>> =
        questionDao.getQuestionsForTemplate(templateId)

    suspend fun insertQuestion(question: Question) {
        questionDao.insertQuestion(question)
    }
    suspend fun insertQuestions(questions: List<Question>) {
        questionDao.insertQuestions(questions)
    }
    suspend fun deleteQuestion(question: Question) {
        questionDao.deleteQuestion(question)
    }

    suspend fun deleteQuestionsForTemplate(templateId: Int) {
        questionDao.deleteQuestionsForTemplate(templateId)
    }

    suspend fun updateQuestionsForTemplate(templateId: Int, newQuestions: List<Question>) {
        questionDao.deleteQuestionsForTemplate(templateId)

        val updatedQuestions = newQuestions.mapIndexed { index, q ->
            q.copy(
                templateId = templateId,
                order = index
            )
        }

        questionDao.insertQuestions(updatedQuestions)
    }

}