package com.example.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

object ExcelParser {

    /**
     * Parses an input stream that could be XLSX, CSV, or TSV.
     * Fully compatible with industrial WDB files (TECNOGRAF, SARPI-DZ, Sonatrach, Saipem, WCP).
     */
    fun parseStream(inputStream: InputStream, filenameHint: String = ""): List<WeldJoint> {
        val bytes = inputStream.readBytes()
        if (bytes.size < 4) return emptyList()

        // Check if ZIP / XLSX (magic bytes: 0x50, 0x4B, 0x03, 0x04)
        val isZip = bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
                    bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()

        return if (isZip || filenameHint.endsWith(".xlsx", ignoreCase = true) || filenameHint.endsWith(".xlsm", ignoreCase = true)) {
            parseXlsxBytes(bytes)
        } else {
            parseCsvOrTsv(bytes)
        }
    }

    /**
     * Inspects an Excel or CSV file without writing to DB, returning file metrics, detected columns,
     * total joint count, and first 10 preview rows.
     */
    fun inspectAndParseStream(
        inputStream: InputStream,
        filenameHint: String = "Welding Data Base01.xlsx",
        sourceDescription: String = "Fichier local"
    ): ExcelInspectionResult {
        val bytes = inputStream.readBytes()
        val fileSizeFormatted = when {
            bytes.size >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f Mo", bytes.size / (1024.0 * 1024.0))
            bytes.size >= 1024 -> String.format(Locale.getDefault(), "%.1f Ko", bytes.size / 1024.0)
            else -> "${bytes.size} octets"
        }

        val welds = parseStream(bytes.inputStream(), filenameHint)
        val detected = if (lastDetectedColumns.isNotEmpty()) lastDetectedColumns else listOf(
            "ISO", "SPOOL", "JOINT", "PART 1", "PART 1 HEAT NUMBER",
            "PART 2", "PART 2 HEAT NUMBER", "WPS", "WELDER", "PROCESS", "RT", "PT", "UT"
        )

        return ExcelInspectionResult(
            fileName = filenameHint,
            fileSizeFormatted = fileSizeFormatted,
            fileSizeBytes = bytes.size.toLong(),
            detectedColumns = detected,
            totalJointsFound = welds.size,
            previewRows = welds.take(10),
            parsedWelds = welds,
            sourceDescription = sourceDescription
        )
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
        return extractWeldsFromRows(rows, sheetFallbackName = "WDB")
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var cellStartedWithQuote = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (!inQuotes && sb.isEmpty()) {
                    inQuotes = true
                    cellStartedWithQuote = true
                } else if (inQuotes) {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    sb.append('"')
                }
            } else if (c == delimiter && !inQuotes) {
                result.add(sb.toString().trim())
                sb.clear()
                cellStartedWithQuote = false
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }

    /**
     * High-speed native XLSX parsing supporting:
     * - Multi-sheet workbooks (aggregates all sheets containing valid welding data)
     * - Multi-row / stacked headers (categories + subheaders)
     * - Vertical merged cells propagation (ISO lines and Spools remain associated to all joints)
     * - Accurate field weld vs shop spool differentiation
     * - Comprehensive header mapping (French, English, Italian industrial piping)
     */
    fun parseXlsxBytes(bytes: ByteArray): List<WeldJoint> {
        val sharedStrings = mutableListOf<String>()
        val sheets = mutableMapOf<String, ByteArray>()
        var workbookXmlBytes: ByteArray? = null

        try {
            val zis = ZipInputStream(ByteArrayInputStream(bytes))
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name == "xl/sharedStrings.xml") {
                    sharedStrings.addAll(parseSharedStrings(zis.readBytes()))
                } else if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                    sheets[name] = zis.readBytes()
                } else if (name == "xl/workbook.xml") {
                    workbookXmlBytes = zis.readBytes()
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

        val sheetNamesMap = parseSheetNamesFromWorkbook(workbookXmlBytes)
        val allWelds = mutableListOf<WeldJoint>()

        // Sort sheets in natural order (sheet1.xml, sheet2.xml, etc.)
        val sortedSheetKeys = sheets.keys.sortedWith(Comparator { a, b ->
            val numA = a.filter { it.isDigit() }.toIntOrNull() ?: 0
            val numB = b.filter { it.isDigit() }.toIntOrNull() ?: 0
            numA.compareTo(numB)
        })

        for (sheetKey in sortedSheetKeys) {
            val sheetBytes = sheets[sheetKey] ?: continue
            val sheetIndex = sheetKey.filter { it.isDigit() }.toIntOrNull() ?: 1
            val humanSheetName = sheetNamesMap[sheetIndex] ?: "Feuille $sheetIndex"

            val rows = parseSheetXml(sheetBytes, sharedStrings)
            val welds = extractWeldsFromRows(rows, sheetFallbackName = humanSheetName)
            if (welds.isNotEmpty()) {
                allWelds.addAll(welds)
            }
        }

        return allWelds
    }

    private fun parseSheetNamesFromWorkbook(workbookXmlBytes: ByteArray?): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        if (workbookXmlBytes == null) return map

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(workbookXmlBytes), "UTF-8")

            var eventType = parser.eventType
            var sheetIndexCounter = 1
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "sheet") {
                    val name = parser.getAttributeValue(null, "name")
                    val sheetId = parser.getAttributeValue(null, "sheetId")?.toIntOrNull() ?: sheetIndexCounter
                    if (!name.isNullOrBlank()) {
                        map[sheetId] = name.trim()
                    }
                    sheetIndexCounter++
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
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
     * Handles stacked headers and vertical merged-cells fill-down.
     */
    private fun extractWeldsFromRows(rows: List<List<String>>, sheetFallbackName: String): List<WeldJoint> {
        if (rows.size < 2) return emptyList()

        val maxHeaderScan = minOf(30, rows.size)
        var bestHeaderRowIdx = -1
        var bestScore = -1

        for (i in 0 until maxHeaderScan) {
            val score = scoreHeaderRow(rows[i])
            if (score > bestScore) {
                bestScore = score
                bestHeaderRowIdx = i
            }
        }

        // Must find at least 2 relevant welding keywords to qualify as a valid welding data table
        if (bestScore < 2 || bestHeaderRowIdx == -1) {
            return emptyList()
        }

        // Multi-tier header resolution: combine adjacent header rows if row i+1 or row i-1 is also a header
        val (headerIndices, dataStartIdx) = buildStackedHeaderIndices(rows, bestHeaderRowIdx)

        val welds = mutableListOf<WeldJoint>()
        var lastValidLineNo = ""
        var lastValidSpoolNo = ""
        var lastValidDrawingNo = ""
        var lastValidWpsNo = ""
        var lastValidMaterial = ""

        for (i in dataStartIdx until rows.size) {
            val row = rows[i]
            if (row.isEmpty() || row.all { it.isBlank() }) continue

            // Skip summary/subtotal or signature lines
            val firstCell = row.firstOrNull { it.isNotBlank() }?.lowercase()?.trim() ?: ""
            if (firstCell.startsWith("total") || firstCell.startsWith("sum") ||
                firstCell.startsWith("visa") || firstCell.startsWith("sign") ||
                firstCell.startsWith("page ") || firstCell.contains("approb") ||
                firstCell.startsWith("date :") || firstCell.startsWith("date:")
            ) {
                continue
            }

            // Read raw values from row
            fun getCell(idx: Int): String = if (idx in row.indices) row[idx].trim() else ""

            val rawJoint = getCell(headerIndices.jointNo)
            val rawLine = getCell(headerIndices.lineNo)
            val rawSpool = getCell(headerIndices.spoolNo)
            val rawDrawing = getCell(headerIndices.drawingNo)
            val rawWps = getCell(headerIndices.wpsNo)
            val rawMaterial = getCell(headerIndices.material)

            // If the row doesn't have a joint, a line, and a spool, it is an empty or separator row
            if (rawJoint.isBlank() && rawLine.isBlank() && rawSpool.isBlank()) {
                continue
            }

            // Carry-forward / fill-down for merged vertical cells in Excel
            if (rawLine.isNotBlank()) {
                if (rawLine != lastValidLineNo) {
                    lastValidLineNo = rawLine
                    // If line changed, reset spool unless row explicitly defines a spool
                    lastValidSpoolNo = if (rawSpool.isNotBlank()) rawSpool else ""
                }
            }

            if (rawSpool.isNotBlank()) {
                lastValidSpoolNo = rawSpool
            }

            if (rawDrawing.isNotBlank()) lastValidDrawingNo = rawDrawing
            if (rawWps.isNotBlank()) lastValidWpsNo = rawWps
            if (rawMaterial.isNotBlank()) lastValidMaterial = rawMaterial

            // Effective Line
            val effectiveLine = when {
                rawLine.isNotBlank() -> rawLine
                lastValidLineNo.isNotBlank() -> lastValidLineNo
                sheetFallbackName.isNotBlank() -> sheetFallbackName
                else -> "ISO-01"
            }

            // Effective Spool
            val effectiveSpool = when {
                rawSpool.isNotBlank() -> rawSpool
                lastValidSpoolNo.isNotBlank() -> lastValidSpoolNo
                else -> "" // Left blank if field weld / unspooled
            }

            // Effective Joint No
            val effectiveJoint = when {
                rawJoint.isNotBlank() -> rawJoint
                else -> "W-${welds.size + 1}"
            }

            val weld = buildWeldFromRow(
                row = row,
                h = headerIndices,
                effectiveLine = effectiveLine,
                effectiveSpool = effectiveSpool,
                effectiveJoint = effectiveJoint,
                lastValidDrawing = lastValidDrawingNo,
                lastValidWps = lastValidWpsNo,
                lastValidMat = lastValidMaterial,
                fallbackIndex = welds.size + 1
            )

            welds.add(weld)
        }

        return welds
    }

    private fun buildStackedHeaderIndices(rows: List<List<String>>, bestRowIdx: Int): Pair<HeaderIndices, Int> {
        val maxCol = rows[bestRowIdx].size
        val bestScore = scoreHeaderRow(rows[bestRowIdx])
        val hasSubheader = bestRowIdx + 1 < rows.size && isSubheaderRow(rows[bestRowIdx + 1], bestScore)

        val combinedHeaders = (0 until maxCol).map { col ->
            val parts = mutableListOf<String>()

            // Check previous row if it looks like a parent header
            if (bestRowIdx > 0 && isSubheaderRow(rows[bestRowIdx - 1], bestScore)) {
                val prev = rows[bestRowIdx - 1].getOrNull(col)?.trim() ?: ""
                if (prev.isNotBlank()) parts.add(prev)
            }

            val current = rows[bestRowIdx].getOrNull(col)?.trim() ?: ""
            if (current.isNotBlank()) parts.add(current)

            // Check next row if it looks like a subheader
            if (hasSubheader) {
                val next = rows[bestRowIdx + 1].getOrNull(col)?.trim() ?: ""
                if (next.isNotBlank()) parts.add(next)
            }

            parts.joinToString(" ")
        }

        val indices = mapHeaders(combinedHeaders)

        // Determine where actual data rows start
        val dataStart = if (hasSubheader) bestRowIdx + 2 else bestRowIdx + 1

        return Pair(indices, dataStart)
    }

    private fun isSubheaderRow(row: List<String>, parentHeaderScore: Int): Boolean {
        if (row.isEmpty() || row.all { it.isBlank() }) return false

        // Data rows have numeric entries (e.g. 6.0, 7.11, 8, etc.) or joint numbers
        val hasNumbers = row.any { it.matches(Regex("""^-?\d+([.,]\d+)?$""")) }
        if (hasNumbers) return false

        val firstCell = row.firstOrNull { it.isNotBlank() }?.trim() ?: ""
        if (firstCell.matches(Regex("""^[WwJj]-?\d+.*""")) ||
            firstCell.matches(Regex("""^\d{1,4}[A-Za-z]?$"""))
        ) {
            return false
        }

        val score = scoreHeaderRow(row)
        return score >= 3 && score >= (parentHeaderScore / 2)
    }

    private fun scoreHeaderRow(row: List<String>): Int {
        var score = 0
        val longKeywords = listOf(
            "joint", "soudure", "giunto",
            "ligne", "linea", "piping",
            "spool", "troncon", "tronçon", "tronc", "pezzo", "repère", "repere",
            "welder", "soudeur", "saldatore", "poinçon", "poincon", "matricule",
            "procedure", "procédé", "procede",
            "diameter", "diametre", "diamètre", "pouce",
            "thick", "epaisseur", "épaisseur", "spessore",
            "material", "materiau", "matériau", "nuance",
            "pointage", "visuel", "visual", "rapport",
            "partie", "composant", "element", "élément"
        )
        val shortKeywords = setOf(
            "iso", "wps", "dmos", "qmos", "cnd", "ndt", "rt", "ut", "pt", "mt", "vt",
            "dn", "wt", "dia", "mat", "rep", "coul", "heat", "cast", "charge", "cce", "weld", "line", "stamp"
        )

        for (cell in row) {
            val c = cell.lowercase().trim()
            if (c.isBlank()) continue
            if (longKeywords.any { c.contains(it) }) {
                score++
            } else {
                val words = c.split(Regex("[^a-z0-9]+"))
                if (words.any { it in shortKeywords }) {
                    score++
                }
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
        val notes: Int = -1,
        val part1: Int = -1,
        val part2: Int = -1,
        val heatNo1: Int = -1,
        val heatNo2: Int = -1,
        val singleHeatNo: Int = -1,
        val rtResult: Int = -1,
        val ptResult: Int = -1,
        val utResult: Int = -1
    ) {
        fun getDetectedColumns(): List<String> {
            val list = mutableListOf<String>()
            if (lineNo != -1) list.add("ISO")
            if (spoolNo != -1) list.add("SPOOL")
            if (jointNo != -1) list.add("JOINT")
            if (part1 != -1) list.add("PART 1")
            if (heatNo1 != -1 || singleHeatNo != -1) list.add("PART 1 HEAT NUMBER")
            if (part2 != -1) list.add("PART 2")
            if (heatNo2 != -1 || singleHeatNo != -1) list.add("PART 2 HEAT NUMBER")
            if (wpsNo != -1) list.add("WPS")
            if (welderId != -1) list.add("WELDER")
            if (process != -1) list.add("PROCESS")
            if (rtResult != -1 || ndtType != -1) list.add("RT")
            if (ptResult != -1) list.add("PT")
            if (utResult != -1) list.add("UT")
            return list
        }
    }

    var lastDetectedColumns: List<String> = emptyList()

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
        var part1 = -1
        var part2 = -1
        var heatNo1 = -1
        var heatNo2 = -1
        var singleHeatNo = -1
        var rtResult = -1
        var ptResult = -1
        var utResult = -1

        // 1. First pass: High-precision matching for Joint, Line, Spool
        for ((idx, hRaw) in headers.withIndex()) {
            val h = hRaw.lowercase().trim()

            // Isométrie / Ligne ("Isometric Dwg No.", "ISO Number")
            if (lineNo == -1) {
                if (h.contains("isometric dwg") || h.contains("dwg no") || h.contains("iso") ||
                    h.contains("isometrique") || h.contains("isométrique") ||
                    h.contains("isometric") || h.contains("line no") || h.contains("ligne") ||
                    h.contains("linea") || h.contains("piping") || h.contains("n° ligne") ||
                    h.contains("no ligne") || h.contains("n° iso") || h.contains("no iso") ||
                    h.contains("code ligne") || h.contains("ligne n°") || h.contains("iso n°")
                ) {
                    lineNo = idx
                }
            }

            // Tronçon / Spool ("SPOOL N°", "Spool Number")
            if (spoolNo == -1) {
                if (h.contains("spool") || h.contains("troncon") || h.contains("tronçon") ||
                    h.contains("tronc") || h.contains("pezzo") || h.contains("pezzi") ||
                    h.contains("rep spool") || h.contains("rep. spool") || h.contains("repère spool") ||
                    h.contains("repere spool") || h.contains("rep. tronçon") || h.contains("rep. troncon") ||
                    h.contains("rep.") || h == "rep" || h == "repere" || h == "repère" ||
                    h.contains("repere") || h.contains("repère") || h.contains("mark") ||
                    h.contains("piece") || h.contains("pièce") || h.contains("skid") ||
                    h.contains("spl") || h.contains("prefab") || h.contains("element") ||
                    h.contains("élément") || h.contains("spool no") || h.contains("spool n°") ||
                    h.contains("n° spool") || h.contains("no spool")
                ) {
                    spoolNo = idx
                }
            }

            // Joint No ("Joint No.", "Joint Number", "Weld No")
            if (jointNo == -1) {
                if (h.contains("joint no") || h.contains("joint n°") || h.contains("n° joint") ||
                    h.contains("no joint") || h.contains("weld no") || h.contains("weld n°") ||
                    h.contains("n° soudure") || h.contains("soudure n°") || h.contains("no soudure") ||
                    h.contains("weld id") || h.contains("giunto") || h.contains("weld_id") ||
                    h == "jnt" || h == "w#" || h == "j#" || h.contains("joint/weld")
                ) {
                    jointNo = idx
                }
            }
        }

        // 2. Second pass: Other technical welding & CND columns, Parts & Heat Numbers
        for ((idx, hRaw) in headers.withIndex()) {
            val h = hRaw.lowercase().trim()
            if (idx == lineNo || idx == spoolNo || idx == jointNo) continue

            when {
                // Specific NDT Results ("Résultat RT", "RT Result", "Résultat PT", "PT Result", "Résultat UT", "UT Result")
                rtResult == -1 && (h.contains("rt result") || h.contains("résultat rt") || h.contains("resultat rt") || h.contains("rt res") || h == "rt" || h.contains("radiograph")) -> rtResult = idx
                ptResult == -1 && (h.contains("pt result") || h.contains("résultat pt") || h.contains("resultat pt") || h.contains("pt res") || h == "pt" || h.contains("penetrant") || h.contains("ressuage")) -> ptResult = idx
                utResult == -1 && (h.contains("ut result") || h.contains("résultat ut") || h.contains("resultat ut") || h.contains("ut res") || h == "ut" || h.contains("ultrason")) -> utResult = idx

                // Secondary check for Joint if not found
                jointNo == -1 && (h.contains("soudure") || h.contains("joint") || h.contains("weld") || h == "no" || h == "n°") -> jointNo = idx

                // Parts / Composants ("PART 01", "PART 02")
                part1 == -1 && (h.contains("part 01") || h.contains("part 1") || h.contains("partie 1") || h.contains("partie1") || h.contains("part_1") ||
                    h.contains("element 1") || h.contains("élément 1") || h.contains("element1") || h.contains("élément1") ||
                    h.contains("item 1") || h.contains("item1") || h.contains("comp 1") || h.contains("comp1") ||
                    h.contains("composant 1") || h.contains("composant1") || h.contains("piece 1") || h.contains("pièce 1") ||
                    h.contains("desig 1") || h.contains("désig 1") || h.contains("designation 1") || h.contains("désignation 1") ||
                    h.contains("mat 1") || h.contains("matiere 1") || h.contains("matière 1") ||
                    h == "p1" || h == "elt 1" || h.contains("type 1") || h.contains("part(1)")
                ) -> part1 = idx

                part2 == -1 && (h.contains("part 02") || h.contains("part 2") || h.contains("partie 2") || h.contains("partie2") || h.contains("part_2") ||
                    h.contains("element 2") || h.contains("élément 2") || h.contains("element2") || h.contains("élément2") ||
                    h.contains("item 2") || h.contains("item2") || h.contains("comp 2") || h.contains("comp2") ||
                    h.contains("composant 2") || h.contains("composant2") || h.contains("piece 2") || h.contains("pièce 2") ||
                    h.contains("desig 2") || h.contains("désig 2") || h.contains("designation 2") || h.contains("désignation 2") ||
                    h.contains("mat 2") || h.contains("matiere 2") || h.contains("matière 2") ||
                    h == "p2" || h == "elt 2" || h.contains("type 2") || h.contains("part(2)")
                ) -> part2 = idx

                // Heat Numbers / N° de Coulée ("HEAT Number PART 01", "HEAT Number PART 02")
                heatNo1 == -1 && (h.contains("heat number part 01") || h.contains("heat number part 1") || h.contains("heat 1") || h.contains("heat no 1") || h.contains("heat no. 1") || h.contains("heat n° 1") ||
                    h.contains("heat_1") || h.contains("heat1") || h.contains("coulee 1") || h.contains("coulée 1") ||
                    h.contains("coulee1") || h.contains("coulée1") || h.contains("n° coulee 1") || h.contains("n° coulée 1") ||
                    h.contains("no coulee 1") || h.contains("no coulée 1") || h.contains("cce 1") || h.contains("cce1") ||
                    h.contains("ccm 1") || h.contains("ccm1") || h.contains("heat(1)") || h.contains("ht 1") || h.contains("ht1") ||
                    h.contains("coul. 1") || h.contains("coul 1") || h.contains("coul1") || h.contains("coul.1") ||
                    h.contains("cast 1") || h.contains("charge 1") || h.contains("lot 1")
                ) -> heatNo1 = idx

                heatNo2 == -1 && (h.contains("heat number part 02") || h.contains("heat number part 2") || h.contains("heat 2") || h.contains("heat no 2") || h.contains("heat no. 2") || h.contains("heat n° 2") ||
                    h.contains("heat_2") || h.contains("heat2") || h.contains("coulee 2") || h.contains("coulée 2") ||
                    h.contains("coulee2") || h.contains("coulée2") || h.contains("n° coulee 2") || h.contains("n° coulée 2") ||
                    h.contains("no coulee 2") || h.contains("no coulée 2") || h.contains("cce 2") || h.contains("cce2") ||
                    h.contains("ccm 2") || h.contains("ccm2") || h.contains("heat(2)") || h.contains("ht 2") || h.contains("ht2") ||
                    h.contains("coul. 2") || h.contains("coul 2") || h.contains("coul2") || h.contains("coul.2") ||
                    h.contains("cast 2") || h.contains("charge 2") || h.contains("lot 2")
                ) -> heatNo2 = idx

                singleHeatNo == -1 && (h.contains("heat no") || h.contains("heat number") || h.contains("heat n°") ||
                    h.contains("n° coulee") || h.contains("n° coulée") || h.contains("no coulee") || h.contains("coulée") ||
                    h.contains("coulee") || h.contains("cce") || h.contains("ccm") || h == "heat" || h == "ht" ||
                    h.contains("coulée/heat") || h.contains("coulee/heat") || h.contains("heat/coulée")
                ) -> singleHeatNo = idx

                drawingNo == -1 && (h.contains("drawing") || h.contains("plan") || h.contains("dwg") || h.contains("disegno")) -> drawingNo = idx
                welderName == -1 && (h.contains("nom") || h.contains("welder name") || h.contains("nom soudeur") || h.contains("nome")) -> welderName = idx
                welderId == -1 && (h.contains("welded by") || h.contains("welder") || h.contains("soudeur") || h.contains("saldatore") || h.contains("stamp") || h.contains("poinçon") || h.contains("poincon") || h.contains("matricule") || h == "w" || h == "op") -> welderId = idx
                wpsNo == -1 && (h.contains("wps no") || h.contains("wps") || h.contains("dmos") || h.contains("qmos") || h.contains("procedure") || h.contains("pqr")) -> wpsNo = idx
                process == -1 && (h.contains("process") || h.contains("procédé") || h.contains("procede") || h.contains("proc")) -> process = idx
                weldType == -1 && (h.contains("weld type") || h.contains("type") || h.contains("tipo") || h.contains("bw/sw") || h.contains("joint type")) -> weldType = idx
                material == -1 && (h.contains("material") || h.contains("matériau") || h.contains("materiau") || h.contains("materiale") || h.contains("nuance") || h.contains("metal") || h.contains("grade")) -> material = idx
                diameter == -1 && (h.contains("dia") || h.contains("pouce") || h.contains("inch") || h.contains("dn") || h.contains("size") || h.contains("nd") || h == "ø") -> diameter = idx
                thickness == -1 && (h.contains("thick") || h.contains("epaisseur") || h.contains("épaisseur") || h.contains("spessore") || h.contains("ép") || h.contains("ep") || h.contains("sched") || h.contains("wt")) -> thickness = idx
                weldDate == -1 && (h.contains("welding date") || h.contains("weld date") || (h.contains("date") || h.contains("data")) && !h.contains("fitup") && !h.contains("visuel") && !h.contains("ndt")) -> weldDate = idx
                fitup == -1 && (h.contains("fitup") || h.contains("fit-up") || h.contains("pointage") || h.contains("assemblage") || h.contains("accoppiamento")) -> fitup = idx
                visual == -1 && (h.contains("visuel") || h.contains("vt") || h.contains("visual") || h.contains("visivo")) -> visual = idx
                ndtReport == -1 && (h.contains("report") || h.contains("rapport") || h.contains("pv") || h.contains("doc")) -> ndtReport = idx
                ndtResult == -1 && (h.contains("ndt res") || h.contains("cnd res") || h.contains("rt res") || h.contains("result") || h.contains("résultat") || h.contains("resultat") || h.contains("esito") || h.contains("decision") || h.contains("décision")) -> ndtResult = idx
                ndtType == -1 && (h.contains("ndt") || h.contains("cnd") || h.contains("rt") || h.contains("ut") || h.contains("controle") || h.contains("contrôle")) -> ndtType = idx
                status == -1 && (h.contains("status") || h.contains("statut") || h.contains("etat") || h.contains("état") || h.contains("stato")) -> status = idx
                notes == -1 && (h.contains("note") || h.contains("remarque") || h.contains("obs") || h.contains("comment") || h.contains("annotazioni")) -> notes = idx
            }
        }

        if (jointNo == -1 && headers.isNotEmpty()) jointNo = 0
        if (lineNo == -1 && headers.size > 1) lineNo = 1

        val res = HeaderIndices(
            jointNo, lineNo, spoolNo, drawingNo, welderId, welderName,
            wpsNo, process, weldType, material, diameter, thickness,
            weldDate, fitup, visual, ndtType, ndtResult, ndtReport, status, notes,
            part1, part2, heatNo1, heatNo2, singleHeatNo,
            rtResult, ptResult, utResult
        )
        lastDetectedColumns = res.getDetectedColumns()
        return res
    }

    private fun buildWeldFromRow(
        row: List<String>,
        h: HeaderIndices,
        effectiveLine: String,
        effectiveSpool: String,
        effectiveJoint: String,
        lastValidDrawing: String,
        lastValidWps: String,
        lastValidMat: String,
        fallbackIndex: Int
    ): WeldJoint {
        fun get(idx: Int, default: String = ""): String =
            if (idx in row.indices && row[idx].trim().isNotBlank()) row[idx].trim() else default

        val drawingNo = get(h.drawingNo, lastValidDrawing)
        val welderId = get(h.welderId, "W-01")
        val welderName = get(h.welderName, "Welder $welderId")
        val wpsNo = get(h.wpsNo, if (lastValidWps.isNotBlank()) lastValidWps else "WPS-01")
        val process = get(h.process, "GTAW+SMAW")
        val weldType = get(h.weldType, "BW")
        val material = get(h.material, if (lastValidMat.isNotBlank()) lastValidMat else "A106 Gr.B")

        val diaStr = get(h.diameter, "4.0").replace(",", ".").replace("\"", "").replace("DN", "").trim()
        val diameter = diaStr.toDoubleOrNull() ?: 4.0

        val thickStr = get(h.thickness, "6.0").replace(",", ".").replace("mm", "").trim()
        val thickness = thickStr.toDoubleOrNull() ?: 6.0

        val rawDate = get(h.weldDate, "")
        val weldDate = if (rawDate.isNotBlank()) parseExcelDate(rawDate) else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Parse Inspection statuses
        val rawFitup = get(h.fitup, "").uppercase()
        val fitupStatus = when {
            rawFitup.contains("ACC") || rawFitup.contains("CONF") || rawFitup.contains("OK") || rawFitup == "A" || rawFitup.contains("VAL") -> "ACCEPTED"
            rawFitup.contains("REJ") || rawFitup.contains("REF") || rawFitup.contains("NON") -> "REJECTED"
            else -> "ACCEPTED"
        }

        val rawVisual = get(h.visual, "").uppercase()
        val visualStatus = when {
            rawVisual.contains("ACC") || rawVisual.contains("CONF") || rawVisual.contains("OK") || rawVisual == "A" || rawVisual.contains("VAL") -> "ACCEPTED"
            rawVisual.contains("REJ") || rawVisual.contains("REF") || rawVisual.contains("NON") -> "REJECTED"
            else -> "ACCEPTED"
        }

        val rawNdtType = get(h.ndtType, "RT").uppercase()
        val ndtType = when {
            rawNdtType.contains("UT") || rawNdtType.contains("ULTRA") -> "UT"
            rawNdtType.contains("PT") || rawNdtType.contains("RESSU") || rawNdtType.contains("LIQU") -> "PT"
            rawNdtType.contains("MT") || rawNdtType.contains("MAG") -> "MT"
            rawNdtType.contains("VT") || rawNdtType.contains("ONLY") || rawNdtType.contains("NON") -> "VT ONLY"
            else -> "RT"
        }

        val rawNdtRes = get(h.ndtResult, "").uppercase()
        val ndtResult = when {
            rawNdtRes.contains("ACC") || rawNdtRes.contains("CONF") || rawNdtRes.contains("OK") || rawNdtRes == "A" || rawNdtRes.contains("VAL") -> "ACCEPTED"
            rawNdtRes.contains("REJ") || rawNdtRes.contains("REP") || rawNdtRes.contains("NON") -> "REJECTED"
            rawNdtRes.contains("REQ") || rawNdtRes.contains("ATT") || rawNdtRes.contains("PEND") -> "PENDING"
            rawNdtRes.contains("NOT") || rawNdtRes.contains("N/A") || ndtType == "VT ONLY" -> "NOT_REQUIRED"
            else -> if (get(h.ndtReport, "").isNotBlank()) "ACCEPTED" else "PENDING"
        }

        val ndtReportNo = get(h.ndtReport, "")

        val rawStatus = get(h.status, "").uppercase()
        val status = when {
            rawStatus.contains("REP") || ndtResult == "REJECTED" || visualStatus == "REJECTED" -> "REPAIR_REQUIRED"
            rawStatus.contains("COMP") || rawStatus.contains("TERM") || rawStatus.contains("CONF") -> "COMPLETED"
            rawStatus.contains("PROG") || rawStatus.contains("EN COURS") -> "IN_PROGRESS"
            visualStatus == "PENDING" || fitupStatus == "PENDING" -> "IN_PROGRESS"
            ndtResult == "PENDING" -> "PENDING_NDT"
            else -> "COMPLETED"
        }

        val notes = get(h.notes, "")

        // Parts / Composants
        val part1 = get(h.part1, "")
        val part2 = get(h.part2, "")

        // Heat Numbers / N° de Coulée
        var heatNo1 = get(h.heatNo1, "")
        var heatNo2 = get(h.heatNo2, "")

        if (heatNo1.isBlank() && heatNo2.isBlank() && h.singleHeatNo != -1) {
            val rawHeat = get(h.singleHeatNo, "")
            if (rawHeat.isNotBlank()) {
                val parts = when {
                    rawHeat.contains("/") -> rawHeat.split("/")
                    rawHeat.contains("+") -> rawHeat.split("+")
                    rawHeat.contains(" - ") -> rawHeat.split(" - ")
                    rawHeat.contains(",") -> rawHeat.split(",")
                    else -> listOf(rawHeat)
                }
                heatNo1 = parts.getOrNull(0)?.trim() ?: ""
                heatNo2 = parts.getOrNull(1)?.trim() ?: ""
            }
        }

        val rt = if (h.rtResult != -1) get(h.rtResult, "N/A") else if (ndtType == "RT") ndtResult else "N/A"
        val pt = if (h.ptResult != -1) get(h.ptResult, "N/A") else if (ndtType == "PT") ndtResult else "N/A"
        val ut = if (h.utResult != -1) get(h.utResult, "N/A") else if (ndtType == "UT") ndtResult else "N/A"

        return WeldJoint(
            id = 0,
            spoolId = 0,
            isoId = 0,
            isoNumber = effectiveLine,
            spoolNumber = effectiveSpool,
            jointNumber = effectiveJoint,
            part1Description = part1,
            part1Material = material,
            part1HeatNumber = heatNo1,
            part2Description = part2,
            part2Material = material,
            part2HeatNumber = heatNo2,
            weldType = weldType,
            process = process,
            wps = wpsNo,
            welder = welderId,
            date = weldDate,
            diameterInch = diameter,
            thicknessMm = thickness,
            visual = visualStatus,
            rt = rt,
            pt = pt,
            ut = ut,
            drawingNo = drawingNo,
            welderName = welderName,
            ndtReportNo = ndtReportNo,
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
        sb.append("Joint No,Line No,Spool No,Drawing No,Part 1,Heat No 1,Part 2,Heat No 2,Welder ID,Welder Name,WPS,Process,Weld Type,Material,Diameter (Inch),Thickness (mm),Weld Date,Fit-up,Visual VT,NDT Type,NDT Result,NDT Report,Overall Status,Notes\n")

        for (w in welds) {
            val row = listOf(
                escapeCsv(w.jointNo),
                escapeCsv(w.lineNo),
                escapeCsv(w.spoolNo),
                escapeCsv(w.drawingNo),
                escapeCsv(w.part1),
                escapeCsv(w.heatNo1),
                escapeCsv(w.part2),
                escapeCsv(w.heatNo2),
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
