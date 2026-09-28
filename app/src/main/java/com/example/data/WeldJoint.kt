package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "welds",
    indices = [
        Index(value = ["lineNo"]),
        Index(value = ["spoolNo"]),
        Index(value = ["jointNo"]),
        Index(value = ["lineNo", "spoolNo", "jointNo"]),
        Index(value = ["lineNo", "jointNo"]),
        Index(value = ["status"]),
        Index(value = ["welderId"]),
        Index(value = ["ndtResult"])
    ]
)
data class WeldJoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jointNo: String,
    val lineNo: String = "",
    val spoolNo: String = "",
    val drawingNo: String = "",
    val welderId: String = "",
    val welderName: String = "",
    val wpsNo: String = "WPS-CS-01",
    val process: String = "GTAW+SMAW",
    val weldType: String = "BW", // BW: Butt Weld, FW: Fillet, SW: Socket
    val material: String = "A106 Gr.B",
    val diameterInch: Double = 4.0,
    val thicknessMm: Double = 6.02,
    val weldDate: String = "",
    val fitupStatus: String = "ACCEPTED", // ACCEPTED, PENDING, REJECTED
    val fitupInspector: String = "",
    val fitupDate: String = "",
    val visualStatus: String = "ACCEPTED", // ACCEPTED, PENDING, REJECTED
    val visualInspector: String = "",
    val visualDate: String = "",
    val ndtType: String = "RT", // RT (Radiography), UT (Ultrasonic), PT (Dye Penetrant), MT (Magnetic), VT ONLY
    val ndtResult: String = "ACCEPTED", // ACCEPTED, PENDING, REJECTED, NOT_REQUIRED
    val ndtReportNo: String = "",
    val ndtDate: String = "",
    val repairCount: Int = 0,
    val pwht: String = "N/A", // N/A, REQUIRED, COMPLETED
    val part1: String = "", // e.g. "Tuyau / Pipe", "Coude 90°", "Bride WN"
    val part2: String = "", // e.g. "Tuyau / Pipe", "Té", "Réduction"
    val heatNo1: String = "", // N° de Coulée / Heat Number Partie 1
    val heatNo2: String = "", // N° de Coulée / Heat Number Partie 2
    val status: String = "COMPLETED", // COMPLETED, IN_PROGRESS, REPAIR_REQUIRED, PENDING_NDT, PENDING_FITUP
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Compute inch-dia calculation factor (diameter in inches)
     */
    val inchDia: Double get() = if (diameterInch > 0) diameterInch else 1.0

    /**
     * Computed flag checking if weld is fully accepted (VT and NDT accepted)
     */
    val isFullyAccepted: Boolean
        get() = visualStatus == "ACCEPTED" &&
                fitupStatus == "ACCEPTED" &&
                (ndtResult == "ACCEPTED" || ndtResult == "NOT_REQUIRED" || ndtType == "VT ONLY") &&
                status != "REPAIR_REQUIRED"

    val isPendingNdt: Boolean
        get() = ndtType != "VT ONLY" && ndtResult == "PENDING"

    val isRepairRequired: Boolean
        get() = status == "REPAIR_REQUIRED" || ndtResult == "REJECTED" || visualStatus == "REJECTED"

    /**
     * Unique composite key identifying the weld joint in isometric drawing
     */
    val isometricKey: String
        get() = "${lineNo.trim().uppercase()}|${spoolNo.trim().uppercase()}|${jointNo.trim().uppercase()}"

    /**
     * Key used for detecting duplicate entries across the WDB
     */
    val duplicateKey: String
        get() = "${lineNo.trim().uppercase()}___${spoolNo.trim().uppercase()}___${jointNo.trim().uppercase()}"
}
