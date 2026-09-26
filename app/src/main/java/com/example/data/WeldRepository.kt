package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit

class WeldRepository(
    private val weldJointDao: WeldJointDao,
    private val context: Context
) {
    val allWelds: Flow<List<WeldJoint>> = weldJointDao.getAllWelds()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun searchWelds(query: String): Flow<List<WeldJoint>> {
        return if (query.isBlank()) {
            weldJointDao.getAllWelds()
        } else {
            weldJointDao.searchWelds(query.trim())
        }
    }

    suspend fun insertWeld(weld: WeldJoint): Long = withContext(Dispatchers.IO) {
        weldJointDao.insertWeld(weld)
    }

    suspend fun updateWeld(weld: WeldJoint) = withContext(Dispatchers.IO) {
        weldJointDao.updateWeld(weld.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteWeld(weld: WeldJoint) = withContext(Dispatchers.IO) {
        weldJointDao.deleteWeld(weld)
    }

    suspend fun deleteWeldById(id: Long) = withContext(Dispatchers.IO) {
        weldJointDao.deleteWeldById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        weldJointDao.clearAllWelds()
    }

    /**
     * Download and parse an Excel / Google Sheets URL.
     * Automatically converts Google Sheets edit URLs to direct CSV export format.
     */
    suspend fun syncFromUrl(rawUrl: String, replaceExisting: Boolean = true): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var url = rawUrl.trim()
            if (url.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("URL cannot be empty"))
            }

            // Normalize URL protocol
            if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                url = "http://$url"
            }

            // Convert Google Drive sharing link into direct download link
            // Example: https://drive.google.com/file/d/FILE_ID/view?usp=sharing -> https://drive.google.com/uc?export=download&id=FILE_ID
            if (url.contains("drive.google.com/file/d/")) {
                val fileId = url.substringAfter("file/d/").substringBefore("/")
                url = "https://drive.google.com/uc?export=download&id=$fileId"
            }

            // Convert Google Sheets edit URL into direct CSV export URL
            // Example: https://docs.google.com/spreadsheets/d/SPREADSHEET_ID/edit#gid=0 -> /export?format=csv
            if (url.contains("docs.google.com/spreadsheets/d/")) {
                val sheetId = url.substringAfter("spreadsheets/d/").substringBefore("/")
                val gid = if (url.contains("gid=")) {
                    url.substringAfter("gid=").substringBefore("&").substringBefore("#")
                } else "0"
                url = "https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv&gid=$gid"
            }

            // Convert Dropbox preview link to direct download
            if (url.contains("dropbox.com") && url.contains("dl=0")) {
                url = url.replace("dl=0", "dl=1")
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "WeldTrack-Android/1.0")
                .header("ngrok-skip-browser-warning", "true") // Bypass ngrok warning page for free tunnels
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
            }

            val responseBody = response.body ?: return@withContext Result.failure(Exception("Empty server response"))
            val bytes = responseBody.bytes()
            val filename = response.header("Content-Disposition") ?: url

            val parsedWelds = if (filename.contains(".xlsx", ignoreCase = true) || (bytes.size > 4 && bytes[0] == 0x50.toByte())) {
                ExcelParser.parseXlsxBytes(bytes)
            } else {
                ExcelParser.parseCsvOrTsv(bytes)
            }

            if (parsedWelds.isEmpty()) {
                return@withContext Result.failure(Exception("Aucune donnée de soudure détectée dans le fichier"))
            }

            if (replaceExisting) {
                weldJointDao.clearAllWelds()
            }
            weldJointDao.insertAll(parsedWelds)

            Result.success(parsedWelds.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Test connection to PC IP or cloud URL without updating the database.
     */
    suspend fun testConnection(rawUrl: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            var url = rawUrl.trim()
            if (url.isEmpty()) {
                return@withContext ConnectionTestResult(false, 0, 0, 0, "L'adresse IP ou URL est vide")
            }
            if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                url = "http://$url"
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "WeldTrack-Android/1.0")
                .header("ngrok-skip-browser-warning", "true")
                .head() // Try HEAD request first for fast ping
                .build()

            var response = try {
                httpClient.newCall(request).execute()
            } catch (e: Exception) {
                // If HEAD fails or is not supported by Python simple HTTP server, fallback to GET
                val getRequest = Request.Builder()
                    .url(url)
                    .header("User-Agent", "WeldTrack-Android/1.0")
                    .header("ngrok-skip-browser-warning", "true")
                    .build()
                httpClient.newCall(getRequest).execute()
            }

            val elapsed = System.currentTimeMillis() - startTime
            val code = response.code
            val contentLength = response.body?.contentLength() ?: 0L
            val sizeKb = if (contentLength > 0) contentLength / 1024 else 0L

            if (response.isSuccessful) {
                ConnectionTestResult(
                    isSuccess = true,
                    statusCode = code,
                    responseTimeMs = elapsed,
                    fileSizeKb = sizeKb,
                    message = "Connexion réussie ($code OK, ${elapsed}ms) ! Fichier Excel accessible."
                )
            } else {
                ConnectionTestResult(
                    isSuccess = false,
                    statusCode = code,
                    responseTimeMs = elapsed,
                    fileSizeKb = 0,
                    message = "Erreur HTTP $code : ${response.message}. Vérifiez le chemin ou le port sur le PC."
                )
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            ConnectionTestResult(
                isSuccess = false,
                statusCode = 0,
                responseTimeMs = elapsed,
                fileSizeKb = 0,
                message = "Inaccessible (${e.localizedMessage ?: "Délai dépassé"}). Vérifiez que le PC est sur le même réseau ou connecté à Internet."
            )
        }
    }

    /**
     * Import from local file input stream (from file picker)
     */
    suspend fun importFromStream(inputStream: InputStream, filename: String, replaceExisting: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val parsedWelds = ExcelParser.parseStream(inputStream, filename)
            if (parsedWelds.isEmpty()) {
                return@withContext Result.failure(Exception("Fichier vide ou format non reconnu"))
            }
            if (replaceExisting) {
                weldJointDao.clearAllWelds()
            }
            weldJointDao.insertAll(parsedWelds)
            Result.success(parsedWelds.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Export all welds in database into CSV format
     */
    suspend fun exportToCsv(): String = withContext(Dispatchers.IO) {
        val welds = weldJointDao.getAllWeldsSnapshot()
        ExcelParser.exportToCsv(welds)
    }

    /**
     * Pre-populates the database with realistic industrial welding records if empty.
     */
    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        if (weldJointDao.getCount() == 0) {
            val sampleData = generateRealisticSampleWelds()
            weldJointDao.insertAll(sampleData)
        }
    }

    suspend fun resetToSampleData() = withContext(Dispatchers.IO) {
        weldJointDao.clearAllWelds()
        val sampleData = generateRealisticSampleWelds()
        weldJointDao.insertAll(sampleData)
    }

    private fun generateRealisticSampleWelds(): List<WeldJoint> {
        return listOf(
            WeldJoint(
                jointNo = "W-001",
                lineNo = "08-CS-150-01",
                spoolNo = "SP-01",
                drawingNo = "ISO-PR-401",
                welderId = "S-101",
                welderName = "Marc Dupont",
                wpsNo = "WPS-CS-01",
                process = "GTAW+SMAW",
                weldType = "BW",
                material = "A106 Gr.B",
                diameterInch = 8.0,
                thicknessMm = 8.18,
                weldDate = "2026-09-18",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-L. Blanc",
                fitupDate = "2026-09-17",
                visualStatus = "ACCEPTED",
                visualInspector = "QC-L. Blanc",
                visualDate = "2026-09-19",
                ndtType = "RT",
                ndtResult = "ACCEPTED",
                ndtReportNo = "RT-2026-042",
                ndtDate = "2026-09-20",
                status = "COMPLETED",
                notes = "Passe de racine TIG 100% pénétrée, remplissage électrode 7018 conforme"
            ),
            WeldJoint(
                jointNo = "W-002",
                lineNo = "08-CS-150-01",
                spoolNo = "SP-01",
                drawingNo = "ISO-PR-401",
                welderId = "S-101",
                welderName = "Marc Dupont",
                wpsNo = "WPS-CS-01",
                process = "GTAW+SMAW",
                weldType = "BW",
                material = "A106 Gr.B",
                diameterInch = 8.0,
                thicknessMm = 8.18,
                weldDate = "2026-09-19",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-L. Blanc",
                fitupDate = "2026-09-18",
                visualStatus = "ACCEPTED",
                visualInspector = "QC-L. Blanc",
                visualDate = "2026-09-20",
                ndtType = "RT",
                ndtResult = "ACCEPTED",
                ndtReportNo = "RT-2026-045",
                ndtDate = "2026-09-21",
                status = "COMPLETED",
                notes = "Aspect visuel régulier, surépaisseur 1.5mm"
            ),
            WeldJoint(
                jointNo = "W-003",
                lineNo = "08-CS-150-01",
                spoolNo = "SP-02",
                drawingNo = "ISO-PR-401",
                welderId = "S-104",
                welderName = "Ahmed Benali",
                wpsNo = "WPS-CS-01",
                process = "GTAW+SMAW",
                weldType = "BW",
                material = "A106 Gr.B",
                diameterInch = 6.0,
                thicknessMm = 7.11,
                weldDate = "2026-09-22",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-L. Blanc",
                fitupDate = "2026-09-21",
                visualStatus = "ACCEPTED",
                visualInspector = "QC-L. Blanc",
                visualDate = "2026-09-23",
                ndtType = "RT",
                ndtResult = "PENDING",
                ndtReportNo = "",
                status = "PENDING_NDT",
                notes = "En attente tir radiographique équipe NDT de nuit"
            ),
            WeldJoint(
                jointNo = "W-004",
                lineNo = "08-CS-150-01",
                spoolNo = "SP-02",
                drawingNo = "ISO-PR-401",
                welderId = "S-104",
                welderName = "Ahmed Benali",
                wpsNo = "WPS-CS-01",
                process = "GTAW+SMAW",
                weldType = "BW",
                material = "A106 Gr.B",
                diameterInch = 6.0,
                thicknessMm = 7.11,
                weldDate = "2026-09-23",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-L. Blanc",
                fitupDate = "2026-09-22",
                visualStatus = "REJECTED",
                visualInspector = "QC-L. Blanc",
                visualDate = "2026-09-23",
                ndtType = "RT",
                ndtResult = "REJECTED",
                ndtReportNo = "VT-REP-012",
                repairCount = 1,
                status = "REPAIR_REQUIRED",
                notes = "Caniveau excessif en position 2h à 4h, meulage et reprise exigés"
            ),
            WeldJoint(
                jointNo = "W-005",
                lineNo = "04-SS-300-02",
                spoolNo = "SP-01",
                drawingNo = "ISO-SS-202",
                welderId = "S-201",
                welderName = "Jean Tremblay",
                wpsNo = "WPS-SS-02",
                process = "GTAW",
                weldType = "BW",
                material = "SS 316L",
                diameterInch = 4.0,
                thicknessMm = 6.02,
                weldDate = "2026-09-20",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-M. Robert",
                fitupDate = "2026-09-19",
                visualStatus = "ACCEPTED",
                visualInspector = "QC-M. Robert",
                visualDate = "2026-09-21",
                ndtType = "PT",
                ndtResult = "ACCEPTED",
                ndtReportNo = "PT-2026-088",
                ndtDate = "2026-09-21",
                status = "COMPLETED",
                notes = "Inertage argon certifié O2 < 50ppm, ressuage ressué sans anomalie"
            ),
            WeldJoint(
                jointNo = "W-006",
                lineNo = "04-SS-300-02",
                spoolNo = "SP-01",
                drawingNo = "ISO-SS-202",
                welderId = "S-201",
                welderName = "Jean Tremblay",
                wpsNo = "WPS-SS-02",
                process = "GTAW",
                weldType = "BW",
                material = "SS 316L",
                diameterInch = 4.0,
                thicknessMm = 6.02,
                weldDate = "2026-09-21",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-M. Robert",
                fitupDate = "2026-09-20",
                visualStatus = "ACCEPTED",
                visualInspector = "QC-M. Robert",
                visualDate = "2026-09-22",
                ndtType = "PT",
                ndtResult = "ACCEPTED",
                ndtReportNo = "PT-2026-091",
                ndtDate = "2026-09-22",
                status = "COMPLETED",
                notes = "Soudure propre, coloration dorée après brossage inox"
            ),
            WeldJoint(
                jointNo = "W-007",
                lineNo = "04-SS-300-02",
                spoolNo = "SP-02",
                drawingNo = "ISO-SS-202",
                welderId = "S-201",
                welderName = "Jean Tremblay",
                wpsNo = "WPS-SS-02",
                process = "GTAW",
                weldType = "BW",
                material = "SS 316L",
                diameterInch = 3.0,
                thicknessMm = 5.49,
                weldDate = "2026-09-24",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-M. Robert",
                fitupDate = "2026-09-23",
                visualStatus = "ACCEPTED",
                visualInspector = "QC-M. Robert",
                visualDate = "2026-09-24",
                ndtType = "PT",
                ndtResult = "PENDING",
                ndtReportNo = "",
                status = "PENDING_NDT",
                notes = "Contrôle ressuage programmé cet après-midi"
            ),
            WeldJoint(
                jointNo = "FW-008",
                lineNo = "04-SS-300-02",
                spoolNo = "SP-02",
                drawingNo = "ISO-SS-202",
                welderId = "S-201",
                welderName = "Jean Tremblay",
                wpsNo = "WPS-SS-03",
                process = "GTAW",
                weldType = "FW",
                material = "SS 316L",
                diameterInch = 2.0,
                thicknessMm = 3.91,
                weldDate = "2026-09-25",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-M. Robert",
                fitupDate = "2026-09-24",
                visualStatus = "ACCEPTED",
                visualInspector = "QC-M. Robert",
                visualDate = "2026-09-25",
                ndtType = "VT ONLY",
                ndtResult = "NOT_REQUIRED",
                ndtReportNo = "",
                status = "COMPLETED",
                notes = "Soudure d'angle sur bride Slip-On conforme ASME B31.3"
            ),
            WeldJoint(
                jointNo = "W-009",
                lineNo = "12-CS-600-03",
                spoolNo = "SP-01",
                drawingNo = "ISO-HP-505",
                welderId = "S-305",
                welderName = "Sofiane Kaci",
                wpsNo = "WPS-CS-02",
                process = "GTAW+SMAW",
                weldType = "BW",
                material = "A333 Gr.6",
                diameterInch = 12.0,
                thicknessMm = 17.48,
                weldDate = "2026-09-17",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-L. Blanc",
                fitupDate = "2026-09-16",
                visualStatus = "ACCEPTED",
                visualInspector = "QC-L. Blanc",
                visualDate = "2026-09-18",
                ndtType = "UT",
                ndtResult = "ACCEPTED",
                ndtReportNo = "UT-2026-112",
                ndtDate = "2026-09-19",
                status = "COMPLETED",
                notes = "Gros diamètre haute pression, contrôle ultrasons 100% sans défaut"
            ),
            WeldJoint(
                jointNo = "W-010",
                lineNo = "12-CS-600-03",
                spoolNo = "SP-01",
                drawingNo = "ISO-HP-505",
                welderId = "S-305",
                welderName = "Sofiane Kaci",
                wpsNo = "WPS-CS-02",
                process = "GTAW+SMAW",
                weldType = "BW",
                material = "A333 Gr.6",
                diameterInch = 12.0,
                thicknessMm = 17.48,
                weldDate = "2026-09-18",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-L. Blanc",
                fitupDate = "2026-09-17",
                visualStatus = "ACCEPTED",
                visualInspector = "QC-L. Blanc",
                visualDate = "2026-09-19",
                ndtType = "UT",
                ndtResult = "ACCEPTED",
                ndtReportNo = "UT-2026-115",
                ndtDate = "2026-09-20",
                status = "COMPLETED",
                notes = "Éprouvé et validé pour épreuve hydrostatique"
            ),
            WeldJoint(
                jointNo = "W-011",
                lineNo = "12-CS-600-03",
                spoolNo = "SP-02",
                drawingNo = "ISO-HP-505",
                welderId = "S-101",
                welderName = "Marc Dupont",
                wpsNo = "WPS-CS-02",
                process = "GTAW+SMAW",
                weldType = "BW",
                material = "A333 Gr.6",
                diameterInch = 10.0,
                thicknessMm = 15.09,
                weldDate = "2026-09-24",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-L. Blanc",
                fitupDate = "2026-09-23",
                visualStatus = "PENDING",
                visualInspector = "",
                visualDate = "",
                ndtType = "UT",
                ndtResult = "PENDING",
                status = "IN_PROGRESS",
                notes = "Passe terminale en cours de refroidissement"
            ),
            WeldJoint(
                jointNo = "W-012",
                lineNo = "06-DP-150-04",
                spoolNo = "SP-01",
                drawingNo = "ISO-DP-101",
                welderId = "S-402",
                welderName = "David Leroy",
                wpsNo = "WPS-DUPLEX-01",
                process = "GTAW",
                weldType = "BW",
                material = "Duplex 2205",
                diameterInch = 6.0,
                thicknessMm = 7.11,
                weldDate = "2026-09-22",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-M. Robert",
                fitupDate = "2026-09-21",
                visualStatus = "ACCEPTED",
                visualInspector = "QC-M. Robert",
                visualDate = "2026-09-23",
                ndtType = "RT",
                ndtResult = "ACCEPTED",
                ndtReportNo = "RT-2026-068",
                ndtDate = "2026-09-24",
                status = "COMPLETED",
                notes = "Soudure Duplex gaz de protection Ar+2%N2, température interpasse < 150°C"
            ),
            WeldJoint(
                jointNo = "W-013",
                lineNo = "06-DP-150-04",
                spoolNo = "SP-01",
                drawingNo = "ISO-DP-101",
                welderId = "S-402",
                welderName = "David Leroy",
                wpsNo = "WPS-DUPLEX-01",
                process = "GTAW",
                weldType = "BW",
                material = "Duplex 2205",
                diameterInch = 6.0,
                thicknessMm = 7.11,
                weldDate = "2026-09-23",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-M. Robert",
                fitupDate = "2026-09-22",
                visualStatus = "ACCEPTED",
                visualInspector = "QC-M. Robert",
                visualDate = "2026-09-24",
                ndtType = "RT",
                ndtResult = "ACCEPTED",
                ndtReportNo = "RT-2026-070",
                ndtDate = "2026-09-25",
                status = "COMPLETED",
                notes = "Pénétration intégrale et ferrite mesurée entre 40% et 55%"
            ),
            WeldJoint(
                jointNo = "SW-014",
                lineNo = "02-CS-150-05",
                spoolNo = "SP-01",
                drawingNo = "ISO-INST-12",
                welderId = "S-104",
                welderName = "Ahmed Benali",
                wpsNo = "WPS-CS-03",
                process = "GTAW",
                weldType = "SW",
                material = "A106 Gr.B",
                diameterInch = 1.5,
                thicknessMm = 3.68,
                weldDate = "2026-09-25",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-L. Blanc",
                fitupDate = "2026-09-24",
                visualStatus = "ACCEPTED",
                visualInspector = "QC-L. Blanc",
                visualDate = "2026-09-25",
                ndtType = "MT",
                ndtResult = "ACCEPTED",
                ndtReportNo = "MT-2026-033",
                ndtDate = "2026-09-25",
                status = "COMPLETED",
                notes = "Prise d'instrumentation Socket Weld jeu de 1.6mm respecté"
            ),
            WeldJoint(
                jointNo = "SW-015",
                lineNo = "02-CS-150-05",
                spoolNo = "SP-01",
                drawingNo = "ISO-INST-12",
                welderId = "S-104",
                welderName = "Ahmed Benali",
                wpsNo = "WPS-CS-03",
                process = "GTAW",
                weldType = "SW",
                material = "A106 Gr.B",
                diameterInch = 1.0,
                thicknessMm = 3.38,
                weldDate = "2026-09-26",
                fitupStatus = "ACCEPTED",
                fitupInspector = "QC-L. Blanc",
                fitupDate = "2026-09-25",
                visualStatus = "ACCEPTED",
                visualInspector = "QC-L. Blanc",
                visualDate = "2026-09-26",
                ndtType = "MT",
                ndtResult = "ACCEPTED",
                ndtReportNo = "MT-2026-034",
                ndtDate = "2026-09-26",
                status = "COMPLETED",
                notes = "Soudure d'évent purgeur d'air"
            ),
            WeldJoint(
                jointNo = "W-016",
                lineNo = "08-CS-150-01",
                spoolNo = "SP-03",
                drawingNo = "ISO-PR-401",
                welderId = "S-101",
                welderName = "Marc Dupont",
                wpsNo = "WPS-CS-01",
                process = "GTAW+SMAW",
                weldType = "BW",
                material = "A106 Gr.B",
                diameterInch = 8.0,
                thicknessMm = 8.18,
                weldDate = "2026-09-26",
                fitupStatus = "PENDING",
                fitupInspector = "",
                fitupDate = "",
                visualStatus = "PENDING",
                visualInspector = "",
                visualDate = "",
                ndtType = "RT",
                ndtResult = "PENDING",
                status = "IN_PROGRESS",
                notes = "Chanfreinage terminé, calage des pointages en cours"
            )
        )
    }
}
