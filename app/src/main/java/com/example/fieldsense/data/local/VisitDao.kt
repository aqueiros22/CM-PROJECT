package com.example.fieldsense.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fieldsense.data.model.Visit
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {

    @Query("SELECT EXISTS(SELECT 1 FROM visits WHERE id = :visitId)")
    suspend fun existsById(visitId: Int): Boolean

    @Query("SELECT * FROM visits WHERE userId = :userId AND isArchived = 0 ORDER BY id DESC")
    fun getActiveVisitsByUser(userId: String): Flow<List<Visit>>

    @Query("SELECT * FROM visits WHERE userId = :userId AND isArchived = 1 ORDER BY id DESC")
    fun getArchivedVisitsByUser(userId: String): Flow<List<Visit>>

    @Query("SELECT * FROM visits ORDER BY id DESC")
    fun getAllVisits(): Flow<List<Visit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: Visit): Long

    @Query("DELETE FROM visits WHERE id = :visitId")
    suspend fun deleteVisitById(visitId: Int)

    @Query("SELECT * FROM visits WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedVisits(userId: String): List<Visit>

    @Update
    suspend fun updateVisit(visit: Visit)
}