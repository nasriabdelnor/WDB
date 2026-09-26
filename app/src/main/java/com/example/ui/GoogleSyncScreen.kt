package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    // PC IP Connection states
    var pcIpInput by remember { mutableStateOf("192.168.1.50") }
    var pcPortInput by remember { mutableStateOf("8080") }
    var pcFileInput by remember { mutableStateOf("ISO_WDB_0002.xlsx") }

    // Google Sheets input
    var sheetUrlInput by remember { mutableStateOf("") }
    var replaceExisting by remember { mutableStateOf(true) }

    var showPcGuide by remember { mutableStateOf(true) }

    // Launcher for local file picker (.xlsx, .xls, .csv)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val filename = uri.lastPathSegment ?: "ISO_WDB_0002.xlsx"
                    viewModel.importLocalFile(inputStream, filename, replaceExisting)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("google_sync_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Synchronisation PC & Fichier Excel",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Connectez directement votre smartphone au PC contenant la base de données Excel (via adresse IP, Wi-Fi ou Internet) ou synchronisez avec Google Sheets.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ElectricCyan)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Connexion et téléchargement en cours...", fontWeight = FontWeight.Bold)
                                    Text("Extraction des feuilles Excel et mise à jour de la base...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ApprovedGreenDark)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Synchronisation Réussie !", fontWeight = FontWeight.Bold, color = ApprovedGreenDark)
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
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RejectRedDark)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Échec de connexion", fontWeight = FontWeight.Bold, color = RejectRedDark)
                                    Text(state.message, fontSize = 12.sp, color = RejectRedDark)
                                }
                                Button(
                                    onClick = { viewModel.clearSyncMessage() },
                                    colors = ButtonDefaults.buttonColors(containerColor = RejectRedDark)
                                ) {
                                    Text("Fermer", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // Section 1: DIRECT PC CONNECTION VIA IP ADDRESS
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pc_ip_sync_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .background(ElectricCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Computer, contentDescription = null, tint = ElectricCyan)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Connexion au PC par Adresse IP", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Accès direct au fichier Excel partagé sur le PC", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset connection profiles
                    Text(
                        text = "Profils de connexion rapides :",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
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
                            label = { Text("Adresse IP du PC ou Domaine") },
                            placeholder = { Text("ex: 192.168.1.50 ou ngrok URL") },
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
                        label = { Text("Nom du fichier Excel ou sous-dossier") },
                        placeholder = { Text("ISO_WDB_0002.xlsx") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("pc_filename_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live URL preview
                    val previewUrl = remember(pcIpInput, pcPortInput, pcFileInput) {
                        val ip = pcIpInput.trim().removePrefix("http://").removePrefix("https://").removeSuffix("/")
                        val port = pcPortInput.trim().ifBlank { "8080" }
                        val file = pcFileInput.trim().removePrefix("/")
                        if (ip.contains(".ngrok") || ip.startsWith("http://") || ip.startsWith("https://") || port == "80" || port == "443") {
                            if (ip.startsWith("http")) "$ip/$file" else "https://$ip/$file"
                        } else {
                            "http://$ip:$port/$file"
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(IndustrialNavy900)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "URL DE SYNCHRONISATION :",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = previewUrl,
                                color = ElectricCyanLight,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Connection Test Result Banner
                    connectionTestResult?.let { result ->
                        Spacer(modifier = Modifier.height(10.dp))
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
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Replace / Merge toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (replaceExisting) "Remplacer la base existante" else "Fusionner avec la base",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = replaceExisting,
                            onCheckedChange = { replaceExisting = it },
                            modifier = Modifier.testTag("pc_replace_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons: Test Connection + Synchronize
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
                                Text("Tester Ping", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                            Text("Synchroniser", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle PC sharing guide
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPcGuide = !showPcGuide },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showPcGuide) "Masquer le guide de partage PC" else "Comment partager depuis le PC en 1 minute ?",
                            fontSize = 12.sp,
                            color = ElectricCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (showPcGuide) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "3 Méthodes simples pour donner une adresse IP à votre fichier Excel :",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "1. Méthode Python (Super rapide, sans installation) :\n" +
                                            "   • Sur le PC, ouvrez PowerShell ou l'invite de commande dans le dossier de votre fichier Excel (ex: Spooling TECNOGRAF).\n" +
                                            "   • Tapez : python -m http.server 8080\n" +
                                            "   • Tapez ipconfig pour connaître l'IP du PC (ex: 192.168.1.50) et cliquez sur Synchroniser sur votre téléphone !\n\n" +
                                            "2. Méthode Logiciel gratuit (HFS - HTTP File Server) :\n" +
                                            "   • Téléchargez HFS sur le PC et glissez votre fichier ISO_WDB_0002.xlsx dedans. Il vous donne directement l'adresse IP.\n\n" +
                                            "3. Méthode Accès à distance via Internet (Hors du bureau) :\n" +
                                            "   • Utilisez Tailscale ou Ngrok (ngrok http 8080) sur votre PC pour avoir une IP/URL accessible depuis n'importe où dans le monde sur votre smartphone 4G/5G.",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: GOOGLE SHEETS / CLOUD SYNC
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().testTag("google_sheets_sync_card"),
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
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = ApprovedGreenDark)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Synchronisation Google Sheets", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Lien partagé Google Docs ou Excel hébergé en ligne", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = sheetUrlInput,
                        onValueChange = { sheetUrlInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_url_input"),
                        label = { Text("URL Google Sheets") },
                        placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                        trailingIcon = {
                            IconButton(onClick = {
                                clipboardManager.getText()?.let {
                                    sheetUrlInput = it.text
                                }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Coller")
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.syncFromGoogleUrl(sheetUrlInput, replaceExisting) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_google_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ApprovedGreenDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Télécharger & Synchroniser Google Sheets", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 3: LOCAL FILE PICKER ON PHONE
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().testTag("local_file_import_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(WeldAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = WeldAmber)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Sélectionner un fichier sur le Téléphone", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Fichiers stockés dans la mémoire du téléphone (.xlsx, .csv)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Parcourez vos dossiers (ex: Stockage interne > LAB Skikda > Spooling TECNOGRAF > ISO_WDB_0002.xlsx).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_excel_file_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sélectionner un Fichier (.xlsx / .csv)")
                    }
                }
            }
        }

        // Section 4: EXPORT EXCEL
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
                            Text("Exporter & Mettre à jour l'Excel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${allWelds.size} soudures enregistrées dans l'application", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FilledTonalButton(
                        onClick = { viewModel.exportAndShareExcel(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_share_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partager / Exporter la Base Excel")
                    }
                }
            }
        }

        // Section 5: Demo Data Reset
        item {
            OutlinedButton(
                onClick = { viewModel.resetToDemoData() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_demo_data_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recharger le modèle industriel de démonstration")
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
