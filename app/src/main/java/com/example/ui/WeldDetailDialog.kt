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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.WeldJoint
import com.example.ui.components.WeldOverallStatusBadge
import com.example.ui.theme.ApprovedGreenDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.RejectRedDark
import com.example.ui.theme.WeldAmberDark

/**
 * Fiche de consultation et d'exploitation technique d'une soudure WDB.
 * En mode consultation/lecture seule stricte : les données proviennent de la base officielle WDB.
 */
@Composable
fun WeldDetailDialog(
    weld: WeldJoint,
    onDismiss: () -> Unit
) {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(IndustrialNavy800)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = weld.jointNo,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Fiche Technique WDB",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Consultation & Exploitation",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Statut d'homologation global
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Statut Contrôle Qualité :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    WeldOverallStatusBadge(weld = weld)
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Section 1 : Localisation & Tuyauterie
                DetailSectionHeader(
                    icon = Icons.Default.PrecisionManufacturing,
                    title = "1. Tuyauterie & Repérage Chantier",
                    tint = ElectricCyan
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailInfoTile(label = "Ligne / Isométrie", value = weld.lineNo.ifBlank { "—" }, modifier = Modifier.weight(1f))
                    DetailInfoTile(label = "Tronçon / Spool", value = weld.spoolNo.ifBlank { "—" }, modifier = Modifier.weight(1f))
                }

                if (weld.drawingNo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    DetailInfoTile(label = "Plan / Drawing ISO", value = weld.drawingNo, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2 : Composants Assemblés & Traçabilité Matière (Heat Numbers)
                DetailSectionHeader(
                    icon = Icons.Default.Straighten,
                    title = "2. Composants Assemblés & Traçabilité (Coulées / Heat)",
                    tint = ElectricCyan
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailInfoTile(
                        label = "Partie 1 (Composant)",
                        value = weld.part1.ifBlank { "—" },
                        modifier = Modifier.weight(1.2f)
                    )
                    DetailInfoTile(
                        label = "N° Coulée 1 (Heat No)",
                        value = weld.heatNo1.ifBlank { "—" },
                        valueColor = if (weld.heatNo1.isNotBlank()) ElectricCyan else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailInfoTile(
                        label = "Partie 2 (Composant)",
                        value = weld.part2.ifBlank { "—" },
                        modifier = Modifier.weight(1.2f)
                    )
                    DetailInfoTile(
                        label = "N° Coulée 2 (Heat No)",
                        value = weld.heatNo2.ifBlank { "—" },
                        valueColor = if (weld.heatNo2.isNotBlank()) ElectricCyan else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3 : Paramètres Soudage & Métallurgie
                DetailSectionHeader(
                    icon = Icons.Default.Engineering,
                    title = "3. Soudage & Spécifications DMOS",
                    tint = WeldAmberDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailInfoTile(
                        label = "Soudeur (Matricule / Nom)",
                        value = if (weld.welderName.isNotBlank() && weld.welderName != weld.welderId) {
                            "${weld.welderId} • ${weld.welderName}"
                        } else weld.welderId.ifBlank { "—" },
                        modifier = Modifier.weight(1.3f)
                    )
                    DetailInfoTile(label = "WPS / DMOS", value = weld.wpsNo.ifBlank { "—" }, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailInfoTile(label = "Procédé", value = weld.process.ifBlank { "—" }, modifier = Modifier.weight(1f))
                    DetailInfoTile(label = "Type de joint", value = weld.weldType.ifBlank { "BW" }, modifier = Modifier.weight(1f))
                    DetailInfoTile(label = "Matière", value = weld.material.ifBlank { "—" }, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailInfoTile(
                        label = "Diamètre",
                        value = if (weld.diameterInch > 0) "${weld.diameterInch}\"" else "—",
                        modifier = Modifier.weight(1f)
                    )
                    DetailInfoTile(
                        label = "Épaisseur",
                        value = if (weld.thicknessMm > 0) "${weld.thicknessMm} mm" else "—",
                        modifier = Modifier.weight(1f)
                    )
                    DetailInfoTile(
                        label = "Date Soudage",
                        value = weld.weldDate.ifBlank { "—" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3 : Contrôles NDT & QA/QC
                DetailSectionHeader(
                    icon = Icons.Default.FactCheck,
                    title = "3. Contrôles CND & Homologation QA/QC",
                    tint = ApprovedGreenDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChipTile(label = "Fit-up / Chanfrein", value = weld.fitupStatus, modifier = Modifier.weight(1f))
                    StatusChipTile(label = "Contrôle Visuel (VT)", value = weld.visualStatus, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailInfoTile(label = "Méthode CND", value = weld.ndtType.ifBlank { "RT" }, modifier = Modifier.weight(1f))
                    StatusChipTile(label = "Résultat CND", value = weld.ndtResult, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(6.dp))
                DetailInfoTile(
                    label = "N° Rapport CND / PV",
                    value = weld.ndtReportNo.ifBlank { "En attente / Non renseigné" },
                    modifier = Modifier.fillMaxWidth()
                )

                if (weld.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    DetailSectionHeader(
                        icon = Icons.Default.Assignment,
                        title = "4. Observations & Notes WDB",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = weld.notes,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Single clean Close / Dismiss Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = IndustrialNavy800),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("close_weld_detail_button")
                ) {
                    Text("Fermer la fiche", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun DetailSectionHeader(
    icon: ImageVector,
    title: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = tint
        )
    }
}

@Composable
private fun DetailInfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun StatusChipTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val cleanValue = value.trim()
    val (chipBg, chipText) = when {
        cleanValue.equals("ACCEPTÉ", ignoreCase = true) || cleanValue.equals("OK", ignoreCase = true) ->
            Color(0xFF86EFAC).copy(alpha = 0.2f) to Color(0xFF15803D)
        cleanValue.equals("REJETÉ", ignoreCase = true) || cleanValue.equals("REFUSÉ", ignoreCase = true) || cleanValue.equals("REPAIR", ignoreCase = true) ->
            Color(0xFFFCA5A5).copy(alpha = 0.2f) to Color(0xFFB91C1C)
        else ->
            Color(0xFFE2E8F0) to Color(0xFF475569)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(3.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = chipBg
            ) {
                Text(
                    text = cleanValue.ifBlank { "EN ATTENTE" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = chipText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
