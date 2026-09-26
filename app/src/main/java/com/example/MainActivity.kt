package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import com.example.ui.AddWeldDialog
import com.example.ui.AnalyticsScreen
import com.example.ui.DashboardScreen
import com.example.ui.GoogleSyncScreen
import com.example.ui.WeldDetailDialog
import com.example.ui.WeldViewModel
import com.example.ui.WeldViewModelFactory
import com.example.ui.WeldsListScreen
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.IndustrialNavy900
import com.example.ui.theme.WeldTrackTheme
import java.util.Locale

enum class AppScreen(val title: String) {
    DASHBOARD("Tableau de bord"),
    WELDS("Base Soudures"),
    SYNC("Synchro Google & Excel"),
    ANALYTICS("Analyses & Rapports")
}

class MainActivity : ComponentActivity() {

    private val viewModel: WeldViewModel by viewModels {
        val database = WeldDatabase.getDatabase(applicationContext)
        val repository = WeldRepository(database.weldJointDao(), applicationContext)
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
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var selectedWeldForDetail by remember { mutableStateOf<WeldJoint?>(null) }
    var showAddWeldDialog by remember { mutableStateOf(false) }

    val allWelds by viewModel.allWelds.collectAsStateWithLifecycle()
    val kpis by viewModel.kpis.collectAsStateWithLifecycle()

    // Handle system back gesture
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        currentScreen = AppScreen.DASHBOARD
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
                                .background(ApprovedGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "WeldTrack DB",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Base Qualité Soudage • ${allWelds.size} joints",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { currentScreen = AppScreen.SYNC },
                        modifier = Modifier.testTag("top_bar_sync_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Synchronisation Google",
                            tint = ElectricCyanLight
                        )
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
                NavigationBarItem(
                    selected = currentScreen == AppScreen.DASHBOARD,
                    onClick = { currentScreen = AppScreen.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_dashboard"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndustrialNavy900,
                        selectedTextColor = ElectricCyanLight,
                        indicatorColor = ElectricCyanLight,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.WELDS,
                    onClick = { currentScreen = AppScreen.WELDS },
                    icon = { Icon(Icons.Default.Construction, contentDescription = "Soudures") },
                    label = { Text("Soudures", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_welds"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndustrialNavy900,
                        selectedTextColor = ElectricCyanLight,
                        indicatorColor = ElectricCyanLight,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.SYNC,
                    onClick = { currentScreen = AppScreen.SYNC },
                    icon = { Icon(Icons.Default.Sync, contentDescription = "Google & Excel") },
                    label = { Text("Synchro", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_sync"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndustrialNavy900,
                        selectedTextColor = ElectricCyanLight,
                        indicatorColor = ElectricCyanLight,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.ANALYTICS,
                    onClick = { currentScreen = AppScreen.ANALYTICS },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Analyses") },
                    label = { Text("Rapports", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_analytics"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndustrialNavy900,
                        selectedTextColor = ElectricCyanLight,
                        indicatorColor = ElectricCyanLight,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
            }
        },
        floatingActionButton = {
            if (currentScreen == AppScreen.DASHBOARD || currentScreen == AppScreen.WELDS) {
                ExtendedFloatingActionButton(
                    onClick = { showAddWeldDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nouveau Joint", fontWeight = FontWeight.Bold) },
                    containerColor = ElectricCyan,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_weld_fab")
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
                AppScreen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToWelds = { statusFilter ->
                        viewModel.setStatusFilter(statusFilter)
                        currentScreen = AppScreen.WELDS
                    },
                    onNavigateToLine = { line ->
                        viewModel.setLineFilter(line)
                        currentScreen = AppScreen.WELDS
                    },
                    onNavigateToSync = { currentScreen = AppScreen.SYNC }
                )

                AppScreen.WELDS -> WeldsListScreen(
                    viewModel = viewModel,
                    onSelectWeld = { selectedWeldForDetail = it }
                )

                AppScreen.SYNC -> GoogleSyncScreen(
                    viewModel = viewModel
                )

                AppScreen.ANALYTICS -> AnalyticsScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    // Detail & Edit Dialog
    selectedWeldForDetail?.let { weld ->
        WeldDetailDialog(
            weld = weld,
            onDismiss = { selectedWeldForDetail = null },
            onSave = { updatedWeld ->
                viewModel.updateWeld(updatedWeld)
                selectedWeldForDetail = null
            },
            onDelete = { weldToDelete ->
                viewModel.deleteWeld(weldToDelete)
                selectedWeldForDetail = null
            }
        )
    }

    // Add Weld Dialog
    if (showAddWeldDialog) {
        val nextNumber = (allWelds.size + 1)
        val suggestedNo = String.format(Locale.getDefault(), "W-%03d", nextNumber)
        AddWeldDialog(
            suggestedJointNo = suggestedNo,
            onDismiss = { showAddWeldDialog = false },
            onAdd = { newWeld ->
                viewModel.addWeld(newWeld)
                showAddWeldDialog = false
            }
        )
    }
}
