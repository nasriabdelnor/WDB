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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DuplicateWeldGroup
import com.example.data.WeldJoint
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ApprovedGreenContainer
import com.example.ui.theme.ApprovedGreenDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.IndustrialNavy700
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.IndustrialNavy900
import com.example.ui.theme.RejectRed
import com.example.ui.theme.RejectRedContainer
import com.example.ui.theme.RejectRedDark
import com.example.ui.theme.WeldAmber

@Composable
fun WdbDuplicatesScreen(
    viewModel: WeldViewModel,
    onNavigateToWelds: (statusFilter: String) -> Unit,
    onSelectWeld: (WeldJoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val allWelds by viewModel.allWelds.collectAsStateWithLifecycle()
    val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val duplicateCount by viewModel.duplicateCount.collectAsStateWithLifecycle()
    val wdbProject by viewModel.wdbProjectInfo.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showConfirmCleanDialog by remember { mutableStateOf(false) }

    val filteredGroups = remember(duplicateGroups, searchQuery) {
        if (searchQuery.isBlank()) {
            duplicateGroups
        } else {
            duplicateGroups.filter { g ->
                g.lineNo.contains(searchQuery, ignoreCase = true) ||
                g.spoolNo.contains(searchQuery, ignoreCase = true) ||
                g.jointNo.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (showConfirmCleanDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCleanDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = RejectRedDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nettoyer les doublons WDB", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "L'application va conserver la 1ère occurrence valide de chaque joint et supprimer les entrées identiques redondantes.\n\nVoulez-vous continuer ?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeDuplicateWelds()
                        showConfirmCleanDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RejectRedDark)
                ) {
                    Text("Confirmer le nettoyage")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCleanDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("wdb_duplicates_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Header Card with version 2.0-light
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().testTag("duplicates_header_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = IndustrialNavy900)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElectricCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ElectricCyanLight, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "CONTRÔLE DOUBLONS WDB",
                                        color = ElectricCyanLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF86EFAC).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "v2.1-light",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF86EFAC),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Détection des Soudures en Double",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (duplicateCount > 0) RejectRedDark else ApprovedGreenDark
                        ) {
                            Text(
                                text = if (duplicateCount > 0) "$duplicateCount doublon(s)" else "0 doublon",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = IndustrialNavy800,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Base active : ${wdbProject.fileName}",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }

        // Summary Counters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total Joints", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${allWelds.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Groupes Doublons", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${duplicateGroups.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (duplicateGroups.isNotEmpty()) RejectRedDark else ApprovedGreenDark)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("État Fichier", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (duplicateCount > 0) "⚠️ Anomalie" else "✅ Conforme", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (duplicateCount > 0) RejectRedDark else ApprovedGreenDark)
                    }
                }
            }
        }

        // Clean action bar if duplicates exist
        if (duplicateCount > 0) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().testTag("clean_all_duplicates_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = RejectRedContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Action Recommandée : Nettoyage",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = RejectRedDark
                            )
                            Text(
                                text = "Conserver la première entrée et supprimer les $duplicateCount copies redondantes.",
                                fontSize = 11.sp,
                                color = RejectRedDark
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { showConfirmCleanDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RejectRedDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("clean_duplicates_action_btn")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nettoyer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search filter for duplicates
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Filtrer parmi les doublons (ISO, Spool, Joint)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricCyan) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("duplicate_search_input")
                )
            }
        } else {
            // Success banner: No duplicates found!
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().testTag("duplicates_clean_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = ApprovedGreenContainer)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ApprovedGreenDark, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Base WDB Parfaitement Conforme",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ApprovedGreenDark
                                )
                                Text(
                                    text = "Aucun numéro de joint en double détecté sur l'ensemble du projet.",
                                    fontSize = 12.sp,
                                    color = ApprovedGreenDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        FilledTonalButton(
                            onClick = { onNavigateToWelds("ALL") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consulter la Recherche & Filtres des Soudures")
                        }
                    }
                }
            }
        }

        // List of duplicate groups
        if (duplicateCount > 0) {
            item {
                Text(
                    text = "Détail des Doublons Trouvés (${filteredGroups.size} groupe(s)) :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(filteredGroups, key = { it.key }) { group ->
                DuplicateGroupCard(
                    group = group,
                    onSelectWeld = onSelectWeld,
                    onDeleteWeld = { weld -> viewModel.deleteWeld(weld) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateWeldGroup,
    onSelectWeld: (WeldJoint) -> Unit,
    onDeleteWeld: (WeldJoint) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().testTag("duplicate_group_${group.jointNo}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Group Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RejectRedContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = group.jointNo,
                            color = RejectRedDark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Ligne : ${group.lineNo.ifBlank { "Sans ligne" }}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (group.spoolNo.isNotBlank()) {
                            Text(
                                text = "Spool : ${group.spoolNo}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElectricCyan
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = RejectRedDark
                ) {
                    Text(
                        text = "${group.count} occurrences",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-items: each duplicate occurrence
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                group.welds.forEachIndexed { index, weld ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectWeld(weld) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Copie #${index + 1} • ID: ${weld.id} • Date: ${weld.weldDate.ifBlank { "N/A" }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Soudeur: ${weld.welderId.ifBlank { "N/A" }} • WPS: ${weld.wpsNo} • Statut: ${weld.status}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (index > 0) {
                                IconButton(
                                    onClick = { onDeleteWeld(weld) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer ce doublon",
                                        tint = RejectRedDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ApprovedGreenContainer
                                ) {
                                    Text(
                                        text = "Original",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ApprovedGreenDark,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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
