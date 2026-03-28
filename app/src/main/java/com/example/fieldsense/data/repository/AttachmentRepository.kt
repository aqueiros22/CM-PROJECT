package com.example.fieldsense.data.repository

import android.net.Uri
import android.util.Log
import com.example.fieldsense.data.model.Attachment
import com.example.fieldsense.data.local.AttachmentDao
import com.example.fieldsense.data.remote.CloudinaryService
import com.example.fieldsense.data.remote.FirestoreService
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttachmentRepository(
    private val attachmentDao: AttachmentDao,
    private val firestoreService: FirestoreService,
    private val cloudinaryService: CloudinaryService
) {

    fun getAttachmentsForVisit(visitId: Int): Flow<List<Attachment>> =
        attachmentDao.getAttachmentsForVisit(visitId)

    suspend fun insertAttachment(visitId: Int, fileName: String, fileUri: Uri, type: String) {
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        // 1. Guarda localmente primeiro
        val attachment = Attachment(
            visitId = visitId,
            fileName = fileName,
            localPath = fileUri.toString(),
            type = type,
            date = date,
            isSynced = false
        )
        val generatedId = attachmentDao.insertAttachment(attachment)
        val savedAttachment = attachment.copy(id = generatedId.toInt())

        // 2. Tenta sincronizar com Firebase
        try {
            val remoteUrl = cloudinaryService.uploadFile(visitId, fileName, fileUri)
            val syncedAttachment = savedAttachment.copy(remoteUrl = remoteUrl, isSynced = true)
            firestoreService.uploadAttachment(syncedAttachment)
            attachmentDao.updateAttachment(syncedAttachment)
        } catch (e: Exception) {
            Log.e("AttachmentRepository", "Cloud sync failed, stored locally only", e)
        }
    }

    suspend fun deleteAttachment(attachment: Attachment) {
        attachmentDao.deleteAttachment(attachment)
        try {
            cloudinaryService.deleteFile(attachment.fileName)
            firestoreService.deleteAttachment(attachment)
        } catch (e: Exception) {
            Log.e("AttachmentRepository", "Cloud delete failed", e)
        }
    }

    suspend fun syncPendingAttachments() {
        val pending = attachmentDao.getUnsyncedAttachments()

        pending.forEach { attachment ->
            try {
                val remoteUrl = cloudinaryService.uploadFile(  // <- mudou
                    attachment.visitId,
                    attachment.fileName,
                    Uri.parse(attachment.localPath)
                )
                val synced = attachment.copy(remoteUrl = remoteUrl, isSynced = true)
                firestoreService.uploadAttachment(synced)
                attachmentDao.updateAttachment(synced)
                Log.d("Sync", "Attachment ${attachment.id} sincronizado com sucesso!")
            } catch (e: Exception) {
                Log.e("Sync", "Ainda sem ligação para o attachment ${attachment.id}")
            }
        }
    }
}