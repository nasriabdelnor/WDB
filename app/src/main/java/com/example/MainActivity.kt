package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.WeldDatabase
import com.example.data.WeldJoint
import com.example.data.WeldRepository
import com.example.ui.CascadeSelectionScreen
import com.example.ui.FilePreviewDialog
import com.example.ui.HomeScreen
import com.example.ui.JointDetailPage
import com.example.ui.QualityControlScreen
import com.example.ui.SearchCategory
import com.example.ui.SearchEngineScreen
import com.example.ui.SyncSuccessDialog
import com.example.ui.WdbSyncScreen
import com.example.ui.WeldViewModel
import com.example.ui.WeldViewModelFactory
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.IndustrialNavy900
import com.example.ui.theme.WeldTrackTheme

enum class AppScreen(val title: String) {
    HOME("Accueil WDB"),
    CASCADE("Sélection ISO"),
    SEARCH("Recherche"),
    SYNC("WDB Sync"),
    QUALITY_CONTROL("Qualité WDB")
}

class MainActivity : ComponentActivity() {

    private val viewModel: WeldViewModel by viewModels {
        val database = WeldDatabase.getDatabase(applicationContext)
        val repository = WeldRepository(
            database.weldJointDao(),
            database.isometricDao(),
            database.spoolDao(),
            applicationContext
        )
        WeldViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WeldTrackTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: WeldViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var selectedWeldForDetail by remember { mutableStateOf<WeldJoint?>(null) }

    val allWelds by viewModel.allWelds.collectAsStateWithLifecycle()
    val pendingInspection by viewModel.pendingInspection.collectAsStateWithLifecycle()
    val syncSuccessSummary by viewModel.syncSuccessSummary.collectAsStateWithLifecycle()

    // Handle system back gesture: Close detail page first, or return to HOME screen
    BackHandler(enabled = selectedWeldForDetail != null || currentScreen != AppScreen.HOME) {
        if (selectedWeldForDetail != null) {
            selectedWeldForDetail = null
        } else {
            currentScreen = AppScreen.HOME
        }
    }

    // Fullscreen Joint Detail Page
    if (selectedWeldForDetail != null) {
        JointDetailPage(
            weld = selectedWeldForDetail!!,
            onBack = { selectedWeldForDetail = null }
        )
        return
    }

    // Post-Sync Success Dialog showing imported records, ISO, Spool, Joints
    syncSuccessSummary?.let { summary ->
        SyncSuccessDialog(
            summary = summary,
            onDismiss = {
                viewModel.dismissSyncSuccess()
                currentScreen = AppScreen.CASCADE
            }
        )
    }

    // Pre-Import File Inspection Dialog
    pendingInspection?.let { inspection ->
        FilePreviewDialog(
            inspection = inspection,
            onConfirm = { viewModel.confirmPendingInspection(replaceExisting = true) },
            onDismiss = { viewModel.cancelPendingInspection() }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (allWelds.isNotEmpty()) ApprovedGreen else Color(0xFFFBBF24))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "WDB",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ElectricCyanLight.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "v1.0",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyanLight,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Welding Data Base • Traçabilité Industrielle",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IndustrialNavy900,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = IndustrialNavy900,
                contentColor = Color.White
            ) {
                // 1. HOME (WDB Dashboard)
                NavigationBarItem(
                    selected = currentScreen == AppScreen.HOME,
                    onClick = { currentScreen = AppScreen.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Accueil WDB") },
                    label = { Text("Accueil", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_home"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndustrialNavy900,
                        selectedTextColor = ElectricCyanLight,
                        indicatorColor = ElectricCyanLight,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                // 2. ISOMETRIC SELECTION (Cascade: ISO -> Spool -> Joints)
                NavigationBarItem(
                    selected = currentScreen == AppScreen.CASCADE,
                    onClick = { currentScreen = AppScreen.CASCADE },
                    icon = { Icon(Icons.Default.Layers, contentDescription = "Sélection ISO") },
                    label = { Text("Sélection ISO", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_cascade"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndustrialNavy900,
                        selectedTextColor = ElectricCyanLight,
                        indicatorColor = ElectricCyanLight,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                // 3. SEARCH ENGINE (Heat Numbers Part 1 & 2, ISO, Spool, Joint, Welder)
                NavigationBarItem(
                    selected = currentScreen == AppScreen.SEARCH,
                    onClick = { currentScreen = AppScreen.SEARCH },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Recherche") },
                    label = { Text("Recherche", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_search"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndustrialNavy900,
                        selectedTextColor = ElectricCyanLight,
                        indicatorColor = ElectricCyanLight,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                // 4. WDB SYNC (Cloud Download & Excel Sync)
                NavigationBarItem(
                    selected = currentScreen == AppScreen.SYNC,
                    onClick = { currentScreen = AppScreen.SYNC },
                    icon = { Icon(Icons.Default.CloudSync, contentDescription = "WDB Sync") },
                    label = { Text("WDB Sync", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_sync"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndustrialNavy900,
                        selectedTextColor = ElectricCyanLight,
                        indicatorColor = ElectricCyanLight,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                // 5. QUALITY CONTROL (Tools)
                NavigationBarItem(
                    selected = currentScreen == AppScreen.QUALITY_CONTROL,
                    onClick = { currentScreen = AppScreen.QUALITY_CONTROL },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Qualité WDB") },
                    label = { Text("Qualité", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_quality_control"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndustrialNavy900,
                        selectedTextColor = ElectricCyanLight,
                        indicatorColor = ElectricCyanLight,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToIsoSelection = { currentScreen = AppScreen.CASCADE },
                    onNavigateToSearchHeatNumber = {
                        viewModel.setSearchCategory(SearchCategory.HEAT_NUMBER)
                        currentScreen = AppScreen.SEARCH
                    }
                )

                AppScreen.CASCADE -> CascadeSelectionScreen(
                    viewModel = viewModel,
                    onSelectJoint = { selectedWeldForDetail = it }
                )

                AppScreen.SEARCH -> SearchEngineScreen(
                    viewModel = viewModel,
                    onSelectJoint = { selectedWeldForDetail = it }
                )

                AppScreen.SYNC -> WdbSyncScreen(
                    viewModel = viewModel
                )

                AppScreen.QUALITY_CONTROL -> QualityControlScreen(
                    viewModel = viewModel,
                    onSelectJoint = { selectedWeldForDetail = it }
                )
            }
        }
    }
}
