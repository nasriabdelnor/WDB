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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.WeldRepository
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.IndustrialNavy900
import com.example.ui.theme.RejectRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WdbSyncScreen(
    viewModel: WeldViewModel,
    modifier: Modifier = Modifier
) {
    val totalWelds by viewModel.totalWeldsCount.collectAsStateWithLifecycle()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
    val lastSyncSource by viewModel.lastSyncSource.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    var googleSheetUrl by remember { mutableStateOf(WeldRepository.DEFAULT_GOOGLE_SHEET_URL) }
    var gdriveUrl by remember { mutableStateOf("") }
    var onedriveUrl by remember { mutableStateOf("") }
    var intranetUrl by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(IndustrialNavy900)
            .padding(16.dp)
            .testTag("wdb_sync_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header: WDB SYNC
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = IndustrialNavy800)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "WDB SYNC",
                        color = ElectricCyanLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Synchronisation Cloud WDB",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Téléchargez et mettez à jour votre base de données Excel Welding Data Base01 directement depuis le cloud.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Sync Status Banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "DERNIÈRE SYNCHRONISATION", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            val formattedTime = lastSyncTimestamp?.let {
                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it))
                            } ?: "Jamais synchronisé"
                            Text(
                                text = "$formattedTime ${lastSyncSource?.let { "• $it" } ?: ""}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElectricCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$totalWelds joints en base",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyanLight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Notification Banner
        item {
            when (val state = syncState) {
                is SyncUiState.Syncing -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ElectricCyan.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ElectricCyan, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Téléchargement et analyse en cours...", color = ElectricCyanLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                is SyncUiState.Success -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ApprovedGreen.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ApprovedGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(state.message, color = Color(0xFF86EFAC), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                is SyncUiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RejectRed.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RejectRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(state.message, color = Color(0xFFFCA5A5), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                SyncUiState.Idle -> Unit
            }
        }

        // Primary Source: Google Sheets WDB
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = IndustrialNavy800),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
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
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ElectricCyan.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "SOURCE PRINCIPALE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ElectricCyanLight,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text("WDB Google Sheet", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ApprovedGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "CONNECTÉ",
                                color = Color(0xFF86EFAC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Base de données centrale hébergée sur Google Sheets. Remplace la base vide et importe l'ensemble des enregistrements de soudage.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = googleSheetUrl,
                        onValueChange = { googleSheetUrl = it },
                        label = { Text("URL Google Sheets WDB", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("gsheet_url_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = IndustrialNavy900,
                            unfocusedContainerColor = IndustrialNavy900
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Workflow Diagram: Google Sheet ↓ Download latest data ↓ Validate columns ↓ Update local WDB database ↓ Refresh ISO / SPOOL / JOINT filters
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = IndustrialNavy900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("WORKFLOW SYNCHRONISATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ElectricCyanLight, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            val steps = listOf(
                                "1. Google Sheet",
                                "2. Téléchargement des dernières données",
                                "3. Validation des 13 colonnes obligatoires",
                                "4. Mise à jour de la base Room locale WDB",
                                "5. Actualisation des filtres ISO / SPOOL / JOINT"
                            )
                            steps.forEachIndexed { idx, step ->
                                Text(
                                    text = step,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (idx < steps.size - 1) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = ElectricCyan.copy(alpha = 0.5f),
                                        modifier = Modifier.size(12.dp).padding(vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Prominent SYNC WDB Button
                    Button(
                        onClick = { viewModel.syncWdbGoogleSheet(googleSheetUrl) },
                        enabled = syncState !is SyncUiState.Syncing,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_sync_wdb_screen"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = IndustrialNavy900)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SYNC WDB", fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 0.5.sp)
                    }
                }
            }
        }

        // Card 1: Google Drive Sync
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = IndustrialNavy800),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Synchronisation Google Drive", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Téléchargez automatiquement la dernière version partagée de votre classeur Excel.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = gdriveUrl,
                        onValueChange = { gdriveUrl = it },
                        placeholder = { Text("Lien Google Drive : https://drive.google.com/file/d/...", color = Color(0xFF64748B), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("sync_gdrive_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = IndustrialNavy900,
                            unfocusedContainerColor = IndustrialNavy900
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.inspectUrl(gdriveUrl, "Google Drive") },
                        enabled = gdriveUrl.isNotBlank() && syncState !is SyncUiState.Syncing,
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("sync_gdrive_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Télécharger & Synchroniser", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Card 2: OneDrive / SharePoint Sync
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = IndustrialNavy800),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF60A5FA))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Synchronisation OneDrive / SharePoint", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connectez-vous au répertoire de partage OneDrive ou SharePoint de votre entreprise.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = onedriveUrl,
                        onValueChange = { onedriveUrl = it },
                        placeholder = { Text("Lien OneDrive : https://1drv.ms/x/... ou SharePoint", color = Color(0xFF64748B), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("sync_onedrive_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = IndustrialNavy900,
                            unfocusedContainerColor = IndustrialNavy900
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.inspectUrl(onedriveUrl, "OneDrive / SharePoint") },
                        enabled = onedriveUrl.isNotBlank() && syncState !is SyncUiState.Syncing,
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("sync_onedrive_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Télécharger & Synchroniser", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Card 3: Intranet / HTTP Server
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = IndustrialNavy800),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = ElectricCyanLight)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Serveur HTTP / Intranet de Chantier", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Adresse IP directe d'un PC serveur local sur le réseau Wi-Fi de chantier.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = intranetUrl,
                        onValueChange = { intranetUrl = it },
                        placeholder = { Text("Ex: http://192.168.1.50:8080/Welding Data Base01.xlsx", color = Color(0xFF64748B), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("sync_intranet_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = IndustrialNavy900,
                            unfocusedContainerColor = IndustrialNavy900
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.inspectUrl(intranetUrl, "Serveur Intranet") },
                        enabled = intranetUrl.isNotBlank() && syncState !is SyncUiState.Syncing,
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("sync_intranet_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = IndustrialNavy900)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Télécharger & Synchroniser", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
