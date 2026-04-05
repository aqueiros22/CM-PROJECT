package com.example.fieldsense.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fieldsense.data.model.Area
import kotlinx.coroutines.flow.Flow

@Dao
interface AreaDao {
    @Query("SELECT * FROM areas WHERE visitId = :visitId")
    fun getAreasForVisit(visitId: Int): Flow<List<Area>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertArea(area: Area): Long

    @Delete
    suspend fun deleteArea(area: Area)

    @Query("SELECT * FROM areas WHERE visitId = :visitId")
    suspend fun getAreasForVisitSync(visitId: Int): List<Area>

    @Update
    suspend fun updateArea(area: Area)

    @Query("SELECT * FROM areas WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedAreas(userId: String): List<Area>

}
