package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val availableLines = remember(allWelds) {
        listOf("ALL") + allWelds.map { it.lineNo }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableSpools by viewModel.availableSpools.collectAsStateWithLifecycle()
    val availableJoints by viewModel.availableJoints.collectAsStateWithLifecycle()
    val isometricTree by viewModel.isometricTree.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Liste, 1 = Arborescence
    var isHierarchyPanelExpanded by remember { mutableStateOf(true) }

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
            // HIERARCHICAL FILTER CARD: Isométrie -> Spool -> Joint
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
                Column(modifier = Modifier.padding(12.dp)) {
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
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ElectricCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Filtre Arborescence Isométrie",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Isométrie (Ligne)  →  Spool  →  Joint soudé",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasActiveFilters) {
                                Text(
                                    text = "Effacer",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RejectRed,
                                    modifier = Modifier
                                        .clickable { viewModel.resetAllFilters() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Icon(
                                imageVector = if (isHierarchyPanelExpanded) Icons.Default.Tune else Icons.Default.FilterList,
                                contentDescription = "Basculer",
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Expandable 3-tier dropdown selectors
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
                            // 1. Isométrie Selector
                            HierarchicalDropdown(
                                label = "1. Isométrie (Ligne)",
                                currentValue = if (lineFilter == "ALL") "Toutes les Isométries" else lineFilter,
                                items = availableLines,
                                itemLabel = { if (it == "ALL") "Toutes les Isométries (${availableLines.size - 1})" else it },
                                onItemSelected = { viewModel.setLineFilter(it) },
                                accentColor = ElectricCyan,
                                testTag = "dropdown_line"
                            )

                            // 2. Spool Selector (dynamically cascade from line)
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

                            // 3. Joint Selector (dynamically cascade from line + spool)
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

                            // Quick Spool Selection Chips when an Isométrie is active
                            if (availableSpools.size > 1) {
                                Column(modifier = Modifier.padding(top = 2.dp)) {
                                    Text(
                                        text = "Spools rapides de la ligne :",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        availableSpools.forEach { spool ->
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
                            }

                            // Quick Joint Selection Chips when a Spool is active
                            if (availableJoints.size > 1 && spoolFilter != "ALL") {
                                Column(modifier = Modifier.padding(top = 2.dp)) {
                                    Text(
                                        text = "Joints soudés du spool ($spoolFilter) :",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        availableJoints.forEach { joint ->
                                            val isSelected = jointFilter == joint
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.setJointFilter(joint) },
                                                label = {
                                                    Text(
                                                        if (joint == "ALL") "Tous" else joint,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = ApprovedGreen.copy(alpha = 0.2f),
                                                    selectedLabelColor = ApprovedGreenDark
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Active Breadcrumb trail if any level is active
                    if (lineFilter != "ALL" || spoolFilter != "ALL" || jointFilter != "ALL") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filtre actif :",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (lineFilter != "ALL") {
                                ActiveFilterChip(
                                    label = "ISO: $lineFilter",
                                    onClear = { viewModel.setLineFilter("ALL") }
                                )
                            }

                            if (spoolFilter != "ALL") {
                                ActiveFilterChip(
                                    label = "Spool: $spoolFilter",
                                    onClear = { viewModel.setSpoolFilter("ALL") }
                                )
                            }

                            if (jointFilter != "ALL") {
                                ActiveFilterChip(
                                    label = "Joint: $jointFilter",
                                    onClear = { viewModel.setJointFilter("ALL") }
                                )
                            }
                        }
                    }
                }
            }

            // Status Filter Chips (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusOptions = listOf(
                    "ALL" to "Tous (${welds.size})",
                    "COMPLETED" to "Conformes",
                    "IN_PROGRESS" to "En cours",
                    "PENDING_NDT" to "Attente CND",
                    "REPAIR_REQUIRED" to "Réparations"
                )

                statusOptions.forEach { (key, label) ->
                    val isSelected = statusFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setStatusFilter(key) },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_chip_$key"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricCyan
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
                    items(welds, key = { it.id }) { weld ->
                        WeldCardItem(
                            weld = weld,
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
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = Modifier.fillMaxWidth().testTag(testTag)
    ) {
        OutlinedTextField(
            value = currentValue,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            trailingIcon = {
                if (enabled) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                val isSelected = (item == "ALL" && currentValue.startsWith("Tout")) || item == currentValue
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = itemLabel(item),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeldCardItem(
    weld: WeldJoint,
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

                WeldOverallStatusBadge(weld = weld)
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
                            text = "SPOOL ${spoolGroup.spoolNo}",
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
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
    }
}
