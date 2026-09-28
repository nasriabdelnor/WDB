package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.East
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.IsometricGroup
import com.example.data.SpoolGroup
import com.example.data.WeldJoint
import com.example.ui.components.InspectionPill
import com.example.ui.components.WeldOverallStatusBadge
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ApprovedGreenContainer
import com.example.ui.theme.ApprovedGreenDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.IndustrialNavy700
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.IndustrialNavy900
import com.example.ui.theme.RejectRed
import com.example.ui.theme.RejectRedContainer
import com.example.ui.theme.RejectRedDark
import com.example.ui.theme.WeldAmber
import com.example.ui.theme.WeldAmberContainer
import com.example.ui.theme.WeldAmberDark

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WeldsListScreen(
    viewModel: WeldViewModel,
    onSelectWeld: (WeldJoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val welds by viewModel.filteredWelds.collectAsStateWithLifecycle()
    val allWelds by viewModel.allWelds.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val lineFilter by viewModel.lineFilter.collectAsStateWithLifecycle()
    val spoolFilter by viewModel.spoolFilter.collectAsStateWithLifecycle()
    val jointFilter by viewModel.jointFilter.collectAsStateWithLifecycle()
    val duplicateCount by viewModel.duplicateCount.collectAsStateWithLifecycle()
    val duplicateIds by viewModel.duplicateIds.collectAsStateWithLifecycle()

    val availableLines = remember(allWelds) {
        val hasBlank = allWelds.any { it.lineNo.isBlank() }
        val lines = allWelds.map { it.lineNo.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("ALL") + (if (hasBlank && lines.isNotEmpty()) listOf("SANS LIGNE") else emptyList()) + lines
    }
    val availableSpools by viewModel.availableSpools.collectAsStateWithLifecycle()
    val availableJoints by viewModel.availableJoints.collectAsStateWithLifecycle()
    val isometricTree by viewModel.isometricTree.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Liste, 1 = Arborescence
    var isHierarchyPanelExpanded by remember { mutableStateOf(false) }
    var showIsoDialog by remember { mutableStateOf(false) }
    var showSpoolDialog by remember { mutableStateOf(false) }
    var showJointDialog by remember { mutableStateOf(false) }

    val hasActiveFilters = lineFilter != "ALL" || spoolFilter != "ALL" || jointFilter != "ALL" ||
            statusFilter != "ALL" || searchQuery.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("welds_list_screen")
    ) {
        // Mode Switcher Tab Row: Liste vs Arborescence ISO
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = ElectricCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = ElectricCyan
                )
            }
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Liste Filtrée (${welds.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                modifier = Modifier.testTag("tab_welds_list")
            )

            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Arborescence Isométrie (${isometricTree.size} ISO)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                modifier = Modifier.testTag("tab_isometric_tree")
            )
        }

        // Universal Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("search_input"),
            placeholder = {
                Text(if (activeTab == 0) "Recherche (ex: W-003, 08-CS-150, SP-02, S-101)…" else "Filtrer l'arborescence (ISO, Spool, Joint)…")
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Recherche", tint = ElectricCyan)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Banner if still showing demo sample welds
        if (allWelds.size <= 16) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ElectricCyan.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Base d'exemple active (16 soudures). Importez votre fichier WDB officiel dans « Synchro WDB » pour charger vos milliers de joints réels.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (activeTab == 1) {
            // MODE 2: ARBORESCENCE ISOMÉTRIE (ISO -> SPOOL -> JOINT)
            IsometricTreeView(
                tree = isometricTree,
                onSelectWeld = onSelectWeld,
                onSelectLineInFilter = { line ->
                    viewModel.setLineFilter(line)
                    activeTab = 0
                }
            )
        } else {
            // MODE 1: LISTE FILTRÉE & FILTRES EN CASCADE
            // HIERARCHICAL FILTER CARD: Compact & Ultra-performant (Isométrie -> Spool -> Joint)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("hierarchy_filter_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Header of filter card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isHierarchyPanelExpanded = !isHierarchyPanelExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ElectricCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Filtres Arborescence (ISO • Spool • Joint)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasActiveFilters) {
                                Text(
                                    text = "Effacer tout",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RejectRed,
                                    modifier = Modifier
                                        .clickable { viewModel.resetAllFilters() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Icon(
                                imageVector = if (isHierarchyPanelExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Basculer",
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 1-Row Compact 3-Button Filter Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Isométrie Quick Button
                        Surface(
                            onClick = { showIsoDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = if (lineFilter != "ALL") ElectricCyan.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (lineFilter != "ALL") ElectricCyan else Color.Transparent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "1. Isométrie", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (lineFilter == "ALL") "Toutes (${availableLines.size - 1})" else lineFilter,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (lineFilter != "ALL") ElectricCyan else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (lineFilter != "ALL") {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Effacer ISO",
                                        tint = RejectRed,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.setLineFilter("ALL") }
                                    )
                                } else {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // 2. Spool Quick Button
                        Surface(
                            onClick = { if (availableSpools.size > 1) showSpoolDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = if (spoolFilter != "ALL") WeldAmber.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (spoolFilter != "ALL") WeldAmber else Color.Transparent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "2. Spool", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (spoolFilter == "ALL") "Tous (${availableSpools.size - 1})" else spoolFilter,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (spoolFilter != "ALL") WeldAmberDark else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (spoolFilter != "ALL") {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Effacer Spool",
                                        tint = RejectRed,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.setSpoolFilter("ALL") }
                                    )
                                } else {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // 3. Joint Quick Button
                        Surface(
                            onClick = { if (availableJoints.size > 1) showJointDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = if (jointFilter != "ALL") ApprovedGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (jointFilter != "ALL") ApprovedGreen else Color.Transparent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "3. Joint", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (jointFilter == "ALL") "Tous (${availableJoints.size - 1})" else jointFilter,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (jointFilter != "ALL") ApprovedGreenDark else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (jointFilter != "ALL") {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Effacer Joint",
                                        tint = RejectRed,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.setJointFilter("ALL") }
                                    )
                                } else {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Quick Spool chips ONLY if a specific line is active
                    if (lineFilter != "ALL" && availableSpools.size > 1) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Spools rapides de $lineFilter :",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val displaySpools = if (availableSpools.size > 30) availableSpools.take(30) else availableSpools
                            items(displaySpools, key = { "quick_spool_$it" }) { spool ->
                                val isSelected = spoolFilter == spool
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setSpoolFilter(spool) },
                                    label = {
                                        Text(
                                            if (spool == "ALL") "Tous" else "Spool $spool",
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WeldAmber.copy(alpha = 0.2f),
                                        selectedLabelColor = WeldAmberDark
                                    )
                                )
                            }
                        }
                    }

                    // Advanced view if user toggles expansion
                    AnimatedVisibility(
                        visible = isHierarchyPanelExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HierarchicalDropdown(
                                label = "1. Isométrie (Ligne)",
                                currentValue = if (lineFilter == "ALL") "Toutes les Isométries" else lineFilter,
                                items = availableLines,
                                itemLabel = { if (it == "ALL") "Toutes les Isométries (${availableLines.size - 1})" else it },
                                onItemSelected = { viewModel.setLineFilter(it) },
                                accentColor = ElectricCyan,
                                testTag = "dropdown_line"
                            )

                            HierarchicalDropdown(
                                label = "2. Tronçon / Spool",
                                currentValue = if (spoolFilter == "ALL") "Tous les Spools (${availableSpools.size - 1})" else spoolFilter,
                                items = availableSpools,
                                itemLabel = { if (it == "ALL") "Tous les Spools" else "Spool $it" },
                                onItemSelected = { viewModel.setSpoolFilter(it) },
                                accentColor = WeldAmber,
                                enabled = availableSpools.size > 1,
                                testTag = "dropdown_spool"
                            )

                            HierarchicalDropdown(
                                label = "3. Joint Soudé Spécifique",
                                currentValue = if (jointFilter == "ALL") "Tous les Joints (${availableJoints.size - 1})" else jointFilter,
                                items = availableJoints,
                                itemLabel = { if (it == "ALL") "Tous les Joints" else "Joint $it" },
                                onItemSelected = { viewModel.setJointFilter(it) },
                                accentColor = ApprovedGreen,
                                enabled = availableJoints.size > 1,
                                testTag = "dropdown_joint"
                            )
                        }
                    }
                }
            }

            // Quick Selection Dialogs triggered from the compact 3-button bar
            if (showIsoDialog) {
                SearchableSelectionDialog(
                    title = "Choisir une Isométrie (Ligne)",
                    items = availableLines,
                    itemLabel = { if (it == "ALL") "Toutes les Isométries (${availableLines.size - 1})" else it },
                    currentValue = lineFilter,
                    accentColor = ElectricCyan,
                    onDismiss = { showIsoDialog = false },
                    onItemSelected = { selected ->
                        viewModel.setLineFilter(selected)
                        showIsoDialog = false
                    }
                )
            }

            if (showSpoolDialog && availableSpools.size > 1) {
                SearchableSelectionDialog(
                    title = if (lineFilter != "ALL") "Spools de $lineFilter" else "Choisir un Spool",
                    items = availableSpools,
                    itemLabel = { if (it == "ALL") "Tous les Spools" else "Spool $it" },
                    currentValue = spoolFilter,
                    accentColor = WeldAmber,
                    onDismiss = { showSpoolDialog = false },
                    onItemSelected = { selected ->
                        viewModel.setSpoolFilter(selected)
                        showSpoolDialog = false
                    }
                )
            }

            if (showJointDialog && availableJoints.size > 1) {
                SearchableSelectionDialog(
                    title = if (spoolFilter != "ALL") "Joints du spool $spoolFilter" else "Choisir un Joint",
                    items = availableJoints,
                    itemLabel = { if (it == "ALL") "Tous les Joints" else "Joint $it" },
                    currentValue = jointFilter,
                    accentColor = ApprovedGreen,
                    onDismiss = { showJointDialog = false },
                    onItemSelected = { selected ->
                        viewModel.setJointFilter(selected)
                        showJointDialog = false
                    }
                )
            }

            // Status Filter Chips (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusOptions = buildList {
                    add("ALL" to "Tous (${welds.size})")
                    add("COMPLETED" to "Conformes")
                    add("IN_PROGRESS" to "En cours")
                    add("PENDING_NDT" to "Attente CND")
                    add("REPAIR_REQUIRED" to "Réparations")
                    if (duplicateCount > 0) {
                        add("DUPLICATES" to "⚠️ Doublons ($duplicateCount)")
                    }
                }

                statusOptions.forEach { (key, label) ->
                    val isSelected = statusFilter == key
                    val isDuplicateChip = key == "DUPLICATES"
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setStatusFilter(key) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isDuplicateChip) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("filter_chip_$key"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDuplicateChip) RejectRedContainer else ElectricCyan.copy(alpha = 0.2f),
                            selectedLabelColor = if (isDuplicateChip) RejectRedDark else ElectricCyan
                        )
                    )
                }
            }

            // Results count summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${welds.size} soudure(s) correspondante(s)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (hasActiveFilters) {
                    Text(
                        text = "Réinitialiser",
                        fontSize = 12.sp,
                        color = ElectricCyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { viewModel.resetAllFilters() }
                            .testTag("reset_filters_btn")
                    )
                }
            }

            // Welds Lazy Column
            if (welds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aucune soudure trouvée",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Aucun joint ne correspond à cette combinaison Isométrie / Spool / Filtre.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.resetAllFilters() }) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Afficher toutes les soudures")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (lineFilter != "ALL" && spoolFilter != "ALL") {
                        item(key = "spool_traceability_header") {
                            SpoolTraceabilityHeaderCard(
                                lineNo = lineFilter,
                                spoolNo = spoolFilter,
                                spoolWelds = welds,
                                onSelectWeld = onSelectWeld
                            )
                        }
                    }

                    items(welds, key = { it.id }) { weld ->
                        WeldCardItem(
                            weld = weld,
                            isDuplicate = duplicateIds.contains(weld.id),
                            onClick = { onSelectWeld(weld) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HierarchicalDropdown(
    label: String,
    currentValue: String,
    items: List<String>,
    itemLabel: (String) -> String,
    onItemSelected: (String) -> Unit,
    accentColor: Color,
    enabled: Boolean = true,
    testTag: String = ""
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        OutlinedTextField(
            value = currentValue,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            trailingIcon = {
                if (enabled) {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Sélectionner",
                            tint = accentColor
                        )
                    }
                }
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { showDialog = true },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
        )
    }

    if (showDialog && enabled) {
        SearchableSelectionDialog(
            title = label,
            items = items,
            itemLabel = itemLabel,
            currentValue = currentValue,
            accentColor = accentColor,
            onDismiss = { showDialog = false },
            onItemSelected = { selected ->
                onItemSelected(selected)
                showDialog = false
            }
        )
    }
}

@Composable
private fun SearchableSelectionDialog(
    title: String,
    items: List<String>,
    itemLabel: (String) -> String,
    currentValue: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onItemSelected: (String) -> Unit
) {
    var filterText by remember { mutableStateOf("") }

    val filteredList = remember(items, filterText) {
        if (filterText.isBlank()) {
            items
        } else {
            val q = filterText.trim().lowercase()
            items.filter { item ->
                item == "ALL" || item.lowercase().contains(q) || itemLabel(item).lowercase().contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredList.size} option(s) sur ${items.size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fast search field
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    placeholder = { Text("Recherche rapide (ex: SP01, 002)...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (filterText.isNotEmpty()) {
                            IconButton(onClick = { filterText = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Virtualized LazyColumn with instant recycling
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredList, key = { it }) { item ->
                        val isSelected = (item == "ALL" && currentValue.startsWith("Tout")) || item == currentValue
                        Surface(
                            onClick = { onItemSelected(item) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = itemLabel(item),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFilterChip(
    label: String,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ElectricCyan.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Supprimer",
                tint = ElectricCyan,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onClear() }
            )
        }
    }
}

@Composable
private fun SpoolTraceabilityHeaderCard(
    lineNo: String,
    spoolNo: String,
    spoolWelds: List<WeldJoint>,
    onSelectWeld: (WeldJoint) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    val drawingNo = spoolWelds.firstOrNull { it.drawingNo.isNotBlank() }?.drawingNo ?: ""
    val completedCount = spoolWelds.count { it.isFullyAccepted }
    val repairCount = spoolWelds.count { it.isRepairRequired }
    val pendingNdtCount = spoolWelds.count { it.isPendingNdt }
    val completionPct = if (spoolWelds.isNotEmpty()) (completedCount * 100) / spoolWelds.size else 0

    // Distinct heat numbers in this spool
    val heatNumbers = remember(spoolWelds) {
        spoolWelds.flatMap { listOf(it.heatNo1.trim(), it.heatNo2.trim()) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("spool_traceability_header_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Spool identity and expand toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(IndustrialNavy800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (spoolNo.equals("SANS SPOOL", ignoreCase = true) || spoolNo.isBlank()) "SOUDURES CHANTIER (SANS SPOOL)" else "SPOOL $spoolNo",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Isométrie : $lineNo" + if (drawingNo.isNotBlank()) " • Plan : $drawingNo" else "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricCyan
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge percentage
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (completionPct == 100) ApprovedGreenContainer else ElectricCyanContainer
                    ) {
                        Text(
                            text = "$completionPct%",
                            color = if (completionPct == 100) ApprovedGreenDark else ElectricCyan,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Quick Stats Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Total Joints", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${spoolWelds.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = ApprovedGreenContainer.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Conformes", fontSize = 10.sp, color = ApprovedGreenDark)
                                Text("$completedCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ApprovedGreenDark)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (repairCount > 0) RejectRedContainer.copy(alpha = 0.5f) else ElectricCyanContainer.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(if (repairCount > 0) "À Réparer" else "Attente CND", fontSize = 10.sp, color = if (repairCount > 0) RejectRedDark else ElectricCyan)
                                Text("${if (repairCount > 0) repairCount else pendingNdtCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (repairCount > 0) RejectRedDark else ElectricCyan)
                            }
                        }
                    }

                    // Heat Numbers summary pills if available
                    if (heatNumbers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Coulées du spool :",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                heatNumbers.forEach { heat ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ElectricCyan.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = heat,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricCyan
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Title for Detailed Joint Traceability Table
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Traçabilité des Pièces & N° Coulée (Heat Numbers)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${spoolWelds.size} joints",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // List of joints with Part 1, Part 2, and Heat numbers
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                spoolWelds.forEach { weld ->
                                    SpoolJointTraceabilityRow(
                                        weld = weld,
                                        onClick = { onSelectWeld(weld) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpoolJointTraceabilityRow(
    weld: WeldJoint,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Line 1: Joint badge + Specs + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(IndustrialNavy800)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = weld.jointNo,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ø ${weld.diameterInch}\" • ${weld.thicknessMm}mm • ${weld.material}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                WeldOverallStatusBadge(weld = weld)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Line 2: The Core Request: Partie 1, Partie 2, Heat 1, Heat 2!
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Partie 1 & Heat 1
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Partie 1: " + weld.part1.ifBlank { "Composant 1" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Coulée : ",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = weld.heatNo1.ifBlank { "—" },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (weld.heatNo1.isNotBlank()) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.East,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(14.dp)
                    )

                    // Partie 2 & Heat 2
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Partie 2: " + weld.part2.ifBlank { "Composant 2" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Coulée : ",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = weld.heatNo2.ifBlank { "—" },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (weld.heatNo2.isNotBlank()) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Line 3: Soudeur & DMOS & inspection summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Soudeur : ${weld.welderId} • DMOS: ${weld.wpsNo}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "CND: ${weld.ndtType} (${weld.ndtResult})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (weld.ndtResult == "ACCEPTED") ApprovedGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeldCardItem(
    weld: WeldJoint,
    isDuplicate: Boolean = false,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weld_item_${weld.jointNo}")
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Joint No + Line No + Spool Badge + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(IndustrialNavy800)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = weld.jointNo,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = weld.lineNo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (weld.spoolNo.isNotBlank()) {
                            Text(
                                text = "Spool: ${weld.spoolNo}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElectricCyan
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDuplicate) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RejectRedContainer,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "⚠️ DOUBLON",
                                color = RejectRedDark,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    WeldOverallStatusBadge(weld = weld)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tech specs: Diameter, Thickness, Material, WPS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ø ${weld.diameterInch}\" (${weld.thicknessMm}mm)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${weld.material} • ${weld.weldType}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "WPS: ${weld.wpsNo}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inspection steps pills: Fit-up, VT, NDT
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                InspectionPill(
                    label = "Fit-up: ${weld.fitupStatus}",
                    status = weld.fitupStatus
                )
                InspectionPill(
                    label = "VT: ${weld.visualStatus}",
                    status = weld.visualStatus
                )
                InspectionPill(
                    label = "${weld.ndtType}: ${weld.ndtResult}",
                    status = weld.ndtResult
                )
            }

            // Traceability: Part 1 & Heat No 1 -> Part 2 & Heat No 2 (Always visible for clarity)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Part 1 & Heat 1
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Partie 1: ${weld.part1.ifBlank { "Composant 1" }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Coulée : ",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = weld.heatNo1.ifBlank { "—" },
                                fontSize = 10.sp,
                                color = if (weld.heatNo1.isNotBlank()) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (weld.heatNo1.isNotBlank()) FontWeight.ExtraBold else FontWeight.Normal
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.East,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(14.dp)
                    )

                    // Part 2 & Heat 2
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Partie 2: ${weld.part2.ifBlank { "Composant 2" }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Coulée : ",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = weld.heatNo2.ifBlank { "—" },
                                fontSize = 10.sp,
                                color = if (weld.heatNo2.isNotBlank()) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (weld.heatNo2.isNotBlank()) FontWeight.ExtraBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Welder & Date & Edit affordance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Soudeur: ${weld.welderId} (${weld.welderName.ifBlank { "Opérateur" }}) • ${weld.weldDate}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifier",
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Détails",
                        fontSize = 11.sp,
                        color = ElectricCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun IsometricTreeView(
    tree: List<IsometricGroup>,
    onSelectWeld: (WeldJoint) -> Unit,
    onSelectLineInFilter: (String) -> Unit
) {
    if (tree.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Aucune isométrie correspondante",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Vérifiez vos critères de recherche ou réinitialisez les filtres.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Arborescence : ${tree.size} Ligne(s) Isométrique(s)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ISO → Spools → Joints",
                        fontSize = 11.sp,
                        color = ElectricCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(tree, key = { it.lineNo }) { isoGroup ->
                IsometricLineCard(
                    isoGroup = isoGroup,
                    onSelectWeld = onSelectWeld,
                    onSelectLineInFilter = onSelectLineInFilter
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun IsometricLineCard(
    isoGroup: IsometricGroup,
    onSelectWeld: (WeldJoint) -> Unit,
    onSelectLineInFilter: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Isometric Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isExpanded) ElectricCyan else IndustrialNavy800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = isoGroup.lineNo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (isoGroup.drawingNo.isNotBlank()) {
                                Text(
                                    text = "Plan: ${isoGroup.drawingNo}",
                                    fontSize = 11.sp,
                                    color = ElectricCyan
                                )
                            }
                            Text(
                                text = "• ${isoGroup.spools.size} Spools • ${isoGroup.totalJoints} Joints",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isoGroup.completionRate >= 100f) ApprovedGreenContainer
                                else if (isoGroup.repairJoints > 0) RejectRedContainer
                                else ElectricCyanContainer
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${String.format(java.util.Locale.FRANCE, "%.0f%%", isoGroup.completionRate)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isoGroup.completionRate >= 100f) ApprovedGreenDark
                            else if (isoGroup.repairJoints > 0) RejectRedDark
                            else ElectricCyan
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (isoGroup.completionRate / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isoGroup.completionRate >= 100f) ApprovedGreen else ElectricCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Expanded Spools Section
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    isoGroup.spools.forEach { spoolGroup ->
                        SpoolAccordionItem(
                            spoolGroup = spoolGroup,
                            onSelectWeld = onSelectWeld
                        )
                    }

                    // Button to filter this line in list mode
                    TextButton(
                        onClick = { onSelectLineInFilter(isoGroup.lineNo) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Ouvrir cette ligne en vue liste", fontSize = 11.sp, color = ElectricCyan)
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SpoolAccordionItem(
    spoolGroup: SpoolGroup,
    onSelectWeld: (WeldJoint) -> Unit
) {
    var isSpoolExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Spool Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isSpoolExpanded = !isSpoolExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(WeldAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (spoolGroup.spoolNo.equals("Sans Spool", ignoreCase = true) || spoolGroup.spoolNo == "SANS SPOOL") "CHANTIER (SANS SPOOL)" else "SPOOL ${spoolGroup.spoolNo}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = WeldAmberDark
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${spoolGroup.joints.size} joints soudés",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${spoolGroup.completedJoints}/${spoolGroup.totalJoints} validés",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (spoolGroup.completedJoints == spoolGroup.totalJoints) ApprovedGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isSpoolExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Joints in Spool
            AnimatedVisibility(visible = isSpoolExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    spoolGroup.joints.forEach { weld ->
                        JointTreeRow(
                            weld = weld,
                            onClick = { onSelectWeld(weld) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JointTreeRow(
    weld: WeldJoint,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(IndustrialNavy800)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = weld.jointNo,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Ø ${weld.diameterInch}\" ${weld.weldType} • ${weld.material}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Soudeur: ${weld.welderId} (${weld.welderName.ifBlank { "N/A" }})",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeldOverallStatusBadge(weld = weld)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifier",
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Traçabilité Matériaux : Partie 1 & Coulée 1 -> Partie 2 & Coulée 2
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "P1: ${weld.part1.ifBlank { "Composant 1" }}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "[${weld.heatNo1.ifBlank { "—" }}]",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (weld.heatNo1.isNotBlank()) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.East,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(12.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "P2: ${weld.part2.ifBlank { "Composant 2" }}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "[${weld.heatNo2.ifBlank { "—" }}]",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (weld.heatNo2.isNotBlank()) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
