package com.example.data

data class SyncSuccessSummary(
    val fileName: String = "WDB Google Sheet",
    val recordsImported: Int,
    val isoCount: Int,
    val spoolCount: Int,
    val jointsCount: Int
)
