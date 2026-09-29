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
    private val isometricDao: IsometricDao,
    private val spoolDao: SpoolDao,
    private val context: Context
) {
    val allWelds: Flow<List<WeldJoint>> = weldJointDao.getAllWelds()
    val allIsometrics: Flow<List<Isometric>> = isometricDao.getAllIsometrics()
    val duplicateWelds: Flow<List<WeldJoint>> = weldJointDao.getDuplicateWelds()
    val duplicateCount: Flow<Int> = weldJointDao.getDuplicateCount()
    val totalWeldsCount: Flow<Int> = weldJointDao.getCountFlow()

    fun getDistinctLines(): Flow<List<String>> = weldJointDao.getDistinctLines()
    fun getDistinctSpools(isoNumber: String): Flow<List<String>> = weldJointDao.getDistinctSpools(isoNumber)
    fun getWeldsByIsometric(isoNumber: String): Flow<List<WeldJoint>> = weldJointDao.getWeldsByIsometric(isoNumber)
    fun getWeldsByIsometricAndSpool(isoNumber: String, spoolNumber: String): Flow<List<WeldJoint>> =
        weldJointDao.getWeldsByIsometricAndSpool(isoNumber, spoolNumber)

    fun filterWelds(
        isoNumber: String? = null,
        spoolNumber: String? = null,
        status: String? = null,
        welder: String? = null
    ): Flow<List<WeldJoint>> = weldJointDao.filterWelds(isoNumber, spoolNumber, status, welder)

    fun searchWelds(query: String): Flow<List<WeldJoint>> {
        return if (query.isBlank()) {
            weldJointDao.getAllWelds()
        } else {
            weldJointDao.searchWelds(query.trim())
        }
    }

    fun searchByHeatNumber(heatNumber: String): Flow<List<WeldJoint>> {
        return if (heatNumber.isBlank()) {
            weldJointDao.getAllWelds()
        } else {
            weldJointDao.searchByHeatNumber(heatNumber.trim())
        }
    }

    fun searchByWelder(welder: String): Flow<List<WeldJoint>> {
        return if (welder.isBlank()) {
            weldJointDao.getAllWelds()
        } else {
            weldJointDao.searchByWelder(welder.trim())
        }
    }

    suspend fun getDuplicateWeldsSnapshot(): List<WeldJoint> = withContext(Dispatchers.IO) {
        weldJointDao.getDuplicateWeldsSnapshot()
    }

    suspend fun findDuplicates(
        isoNumber: String,
        spoolNumber: String,
        jointNumber: String,
        excludeId: Long = 0
    ): List<WeldJoint> = withContext(Dispatchers.IO) {
        weldJointDao.findDuplicates(isoNumber, spoolNumber, jointNumber, excludeId)
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
        isometricDao.clearAll()
        spoolDao.clearAll()
    }

    suspend fun getCount(): Int = withContext(Dispatchers.IO) {
        weldJointDao.getCount()
    }

    /**
     * Requirement: The application must start empty.
     * No demo or placeholder data seeded.
     */
    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        // Deliberately empty: Application starts empty per WDB v1.0 specification
    }

    /**
     * Inspects a file from InputStream without committing to the database.
     */
    suspend fun inspectFromInputStream(
        inputStream: InputStream,
        filenameHint: String = "Welding Data Base01.xlsx",
        sourceDescription: String = "Fichier local"
    ): Result<ExcelInspectionResult> = withContext(Dispatchers.IO) {
        try {
            val result = ExcelParser.inspectAndParseStream(inputStream, filenameHint, sourceDescription)
            if (result.parsedWelds.isEmpty()) {
                Result.failure(Exception("Aucun joint de soudure détecté dans $filenameHint."))
            } else {
                Result.success(result)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inspects a remote file from Google Drive, OneDrive, or HTTP URL without committing to the database.
     */
    suspend fun inspectFromUrl(
        rawUrl: String,
        sourceDescription: String = "Cloud / URL"
    ): Result<ExcelInspectionResult> = withContext(Dispatchers.IO) {
        try {
            var url = rawUrl.trim()
            if (url.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("L'URL ne peut pas être vide."))
            }

            if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                url = "https://$url"
            }

            // Google Drive / Google Sheets transformation
            if (url.contains("drive.google.com/file/d/")) {
                val fileId = url.substringAfter("file/d/").substringBefore("/")
                url = "https://drive.google.com/uc?export=download&id=$fileId"
            } else if (url.contains("docs.google.com/spreadsheets/d/")) {
                val sheetId = url.substringAfter("spreadsheets/d/").substringBefore("/")
                url = "https://docs.google.com/spreadsheets/d/$sheetId/export?format=xlsx"
            }

            // OneDrive / SharePoint download link adjustment
            if (url.contains("sharepoint.com") || url.contains("1drv.ms")) {
                if (!url.contains("download=1")) {
                    url = if (url.contains("?")) "$url&download=1" else "$url?download=1"
                }
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile) WDB-WeldingDataBase/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Échec du téléchargement (${response.code} : ${response.message})"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Fichier distant vide."))
            val bytes = body.bytes()

            // Check if Google returned an HTML auth/login page instead of real file
            val isHtml = bytes.size > 10 && (
                bytes.take(100).toByteArray().toString(Charsets.UTF_8).contains("<html", ignoreCase = true) ||
                bytes.take(100).toByteArray().toString(Charsets.UTF_8).contains("<!doctype", ignoreCase = true) ||
                bytes.take(200).toByteArray().toString(Charsets.UTF_8).contains("ServiceLogin", ignoreCase = true)
            )
            if (isHtml) {
                return@withContext Result.failure(
                    Exception("Le document Google Sheets nécessite une authentification ou un droit d'accès. Assurez-vous que le lien est configuré sur 'Tous les utilisateurs disposant du lien peuvent voir' ou importez directement le fichier XLSX.")
                )
            }

            val filename = if (url.contains(".csv", ignoreCase = true)) "Welding Data Base01.csv" else "Welding Data Base01.xlsx"

            val result = ExcelParser.inspectAndParseStream(bytes.inputStream(), filename, sourceDescription)
            if (result.parsedWelds.isEmpty()) {
                Result.failure(Exception("Le fichier téléchargé ne contient pas de données de soudage valides."))
            } else {
                Result.success(result)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Commits pre-inspected welds into the database (building isometrics and spools relational hierarchy).
     */
    suspend fun commitInspectedWelds(
        parsedWelds: List<WeldJoint>,
        replaceExisting: Boolean = true
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (parsedWelds.isEmpty()) {
                return@withContext Result.failure(Exception("Liste de joints vide."))
            }

            if (replaceExisting) {
                clearAll()
            }

            // Populate Isometrics, Spools and enriched Welds
            val isoGroups = parsedWelds.groupBy { it.isoNumber.trim().ifBlank { "ISO-01" } }
            val allEnrichedWelds = mutableListOf<WeldJoint>()

            for ((isoNum, weldsInIso) in isoGroups) {
                var iso = isometricDao.findByNumber(isoNum)
                val isoId = if (iso != null) {
                    iso.id
                } else {
                    isometricDao.insert(
                        Isometric(
                            isoNumber = isoNum,
                            drawingNo = weldsInIso.firstOrNull { it.drawingNo.isNotBlank() }?.drawingNo ?: "",
                            totalJoints = weldsInIso.size
                        )
                    )
                }

                val spoolGroups = weldsInIso.groupBy { it.spoolNumber.trim().ifBlank { "SP-01" } }
                for ((spoolNum, weldsInSpool) in spoolGroups) {
                    var spool = spoolDao.findSpool(isoNum, spoolNum)
                    val spoolId = if (spool != null) {
                        spool.id
                    } else {
                        spoolDao.insert(
                            Spool(
                                isoId = isoId,
                                isoNumber = isoNum,
                                spoolNumber = spoolNum,
                                totalJoints = weldsInSpool.size
                            )
                        )
                    }

                    for (w in weldsInSpool) {
                        allEnrichedWelds.add(
                            w.copy(
                                isoId = isoId,
                                spoolId = spoolId,
                                isoNumber = isoNum,
                                spoolNumber = spoolNum
                            )
                        )
                    }
                }
            }

            weldJointDao.insertAll(allEnrichedWelds)
            Result.success(parsedWelds.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Import Excel (XLSX, CSV, TSV) from an InputStream and rebuild relational tables.
     */
    suspend fun importFromInputStream(
        inputStream: InputStream,
        filenameHint: String = "Welding Data Base01.xlsx",
        replaceExisting: Boolean = true
    ): Result<Int> = withContext(Dispatchers.IO) {
        val inspectRes = inspectFromInputStream(inputStream, filenameHint)
        inspectRes.fold(
            onSuccess = { res -> commitInspectedWelds(res.parsedWelds, replaceExisting) },
            onFailure = { err -> Result.failure(err) }
        )
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    companion object {
        const val DEFAULT_GOOGLE_SHEET_URL =
            "https://docs.google.com/spreadsheets/d/1d20rVFyyJT6a5MJIHpuLJtzNKJRTqm4s/edit"
    }

    /**
     * Executes the full WDB synchronization workflow per prompt specifications:
     * Google Sheet -> Download latest data -> Validate columns -> Update local WDB database -> Refresh ISO/SPOOL/JOINT
     * Returns SyncSuccessSummary with real numbers only:
     * File: WDB Google Sheet
     * Records imported: XXXX
     * ISO: XXXX
     * SPOOL: XXXX
     * JOINTS: XXXX
     */
    suspend fun syncWdbFromGoogleSheet(
        sheetUrl: String = DEFAULT_GOOGLE_SHEET_URL
    ): Result<SyncSuccessSummary> = withContext(Dispatchers.IO) {
        try {
            var url = sheetUrl.trim()
            if (url.isEmpty()) url = DEFAULT_GOOGLE_SHEET_URL

            val sheetId = when {
                url.contains("spreadsheets/d/") -> url.substringAfter("spreadsheets/d/").substringBefore("/")
                url.contains("file/d/") -> url.substringAfter("file/d/").substringBefore("/")
                url.contains("id=") -> url.substringAfter("id=").substringBefore("&")
                else -> "1d20rVFyyJT6a5MJIHpuLJtzNKJRTqm4s"
            }

            // Attempt all candidate Google Sheets download endpoints
            val candidateUrls = listOf(
                "https://docs.google.com/spreadsheets/d/$sheetId/export?format=xlsx",
                "https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv",
                "https://drive.google.com/uc?export=download&id=$sheetId",
                "https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv"
            )

            var downloadedResult: ExcelInspectionResult? = null
            var lastError: Exception? = null

            for (candidate in candidateUrls) {
                try {
                    val inspectRes = inspectFromUrl(candidate, "WDB Google Sheet")
                    if (inspectRes.isSuccess) {
                        val res = inspectRes.getOrThrow()
                        if (res.parsedWelds.isNotEmpty()) {
                            downloadedResult = res
                            break
                        }
                    } else {
                        lastError = inspectRes.exceptionOrNull() as? Exception
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }

            if (downloadedResult == null || downloadedResult.parsedWelds.isEmpty()) {
                val msg = lastError?.localizedMessage
                    ?: "Impossible d'accéder au fichier Google Sheets. Veuillez vérifier que le lien est configuré sur 'Tous les utilisateurs disposant du lien peuvent voir' ou importer directement le fichier Excel."
                return@withContext Result.failure(Exception(msg))
            }

            val finalInspection = downloadedResult

            // Validate required columns
            val requiredKeywords = listOf("ISO", "SPOOL", "JOINT")
            val hasRequired = requiredKeywords.all { kw ->
                finalInspection.detectedColumns.any { it.contains(kw, ignoreCase = true) }
            }
            if (!hasRequired) {
                return@withContext Result.failure(Exception("Validation des colonnes échouée : Colonnes ISO / SPOOL / JOINT manquantes dans la feuille."))
            }

            // Update local WDB database (replaces empty database)
            val commitRes = commitInspectedWelds(finalInspection.parsedWelds, replaceExisting = true)
            if (commitRes.isFailure) {
                return@withContext Result.failure(commitRes.exceptionOrNull() ?: Exception("Erreur d'insertion dans la base WDB."))
            }

            // Real counts from the spreadsheet only
            val isos = finalInspection.parsedWelds.map { it.isoNumber.trim() }.filter { it.isNotBlank() }.distinct()
            val spools = finalInspection.parsedWelds.map { "${it.isoNumber}|${it.spoolNumber}" }.distinct()
            val totalJoints = finalInspection.parsedWelds.size

            val summary = SyncSuccessSummary(
                fileName = "WDB Google Sheet",
                recordsImported = totalJoints,
                isoCount = isos.size,
                spoolCount = spools.size,
                jointsCount = totalJoints
            )

            Result.success(summary)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun syncFromUrl(rawUrl: String, replaceExisting: Boolean = true): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var url = rawUrl.trim()
            if (url.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("URL cannot be empty"))
            }

            if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                url = "http://$url"
            }

            if (url.contains("drive.google.com/file/d/")) {
                val fileId = url.substringAfter("file/d/").substringBefore("/")
                url = "https://drive.google.com/uc?export=download&id=$fileId"
            }

            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Erreur HTTP ${response.code}: ${response.message}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Réponse vide"))
            val bytes = body.bytes()
            val filename = if (url.contains(".csv")) "database.csv" else "Welding Data Base01.xlsx"

            importFromInputStream(bytes.inputStream(), filename, replaceExisting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportToCsv(): Flow<String> = kotlinx.coroutines.flow.flow {
        val welds = weldJointDao.getAllWeldsSnapshot()
        emit(ExcelParser.exportToCsv(welds))
    }
}
