package com.example.fieldsense.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attachments")
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val visitId: Int,           // ligação à visita
    val fileName: String,       // nome do ficheiro
    val localPath: String,      // caminho local no telemóvel
    val remoteUrl: String = "", // URL do Firebase Storage (vazio até sincronizar)
    val type: String,           // "image", "pdf", "video", etc.
    val date: String,
    val isSynced: Boolean = false
)