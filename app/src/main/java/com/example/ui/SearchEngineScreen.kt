package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.data.WeldJoint
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.IndustrialNavy900
import com.example.ui.theme.RejectRed

@Composable
fun SearchEngineScreen(
    viewModel: WeldViewModel,
    onSelectJoint: (WeldJoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchCategory by viewModel.searchCategory.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val allWelds by viewModel.allWelds.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IndustrialNavy900)
            .padding(16.dp)
            .testTag("search_engine_screen")
    ) {
        // Header
        Text(
            text = "MOTEUR DE RECHERCHE TRAÇABILITÉ",
            color = ElectricCyanLight,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Recherche Rapide & N° de Coulée",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Rechercher par N° Coulée (Heat No), ISO, Spool, Joint, Soudeur…", color = Color(0xFF64748B), fontSize = 12.5.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricCyanLight) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = Color(0xFF94A3B8))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = IndustrialNavy800,
                unfocusedContainerColor = IndustrialNavy800,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = Color(0xFF334155)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input_field"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter categories pills including Part 1 and Part 2 Heat Numbers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchCategory.values().forEach { category ->
                val selected = category == searchCategory
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setSearchCategory(category) },
                    label = {
                        Text(
                            text = category.label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = IndustrialNavy800,
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                        labelColor = Color(0xFF94A3B8),
                        selectedLabelColor = ElectricCyanLight
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0xFF334155),
                        selectedBorderColor = ElectricCyan,
                        enabled = true,
                        selected = selected
                    ),
                    modifier = Modifier.testTag("filter_chip_${category.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Results counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (searchQuery.isBlank()) "Saisissez un mot-clé ou filtrez par N° de coulée"
                       else "${searchResults.size} résultat(s) trouvé(s)",
                color = if (searchResults.isNotEmpty()) ElectricCyanLight else Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (allWelds.isNotEmpty()) {
                Text(
                    text = "${allWelds.size} joints indexés",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Results List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("search_results_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(searchResults, key = { it.id }) { weld ->
                SearchWeldResultCard(
                    weld = weld,
                    searchQuery = searchQuery,
                    onClick = { onSelectJoint(weld) }
                )
            }

            if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun joint ou numéro de coulée ne correspond à \"$searchQuery\"",
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
private fun SearchWeldResultCard(
    weld: WeldJoint,
    searchQuery: String,
    onClick: () -> Unit
) {
    val q = searchQuery.trim()
    val matchPart1Heat = q.isNotBlank() && weld.part1HeatNumber.contains(q, ignoreCase = true)
    val matchPart2Heat = q.isNotBlank() && weld.part2HeatNumber.contains(q, ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("search_result_${weld.jointNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = IndustrialNavy800),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (matchPart1Heat || matchPart2Heat) ElectricCyan else Color(0xFF334155)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Joint & Location
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
                            text = "ISO: ${weld.isoNumber} • Spool: ${weld.spoolNumber}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

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
            Spacer(modifier = Modifier.height(10.dp))

            // PROMINENT HEAT NUMBERS (PART 1 & PART 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // PART 1 HEAT NUMBER
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (matchPart1Heat) ElectricCyan.copy(alpha = 0.22f) else IndustrialNavy900,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (matchPart1Heat) ElectricCyan else Color(0xFF334155)
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "HEAT NUMBER PART 1",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (matchPart1Heat) ElectricCyanLight else Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = weld.part1HeatNumber.ifBlank { "Non renseigné" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (weld.part1HeatNumber.isNotBlank()) Color.White else Color(0xFF64748B)
                        )
                    }
                }

                // PART 2 HEAT NUMBER
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (matchPart2Heat) ElectricCyan.copy(alpha = 0.22f) else IndustrialNavy900,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (matchPart2Heat) ElectricCyan else Color(0xFF334155)
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "HEAT NUMBER PART 2",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (matchPart2Heat) ElectricCyanLight else Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = weld.part2HeatNumber.ifBlank { "Non renseigné" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (weld.part2HeatNumber.isNotBlank()) Color.White else Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Technical details footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Soudeur: ${weld.welder.ifBlank { "N/A" }} • WPS: ${weld.wps}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Text(
                    text = "RT: ${weld.rt} • PT: ${weld.pt}",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
