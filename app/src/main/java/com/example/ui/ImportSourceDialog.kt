package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.IndustrialNavy900

@Composable
fun ImportSourceDialog(
    viewModel: WeldViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    var googleDriveUrl by remember { mutableStateOf("") }
    var oneDriveUrl by remember { mutableStateOf("") }
    var directUrl by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val filename = uri.lastPathSegment?.substringAfterLast('/') ?: "Welding Data Base01.xlsx"
                    viewModel.inspectFile(inputStream, filename, "Fichier local de l'appareil")
                    onDismiss()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
                .testTag("import_source_dialog"),
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "IMPORTATION MULTI-SOURCES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ElectricCyanLight,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Choisir la Source Excel WDB",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_import_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4 Tabs: Local, Google Drive, OneDrive, URL
                val tabs = listOf("Local", "Google Drive", "OneDrive", "URL Directe")
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = IndustrialNavy800,
                    contentColor = ElectricCyanLight,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ElectricCyan
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("import_tab_$index")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                when (selectedTab) {
                    // TAB 0: LOCAL DEVICE XLSX
                    0 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = IndustrialNavy800)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = ElectricCyanLight)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Fichier Local sur Tablette / Téléphone", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Sélectionnez votre fichier Welding Data Base01 (.xlsx, .csv) stocké dans vos Téléchargements ou sur votre carte SD.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { filePickerLauncher.launch("*/*") },
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("browse_local_file_btn"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = IndustrialNavy900)
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Parcourir les fichiers de l'appareil", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Quick sample template option
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = IndustrialNavy800.copy(alpha = 0.6f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Synchroniser avec le Google Sheet WDB officiel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Importez en temps réel les enregistrements de soudure depuis la base Google Sheets officielle.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.syncWdbGoogleSheet()
                                            onDismiss()
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("load_sample_template_btn"),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyanLight),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("⚡ Synchroniser WDB Google Sheet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    // TAB 1: GOOGLE DRIVE
                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = IndustrialNavy800)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF38BDF8))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Lien Google Drive / Sheets", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Collez le lien de partage Google Drive ou Google Sheets de votre base de soudage (accessible avec le lien).",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = googleDriveUrl,
                                        onValueChange = { googleDriveUrl = it },
                                        placeholder = { Text("https://drive.google.com/file/d/...", color = Color(0xFF64748B), fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().testTag("gdrive_url_input"),
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
                                    Button(
                                        onClick = {
                                            viewModel.inspectUrl(googleDriveUrl, "Google Drive")
                                            onDismiss()
                                        },
                                        enabled = googleDriveUrl.isNotBlank() && syncState !is SyncUiState.Syncing,
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("gdrive_download_btn"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White)
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Télécharger & Inspecter Google Drive", fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.syncWdbGoogleSheet()
                                            onDismiss()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("sync_official_gsheet_btn"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyanLight),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Connecter le Google Sheet WDB officiel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // TAB 2: ONEDRIVE / SHAREPOINT
                    2 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = IndustrialNavy800)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF60A5FA))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Lien OneDrive / SharePoint", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Collez le lien de partage OneDrive ou SharePoint de votre fichier Excel Welding Data Base01.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = oneDriveUrl,
                                        onValueChange = { oneDriveUrl = it },
                                        placeholder = { Text("https://1drv.ms/x/... ou https://company.sharepoint.com/...", color = Color(0xFF64748B), fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().testTag("onedrive_url_input"),
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
                                    Button(
                                        onClick = {
                                            viewModel.inspectUrl(oneDriveUrl, "OneDrive / SharePoint")
                                            onDismiss()
                                        },
                                        enabled = oneDriveUrl.isNotBlank() && syncState !is SyncUiState.Syncing,
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("onedrive_download_btn"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White)
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Télécharger & Inspecter OneDrive", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // TAB 3: DIRECT URL
                    3 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = IndustrialNavy800)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Link, contentDescription = null, tint = ElectricCyanLight)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Téléchargement Direct HTTP / Intranet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Téléchargez directement depuis un serveur HTTP, intranet de chantier ou serveur FTP/HTTP local.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = directUrl,
                                        onValueChange = { directUrl = it },
                                        placeholder = { Text("http://192.168.1.100:8080/Welding Data Base01.xlsx", color = Color(0xFF64748B), fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().testTag("direct_url_input"),
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
                                    Button(
                                        onClick = {
                                            viewModel.inspectUrl(directUrl, "URL Directe")
                                            onDismiss()
                                        },
                                        enabled = directUrl.isNotBlank() && syncState !is SyncUiState.Syncing,
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("direct_download_btn"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = IndustrialNavy900)
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Télécharger & Inspecter URL", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
