package com.example.fieldsense.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
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