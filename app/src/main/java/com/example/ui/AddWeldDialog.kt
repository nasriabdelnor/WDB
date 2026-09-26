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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.WeldJoint
import com.example.ui.theme.ElectricCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddWeldDialog(
    suggestedJointNo: String,
    onDismiss: () -> Unit,
    onAdd: (WeldJoint) -> Unit
) {
    val currentDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var jointNo by remember { mutableStateOf(suggestedJointNo) }
    var lineNo by remember { mutableStateOf("08-CS-150-01") }
    var spoolNo by remember { mutableStateOf("SP-01") }
    var drawingNo by remember { mutableStateOf("ISO-PR-401") }
    var welderId by remember { mutableStateOf("S-101") }
    var welderName by remember { mutableStateOf("Marc Dupont") }
    var wpsNo by remember { mutableStateOf("WPS-CS-01") }
    var process by remember { mutableStateOf("GTAW+SMAW") }
    var weldType by remember { mutableStateOf("BW") }
    var material by remember { mutableStateOf("A106 Gr.B") }
    var diameterText by remember { mutableStateOf("6.0") }
    var thicknessText by remember { mutableStateOf("7.11") }

    var fitupStatus by remember { mutableStateOf("ACCEPTED") }
    var visualStatus by remember { mutableStateOf("PENDING") }
    var ndtType by remember { mutableStateOf("RT") }
    var ndtResult by remember { mutableStateOf("PENDING") }
    var notes by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("add_weld_dialog")
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ajouter une Nouvelle Soudure",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = jointNo,
                        onValueChange = { jointNo = it },
                        label = { Text("N° Joint *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lineNo,
                        onValueChange = { lineNo = it },
                        label = { Text("Ligne / ISO *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = spoolNo,
                        onValueChange = { spoolNo = it },
                        label = { Text("Tronçon (Spool)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = drawingNo,
                        onValueChange = { drawingNo = it },
                        label = { Text("Plan DWG") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = welderId,
                        onValueChange = { welderId = it },
                        label = { Text("Poinçon Soudeur *") },
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = material,
                        onValueChange = { material = it },
                        label = { Text("Matériau") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = wpsNo,
                        onValueChange = { wpsNo = it },
                        label = { Text("DMOS / WPS") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = diameterText,
                        onValueChange = { diameterText = it },
                        label = { Text("Diamètre (\")") },
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectDropdown(
                        label = "Méthode CND",
                        options = listOf("RT", "UT", "PT", "MT", "VT ONLY"),
                        selected = ndtType,
                        onSelect = { ndtType = it },
                        modifier = Modifier.weight(1f)
                    )
                    SelectDropdown(
                        label = "Pointage (Fitup)",
                        options = listOf("ACCEPTED", "PENDING", "REJECTED"),
                        selected = fitupStatus,
                        onSelect = { fitupStatus = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes & Observations de chantier") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            val newWeld = WeldJoint(
                                jointNo = jointNo.ifBlank { "W-NEW" },
                                lineNo = lineNo.ifBlank { "LIGNE-01" },
                                spoolNo = spoolNo,
                                drawingNo = drawingNo,
                                welderId = welderId.ifBlank { "S-01" },
                                welderName = welderName,
                                wpsNo = wpsNo,
                                process = process,
                                weldType = weldType,
                                material = material,
                                diameterInch = diameterText.toDoubleOrNull() ?: 4.0,
                                thicknessMm = thicknessText.toDoubleOrNull() ?: 6.0,
                                weldDate = currentDate,
                                fitupStatus = fitupStatus,
                                fitupInspector = "QC Terrain",
                                fitupDate = currentDate,
                                visualStatus = visualStatus,
                                visualInspector = "",
                                visualDate = "",
                                ndtType = ndtType,
                                ndtResult = ndtResult,
                                status = if (visualStatus == "ACCEPTED" && ndtResult == "ACCEPTED") "COMPLETED" else "IN_PROGRESS",
                                notes = notes,
                                updatedAt = System.currentTimeMillis()
                            )
                            onAdd(newWeld)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("submit_new_weld_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter le joint")
                    }
                }
            }
        }
    }
}
