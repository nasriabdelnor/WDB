package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "spools",
    indices = [
        Index(value = ["isoId"]),
        Index(value = ["isoNumber"]),
        Index(value = ["spoolNumber"]),
        Index(value = ["isoNumber", "spoolNumber"], unique = true)
    ]
)
data class Spool(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val isoId: Long = 0,
    val isoNumber: String = "",
    val spoolNumber: String,
    val totalJoints: Int = 0
)
