package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DuplicateWeldGroup
import com.example.data.ExcelInspectionResult
import com.example.data.Isometric
import com.example.data.Spool
import com.example.data.SyncSuccessSummary
import com.example.data.WeldJoint
import com.example.data.WeldRepository
import com.example.util.PdfExportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

sealed interface SyncUiState {
    object Idle : SyncUiState
    object Syncing : SyncUiState
    data class Success(val message: String, val count: Int) : SyncUiState
    data class Error(val message: String) : SyncUiState
}

enum class SearchCategory(val label: String) {
    ALL("Tous les champs"),
    HEAT_NUMBER("Tous les Heat Numbers"),
    HEAT_NUMBER_PART1("Heat Number Part 1"),
    HEAT_NUMBER_PART2("Heat Number Part 2"),
    ISO("N° Isométrie (ISO)"),
    SPOOL("N° Spool"),
    JOINT("N° Joint"),
    WELDER("Soudeur")
}

class WeldViewModel(
    private val repository: WeldRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            // Empty database on first launch per WDB v1.0 specifications
            repository.checkAndSeedInitialData()
        }
    }

    val allWelds: StateFlow<List<WeldJoint>> = repository.allWelds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allIsometrics: StateFlow<List<Isometric>> = repository.allIsometrics
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalWeldsCount: StateFlow<Int> = repository.totalWeldsCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // --- Inspection & Multi-Source Import ---
    private val _pendingInspection = MutableStateFlow<ExcelInspectionResult?>(null)
    val pendingInspection: StateFlow<ExcelInspectionResult?> = _pendingInspection.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    private val _lastSyncSource = MutableStateFlow<String?>(null)
    val lastSyncSource: StateFlow<String?> = _lastSyncSource.asStateFlow()

    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    private val _syncSuccessSummary = MutableStateFlow<SyncSuccessSummary?>(null)
    val syncSuccessSummary: StateFlow<SyncSuccessSummary?> = _syncSuccessSummary.asStateFlow()

    fun dismissSyncSuccess() {
        _syncSuccessSummary.value = null
    }

    /**
     * Executes the direct SYNC WDB workflow:
     * Google Sheet -> Download latest data -> Validate columns -> Update local WDB database -> Refresh ISO/SPOOL/JOINT
     * Then displays SyncSuccessDialog with the exact format:
     * File: WDB Google Sheet
     * Records imported: XXXX
     * ISO: XXXX
     * SPOOL: XXXX
     * JOINTS: XXXX
     */
    fun syncWdbGoogleSheet(customUrl: String? = null) {
        viewModelScope.launch {
            _syncState.value = SyncUiState.Syncing
            val targetUrl = customUrl?.trim()?.ifBlank { null } ?: WeldRepository.DEFAULT_GOOGLE_SHEET_URL
            val result = repository.syncWdbFromGoogleSheet(targetUrl)
            result.fold(
                onSuccess = { summary ->
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    _lastSyncSource.value = "WDB Google Sheet"
                    _syncState.value = SyncUiState.Success(
                        "Synchronisation WDB terminée : ${summary.recordsImported} joints importés depuis Google Sheets.",
                        summary.recordsImported
                    )
                    _syncSuccessSummary.value = summary
                    // Auto-refresh filters
                    _selectedIso.value = null
                    _selectedSpool.value = null
                    _selectedJointForDetail.value = null
                },
                onFailure = { error ->
                    _syncState.value = SyncUiState.Error("Erreur de synchronisation WDB : ${error.localizedMessage ?: "Inaccessible"}")
                }
            )
        }
    }

    fun inspectFile(inputStream: InputStream, filename: String, sourceDescription: String = "Fichier local (.xlsx/.csv)") {
        viewModelScope.launch {
            _syncState.value = SyncUiState.Syncing
            val result = repository.inspectFromInputStream(inputStream, filename, sourceDescription)
            result.fold(
                onSuccess = { inspectRes ->
                    _pendingInspection.value = inspectRes
                    _syncState.value = SyncUiState.Idle
                },
                onFailure = { error ->
                    _syncState.value = SyncUiState.Error("Échec de lecture : ${error.localizedMessage ?: "Format non reconnu"}")
                }
            )
        }
    }

    fun inspectUrl(url: String, sourceDescription: String) {
        viewModelScope.launch {
            _syncState.value = SyncUiState.Syncing
            val result = repository.inspectFromUrl(url, sourceDescription)
            result.fold(
                onSuccess = { inspectRes ->
                    _pendingInspection.value = inspectRes
                    _syncState.value = SyncUiState.Idle
                },
                onFailure = { error ->
                    _syncState.value = SyncUiState.Error("Erreur téléchargement : ${error.localizedMessage ?: "URL invalide ou inaccessible"}")
                }
            )
        }
    }

    fun confirmPendingInspection(replaceExisting: Boolean = true) {
        val inspection = _pendingInspection.value ?: return
        viewModelScope.launch {
            _syncState.value = SyncUiState.Syncing
            val result = repository.commitInspectedWelds(inspection.parsedWelds, replaceExisting)
            result.fold(
                onSuccess = { count ->
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    _lastSyncSource.value = inspection.sourceDescription
                    _syncState.value = SyncUiState.Success("${count} joints de soudure importés avec succès depuis ${inspection.sourceDescription} !", count)
                    _pendingInspection.value = null
                    val first = inspection.parsedWelds.firstOrNull()?.isoNumber
                    if (first != null && _selectedIso.value == null) {
                        _selectedIso.value = first
                    }
                },
                onFailure = { error ->
                    _syncState.value = SyncUiState.Error("Erreur lors de l'enregistrement : ${error.localizedMessage}")
                }
            )
        }
    }

    fun cancelPendingInspection() {
        _pendingInspection.value = null
    }

    fun resetSyncState() {
        _syncState.value = SyncUiState.Idle
    }

    // --- Cascade Selection State: ISO -> SPOOL -> JOINT ---
    private val _selectedIso = MutableStateFlow<String?>(null)
    val selectedIso: StateFlow<String?> = _selectedIso.asStateFlow()

    private val _selectedSpool = MutableStateFlow<String?>(null)
    val selectedSpool: StateFlow<String?> = _selectedSpool.asStateFlow()

    private val _selectedJointForDetail = MutableStateFlow<WeldJoint?>(null)
    val selectedJointForDetail: StateFlow<WeldJoint?> = _selectedJointForDetail.asStateFlow()

    val distinctIsos: StateFlow<List<String>> = allWelds.map { welds ->
        welds.map { it.isoNumber.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }.flowOn(Dispatchers.Default)
    .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val spoolsForSelectedIso: StateFlow<List<String>> = combine(allWelds, _selectedIso) { welds, iso ->
        if (iso.isNullOrBlank()) emptyList()
        else welds.filter { it.isoNumber.equals(iso, ignoreCase = true) }
            .map { it.spoolNumber.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }.flowOn(Dispatchers.Default)
    .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val jointsForSelectedSpool: StateFlow<List<WeldJoint>> = combine(allWelds, _selectedIso, _selectedSpool) { welds, iso, spool ->
        when {
            iso.isNullOrBlank() -> emptyList()
            spool.isNullOrBlank() -> welds.filter { it.isoNumber.equals(iso, ignoreCase = true) }.sortedBy { it.jointNumber }
            else -> welds.filter { it.isoNumber.equals(iso, ignoreCase = true) && it.spoolNumber.equals(spool, ignoreCase = true) }
                .sortedBy { it.jointNumber }
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    fun selectIso(iso: String?) {
        _selectedIso.value = iso
        _selectedSpool.value = null
        _selectedJointForDetail.value = null
    }

    fun selectSpool(spool: String?) {
        _selectedSpool.value = spool
        _selectedJointForDetail.value = null
    }

    fun selectJointForDetail(joint: WeldJoint?) {
        _selectedJointForDetail.value = joint
    }

    // --- Search Engine State with Heat Number Part 1 & Part 2 ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchCategory = MutableStateFlow(SearchCategory.ALL)
    val searchCategory: StateFlow<SearchCategory> = _searchCategory.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchCategory(category: SearchCategory) {
        _searchCategory.value = category
    }

    val searchResults: StateFlow<List<WeldJoint>> = combine(allWelds, _searchQuery, _searchCategory) { welds, query, category ->
        val q = query.trim()
        if (q.isBlank()) {
            emptyList()
        } else {
            welds.filter { w ->
                when (category) {
                    SearchCategory.ALL ->
                        w.jointNumber.contains(q, ignoreCase = true) ||
                        w.isoNumber.contains(q, ignoreCase = true) ||
                        w.spoolNumber.contains(q, ignoreCase = true) ||
                        w.part1HeatNumber.contains(q, ignoreCase = true) ||
                        w.part2HeatNumber.contains(q, ignoreCase = true) ||
                        w.welder.contains(q, ignoreCase = true) ||
                        w.part1Description.contains(q, ignoreCase = true) ||
                        w.part2Description.contains(q, ignoreCase = true)

                    SearchCategory.HEAT_NUMBER ->
                        w.part1HeatNumber.contains(q, ignoreCase = true) ||
                        w.part2HeatNumber.contains(q, ignoreCase = true)

                    SearchCategory.HEAT_NUMBER_PART1 ->
                        w.part1HeatNumber.contains(q, ignoreCase = true)

                    SearchCategory.HEAT_NUMBER_PART2 ->
                        w.part2HeatNumber.contains(q, ignoreCase = true)

                    SearchCategory.ISO ->
                        w.isoNumber.contains(q, ignoreCase = true)

                    SearchCategory.SPOOL ->
                        w.spoolNumber.contains(q, ignoreCase = true)

                    SearchCategory.JOINT ->
                        w.jointNumber.contains(q, ignoreCase = true)

                    SearchCategory.WELDER ->
                        w.welder.contains(q, ignoreCase = true) ||
                        w.welderName.contains(q, ignoreCase = true)
                }
            }
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    // --- Quality Control / Duplicates ---
    val duplicateWelds: StateFlow<List<WeldJoint>> = repository.duplicateWelds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val duplicateGroups: StateFlow<List<DuplicateWeldGroup>> = duplicateWelds.map { duplicates ->
        duplicates.groupBy { w ->
            val l = w.isoNumber.trim().uppercase()
            val s = w.spoolNumber.trim().uppercase()
            val j = w.jointNumber.trim().uppercase()
            "$l|$s|$j"
        }.map { (key, group) ->
            val first = group.first()
            DuplicateWeldGroup(
                key = key,
                lineNo = first.isoNumber,
                spoolNo = first.spoolNumber,
                jointNo = first.jointNumber,
                count = group.size,
                welds = group
            )
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val duplicateCount: StateFlow<Int> = repository.duplicateCount
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)

    fun removeDuplicateWelds() {
        viewModelScope.launch {
            val groups = duplicateGroups.value
            val toDelete = mutableListOf<WeldJoint>()
            for (g in groups) {
                if (g.welds.size > 1) {
                    toDelete.addAll(g.welds.drop(1))
                }
            }
            if (toDelete.isNotEmpty()) {
                for (w in toDelete) {
                    repository.deleteWeld(w)
                }
                _syncState.value = SyncUiState.Success("${toDelete.size} doublon(s) nettoyé(s) avec succès dans la base WDB.", toDelete.size)
            } else {
                _syncState.value = SyncUiState.Success("Aucun doublon à supprimer dans la base.", 0)
            }
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            repository.clearAll()
            _selectedIso.value = null
            _selectedSpool.value = null
            _selectedJointForDetail.value = null
            _syncState.value = SyncUiState.Success("Base WDB réinitialisée.", 0)
        }
    }

    fun exportJointPdf(context: Context, weld: WeldJoint) {
        PdfExportService.exportJointReport(context, weld)
    }

    // Direct import shortcut for backward compatibility
    fun importLocalFile(inputStream: InputStream, filename: String = "Welding Data Base01.xlsx", replaceExisting: Boolean = true) {
        inspectFile(inputStream, filename)
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
