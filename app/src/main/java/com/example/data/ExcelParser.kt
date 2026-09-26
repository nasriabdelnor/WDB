package com.example.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

object ExcelParser {

    /**
     * Parses an input stream that could be XLSX, CSV, or TSV.
     */
    fun parseStream(inputStream: InputStream, filenameHint: String = ""): List<WeldJoint> {
        val bytes = inputStream.readBytes()
        if (bytes.size < 4) return emptyList()

        // Check if ZIP / XLSX (magic bytes: 0x50, 0x4B, 0x03, 0x04)
        val isZip = bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
                    bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()

        return if (isZip || filenameHint.endsWith(".xlsx", ignoreCase = true)) {
            parseXlsxBytes(bytes)
        } else {
            parseCsvOrTsv(bytes)
        }
    }

    /**
     * Parses raw CSV or TSV bytes supporting comma, semicolon, tab, and quotes.
     */
    fun parseCsvOrTsv(bytes: ByteArray): List<WeldJoint> {
        val text = String(bytes, Charsets.UTF_8)
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val firstLine = lines.first()
        val delimiter = when {
            firstLine.count { it == ';' } > firstLine.count { it == ',' } -> ';'
            firstLine.count { it == '\t' } > firstLine.count { it == ',' } -> '\t'
            else -> ','
        }

        val rows = lines.map { parseCsvLine(it, delimiter) }
        return extractWeldsFromRows(rows)
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                result.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }

    /**
     * High-speed native XLSX parsing supporting multi-sheet, rich-text, and industrial templates
     * (e.g., TECNOGRAF, Spooling WDB, Skikda piping databases).
     */
    fun parseXlsxBytes(bytes: ByteArray): List<WeldJoint> {
        val sharedStrings = mutableListOf<String>()
        val sheets = mutableMapOf<String, ByteArray>()

        try {
            val zis = ZipInputStream(ByteArrayInputStream(bytes))
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name == "xl/sharedStrings.xml") {
                    sharedStrings.addAll(parseSharedStrings(zis.readBytes()))
                } else if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                    sheets[name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return parseCsvOrTsv(bytes)
        }

        if (sheets.isEmpty()) return emptyList()

        // Evaluate all sheets and choose the one with the best welding database rows
        var bestWelds = emptyList<WeldJoint>()

        for ((sheetName, sheetBytes) in sheets) {
            val rows = parseSheetXml(sheetBytes, sharedStrings)
            val welds = extractWeldsFromRows(rows)
            if (welds.size > bestWelds.size) {
                bestWelds = welds
            }
        }

        return bestWelds
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val list = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

            var eventType = parser.eventType
            var inText = false
            val currentText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "t") {
                            inText = true
                        } else if (parser.name == "si") {
                            currentText.clear()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inText) {
                            currentText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "t") {
                            inText = false
                        } else if (parser.name == "si") {
                            list.add(currentText.toString())
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseSheetXml(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

            var eventType = parser.eventType
            val currentRow = mutableMapOf<Int, String>()
            var currentCellCol = 0
            var lastCol = -1
            var cellType = ""
            var inValue = false
            var inInlineStr = false
            val cellValue = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "row" -> {
                                currentRow.clear()
                                lastCol = -1
                            }
                            "c" -> {
                                val cellRef = parser.getAttributeValue(null, "r") ?: ""
                                currentCellCol = if (cellRef.isNotBlank()) {
                                    columnRefToIndex(cellRef)
                                } else {
                                    lastCol + 1
                                }
                                lastCol = currentCellCol
                                cellType = parser.getAttributeValue(null, "t") ?: ""
                                cellValue.clear()
                            }
                            "v" -> inValue = true
                            "t" -> inInlineStr = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inValue || inInlineStr) {
                            cellValue.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "v", "t" -> {
                                inValue = false
                                inInlineStr = false
                            }
                            "c" -> {
                                val raw = cellValue.toString().trim()
                                val resolved = if (cellType == "s") {
                                    val idx = raw.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) {
                                        sharedStrings[idx]
                                    } else raw
                                } else {
                                    raw
                                }
                                currentRow[currentCellCol] = resolved
                            }
                            "row" -> {
                                if (currentRow.isNotEmpty()) {
                                    val maxCol = (currentRow.keys.maxOrNull() ?: 0)
                                    val rowList = (0..maxCol).map { col ->
                                        currentRow[col] ?: ""
                                    }
                                    rows.add(rowList)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rows
    }

    private fun columnRefToIndex(cellRef: String): Int {
        var col = 0
        for (ch in cellRef) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
            } else if (ch in 'a'..'z') {
                col = col * 26 + (ch - 'a' + 1)
            } else {
                break
            }
        }
        return if (col > 0) col - 1 else 0
    }

    /**
     * Intelligently scans rows to find the actual table header row, then parses all data rows.
     */
    private fun extractWeldsFromRows(rows: List<List<String>>): List<WeldJoint> {
        if (rows.size < 2) return emptyList()

        // 1. Locate the header row by scoring keywords in the first 25 rows
        val maxHeaderScan = minOf(25, rows.size)
        var bestHeaderRowIdx = 0
        var bestScore = -1

        for (i in 0 until maxHeaderScan) {
            val score = scoreHeaderRow(rows[i])
            if (score > bestScore) {
                bestScore = score
                bestHeaderRowIdx = i
            }
        }

        val headerRow = rows[bestHeaderRowIdx].map { it.trim() }
        val headerIndices = mapHeaders(headerRow)

        val welds = mutableListOf<WeldJoint>()
        for (i in (bestHeaderRowIdx + 1) until rows.size) {
            val row = rows[i]
            if (row.isEmpty() || row.all { it.isBlank() }) continue

            // Skip summary/subtotal or signature lines
            val firstCell = row.firstOrNull { it.isNotBlank() }?.lowercase() ?: ""
            if (firstCell.startsWith("total") || firstCell.startsWith("sum") || firstCell.startsWith("visa") || firstCell.startsWith("sign")) {
                continue
            }

            val weld = buildWeldFromRow(row, headerIndices, fallbackIndex = welds.size + 1)
            // Ensure weld has at least some meaningful identification
            if (weld.jointNo.isNotBlank()) {
                welds.add(weld)
            }
        }
        return welds
    }

    private fun scoreHeaderRow(row: List<String>): Int {
        var score = 0
        val keywords = listOf(
            "joint", "weld", "soudure", "giunto",
            "line", "ligne", "linea", "iso", "piping",
            "spool", "troncon", "tronçon",
            "welder", "soudeur", "saldatore", "stamp", "poinçon", "poincon",
            "wps", "dmos", "qmos",
            "dia", "diameter", "diametre", "diamètre", "inch", "pouce", "dn",
            "thick", "epaisseur", "épaisseur", "spessore", "wt",
            "mat", "material", "materiau", "matériau", "spec",
            "ndt", "cnd", "rt", "ut", "pt", "mt", "vt",
            "fitup", "fit-up", "pointage", "visuel", "visual"
        )
        for (cell in row) {
            val c = cell.lowercase().trim()
            if (keywords.any { c.contains(it) }) {
                score++
            }
        }
        return score
    }

    class HeaderIndices(
        val jointNo: Int = -1,
        val lineNo: Int = -1,
        val spoolNo: Int = -1,
        val drawingNo: Int = -1,
        val welderId: Int = -1,
        val welderName: Int = -1,
        val wpsNo: Int = -1,
        val process: Int = -1,
        val weldType: Int = -1,
        val material: Int = -1,
        val diameter: Int = -1,
        val thickness: Int = -1,
        val weldDate: Int = -1,
        val fitup: Int = -1,
        val visual: Int = -1,
        val ndtType: Int = -1,
        val ndtResult: Int = -1,
        val ndtReport: Int = -1,
        val status: Int = -1,
        val notes: Int = -1
    )

    private fun mapHeaders(headers: List<String>): HeaderIndices {
        var jointNo = -1
        var lineNo = -1
        var spoolNo = -1
        var drawingNo = -1
        var welderId = -1
        var welderName = -1
        var wpsNo = -1
        var process = -1
        var weldType = -1
        var material = -1
        var diameter = -1
        var thickness = -1
        var weldDate = -1
        var fitup = -1
        var visual = -1
        var ndtType = -1
        var ndtResult = -1
        var ndtReport = -1
        var status = -1
        var notes = -1

        for ((idx, hRaw) in headers.withIndex()) {
            val h = hRaw.lowercase().trim()
            when {
                jointNo == -1 && (h.contains("joint") || h.contains("weld no") || h.contains("soudure") || h.contains("giunto") || h == "no" || h == "n°" || h == "weld_id" || h == "weld" || h == "jnt") -> jointNo = idx
                lineNo == -1 && (h.contains("line") || h.contains("ligne") || h.contains("linea") || h.contains("iso") || h.contains("piping")) -> lineNo = idx
                spoolNo == -1 && (h.contains("spool") || h.contains("troncon") || h.contains("tronçon") || h.contains("pezzo")) -> spoolNo = idx
                drawingNo == -1 && (h.contains("drawing") || h.contains("plan") || h.contains("dwg") || h.contains("disegno")) -> drawingNo = idx
                welderName == -1 && (h.contains("nom") || h.contains("welder name") || h.contains("nom soudeur") || h.contains("nome")) -> welderName = idx
                welderId == -1 && (h.contains("welder") || h.contains("soudeur") || h.contains("saldatore") || h.contains("stamp") || h.contains("poinçon") || h.contains("poincon") || h == "w" || h == "op") -> welderId = idx
                wpsNo == -1 && (h.contains("wps") || h.contains("dmos") || h.contains("qmos") || h.contains("procedure")) -> wpsNo = idx
                process == -1 && (h.contains("process") || h.contains("procédé") || h.contains("procede") || h.contains("proc")) -> process = idx
                weldType == -1 && (h.contains("type") || h.contains("tipo") || h.contains("bw/sw") || h.contains("joint type")) -> weldType = idx
                material == -1 && (h.contains("material") || h.contains("matériau") || h.contains("materiau") || h.contains("materiale") || h.contains("nuance") || h.contains("metal")) -> material = idx
                diameter == -1 && (h.contains("dia") || h.contains("pouce") || h.contains("inch") || h.contains("dn") || h.contains("size") || h.contains("nd") || h == "ø") -> diameter = idx
                thickness == -1 && (h.contains("thick") || h.contains("epaisseur") || h.contains("épaisseur") || h.contains("spessore") || h.contains("ép") || h.contains("ep") || h.contains("sched") || h.contains("wt")) -> thickness = idx
                weldDate == -1 && (h.contains("date") || h.contains("data") && !h.contains("fitup") && !h.contains("visuel") && !h.contains("ndt")) -> weldDate = idx
                fitup == -1 && (h.contains("fitup") || h.contains("fit-up") || h.contains("pointage") || h.contains("assemblage") || h.contains("accoppiamento")) -> fitup = idx
                visual == -1 && (h.contains("visuel") || h.contains("vt") || h.contains("visual") || h.contains("visivo")) -> visual = idx
                ndtReport == -1 && (h.contains("report") || h.contains("rapport") || h.contains("pv") || h.contains("doc")) -> ndtReport = idx
                ndtResult == -1 && (h.contains("ndt res") || h.contains("cnd res") || h.contains("rt res") || h.contains("result") || h.contains("esito")) -> ndtResult = idx
                ndtType == -1 && (h.contains("ndt") || h.contains("cnd") || h.contains("controle") || h.contains("contrôle") || h.contains("controllo")) -> ndtType = idx
                status == -1 && (h.contains("status") || h.contains("statut") || h.contains("etat") || h.contains("état") || h.contains("stato")) -> status = idx
                notes == -1 && (h.contains("note") || h.contains("remarque") || h.contains("obs") || h.contains("comment") || h.contains("annotazioni")) -> notes = idx
            }
        }

        if (jointNo == -1 && headers.isNotEmpty()) jointNo = 0
        if (lineNo == -1 && headers.size > 1) lineNo = 1

        return HeaderIndices(
            jointNo, lineNo, spoolNo, drawingNo, welderId, welderName,
            wpsNo, process, weldType, material, diameter, thickness,
            weldDate, fitup, visual, ndtType, ndtResult, ndtReport, status, notes
        )
    }

    private fun buildWeldFromRow(
        row: List<String>,
        h: HeaderIndices,
        fallbackIndex: Int
    ): WeldJoint {
        fun get(idx: Int, default: String = ""): String =
            if (idx in row.indices) row[idx].trim() else default

        val rawJoint = get(h.jointNo, "W-$fallbackIndex")
        val jointNo = if (rawJoint.isNotBlank()) rawJoint else "W-$fallbackIndex"
        val lineNo = get(h.lineNo, "LINE-01")
        val spoolNo = get(h.spoolNo, "SP-01")
        val drawingNo = get(h.drawingNo, "DWG-01")
        val welderId = get(h.welderId, "W-01")
        val welderName = get(h.welderName, "Welder $welderId")
        val wpsNo = get(h.wpsNo, "WPS-CS-01")
        val process = get(h.process, "GTAW+SMAW")
        val weldType = get(h.weldType, "BW")
        val material = get(h.material, "A106 Gr.B")

        val diaStr = get(h.diameter, "4.0").replace(",", ".").replace("\"", "").replace("DN", "").trim()
        val diameter = diaStr.toDoubleOrNull() ?: 4.0

        val thickStr = get(h.thickness, "6.0").replace(",", ".").replace("mm", "").trim()
        val thickness = thickStr.toDoubleOrNull() ?: 6.0

        val rawDate = get(h.weldDate, "2026-09-20")
        val weldDate = parseExcelDate(rawDate)

        // Parse Inspection statuses
        val rawFitup = get(h.fitup, "ACCEPTED").uppercase()
        val fitupStatus = when {
            rawFitup.contains("ACC") || rawFitup.contains("CONF") || rawFitup.contains("OK") || rawFitup == "A" || rawFitup.contains("VAL") -> "ACCEPTED"
            rawFitup.contains("REJ") || rawFitup.contains("REF") || rawFitup.contains("NON") -> "REJECTED"
            else -> "PENDING"
        }

        val rawVisual = get(h.visual, "ACCEPTED").uppercase()
        val visualStatus = when {
            rawVisual.contains("ACC") || rawVisual.contains("CONF") || rawVisual.contains("OK") || rawVisual == "A" || rawVisual.contains("VAL") -> "ACCEPTED"
            rawVisual.contains("REJ") || rawVisual.contains("REF") || rawVisual.contains("NON") -> "REJECTED"
            else -> "PENDING"
        }

        val rawNdtType = get(h.ndtType, "RT").uppercase()
        val ndtType = when {
            rawNdtType.contains("UT") || rawNdtType.contains("ULTRA") -> "UT"
            rawNdtType.contains("PT") || rawNdtType.contains("RESSU") || rawNdtType.contains("LIQU") -> "PT"
            rawNdtType.contains("MT") || rawNdtType.contains("MAG") -> "MT"
            rawNdtType.contains("VT") || rawNdtType.contains("ONLY") || rawNdtType.contains("NON") -> "VT ONLY"
            else -> "RT"
        }

        val rawNdtRes = get(h.ndtResult, "ACCEPTED").uppercase()
        val ndtResult = when {
            rawNdtRes.contains("ACC") || rawNdtRes.contains("CONF") || rawNdtRes.contains("OK") || rawNdtRes == "A" || rawNdtRes.contains("VAL") -> "ACCEPTED"
            rawNdtRes.contains("REJ") || rawNdtRes.contains("REP") || rawNdtRes.contains("NON") -> "REJECTED"
            rawNdtRes.contains("REQ") || rawNdtRes.contains("ATT") || rawNdtRes.contains("PEND") -> "PENDING"
            rawNdtRes.contains("NOT") || rawNdtRes.contains("N/A") || ndtType == "VT ONLY" -> "NOT_REQUIRED"
            else -> "PENDING"
        }

        val ndtReportNo = get(h.ndtReport, "")

        val rawStatus = get(h.status, "").uppercase()
        val status = when {
            rawStatus.contains("REP") || ndtResult == "REJECTED" || visualStatus == "REJECTED" -> "REPAIR_REQUIRED"
            visualStatus == "PENDING" || fitupStatus == "PENDING" -> "IN_PROGRESS"
            ndtResult == "PENDING" -> "PENDING_NDT"
            rawStatus.contains("COMP") || rawStatus.contains("TERM") || rawStatus.contains("CONF") -> "COMPLETED"
            else -> "COMPLETED"
        }

        val notes = get(h.notes, "")

        return WeldJoint(
            id = 0,
            jointNo = jointNo,
            lineNo = lineNo,
            spoolNo = spoolNo,
            drawingNo = drawingNo,
            welderId = welderId,
            welderName = welderName,
            wpsNo = wpsNo,
            process = process,
            weldType = weldType,
            material = material,
            diameterInch = diameter,
            thicknessMm = thickness,
            weldDate = weldDate,
            fitupStatus = fitupStatus,
            fitupInspector = "QC Inspector",
            fitupDate = weldDate,
            visualStatus = visualStatus,
            visualInspector = "QC Inspector",
            visualDate = weldDate,
            ndtType = ndtType,
            ndtResult = ndtResult,
            ndtReportNo = ndtReportNo,
            ndtDate = weldDate,
            repairCount = if (status == "REPAIR_REQUIRED") 1 else 0,
            status = status,
            notes = notes,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun parseExcelDate(value: String): String {
        val num = value.toDoubleOrNull()
        if (num != null && num > 30000 && num < 60000) {
            val epochMillis = ((num - 25569) * 86400 * 1000).toLong()
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(epochMillis))
        }
        return value
    }

    fun exportToCsv(welds: List<WeldJoint>): String {
        val sb = StringBuilder()
        sb.append("Joint No,Line No,Spool No,Drawing No,Welder ID,Welder Name,WPS,Process,Weld Type,Material,Diameter (Inch),Thickness (mm),Weld Date,Fit-up,Visual VT,NDT Type,NDT Result,NDT Report,Overall Status,Notes\n")

        for (w in welds) {
            val row = listOf(
                escapeCsv(w.jointNo),
                escapeCsv(w.lineNo),
                escapeCsv(w.spoolNo),
                escapeCsv(w.drawingNo),
                escapeCsv(w.welderId),
                escapeCsv(w.welderName),
                escapeCsv(w.wpsNo),
                escapeCsv(w.process),
                escapeCsv(w.weldType),
                escapeCsv(w.material),
                w.diameterInch.toString(),
                w.thicknessMm.toString(),
                escapeCsv(w.weldDate),
                w.fitupStatus,
                w.visualStatus,
                w.ndtType,
                w.ndtResult,
                escapeCsv(w.ndtReportNo),
                w.status,
                escapeCsv(w.notes)
            )
            sb.append(row.joinToString(","))
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(str: String): String {
        return if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            "\"" + str.replace("\"", "\"\"") + "\""
        } else {
            str
        }
    }
}
