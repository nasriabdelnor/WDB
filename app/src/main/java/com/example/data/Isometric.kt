package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "isometrics",
    indices = [
        Index(value = ["isoNumber"], unique = true)
    ]
)
data class Isometric(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val isoNumber: String,
    val drawingNo: String = "",
    val totalSpools: Int = 0,
    val totalJoints: Int = 0
)
