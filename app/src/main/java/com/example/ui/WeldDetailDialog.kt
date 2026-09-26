package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.WeldJoint
import com.example.ui.components.WeldOverallStatusBadge
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.RejectRed
import com.example.ui.theme.RejectRedDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeldDetailDialog(
    weld: WeldJoint,
    onDismiss: () -> Unit,
    onSave: (WeldJoint) -> Unit,
    onDelete: (WeldJoint) -> Unit
) {
    var jointNo by remember { mutableStateOf(weld.jointNo) }
    var lineNo by remember { mutableStateOf(weld.lineNo) }
    var spoolNo by remember { mutableStateOf(weld.spoolNo) }
    var drawingNo by remember { mutableStateOf(weld.drawingNo) }
    var welderId by remember { mutableStateOf(weld.welderId) }
    var welderName by remember { mutableStateOf(weld.welderName) }
    var wpsNo by remember { mutableStateOf(weld.wpsNo) }
    var process by remember { mutableStateOf(weld.process) }
    var weldType by remember { mutableStateOf(weld.weldType) }
    var material by remember { mutableStateOf(weld.material) }
    var diameterText by remember { mutableStateOf(weld.diameterInch.toString()) }
    var thicknessText by remember { mutableStateOf(weld.thicknessMm.toString()) }
    var weldDate by remember { mutableStateOf(weld.weldDate) }

    var fitupStatus by remember { mutableStateOf(weld.fitupStatus) }
    var visualStatus by remember { mutableStateOf(weld.visualStatus) }
    var ndtType by remember { mutableStateOf(weld.ndtType) }
    var ndtResult by remember { mutableStateOf(weld.ndtResult) }
    var ndtReportNo by remember { mutableStateOf(weld.ndtReportNo) }
    var status by remember { mutableStateOf(weld.status) }
    var notes by remember { mutableStateOf(weld.notes) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer la soudure ?") },
            text = { Text("Êtes-vous sûr de vouloir supprimer le joint $jointNo de la base de données ?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(weld)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RejectRed)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("weld_detail_dialog")
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(IndustrialNavy800)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = jointNo,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Fiche Contrôle Joint",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Identification
                Text(
                    text = "1. Identification & Tuyauterie",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = jointNo,
                        onValueChange = { jointNo = it },
                        label = { Text("N° Joint") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lineNo,
                        onValueChange = { lineNo = it },
                        label = { Text("Ligne / Isométrique") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = spoolNo,
                        onValueChange = { spoolNo = it },
                        label = { Text("Tronçon / Spool") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = drawingNo,
                        onValueChange = { drawingNo = it },
                        label = { Text("N° Plan (DWG)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Soudage & Opérateur
                Text(
                    text = "2. Paramètres Soudage & Soudeur",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = welderId,
                        onValueChange = { welderId = it },
                        label = { Text("Poinçon Soudeur") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = welderName,
                        onValueChange = { welderName = it },
                        label = { Text("Nom Soudeur") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wpsNo,
                        onValueChange = { wpsNo = it },
                        label = { Text("WPS / DMOS") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = material,
                        onValueChange = { material = it },
                        label = { Text("Matériau") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = diameterText,
                        onValueChange = { diameterText = it },
                        label = { Text("Diamètre (Pouce)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = thicknessText,
                        onValueChange = { thicknessText = it },
                        label = { Text("Épaisseur (mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Contrôle Qualité & CND
                Text(
                    text = "3. Contrôles Qualité (VT / CND)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectDropdown(
                        label = "Pointage (Fit-up)",
                        options = listOf("ACCEPTED", "PENDING", "REJECTED"),
                        selected = fitupStatus,
                        onSelect = { fitupStatus = it },
                        modifier = Modifier.weight(1f)
                    )
                    SelectDropdown(
                        label = "Visuel (VT)",
                        options = listOf("ACCEPTED", "PENDING", "REJECTED"),
                        selected = visualStatus,
                        onSelect = { visualStatus = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectDropdown(
                        label = "Méthode CND",
                        options = listOf("RT", "UT", "PT", "MT", "VT ONLY"),
                        selected = ndtType,
                        onSelect = { ndtType = it },
                        modifier = Modifier.weight(1f)
                    )
                    SelectDropdown(
                        label = "Résultat CND",
                        options = listOf("ACCEPTED", "PENDING", "REJECTED", "NOT_REQUIRED"),
                        selected = ndtResult,
                        onSelect = { ndtResult = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = ndtReportNo,
                    onValueChange = { ndtReportNo = it },
                    label = { Text("N° Rapport / PV de contrôle CND") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                SelectDropdown(
                    label = "Statut Général",
                    options = listOf("COMPLETED", "IN_PROGRESS", "PENDING_NDT", "REPAIR_REQUIRED"),
                    selected = status,
                    onSelect = { status = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observations / Remarques QC") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Save & Delete buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RejectRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("delete_weld_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Supprimer")
                    }

                    Button(
                        onClick = {
                            val updated = weld.copy(
                                jointNo = jointNo.trim(),
                                lineNo = lineNo.trim(),
                                spoolNo = spoolNo.trim(),
                                drawingNo = drawingNo.trim(),
                                welderId = welderId.trim(),
                                welderName = welderName.trim(),
                                wpsNo = wpsNo.trim(),
                                process = process.trim(),
                                weldType = weldType.trim(),
                                material = material.trim(),
                                diameterInch = diameterText.toDoubleOrNull() ?: weld.diameterInch,
                                thicknessMm = thicknessText.toDoubleOrNull() ?: weld.thicknessMm,
                                fitupStatus = fitupStatus,
                                visualStatus = visualStatus,
                                ndtType = ndtType,
                                ndtResult = ndtResult,
                                ndtReportNo = ndtReportNo.trim(),
                                status = status,
                                notes = notes.trim(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_weld_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enregistrer les modifications")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 11.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
