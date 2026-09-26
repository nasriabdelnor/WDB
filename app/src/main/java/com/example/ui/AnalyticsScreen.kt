package com.example.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.RejectRed
import com.example.ui.theme.WeldAmber
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: WeldViewModel,
    modifier: Modifier = Modifier
) {
    val kpis by viewModel.kpis.collectAsStateWithLifecycle()
    val welders by viewModel.welderStats.collectAsStateWithLifecycle()
    val lines by viewModel.lineProgressStats.collectAsStateWithLifecycle()
    val ndtStats by viewModel.ndtTypeStats.collectAsStateWithLifecycle()
    val allWelds by viewModel.allWelds.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Rapports & Analyses Qualité",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Statistiques avancées pour ingénieurs soudage et coordinateurs QC.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Global Rate Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Taux de Qualité Global du Projet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Icon(Icons.Default.Speed, contentDescription = null, tint = ElectricCyan)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val qualityRate = if (kpis.totalWelds > 0) {
                        100f - kpis.defectRatePercentage
                    } else 100f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = String.format(Locale.FRANCE, "%.1f %%", qualityRate),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (qualityRate >= 95f) ApprovedGreen else WeldAmber
                            )
                            Text(
                                text = "Taux de conformité 1er coup (First Time Yield)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${kpis.acceptedCount} / ${kpis.totalWelds} soudures",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.FRANCE, "%.1f Dia-Pouce", kpis.totalInchDia),
                                fontSize = 11.sp,
                                color = ElectricCyan
                            )
                        }
                    }
                }
            }
        }

        // Welder Quality Index (Poinçons)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                            text = "Indice Qualité Soudeurs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Icon(Icons.Default.Engineering, contentDescription = null, tint = ElectricCyan)
                    }
                    Text(
                        text = "Suivi individuel des défauts et conformité ASME IX / ISO 9606",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        welders.forEach { w ->
                            val successRate = if (w.totalWelds > 0) (w.acceptedWelds.toFloat() / w.totalWelds) * 100f else 100f
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(IndustrialNavy800),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(w.welderId, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(w.welderName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    Text(
                                        text = String.format(Locale.FRANCE, "%.0f%% conforme", successRate),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (successRate >= 95f) ApprovedGreen else RejectRed
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LinearProgressIndicator(
                                    progress = { (successRate / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (successRate >= 95f) ApprovedGreen else RejectRed,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${w.totalWelds} soudures • ${String.format(Locale.FRANCE, "%.1f", w.totalInchDia)}\" Dia",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (w.repairCount > 0) "${w.repairCount} réparation(s)" else "Zéro défaut",
                                        fontSize = 11.sp,
                                        color = if (w.repairCount > 0) RejectRed else ApprovedGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Materials Distribution Card
        item {
            val materials = allWelds.groupBy { it.material.ifBlank { "Autre" } }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Répartition par Matériau Métallurgique",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    materials.forEach { (mat, list) ->
                        val pct = if (allWelds.isNotEmpty()) (list.size.toFloat() / allWelds.size) * 100f else 0f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mat, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${list.size} soudures (${String.format(Locale.FRANCE, "%.0f%%", pct)})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
