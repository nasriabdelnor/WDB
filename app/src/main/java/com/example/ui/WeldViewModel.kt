package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ConnectionTestResult
import com.example.data.IsometricGroup
import com.example.data.LineProgress
import com.example.data.NdtTypeStat
import com.example.data.PcConnectionProfile
import com.example.data.SpoolGroup
import com.example.data.WelderPerformance
import com.example.data.WeldingKpis
import com.example.data.WeldJoint
import com.example.data.WeldRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface SyncUiState {
    object Idle : SyncUiState
    object Syncing : SyncUiState
    data class Success(val message: String, val count: Int) : SyncUiState
    data class Error(val message: String) : SyncUiState
}

class WeldViewModel(
    private val repository: WeldRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    val allWelds: StateFlow<List<WeldJoint>> = repository.allWelds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _lineFilter = MutableStateFlow("ALL")
    val lineFilter: StateFlow<String> = _lineFilter.asStateFlow()

    private val _spoolFilter = MutableStateFlow("ALL")
    val spoolFilter: StateFlow<String> = _spoolFilter.asStateFlow()

    private val _jointFilter = MutableStateFlow("ALL")
    val jointFilter: StateFlow<String> = _jointFilter.asStateFlow()

    private val _welderFilter = MutableStateFlow("ALL")
    val welderFilter: StateFlow<String> = _welderFilter.asStateFlow()

    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    private val _googleSheetUrl = MutableStateFlow("")
    val googleSheetUrl: StateFlow<String> = _googleSheetUrl.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    private val _connectionTestResult = MutableStateFlow<ConnectionTestResult?>(null)
    val connectionTestResult: StateFlow<ConnectionTestResult?> = _connectionTestResult.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _savedPcProfiles = MutableStateFlow<List<PcConnectionProfile>>(
        listOf(
            PcConnectionProfile("1", "PC Bureau (Local Wi-Fi)", "192.168.1.50", "8080", "ISO_WDB_0002.xlsx"),
            PcConnectionProfile("2", "PC Chantier (LAN)", "10.0.0.15", "8000", "ISO_WDB_0002.xlsx"),
            PcConnectionProfile("3", "Accès Internet Public (Ngrok / Cloud)", "mon-pc-wdb.ngrok-free.app", "80", "ISO_WDB_0002.xlsx")
        )
    )
    val savedPcProfiles: StateFlow<List<PcConnectionProfile>> = _savedPcProfiles.asStateFlow()

    // Available spools dynamically filtered by selected line
    val availableSpools: StateFlow<List<String>> = combine(allWelds, lineFilter) { welds, line ->
        val filtered = if (line == "ALL") welds else welds.filter { it.lineNo == line }
        listOf("ALL") + filtered.map { it.spoolNo }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = listOf("ALL"))

    // Available joints dynamically filtered by selected line and spool
    val availableJoints: StateFlow<List<String>> = combine(allWelds, lineFilter, spoolFilter) { welds, line, spool ->
        val filtered = welds.filter { w ->
            (line == "ALL" || w.lineNo == line) &&
            (spool == "ALL" || w.spoolNo == spool)
        }
        listOf("ALL") + filtered.map { it.jointNo }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = listOf("ALL"))

    // Hierarchical Tree View: Isometric Lines -> Spools -> Welded Joints
    val isometricTree: StateFlow<List<IsometricGroup>> = combine(
        allWelds,
        searchQuery,
        statusFilter
    ) { welds, query, status ->
        val filtered = welds.filter { w ->
            val matchesQuery = query.isBlank() ||
                    w.jointNo.contains(query, ignoreCase = true) ||
                    w.lineNo.contains(query, ignoreCase = true) ||
                    w.spoolNo.contains(query, ignoreCase = true) ||
                    w.welderId.contains(query, ignoreCase = true) ||
                    w.welderName.contains(query, ignoreCase = true)

            val matchesStatus = when (status) {
                "ALL" -> true
                "COMPLETED" -> w.status == "COMPLETED" || w.isFullyAccepted
                "IN_PROGRESS" -> w.status == "IN_PROGRESS"
                "PENDING_NDT" -> w.isPendingNdt
                "REPAIR_REQUIRED" -> w.isRepairRequired
                else -> true
            }
            matchesQuery && matchesStatus
        }

        filtered.groupBy { it.lineNo.ifBlank { "Sans Ligne" } }
            .map { (lineNo, lineWelds) ->
                val drawing = lineWelds.firstOrNull { it.drawingNo.isNotBlank() }?.drawingNo ?: ""
                val spools = lineWelds.groupBy { it.spoolNo.ifBlank { "Sans Spool" } }
                    .map { (spoolNo, spoolWelds) ->
                        SpoolGroup(
                            spoolNo = spoolNo,
                            totalJoints = spoolWelds.size,
                            completedJoints = spoolWelds.count { it.isFullyAccepted },
                            repairJoints = spoolWelds.count { it.isRepairRequired },
                            joints = spoolWelds.sortedBy { it.jointNo }
                        )
                    }.sortedBy { it.spoolNo }

                val total = lineWelds.size
                val completed = lineWelds.count { it.isFullyAccepted }
                val repairs = lineWelds.count { it.repairCount > 0 || it.isRepairRequired }
                val rate = if (total > 0) (completed.toFloat() / total) * 100f else 0f

                IsometricGroup(
                    lineNo = lineNo,
                    drawingNo = drawing,
                    totalJoints = total,
                    completedJoints = completed,
                    repairJoints = repairs,
                    completionRate = rate,
                    spools = spools
                )
            }.sortedBy { it.lineNo }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    // Filtered welds based on search & active hierarchical filters (Isométrie -> Spool -> Joint)
    val filteredWelds: StateFlow<List<WeldJoint>> = combine(
        allWelds,
        searchQuery,
        statusFilter,
        lineFilter,
        spoolFilter,
        jointFilter
    ) { args: Array<Any> ->
        @Suppress("UNCHECKED_CAST")
        val welds = args[0] as List<WeldJoint>
        val query = args[1] as String
        val status = args[2] as String
        val line = args[3] as String
        val spool = args[4] as String
        val joint = args[5] as String

        welds.filter { w ->
            val matchesQuery = query.isBlank() ||
                    w.jointNo.contains(query, ignoreCase = true) ||
                    w.lineNo.contains(query, ignoreCase = true) ||
                    w.spoolNo.contains(query, ignoreCase = true) ||
                    w.welderId.contains(query, ignoreCase = true) ||
                    w.welderName.contains(query, ignoreCase = true) ||
                    w.wpsNo.contains(query, ignoreCase = true) ||
                    w.material.contains(query, ignoreCase = true) ||
                    w.ndtReportNo.contains(query, ignoreCase = true)

            val matchesStatus = when (status) {
                "ALL" -> true
                "COMPLETED" -> w.status == "COMPLETED" || w.isFullyAccepted
                "IN_PROGRESS" -> w.status == "IN_PROGRESS"
                "PENDING_NDT" -> w.isPendingNdt
                "REPAIR_REQUIRED" -> w.isRepairRequired
                else -> true
            }

            val matchesLine = line == "ALL" || w.lineNo.equals(line, ignoreCase = true)
            val matchesSpool = spool == "ALL" || w.spoolNo.equals(spool, ignoreCase = true)
            val matchesJoint = joint == "ALL" || w.jointNo.equals(joint, ignoreCase = true)

            matchesQuery && matchesStatus && matchesLine && matchesSpool && matchesJoint
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Calculated Dashboard KPIs
    val kpis: StateFlow<WeldingKpis> = allWelds.combine(MutableStateFlow(Unit)) { welds, _ ->
        if (welds.isEmpty()) {
            WeldingKpis()
        } else {
            val total = welds.size
            val inchDia = welds.sumOf { it.diameterInch }
            val accepted = welds.count { it.isFullyAccepted }
            val inProgress = welds.count { it.status == "IN_PROGRESS" }
            val pendingNdt = welds.count { it.isPendingNdt }
            val repairs = welds.count { it.isRepairRequired }
            val completion = if (total > 0) (accepted.toFloat() / total) * 100f else 0f
            val defectRate = if (total > 0) (repairs.toFloat() / total) * 100f else 0f

            WeldingKpis(
                totalWelds = total,
                totalInchDia = inchDia,
                acceptedCount = accepted,
                inProgressCount = inProgress,
                pendingNdtCount = pendingNdt,
                repairCount = repairs,
                completionPercentage = completion,
                defectRatePercentage = defectRate
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeldingKpis()
    )

    // Welder Performance Leaderboard
    val welderStats: StateFlow<List<WelderPerformance>> = allWelds.combine(MutableStateFlow(Unit)) { welds, _ ->
        welds.groupBy { it.welderId.ifBlank { "Unassigned" } }
            .map { (welderId, group) ->
                val name = group.firstOrNull { it.welderName.isNotBlank() }?.welderName ?: "Soudeur $welderId"
                val total = group.size
                val totalInch = group.sumOf { it.diameterInch }
                val accepted = group.count { it.isFullyAccepted }
                val repairs = group.count { it.isRepairRequired }
                val defectRate = if (total > 0) (repairs.toFloat() / total) * 100f else 0f
                WelderPerformance(
                    welderId = welderId,
                    welderName = name,
                    totalWelds = total,
                    totalInchDia = totalInch,
                    acceptedWelds = accepted,
                    repairCount = repairs,
                    defectRate = defectRate
                )
            }.sortedByDescending { it.totalWelds }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Isometric Line Progress
    val lineProgressStats: StateFlow<List<LineProgress>> = allWelds.combine(MutableStateFlow(Unit)) { welds, _ ->
        welds.groupBy { it.lineNo.ifBlank { "Sans Ligne" } }
            .map { (lineNo, group) ->
                val total = group.size
                val completed = group.count { it.isFullyAccepted }
                val pct = if (total > 0) (completed.toFloat() / total) * 100f else 0f
                LineProgress(lineNo, total, completed, pct)
            }.sortedByDescending { it.totalWelds }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // NDT Type Breakdown
    val ndtTypeStats: StateFlow<List<NdtTypeStat>> = allWelds.combine(MutableStateFlow(Unit)) { welds, _ ->
        val types = listOf("RT", "UT", "PT", "MT", "VT ONLY")
        types.map { type ->
            val group = welds.filter { it.ndtType == type }
            val total = group.size
            val accepted = group.count { it.ndtResult == "ACCEPTED" || (type == "VT ONLY" && it.visualStatus == "ACCEPTED") }
            val pending = group.count { it.ndtResult == "PENDING" }
            val rejected = group.count { it.ndtResult == "REJECTED" }
            NdtTypeStat(type, total, accepted, pending, rejected)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: String) {
        _statusFilter.value = status
    }

    fun setLineFilter(line: String) {
        _lineFilter.value = line
        // When line changes, reset spool and joint if they no longer apply
        _spoolFilter.value = "ALL"
        _jointFilter.value = "ALL"
    }

    fun setSpoolFilter(spool: String) {
        _spoolFilter.value = spool
        _jointFilter.value = "ALL"
    }

    fun setJointFilter(joint: String) {
        _jointFilter.value = joint
    }

    fun setWelderFilter(welder: String) {
        _welderFilter.value = welder
    }

    fun resetAllFilters() {
        _searchQuery.value = ""
        _statusFilter.value = "ALL"
        _lineFilter.value = "ALL"
        _spoolFilter.value = "ALL"
        _jointFilter.value = "ALL"
        _welderFilter.value = "ALL"
    }

    fun setGoogleSheetUrl(url: String) {
        _googleSheetUrl.value = url
    }

    fun selectLineAndSpool(line: String, spool: String = "ALL", joint: String = "ALL") {
        _lineFilter.value = line
        _spoolFilter.value = spool
        _jointFilter.value = joint
    }

    fun clearSyncMessage() {
        _syncState.value = SyncUiState.Idle
    }

    fun clearConnectionTest() {
        _connectionTestResult.value = null
    }

    fun testPcConnection(
        ipAddress: String,
        port: String = "8080",
        filePath: String = "ISO_WDB_0002.xlsx"
    ) {
        val cleanIp = ipAddress.trim().removePrefix("http://").removePrefix("https://").removeSuffix("/")
        if (cleanIp.isBlank()) {
            _connectionTestResult.value = ConnectionTestResult(false, 0, 0, 0, "Veuillez saisir l'adresse IP de votre PC")
            return
        }

        val cleanPort = port.trim().ifBlank { "8080" }
        val cleanPath = filePath.trim().removePrefix("/")
        val finalUrl = if (cleanIp.contains(".ngrok") || cleanIp.contains("http://") || cleanIp.contains("https://") || cleanPort == "80" || cleanPort == "443") {
            if (cleanIp.startsWith("http")) "$cleanIp/$cleanPath" else "https://$cleanIp/$cleanPath"
        } else {
            "http://$cleanIp:$cleanPort/$cleanPath"
        }

        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionTestResult.value = null
            val result = repository.testConnection(finalUrl)
            _connectionTestResult.value = result
            _isTestingConnection.value = false
        }
    }

    fun syncFromPcIp(
        ipAddress: String,
        port: String = "8080",
        filePath: String = "ISO_WDB_0002.xlsx",
        replaceExisting: Boolean = true
    ) {
        val cleanIp = ipAddress.trim().removePrefix("http://").removePrefix("https://").removeSuffix("/")
        if (cleanIp.isBlank()) {
            _syncState.value = SyncUiState.Error("Veuillez saisir l'adresse IP de votre PC (ex: 192.168.1.50)")
            return
        }

        val cleanPort = port.trim().ifBlank { "8080" }
        val cleanPath = filePath.trim().removePrefix("/")
        val finalUrl = if (cleanIp.contains(".ngrok") || cleanIp.contains("http://") || cleanIp.contains("https://") || cleanPort == "80" || cleanPort == "443") {
            if (cleanIp.startsWith("http")) "$cleanIp/$cleanPath" else "https://$cleanIp/$cleanPath"
        } else {
            "http://$cleanIp:$cleanPort/$cleanPath"
        }

        viewModelScope.launch {
            _syncState.value = SyncUiState.Syncing
            val result = repository.syncFromUrl(finalUrl, replaceExisting)
            result.fold(
                onSuccess = { count ->
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    _syncState.value = SyncUiState.Success(
                        "Connecté avec succès au PC ($cleanIp) ! $count soudures synchronisées depuis le fichier Excel.",
                        count
                    )
                },
                onFailure = { error ->
                    _syncState.value = SyncUiState.Error(
                        "Impossible de joindre le PC sur $finalUrl : ${error.localizedMessage ?: "Vérifiez que le serveur ou partage sur le PC est actif et sur le même réseau Wi-Fi/Internet"}."
                    )
                }
            )
        }
    }

    fun savePcProfile(name: String, ip: String, port: String, filename: String) {
        val newProfile = PcConnectionProfile(
            id = System.currentTimeMillis().toString(),
            name = name.ifBlank { "PC $ip" },
            ip = ip,
            port = port,
            filename = filename
        )
        _savedPcProfiles.value = _savedPcProfiles.value + newProfile
    }

    fun deletePcProfile(id: String) {
        _savedPcProfiles.value = _savedPcProfiles.value.filter { it.id != id }
    }

    fun addWeld(weld: WeldJoint) {
        viewModelScope.launch {
            repository.insertWeld(weld)
        }
    }

    fun updateWeld(weld: WeldJoint) {
        viewModelScope.launch {
            repository.updateWeld(weld)
        }
    }

    fun deleteWeld(weld: WeldJoint) {
        viewModelScope.launch {
            repository.deleteWeld(weld)
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            _syncState.value = SyncUiState.Syncing
            repository.resetToSampleData()
            _lastSyncTimestamp.value = System.currentTimeMillis()
            _syncState.value = SyncUiState.Success("Base de démonstration industrielle rechargée (16 soudures)", 16)
        }
    }

    fun syncFromGoogleUrl(url: String, replaceExisting: Boolean = true) {
        if (url.isBlank()) {
            _syncState.value = SyncUiState.Error("Veuillez saisir un lien Google Sheets ou URL Excel valide")
            return
        }
        viewModelScope.launch {
            _syncState.value = SyncUiState.Syncing
            val result = repository.syncFromUrl(url, replaceExisting)
            result.fold(
                onSuccess = { count ->
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    _syncState.value = SyncUiState.Success("Synchronisation réussie avec Google : $count soudures importées !", count)
                },
                onFailure = { error ->
                    _syncState.value = SyncUiState.Error("Erreur de synchronisation : ${error.localizedMessage ?: "Vérifiez l'URL et les permissions de partage du fichier"}")
                }
            )
        }
    }

    fun importLocalFile(inputStream: InputStream, filename: String, replaceExisting: Boolean = true) {
        viewModelScope.launch {
            _syncState.value = SyncUiState.Syncing
            val result = repository.importFromStream(inputStream, filename, replaceExisting)
            result.fold(
                onSuccess = { count ->
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    _syncState.value = SyncUiState.Success("Fichier Excel importé avec succès : $count soudures chargées !", count)
                },
                onFailure = { error ->
                    _syncState.value = SyncUiState.Error("Erreur d'import : ${error.localizedMessage ?: "Format de fichier non reconnu"}")
                }
            )
        }
    }

    fun exportAndShareExcel(context: Context) {
        viewModelScope.launch {
            try {
                val csvContent = repository.exportToCsv()
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "welding_database_$timeStamp.csv"

                val file = File(context.cacheDir, fileName)
                file.writeText(csvContent, Charsets.UTF_8)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Export Welding DataBase - $fileName")
                    putExtra(Intent.EXTRA_TEXT, "Voici la base de données de soudage mise à jour depuis l'application WeldTrack.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(sendIntent, "Partager la base de soudage Excel")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                e.printStackTrace()
                _syncState.value = SyncUiState.Error("Erreur lors de l'export Excel: ${e.message}")
            }
        }
    }
}

class WeldViewModelFactory(
    private val repository: WeldRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeldViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeldViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
