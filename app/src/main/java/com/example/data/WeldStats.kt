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

enum class TransferMode {
    ASK,        // Demander à l'utilisateur
    CABLE_USB,  // Transfert direct par câble USB / stockage local
    CLOUD_WEB,  // Transfert par Internet (OneDrive, Google Drive, Plateforme)
    PC_NETWORK  // Connexion directe réseau PC (Adresse IP / Wi-Fi)
}

enum class AppMode {
    CLIENT_LITE, // Version Client très légère (dédiée terrain: filtre, recherche, détection de doublons, sans détails sensibles)
    MASTER       // Version Master complète (Tableau de bord exhaustif, performance soudeurs, KPIs avancés)
}

data class DuplicateWeldGroup(
    val key: String,
    val lineNo: String,
    val spoolNo: String,
    val jointNo: String,
    val count: Int,
    val welds: List<WeldJoint>
)


data class WdbProjectInfo(
    val folderPath: String = "X:\\7-NDT\\9-SUIVI DE CONTROLE ET NDT PROJET LAB\\WCP",
    val fileName: String = "Welding Data Base01.xlsx",
    val transferMode: TransferMode = TransferMode.CLOUD_WEB,
    val oneDriveUrl: String = "https://sarpidz-my.sharepoint.com/:x:/r/personal/abdenor_nasri_sarpi-dz_com/Documents/Welding%20Data%20Base01.xlsx?d=w5ec75b1ea7ca4f6d9b6122e0072b8330&csf=1&web=1&e=5dSkgj"
)

