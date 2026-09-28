package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TransferMode
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ApprovedGreenContainer
import com.example.ui.theme.ApprovedGreenDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.IndustrialNavy900
import com.example.ui.theme.RejectRed
import com.example.ui.theme.RejectRedContainer
import com.example.ui.theme.RejectRedDark
import com.example.ui.theme.WeldAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoogleSyncScreen(
    viewModel: WeldViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val lastSync by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
    val allWelds by viewModel.allWelds.collectAsStateWithLifecycle()
    val connectionTestResult by viewModel.connectionTestResult.collectAsStateWithLifecycle()
    val isTestingConnection by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val savedProfiles by viewModel.savedPcProfiles.collectAsStateWithLifecycle()
    val wdbProject by viewModel.wdbProjectInfo.collectAsStateWithLifecycle()
    val showModeDialog by viewModel.showModeSelectorDialog.collectAsStateWithLifecycle()

    // Mode Selector Dialog / Custom Edit Dialog states
    var showEditConfigDialog by remember { mutableStateOf(false) }
    var tempFolderInput by remember { mutableStateOf(wdbProject.folderPath) }
    var tempFileInput by remember { mutableStateOf(wdbProject.fileName) }

    // PC IP Connection states
    var pcIpInput by remember { mutableStateOf("192.168.1.50") }
    var pcPortInput by remember { mutableStateOf("8080") }
    var pcFileInput by remember { mutableStateOf(wdbProject.fileName) }

    // Cloud / OneDrive / SharePoint / Google input prefilled with the official SARPI-DZ SharePoint URL
    var cloudUrlInput by remember {
        mutableStateOf(
            if (wdbProject.oneDriveUrl.isNotBlank()) wdbProject.oneDriveUrl
            else "https://sarpidz-my.sharepoint.com/:x:/r/personal/abdenor_nasri_sarpi-dz_com/Documents/Welding%20Data%20Base01.xlsx?d=w5ec75b1ea7ca4f6d9b6122e0072b8330&csf=1&web=1&e=5dSkgj"
        )
    }
    var replaceExisting by remember { mutableStateOf(true) }

    var showPcGuide by remember { mutableStateOf(false) }

    // Launcher for local file picker (.xlsx, .xls, .csv)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val filename = uri.lastPathSegment ?: wdbProject.fileName
                    viewModel.importLocalFile(inputStream, filename, replaceExisting)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Modal Dialog: Ask Transfer Mode if requested
    if (showModeDialog || wdbProject.transferMode == TransferMode.ASK) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowModeSelectorDialog(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = ElectricCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mode de Transfert WDB", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Comment souhaitez-vous injecter le fichier Excel \"${wdbProject.fileName}\" dans l'application ?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: USB Cable
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setTransferMode(TransferMode.CABLE_USB)
                                viewModel.setShowModeSelectorDialog(false)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (wdbProject.transferMode == TransferMode.CABLE_USB) ElectricCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElectricCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Usb, contentDescription = null, tint = ElectricCyan)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Transfert Direct par Câble USB", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Copie directe PC > Téléphone (Stockage)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Option 2: Internet / OneDrive
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setTransferMode(TransferMode.CLOUD_WEB)
                                viewModel.setShowModeSelectorDialog(false)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (wdbProject.transferMode == TransferMode.CLOUD_WEB) ApprovedGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ApprovedGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = ApprovedGreenDark)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Transfert par Internet / OneDrive", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Lien partagé OneDrive, SharePoint ou Cloud", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Option 3: PC IP Network
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setTransferMode(TransferMode.PC_NETWORK)
                                viewModel.setShowModeSelectorDialog(false)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (wdbProject.transferMode == TransferMode.PC_NETWORK) WeldAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(WeldAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Computer, contentDescription = null, tint = WeldAmber)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Connexion Réseau PC (Adresse IP)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Partage en direct via Wi-Fi ou LAN", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowModeSelectorDialog(false) }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Modal Dialog: Edit Path and File Name
    if (showEditConfigDialog) {
        AlertDialog(
            onDismissRequest = { showEditConfigDialog = false },
            title = { Text("Modifier Chemin & Nom du Fichier WDB", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Configurez l'emplacement du fichier Excel sur votre PC ou serveur :",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempFolderInput,
                        onValueChange = { tempFolderInput = it },
                        label = { Text("Chemin du dossier sur PC") },
                        placeholder = { Text("X:\\7-NDT\\9-SUIVI DE CONTROLE ET NDT PROJET LAB\\WCP") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempFileInput,
                        onValueChange = { tempFileInput = it },
                        label = { Text("Nom du fichier Excel") },
                        placeholder = { Text("Welding Data Base01.xlsx") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateWdbConfig(tempFolderInput, tempFileInput)
                        pcFileInput = tempFileInput.trim()
                        showEditConfigDialog = false
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        tempFolderInput = "X:\\7-NDT\\9-SUIVI DE CONTROLE ET NDT PROJET LAB\\WCP"
                        tempFileInput = "Welding Data Base01.xlsx"
                        viewModel.updateWdbConfig(tempFolderInput, tempFileInput)
                        pcFileInput = tempFileInput
                        showEditConfigDialog = false
                    }
                ) {
                    Text("Défaut LAB")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("google_sync_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Text(
                text = "Injection Base WDB & Synchronisation",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Injectez votre fichier officiel de soudage (Câble USB, OneDrive / Cloud, ou Adresse IP réseau).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Active WDB Card Configuration
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().testTag("wdb_config_summary_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = IndustrialNavy900)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BASE WDB ACTIVE :",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                        }

                        IconButton(
                            onClick = {
                                tempFolderInput = wdbProject.folderPath
                                tempFileInput = wdbProject.fileName
                                showEditConfigDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = wdbProject.fileName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Chemin PC : ${wdbProject.folderPath}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (wdbProject.transferMode) {
                                TransferMode.CABLE_USB -> ElectricCyan.copy(alpha = 0.2f)
                                TransferMode.CLOUD_WEB -> ApprovedGreenDark.copy(alpha = 0.3f)
                                TransferMode.PC_NETWORK -> WeldAmber.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = when (wdbProject.transferMode) {
                                    TransferMode.CABLE_USB -> "Mode sélectionné : Câble USB Direct"
                                    TransferMode.CLOUD_WEB -> "Mode sélectionné : Internet / OneDrive"
                                    TransferMode.PC_NETWORK -> "Mode sélectionné : Réseau PC (IP)"
                                    else -> "Mode : À définir"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        TextButton(
                            onClick = { viewModel.setShowModeSelectorDialog(true) }
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricCyanLight)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Changer", fontSize = 11.sp, color = ElectricCyanLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live Status Banner
        item {
            AnimatedVisibility(visible = syncState !is SyncUiState.Idle) {
                when (val state = syncState) {
                    is SyncUiState.Syncing -> {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("sync_loading_banner"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = ElectricCyan)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Chargement & Injection en cours...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Extraction des spools, joints et statuts CND...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    is SyncUiState.Success -> {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("sync_success_banner"),
                            colors = CardDefaults.cardColors(containerColor = ApprovedGreenContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ApprovedGreenDark)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Injection Réussie !", fontWeight = FontWeight.Bold, color = ApprovedGreenDark)
                                    Text(state.message, fontSize = 12.sp, color = ApprovedGreenDark)
                                }
                                Button(
                                    onClick = { viewModel.clearSyncMessage() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ApprovedGreenDark)
                                ) {
                                    Text("OK", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    is SyncUiState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("sync_error_banner"),
                            colors = CardDefaults.cardColors(containerColor = RejectRedContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RejectRedDark)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Erreur lors de l'injection", fontWeight = FontWeight.Bold, color = RejectRedDark, fontSize = 14.sp)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(state.message, fontSize = 12.sp, color = RejectRedDark)

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.clearSyncMessage()
                                            filePickerLauncher.launch("*/*")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RejectRedDark),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("📂 Choisir fichier XLSX", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.clearSyncMessage() },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Fermer", fontSize = 11.sp, color = RejectRedDark)
                                    }
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // Mode Navigation Tabs
        item {
            val selectedTabIndex = when (wdbProject.transferMode) {
                TransferMode.CABLE_USB -> 0
                TransferMode.CLOUD_WEB -> 1
                TransferMode.PC_NETWORK -> 2
                else -> 0
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = ElectricCyan
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { viewModel.setTransferMode(TransferMode.CABLE_USB) },
                    text = { Text("1. Câble USB", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { viewModel.setTransferMode(TransferMode.CLOUD_WEB) },
                    text = { Text("2. OneDrive", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { viewModel.setTransferMode(TransferMode.PC_NETWORK) },
                    text = { Text("3. Réseau IP", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        // ==========================================
        // OPTION 1 : DIRECT TRANSFER VIA USB CABLE
        // ==========================================
        if (wdbProject.transferMode == TransferMode.CABLE_USB || wdbProject.transferMode == TransferMode.ASK) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().testTag("cable_transfer_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElectricCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Usb, contentDescription = null, tint = ElectricCyan)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Transfert Direct par Câble USB", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Branchez votre smartphone au PC avec le câble", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Step-by-step instruction
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Procédure simple en 3 étapes :",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "1. Branchez votre téléphone au PC par câble USB et choisissez « Transfert de fichiers » (MTP) sur l'écran du smartphone.\n" +
                                            "2. Sur le PC, rendez-vous dans le dossier :\n" +
                                            "   📁 ${wdbProject.folderPath}\n" +
                                            "   et copiez le fichier « ${wdbProject.fileName} » dans le dossier Téléchargements (Download) ou Documents de votre téléphone.\n" +
                                            "3. Appuyez sur le bouton vert ci-dessous pour injecter le fichier !",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Replace toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (replaceExisting) "Remplacer l'ancienne base" else "Fusionner avec la base",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = replaceExisting,
                                onCheckedChange = { replaceExisting = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("inject_cable_file_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = ApprovedGreenDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📥 Injecter « ${wdbProject.fileName} »", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // ==========================================
        // OPTION 2 : INTERNET / ONEDRIVE / CLOUD TRANSFER
        // ==========================================
        if (wdbProject.transferMode == TransferMode.CLOUD_WEB) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().testTag("onedrive_sync_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ApprovedGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = ApprovedGreenDark)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Transfert par Internet (OneDrive / Cloud)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Dossier OneDrive, SharePoint ou plateforme Web", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "💡 Comment partager via OneDrive / SharePoint :",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1. Mettez « ${wdbProject.fileName} » dans votre dossier OneDrive.\n" +
                                            "2. Clic droit > « Partager » > « Copier le lien ».\n" +
                                            "3. Collez ce lien ci-dessous. L'application le télécharge directement !",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Lien SharePoint configuré :", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Surface(
                                onClick = {
                                    cloudUrlInput = "https://sarpidz-my.sharepoint.com/:x:/r/personal/abdenor_nasri_sarpi-dz_com/Documents/Welding%20Data%20Base01.xlsx?d=w5ec75b1ea7ca4f6d9b6122e0072b8330&csf=1&web=1&e=5dSkgj"
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = ApprovedGreen.copy(alpha = 0.2f)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = ApprovedGreenDark, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lien SARPI-DZ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ApprovedGreenDark)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = cloudUrlInput,
                            onValueChange = { cloudUrlInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onedrive_url_input"),
                            label = { Text("Lien OneDrive, SharePoint, Google Sheets ou URL") },
                            placeholder = { Text("https://1drv.ms/x/... ou https://onedrive.live.com/...") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    clipboardManager.getText()?.let {
                                        cloudUrlInput = it.text
                                    }
                                }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Coller")
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (replaceExisting) "Remplacer l'ancienne base" else "Fusionner avec la base",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = replaceExisting,
                                onCheckedChange = { replaceExisting = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.syncFromCloudOrOneDrive(cloudUrlInput, replaceExisting) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sync_onedrive_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ApprovedGreenDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("☁️ Télécharger & Injecter depuis Internet", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Direct Local Fallback Button
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("onedrive_fallback_pick_file"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp), tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📂 Ou Importer le fichier XLSX en local", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricCyan)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Astuce : Si le lien SharePoint SARPI-DZ bloque le téléchargement direct (mot de passe entreprise), appuyez sur le bouton « Importer le fichier XLSX en local » ci-dessus.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // OPTION 3 : PC IP / LAN CONNECTION
        // ==========================================
        if (wdbProject.transferMode == TransferMode.PC_NETWORK) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().testTag("pc_ip_sync_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElectricCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Computer, contentDescription = null, tint = ElectricCyan)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Connexion Directe PC par IP", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Connexion Wi-Fi locale ou tunnel distant (Ngrok)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Preset connection profiles
                        Text(
                            text = "Profils de connexion :",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            savedProfiles.forEach { profile ->
                                val isSelected = pcIpInput == profile.ip && pcPortInput == profile.port
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            pcIpInput = profile.ip
                                            pcPortInput = profile.port
                                            pcFileInput = profile.filename
                                            viewModel.clearConnectionTest()
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) ElectricCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = profile.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${profile.ip}:${profile.port}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // IP Address & Port Inputs
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pcIpInput,
                                onValueChange = {
                                    pcIpInput = it
                                    viewModel.clearConnectionTest()
                                },
                                label = { Text("Adresse IP du PC") },
                                placeholder = { Text("192.168.1.50") },
                                leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, tint = ElectricCyan) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(2f).testTag("pc_ip_input")
                            )

                            OutlinedTextField(
                                value = pcPortInput,
                                onValueChange = {
                                    pcPortInput = it
                                    viewModel.clearConnectionTest()
                                },
                                label = { Text("Port") },
                                placeholder = { Text("8080") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("pc_port_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // File path input
                        OutlinedTextField(
                            value = pcFileInput,
                            onValueChange = {
                                pcFileInput = it
                                viewModel.clearConnectionTest()
                            },
                            label = { Text("Nom du fichier Excel") },
                            placeholder = { Text("Welding Data Base01.xlsx") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("pc_filename_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Connection Test Result Banner
                        connectionTestResult?.let { result ->
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("connection_test_result_card"),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (result.isSuccess) ApprovedGreenContainer else RejectRedContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (result.isSuccess) ApprovedGreenDark else RejectRedDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (result.isSuccess) "Serveur PC Réactif (${result.responseTimeMs} ms)" else "Serveur Non Accessible",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (result.isSuccess) ApprovedGreenDark else RejectRedDark
                                        )
                                        Text(
                                            text = result.message + if (result.fileSizeKb > 0) " (${result.fileSizeKb} Ko)" else "",
                                            fontSize = 11.sp,
                                            color = if (result.isSuccess) ApprovedGreenDark else RejectRedDark
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Action buttons: Test Ping + Synchronize
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.testPcConnection(pcIpInput, pcPortInput, pcFileInput) },
                                enabled = !isTestingConnection,
                                modifier = Modifier.weight(1f).testTag("test_pc_connection_btn"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tester Ping", fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = { viewModel.syncFromPcIp(pcIpInput, pcPortInput, pcFileInput, replaceExisting) },
                                modifier = Modifier.weight(1.5f).testTag("sync_from_pc_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Synchroniser par IP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Toggle PC sharing guide
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showPcGuide = !showPcGuide },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showPcGuide) "Masquer les commandes PC" else "Comment lancer le partage sur le PC en 1 commande ?",
                                fontSize = 12.sp,
                                color = ElectricCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        }

                        if (showPcGuide) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = IndustrialNavy900),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Sur le PC (PowerShell ou Invite de commandes) :",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "cd \"${wdbProject.folderPath}\"\npython -m http.server 8080",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = ElectricCyanLight
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tapez ensuite 'ipconfig' pour voir l'adresse IP de votre PC et renseignez-la ci-dessus.",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: EXPORT EXCEL
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().testTag("export_excel_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(IndustrialNavy800),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = ElectricCyanLight)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Exporter la Base Actuelle", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${allWelds.size} soudures enregistrées en base locale", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FilledTonalButton(
                        onClick = { viewModel.exportAndShareExcel(context) },
                        modifier = Modifier.fillMaxWidth().testTag("export_share_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partager / Exporter le fichier WDB")
                    }
                }
            }
        }

        // Section: Reset Demo Data
        item {
            OutlinedButton(
                onClick = { viewModel.resetToDemoData() },
                modifier = Modifier.fillMaxWidth().testTag("reset_demo_data_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recharger le modèle WCP Projet LAB de démo")
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
