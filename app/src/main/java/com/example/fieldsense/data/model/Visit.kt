package com.example.fieldsense.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val code: String = "",
    val name: String = "",
    val date: String = "",
    val location: String = "",
    val area: String? = null,
    val isSynced: Boolean = false
)