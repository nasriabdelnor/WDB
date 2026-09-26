package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LineProgress
import com.example.data.NdtTypeStat
import com.example.data.WelderPerformance
import com.example.data.WeldingKpis
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ApprovedGreenDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.IndustrialNavy700
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.IndustrialNavy900
import com.example.ui.theme.PendingPurple
import com.example.ui.theme.RejectRed
import com.example.ui.theme.WeldAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: WeldViewModel,
    onNavigateToWelds: (statusFilter: String) -> Unit,
    onNavigateToLine: (line: String) -> Unit = {},
    onNavigateToSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val kpis by viewModel.kpis.collectAsStateWithLifecycle()
    val welders by viewModel.welderStats.collectAsStateWithLifecycle()
    val lines by viewModel.lineProgressStats.collectAsStateWithLifecycle()
    val ndtStats by viewModel.ndtTypeStats.collectAsStateWithLifecycle()
    val lastSync by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Hero Header Card with Welding Arc Style
            HeroHeaderCard(
                kpis = kpis,
                lastSync = lastSync,
                onNavigateToSync = onNavigateToSync,
                onExportExcel = { viewModel.exportAndShareExcel(context) }
            )
        }

        // 4 Key Performance Indicators (Grid)
        item {
            Text(
                text = "Indicateurs Clés de Contrôle (KPI)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Total Soudures",
                        value = "${kpis.totalWelds}",
                        subtitle = String.format(Locale.FRANCE, "%.1f Dia-Pouce", kpis.totalInchDia),
                        icon = Icons.Default.Layers,
                        accentColor = ElectricCyan,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToWelds("ALL") },
                        testTag = "kpi_total_welds"
                    )

                    KpiCard(
                        title = "Conformes",
                        value = "${kpis.acceptedCount}",
                        subtitle = String.format(Locale.FRANCE, "%.1f %% validé", kpis.completionPercentage),
                        icon = Icons.Default.CheckCircle,
                        accentColor = ApprovedGreen,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToWelds("COMPLETED") },
                        testTag = "kpi_completed_welds"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "En attente CND",
                        value = "${kpis.pendingNdtCount}",
                        subtitle = "Contrôles RT/UT/PT",
                        icon = Icons.Default.HourglassBottom,
                        accentColor = PendingPurple,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToWelds("PENDING_NDT") },
                        testTag = "kpi_pending_ndt"
                    )

                    KpiCard(
                        title = "Taux de Réparation",
                        value = String.format(Locale.FRANCE, "%.1f%%", kpis.defectRatePercentage),
                        subtitle = "${kpis.repairCount} rejet(s) à meuler",
                        icon = Icons.Default.Warning,
                        accentColor = if (kpis.defectRatePercentage > 4f) RejectRed else WeldAmber,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToWelds("REPAIR_REQUIRED") },
                        testTag = "kpi_repairs"
                    )
                }
            }
        }

        // Global Status Distribution Bar
        item {
            GlobalStatusDistributionCard(kpis = kpis)
        }

        // NDT Methods Breakdown (RT, UT, PT, MT)
        item {
            NdtBreakdownCard(ndtStats = ndtStats)
        }

        // Isometric Lines Progress
        item {
            LinesProgressCard(lines = lines, onNavigateToWelds = onNavigateToWelds, onNavigateToLine = onNavigateToLine)
        }

        // Top Welders Performance
        item {
            WeldersLeaderboardCard(welders = welders)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroHeaderCard(
    kpis: WeldingKpis,
    lastSync: Long?,
    onNavigateToSync: () -> Unit,
    onExportExcel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = IndustrialNavy900)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            IndustrialNavy800,
                            IndustrialNavy900
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WELDING QUALITY DASHBOARD",
                            color = ElectricCyanLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Base de données Soudage",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(IndustrialNavy700)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (lastSync != null) {
                                val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                                "Synchro: ${fmt.format(Date(lastSync))}"
                            } else "Excel Sync prêt",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress ratio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Progression Globale du Projet",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                    Text(
                        text = String.format(Locale.FRANCE, "%.1f %%", kpis.completionPercentage),
                        color = ElectricCyanLight,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (kpis.completionPercentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ElectricCyanLight,
                    trackColor = IndustrialNavy700,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToSync,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hero_sync_google_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Synchro Google", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    FilledTonalButton(
                        onClick = onExportExcel,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hero_export_excel_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exporter Excel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    ElevatedCard(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun GlobalStatusDistributionCard(kpis: WeldingKpis) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "État d'Avancement des Soudures",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            val total = kpis.totalWelds.coerceAtLeast(1).toFloat()
            val pAccepted = (kpis.acceptedCount / total).coerceIn(0f, 1f)
            val pInProgress = (kpis.inProgressCount / total).coerceIn(0f, 1f)
            val pPendingNdt = (kpis.pendingNdtCount / total).coerceIn(0f, 1f)
            val pRepair = (kpis.repairCount / total).coerceIn(0f, 1f)

            // Segmented Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.LightGray.copy(alpha = 0.3f))
            ) {
                if (pAccepted > 0) {
                    Box(
                        modifier = Modifier
                            .weight(pAccepted)
                            .fillMaxSize()
                            .background(ApprovedGreen)
                    )
                }
                if (pInProgress > 0) {
                    Box(
                        modifier = Modifier
                            .weight(pInProgress)
                            .fillMaxSize()
                            .background(WeldAmber)
                    )
                }
                if (pPendingNdt > 0) {
                    Box(
                        modifier = Modifier
                            .weight(pPendingNdt)
                            .fillMaxSize()
                            .background(PendingPurple)
                    )
                }
                if (pRepair > 0) {
                    Box(
                        modifier = Modifier
                            .weight(pRepair)
                            .fillMaxSize()
                            .background(RejectRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem(color = ApprovedGreen, label = "Conformes", count = kpis.acceptedCount)
                LegendItem(color = WeldAmber, label = "En cours", count = kpis.inProgressCount)
                LegendItem(color = PendingPurple, label = "Attente CND", count = kpis.pendingNdtCount)
                LegendItem(color = RejectRed, label = "Réparations", count = kpis.repairCount)
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "$count", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NdtBreakdownCard(ndtStats: List<NdtTypeStat>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Contrôles Non Destructifs (CND / NDT)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Taux d'acceptation par méthode d'examen",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ndtStats.filter { it.total > 0 }.forEach { stat ->
                    val rate = if (stat.total > 0) (stat.accepted.toFloat() / stat.total) * 100f else 0f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stat.ndtType,
                            modifier = Modifier.width(70.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            LinearProgressIndicator(
                                progress = { (rate / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (stat.rejected > 0) WeldAmber else ApprovedGreen,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "${stat.accepted}/${stat.total} (${String.format(Locale.FRANCE, "%.0f%%", rate)})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinesProgressCard(
    lines: List<LineProgress>,
    onNavigateToWelds: (statusFilter: String) -> Unit,
    onNavigateToLine: (line: String) -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Avancement par Ligne Isométrique",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Voir tout",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToWelds("ALL") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lines.take(4).forEach { line ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                            .clickable { onNavigateToLine(line.lineNo) }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = line.lineNo,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${line.completedWelds}/${line.totalWelds} soudures (${String.format(Locale.FRANCE, "%.0f%%", line.percent)})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (line.percent >= 100f) ApprovedGreen else ElectricCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (line.percent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (line.percent >= 100f) ApprovedGreen else ElectricCyan,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeldersLeaderboardCard(welders: List<WelderPerformance>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Performance des Soudeurs (Poinçons)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Suivi qualité et taux de défaut par opérateur",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                welders.take(5).forEach { welder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(IndustrialNavy800),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = welder.welderId,
                                color = ElectricCyanLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = welder.welderName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${welder.totalWelds} soudures | ${String.format(Locale.FRANCE, "%.1f", welder.totalInchDia)}\" Dia",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${welder.acceptedWelds} validées",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApprovedGreen
                            )
                            if (welder.repairCount > 0) {
                                Text(
                                    text = "${welder.repairCount} rejet (${String.format(Locale.FRANCE, "%.1f%%", welder.defectRate)})",
                                    fontSize = 10.sp,
                                    color = RejectRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = "0 défaut (0%)",
                                    fontSize = 10.sp,
                                    color = ApprovedGreenDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
