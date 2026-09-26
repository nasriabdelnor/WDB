package com.example.data

data class WeldingKpis(
    val totalWelds: Int = 0,
    val totalInchDia: Double = 0.0,
    val acceptedCount: Int = 0,
    val inProgressCount: Int = 0,
    val pendingNdtCount: Int = 0,
    val repairCount: Int = 0,
    val completionPercentage: Float = 0f,
    val defectRatePercentage: Float = 0f
)

data class WelderPerformance(
    val welderId: String,
    val welderName: String,
    val totalWelds: Int,
    val totalInchDia: Double,
    val acceptedWelds: Int,
    val repairCount: Int,
    val defectRate: Float
)

data class LineProgress(
    val lineNo: String,
    val totalWelds: Int,
    val completedWelds: Int,
    val percent: Float
)

data class NdtTypeStat(
    val ndtType: String,
    val total: Int,
    val accepted: Int,
    val pending: Int,
    val rejected: Int
)

data class ConnectionTestResult(
    val isSuccess: Boolean = false,
    val statusCode: Int = 0,
    val responseTimeMs: Long = 0,
    val fileSizeKb: Long = 0,
    val message: String = ""
)

data class SpoolGroup(
    val spoolNo: String,
    val totalJoints: Int,
    val completedJoints: Int,
    val repairJoints: Int,
    val joints: List<WeldJoint>
)

data class IsometricGroup(
    val lineNo: String,
    val drawingNo: String = "",
    val totalJoints: Int,
    val completedJoints: Int,
    val repairJoints: Int,
    val completionRate: Float,
    val spools: List<SpoolGroup>
)

data class PcConnectionProfile(
    val id: String,
    val name: String,
    val ip: String,
    val port: String,
    val filename: String
)

