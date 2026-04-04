package com.example.fieldsense.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fieldsense.data.model.Template
import kotlinx.coroutines.flow.Flow
@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY id DESC")
    fun getTemplates(): Flow<List<Template>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertTemplate(template: Template): Long

    @Delete
    suspend fun deleteTemplate(template: Template)

    @Update
    suspend fun updateTemplate(template: Template)

    @Query("SELECT * FROM templates WHERE isSynced = 0")
    suspend fun getUnsyncedTemplates(): List<Template>

}