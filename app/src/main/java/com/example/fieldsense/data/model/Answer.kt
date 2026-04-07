package com.example.fieldsense.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "answers")
data class Answer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val checklistId: Int = 0,
    val questionId: Int = 0,
    val questionText: String = "",
    val questionType: QuestionType = QuestionType.CHECKBOX,
    val value: String = ""
)