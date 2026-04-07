package com.example.fieldsense.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class QuestionType {
    CHECKBOX, TEXT, RATING
}

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true)val id: Int = 0,
    val templateId: Int = 0,
    val text: String = "",
    val type: QuestionType = QuestionType.CHECKBOX,
    val order: Int = 0
)