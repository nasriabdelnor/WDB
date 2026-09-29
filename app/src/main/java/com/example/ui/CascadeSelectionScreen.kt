package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.WeldJoint
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.IndustrialNavy900
import com.example.ui.theme.RejectRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CascadeSelectionScreen(
    viewModel: WeldViewModel,
    onSelectJoint: (WeldJoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val allWelds by viewModel.allWelds.collectAsStateWithLifecycle()
    val distinctIsos by viewModel.distinctIsos.collectAsStateWithLifecycle()
    val selectedIso by viewModel.selectedIso.collectAsStateWithLifecycle()
    val spoolsForIso by viewModel.spoolsForSelectedIso.collectAsStateWithLifecycle()
    val selectedSpool by viewModel.selectedSpool.collectAsStateWithLifecycle()
    val jointsList by viewModel.jointsForSelectedSpool.collectAsStateWithLifecycle()

    var isoExpanded by remember { mutableStateOf(false) }
    var spoolExpanded by remember { mutableStateOf(false) }
    var isoSearchFilter by remember { mutableStateOf("") }

    val filteredIsos = remember(distinctIsos, isoSearchFilter) {
        if (isoSearchFilter.isBlank()) distinctIsos
        else distinctIsos.filter { it.contains(isoSearchFilter, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IndustrialNavy900)
            .padding(16.dp)
            .testTag("cascade_selection_screen")
    ) {
        // Cascade Step Tracker Bar
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = IndustrialNavy800)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CASCADE WORKFLOW",
                    color = ElectricCyanLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Filtrage : ISO → Spool → Joints",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Breadcrumb Steps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CascadeStepBadge(
                        step = "1. ISO",
                        label = selectedIso ?: "Sélectionner",
                        isCompleted = selectedIso != null,
                        isActive = selectedIso == null,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    CascadeStepBadge(
                        step = "2. SPOOL",
                        label = selectedSpool ?: "Sélectionner",
                        isCompleted = selectedSpool != null,
                        isActive = selectedIso != null && selectedSpool == null,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    CascadeStepBadge(
                        step = "3. JOINTS",
                        label = if (selectedSpool != null) "${jointsList.size} joints" else "En attente",
                        isCompleted = selectedSpool != null && jointsList.isNotEmpty(),
                        isActive = selectedSpool != null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (allWelds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No Welding Database loaded.\nVeuillez d'abord importer Welding Data Base01.xlsx sur l'accueil.",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            return
        }

        // STEP 1: ISO SELECTION DROPDOWN
        ExposedDropdownMenuBox(
            expanded = isoExpanded,
            onExpandedChange = { isoExpanded = it },
            modifier = Modifier.fillMaxWidth().testTag("iso_dropdown_box")
        ) {
            OutlinedTextField(
                value = selectedIso ?: "Sélectionnez un Plan Isométrique (ISO)",
                onValueChange = {},
                readOnly = true,
                label = { Text("Étape 1 : Isométrie (ISO Number)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isoExpanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = IndustrialNavy800,
                    unfocusedContainerColor = IndustrialNavy800,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedLabelColor = ElectricCyanLight,
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = isoExpanded,
                onDismissRequest = { isoExpanded = false },
                modifier = Modifier.background(IndustrialNavy800)
            ) {
                // Quick filter within dropdown
                OutlinedTextField(
                    value = isoSearchFilter,
                    onValueChange = { isoSearchFilter = it },
                    placeholder = { Text("Filtrer les ISO...", color = Color(0xFF64748B), fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    singleLine = true
                )

                filteredIsos.forEach { iso ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = iso,
                                color = if (iso == selectedIso) ElectricCyanLight else Color.White,
                                fontWeight = if (iso == selectedIso) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            viewModel.selectIso(iso)
                            isoExpanded = false
                            isoSearchFilter = ""
                        },
                        modifier = Modifier.testTag("iso_item_$iso")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // STEP 2: SPOOL SELECTION (Only visible / active once ISO is selected)
        if (selectedIso != null) {
            ExposedDropdownMenuBox(
                expanded = spoolExpanded,
                onExpandedChange = { spoolExpanded = it },
                modifier = Modifier.fillMaxWidth().testTag("spool_dropdown_box")
            ) {
                OutlinedTextField(
                    value = selectedSpool ?: "Sélectionnez un Tronçon (Spool)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Étape 2 : Spool du plan $selectedIso") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = spoolExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = IndustrialNavy800,
                        unfocusedContainerColor = IndustrialNavy800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = ElectricCyanLight,
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = spoolExpanded,
                    onDismissRequest = { spoolExpanded = false },
                    modifier = Modifier.background(IndustrialNavy800)
                ) {
                    DropdownMenuItem(
                        text = { Text("Tous les spools de $selectedIso", color = ElectricCyanLight, fontWeight = FontWeight.Bold) },
                        onClick = {
                            viewModel.selectSpool(null)
                            spoolExpanded = false
                        }
                    )

                    spoolsForIso.forEach { spool ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = spool,
                                    color = if (spool == selectedSpool) ElectricCyanLight else Color.White,
                                    fontWeight = if (spool == selectedSpool) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                viewModel.selectSpool(spool)
                                spoolExpanded = false
                            },
                            modifier = Modifier.testTag("spool_item_$spool")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // STEP 3: JOINTS LIST FOR SELECTED SPOOL / ISO
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedSpool != null) "JOINTS DU SPOOL $selectedSpool (${jointsList.size})"
                       else if (selectedIso != null) "JOINTS DE L'ISO $selectedIso (${jointsList.size})"
                       else "VEUILLEZ SÉLECTIONNER UN ISO",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ElectricCyanLight,
                letterSpacing = 0.5.sp
            )

            if (selectedIso != null || selectedSpool != null) {
                Surface(
                    onClick = {
                        viewModel.selectIso(null)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF334155)
                ) {
                    Text(
                        text = "Réinitialiser",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("joints_list_view"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(jointsList, key = { it.id }) { weld ->
                WeldJointCard(
                    weld = weld,
                    onClick = { onSelectJoint(weld) }
                )
            }

            if (selectedIso != null && jointsList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun joint trouvé pour cette sélection.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CascadeStepBadge(
    step: String,
    label: String,
    isCompleted: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = when {
            isCompleted -> ApprovedGreen.copy(alpha = 0.15f)
            isActive -> ElectricCyan.copy(alpha = 0.15f)
            else -> Color(0xFF1E293B)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                isCompleted -> ApprovedGreen.copy(alpha = 0.5f)
                isActive -> ElectricCyan.copy(alpha = 0.5f)
                else -> Color(0xFF334155)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = step,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = when {
                    isCompleted -> Color(0xFF86EFAC)
                    isActive -> ElectricCyanLight
                    else -> Color(0xFF64748B)
                }
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted || isActive) Color.White else Color(0xFF94A3B8),
                maxLines = 1
            )
        }
    }
}

@Composable
fun WeldJointCard(
    weld: WeldJoint,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("joint_item_${weld.jointNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = IndustrialNavy800),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Joint Title & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "J",
                            color = ElectricCyanLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "JOINT ${weld.jointNumber}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${weld.isoNumber} • Spool ${weld.spoolNumber}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Overall status tag
                val statusColor = when (weld.status) {
                    "COMPLETED" -> ApprovedGreen
                    "REPAIR_REQUIRED" -> RejectRed
                    else -> Color(0xFFFBBF24)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = weld.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Traceability Row: Heat Numbers & Welder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "PART 1 HEAT:", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    Text(
                        text = weld.part1HeatNumber.ifBlank { "-" },
                        fontSize = 12.sp,
                        color = if (weld.part1HeatNumber.isNotBlank()) ElectricCyanLight else Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "PART 2 HEAT:", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    Text(
                        text = weld.part2HeatNumber.ifBlank { "-" },
                        fontSize = 12.sp,
                        color = if (weld.part2HeatNumber.isNotBlank()) ElectricCyanLight else Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(modifier = Modifier.weight(0.9f)) {
                    Text(text = "SOUDEUR:", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    Text(
                        text = weld.welder.ifBlank { weld.welderName }.ifBlank { "-" },
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
