package com.example.data

data class ExcelInspectionResult(
    val fileName: String,
    val fileSizeFormatted: String,
    val fileSizeBytes: Long,
    val detectedColumns: List<String>,
    val totalJointsFound: Int,
    val previewRows: List<WeldJoint>,
    val parsedWelds: List<WeldJoint>,
    val sourceDescription: String = "Fichier local"
)
