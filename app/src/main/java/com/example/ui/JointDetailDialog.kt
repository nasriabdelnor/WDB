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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.WeldJoint
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.IndustrialNavy900
import com.example.ui.theme.RejectRed
import com.example.ui.theme.RejectRedDark
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ApprovedGreenDark
import com.example.util.PdfExportService

@Composable
fun JointDetailDialog(
    weld: WeldJoint,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .testTag("joint_detail_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = IndustrialNavy900,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: WELD JOINT {jointNumber}
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WDB TRACEABILITY",
                            color = ElectricCyanLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "WELD JOINT ${weld.jointNumber}",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_joint_detail")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. IDENTIFICATION
                SectionCard(title = "IDENTIFICATION") {
                    DataRow(label = "ISO:", value = weld.isoNumber.ifBlank { "N/A" }, highlight = true)
                    DataRow(label = "SPOOL:", value = weld.spoolNumber.ifBlank { "N/A" }, highlight = true)
                    DataRow(label = "JOINT:", value = weld.jointNumber, highlight = true)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. PART 1
                SectionCard(title = "PART 1") {
                    DataRow(label = "Description:", value = weld.part1Description.ifBlank { "Tuyau / Pipe" })
                    DataRow(label = "Material:", value = weld.part1Material.ifBlank { weld.material })
                    DataRow(label = "Heat Number:", value = weld.part1HeatNumber.ifBlank { "Non renseigné" }, badgeColor = ElectricCyan)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. PART 2
                SectionCard(title = "PART 2") {
                    DataRow(label = "Description:", value = weld.part2Description.ifBlank { "Composant / Fitting" })
                    DataRow(label = "Material:", value = weld.part2Material.ifBlank { weld.material })
                    DataRow(label = "Heat Number:", value = weld.part2HeatNumber.ifBlank { "Non renseigné" }, badgeColor = ElectricCyan)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. WELDING
                SectionCard(title = "WELDING") {
                    DataRow(label = "Process:", value = weld.process)
                    DataRow(label = "WPS:", value = weld.wps)
                    DataRow(label = "Welder:", value = weld.welder.ifBlank { weld.welderName })
                    DataRow(label = "Date:", value = weld.date.ifBlank { "N/A" })
                    DataRow(label = "Type / Size:", value = "${weld.weldType} • ${weld.diameterInch}\" (${weld.thicknessMm} mm)")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. INSPECTION
                SectionCard(title = "INSPECTION") {
                    DataRow(label = "Visual (VT):", value = weld.visual, statusHighlight = weld.visual)
                    DataRow(label = "RT:", value = weld.rt, statusHighlight = weld.rt)
                    DataRow(label = "PT:", value = weld.pt, statusHighlight = weld.pt)
                    DataRow(label = "UT:", value = weld.ut, statusHighlight = weld.ut)
                    if (weld.ndtReportNo.isNotBlank()) {
                        DataRow(label = "Report No:", value = weld.ndtReportNo)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // EXPORT PDF BUTTON
                Button(
                    onClick = {
                        PdfExportService.exportJointReport(context, weld)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("export_pdf_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = IndustrialNavy900
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXPORT PDF",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = IndustrialNavy800)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ElectricCyanLight,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DataRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    badgeColor: Color? = null,
    statusHighlight: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
            badgeColor != null && value.isNotBlank() && value != "Non renseigné" -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = value,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            statusHighlight != null -> {
                val color = when {
                    statusHighlight.contains("ACC", ignoreCase = true) || statusHighlight.contains("CONF", ignoreCase = true) || statusHighlight.contains("OK", ignoreCase = true) -> ApprovedGreen
                    statusHighlight.contains("REJ", ignoreCase = true) -> RejectRed
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
                    fontSize = 13.sp,
                    fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (highlight) Color.White else Color(0xFFE2E8F0)
                )
            }
        }
    }
}
