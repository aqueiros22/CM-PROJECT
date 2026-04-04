package com.example.fieldsense.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visit_checklists")
data class VisitChecklist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val visitId: Int,
    val templateId: Int,
    val templateName: String,
    val date: String,
    val isSynced: Boolean = false
)