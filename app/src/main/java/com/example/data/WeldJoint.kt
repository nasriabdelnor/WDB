package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "welds",
    indices = [
        Index(value = ["isoId"]),
        Index(value = ["spoolId"]),
        Index(value = ["isoNumber"]),
        Index(value = ["spoolNumber"]),
        Index(value = ["jointNumber"]),
        Index(value = ["isoNumber", "spoolNumber", "jointNumber"]),
        Index(value = ["part1HeatNumber"]),
        Index(value = ["part2HeatNumber"]),
        Index(value = ["welder"]),
        Index(value = ["status"])
    ]
)
data class WeldJoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val spoolId: Long = 0,
    val isoId: Long = 0,
    val isoNumber: String = "",
    val spoolNumber: String = "",
    val jointNumber: String,

    // PART 1
    val part1Description: String = "",
    val part1Material: String = "",
    val part1HeatNumber: String = "",

    // PART 2
    val part2Description: String = "",
    val part2Material: String = "",
    val part2HeatNumber: String = "",

    // WELDING
    val weldType: String = "BW",
    val process: String = "GTAW+SMAW",
    val wps: String = "WPS-CS-01",
    val welder: String = "",
    val date: String = "",
    val diameterInch: Double = 4.0,
    val thicknessMm: Double = 6.0,

    // NDT
    val visual: String = "ACCEPTED",
    val rt: String = "N/A",
    val pt: String = "N/A",
    val ut: String = "N/A",

    // Tracking & Quality
    val drawingNo: String = "",
    val welderName: String = "",
    val ndtReportNo: String = "",
    val status: String = "COMPLETED",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    @androidx.room.Ignore
    constructor(
        id: Long = 0,
        jointNo: String = "",
        lineNo: String = "",
        spoolNo: String = "",
        welderId: String = "",
        welderName: String = "",
        diameterInch: Double = 4.0,
        thicknessMm: Double = 6.0,
        status: String = "COMPLETED",
        part1: String = "",
        part2: String = "",
        heatNo1: String = "",
        heatNo2: String = "",
        wpsNo: String = "WPS-CS-01",
        process: String = "GTAW+SMAW",
        weldType: String = "BW",
        visualStatus: String = "ACCEPTED",
        ndtType: String = "RT",
        ndtResult: String = "ACCEPTED",
        ndtReportNo: String = "",
        notes: String = ""
    ) : this(
        id = id,
        spoolId = 0,
        isoId = 0,
        isoNumber = lineNo,
        spoolNumber = spoolNo,
        jointNumber = jointNo,
        part1Description = part1,
        part1Material = "A106 Gr.B",
        part1HeatNumber = heatNo1,
        part2Description = part2,
        part2Material = "A106 Gr.B",
        part2HeatNumber = heatNo2,
        weldType = weldType,
        process = process,
        wps = wpsNo,
        welder = welderId,
        welderName = welderName,
        date = "",
        diameterInch = diameterInch,
        thicknessMm = thicknessMm,
        visual = visualStatus,
        rt = if (ndtType == "RT") ndtResult else "N/A",
        pt = if (ndtType == "PT") ndtResult else "N/A",
        ut = if (ndtType == "UT") ndtResult else "N/A",
        drawingNo = "",
        ndtReportNo = ndtReportNo,
        status = status,
        notes = notes,
        updatedAt = System.currentTimeMillis()
    )

    // Backward compatibility aliases
    val jointNo: String get() = jointNumber
    val lineNo: String get() = isoNumber
    val spoolNo: String get() = spoolNumber
    val welderId: String get() = welder
    val wpsNo: String get() = wps
    val weldDate: String get() = date
    val part1: String get() = part1Description
    val part2: String get() = part2Description
    val heatNo1: String get() = part1HeatNumber
    val heatNo2: String get() = part2HeatNumber
    val material: String get() = if (part1Material.isNotBlank()) part1Material else "A106 Gr.B"
    val visualStatus: String get() = visual
    val fitupStatus: String get() = "ACCEPTED"
    val ndtType: String get() = when {
        rt != "N/A" && rt.isNotBlank() -> "RT"
        pt != "N/A" && pt.isNotBlank() -> "PT"
        ut != "N/A" && ut.isNotBlank() -> "UT"
        else -> "RT"
    }
    val ndtResult: String get() = when {
        rt.contains("REJ", ignoreCase = true) || pt.contains("REJ", ignoreCase = true) || ut.contains("REJ", ignoreCase = true) -> "REJECTED"
        rt.contains("ACC", ignoreCase = true) || pt.contains("ACC", ignoreCase = true) || ut.contains("ACC", ignoreCase = true) -> "ACCEPTED"
        rt.contains("CONF", ignoreCase = true) || pt.contains("CONF", ignoreCase = true) || ut.contains("CONF", ignoreCase = true) -> "ACCEPTED"
        rt.contains("PEND", ignoreCase = true) || pt.contains("PEND", ignoreCase = true) || ut.contains("PEND", ignoreCase = true) -> "PENDING"
        else -> if (visual.contains("REJ", ignoreCase = true)) "REJECTED" else "ACCEPTED"
    }
    val repairCount: Int get() = if (status == "REPAIR_REQUIRED" || ndtResult == "REJECTED" || visual == "REJECTED") 1 else 0
    val inchDia: Double get() = if (diameterInch > 0) diameterInch else 1.0
    val isFullyAccepted: Boolean get() = (visual == "ACCEPTED" || visual == "CONFORME") &&
            (rt != "REJECTED" && pt != "REJECTED" && ut != "REJECTED") &&
            status != "REPAIR_REQUIRED"
    val isPendingNdt: Boolean get() = rt == "PENDING" || pt == "PENDING" || ut == "PENDING" || status == "PENDING_NDT"
    val isRepairRequired: Boolean get() = status == "REPAIR_REQUIRED" || rt == "REJECTED" || pt == "REJECTED" || ut == "REJECTED" || visual == "REJECTED"
    val isometricKey: String get() = "${isoNumber.trim().uppercase()}|${spoolNumber.trim().uppercase()}|${jointNumber.trim().uppercase()}"
    val duplicateKey: String get() = "${isoNumber.trim().uppercase()}___${spoolNumber.trim().uppercase()}___${jointNumber.trim().uppercase()}"
}
