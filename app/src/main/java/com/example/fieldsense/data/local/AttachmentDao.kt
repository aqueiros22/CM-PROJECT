package com.example.fieldsense.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fieldsense.data.model.Attachment
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT EXISTS(SELECT 1 FROM attachments WHERE id = :attachmentId)")
    suspend fun existsById(attachmentId: Int): Boolean

    @Query("SELECT * FROM attachments WHERE visitId = :visitId ORDER BY id DESC")
    fun getAttachmentsForVisit(visitId: Int): Flow<List<Attachment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: Attachment): Long

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)

    @Update
    suspend fun updateAttachment(attachment: Attachment)

    @Query("SELECT * FROM attachments WHERE isSynced = 0")
    suspend fun getUnsyncedAttachments(): List<Attachment>

}