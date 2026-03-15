package com.example.fieldsense.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val visitId: Int,
    val content: String,
    val date: String,
    val isSynced: Boolean = false
)