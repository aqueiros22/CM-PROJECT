package com.example.fieldsense.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {


    @Query("SELECT * FROM visits ORDER BY id DESC")
    fun getAllVisits(): Flow<List<Visit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: Visit): Long

    @Query("DELETE FROM visits WHERE id = :visitId")
    suspend fun deleteVisitById(visitId: Int)

    @Query("SELECT * FROM visits WHERE isSynced = 0")
    suspend fun getUnsyncedVisits(): List<Visit>

    @Update
    suspend fun updateVisit(visit: Visit)
}