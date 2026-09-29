package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WeldJoint
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.IndustrialNavy900
import com.example.ui.theme.RejectRed
import com.example.util.PdfExportService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JointDetailPage(
    weld: WeldJoint,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "TRAÇABILITÉ COMPLÈTE DU JOINT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ElectricCyanLight,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "WELD JOINT ${weld.jointNumber}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_joint_detail")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier.padding(end = 12.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = when (weld.status) {
                            "COMPLETED" -> ApprovedGreen.copy(alpha = 0.2f)
                            "REPAIR_REQUIRED" -> RejectRed.copy(alpha = 0.2f)
                            else -> Color(0xFFFBBF24).copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = weld.status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (weld.status) {
                                "COMPLETED" -> ApprovedGreen
                                "REPAIR_REQUIRED" -> RejectRed
                                else -> Color(0xFFFBBF24)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IndustrialNavy900)
            )
        },
        containerColor = IndustrialNavy900
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("joint_detail_page")
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. IDENTIFICATION
            TraceabilityCard(title = "1. IDENTIFICATION DU JOINT") {
                DetailDataRow(label = "ISO (Plan isométrique):", value = weld.isoNumber.ifBlank { "N/A" }, highlight = true)
                DetailDataRow(label = "SPOOL (Tronçon):", value = weld.spoolNumber.ifBlank { "N/A" }, highlight = true)
                DetailDataRow(label = "JOINT N°:", value = weld.jointNumber, highlight = true)
                if (weld.drawingNo.isNotBlank()) {
                    DetailDataRow(label = "Drawing Ref:", value = weld.drawingNo)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. PART 1 TRACEABILITY
            TraceabilityCard(title = "2. PARTIE 1 (ÉLÉMENT AMONT)") {
                DetailDataRow(label = "Description:", value = weld.part1Description.ifBlank { "Tuyau / Pipe" })
                DetailDataRow(label = "Matière (Material):", value = weld.part1Material.ifBlank { weld.material })
                DetailDataRow(
                    label = "Heat Number (N° Coulée):",
                    value = weld.part1HeatNumber.ifBlank { "NON RENSEIGNÉ" },
                    prominentBadge = true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. PART 2 TRACEABILITY
            TraceabilityCard(title = "3. PARTIE 2 (ÉLÉMENT AVAL)") {
                DetailDataRow(label = "Description:", value = weld.part2Description.ifBlank { "Composant / Fitting" })
                DetailDataRow(label = "Matière (Material):", value = weld.part2Material.ifBlank { weld.material })
                DetailDataRow(
                    label = "Heat Number (N° Coulée):",
                    value = weld.part2HeatNumber.ifBlank { "NON RENSEIGNÉ" },
                    prominentBadge = true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. WELDING SPECIFICATIONS
            TraceabilityCard(title = "4. DONNÉES DE SOUDAGE") {
                DetailDataRow(label = "Procédé (Process):", value = weld.process)
                DetailDataRow(label = "WPS (DMOS N°):", value = weld.wps)
                DetailDataRow(label = "Soudeur (Welder ID):", value = weld.welder.ifBlank { weld.welderName })
                DetailDataRow(label = "Date de soudage:", value = weld.date.ifBlank { "N/A" })
                DetailDataRow(label = "Type de joint:", value = weld.weldType)
                DetailDataRow(label = "Diamètre / Épaisseur:", "${weld.diameterInch}\" (${weld.thicknessMm} mm)")
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. INSPECTION & CND (NDT)
            TraceabilityCard(title = "5. CONTRÔLE QUALITÉ & CND (NDT)") {
                DetailDataRow(label = "Contrôle Visuel (VT):", value = weld.visual, statusHighlight = weld.visual)
                DetailDataRow(label = "Résultat RT (Radio):", value = weld.rt, statusHighlight = weld.rt)
                DetailDataRow(label = "Résultat PT (Ressuage):", value = weld.pt, statusHighlight = weld.pt)
                DetailDataRow(label = "Résultat UT (Ultrasons):", value = weld.ut, statusHighlight = weld.ut)
                if (weld.ndtReportNo.isNotBlank()) {
                    DetailDataRow(label = "N° Rapport / PV CND:", value = weld.ndtReportNo)
                }
                if (weld.notes.isNotBlank()) {
                    DetailDataRow(label = "Remarques:", value = weld.notes)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BUTTON: EXPORT PDF
            Button(
                onClick = { PdfExportService.exportJointReport(context, weld) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("export_pdf_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = IndustrialNavy900
                )
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EXPORT PDF",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TraceabilityCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = IndustrialNavy800),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ElectricCyanLight,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun DetailDataRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    prominentBadge: Boolean = false,
    statusHighlight: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Medium
        )

        when {
            prominentBadge && value != "NON RENSEIGNÉ" -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElectricCyan.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = value,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            statusHighlight != null -> {
                val color = when {
                    statusHighlight.contains("ACC", ignoreCase = true) || statusHighlight.contains("CONF", ignoreCase = true) || statusHighlight.contains("OK", ignoreCase = true) -> ApprovedGreen
                    statusHighlight.contains("REJ", ignoreCase = true) || statusHighlight.contains("RÉPARATION", ignoreCase = true) -> RejectRed
                    statusHighlight.contains("PEND", ignoreCase = true) -> Color(0xFFFBBF24)
                    else -> Color(0xFF94A3B8)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = value,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            else -> {
                Text(
                    text = value,
                    fontSize = 13.5.sp,
                    fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (highlight) Color.White else Color(0xFFE2E8F0)
                )
            }
        }
    }
}
