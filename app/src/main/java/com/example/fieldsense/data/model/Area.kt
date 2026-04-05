package com.example.fieldsense.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.maplibre.spatialk.geojson.Position


@Entity(
    tableName = "areas",
    foreignKeys = [
        ForeignKey(
            entity = Visit::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ]
)

data class Area (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val visitId: Int,
    val points: String = "",
    val isSynced: Boolean = false
) {
    fun getPositions(): List<Position> {
        if (points.isBlank()) return emptyList()

        val pointsStr = points.split(";")
        return pointsStr.map {
            val coords = it.split(",")
            // List configuration is wrong
            if (coords.size != 2){
                return emptyList()
            }
            Position(coords[1].toDouble(), coords[0].toDouble())
        }
    }
}
