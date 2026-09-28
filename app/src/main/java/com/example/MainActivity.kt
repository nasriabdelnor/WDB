package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.GoogleSyncScreen
import com.example.ui.WdbDuplicatesScreen
import com.example.ui.WeldDetailDialog
import com.example.ui.WeldViewModel
import com.example.ui.WeldViewModelFactory
import com.example.ui.WeldsListScreen
import com.example.ui.theme.ApprovedGreen
import com.example.ui.theme.ApprovedGreenDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.IndustrialNavy800
import com.example.ui.theme.IndustrialNavy900
import com.example.ui.theme.RejectRed
import com.example.ui.theme.RejectRedContainer
import com.example.ui.theme.RejectRedDark
import com.example.ui.theme.WeldTrackTheme
import java.util.Locale

enum class AppScreen(val title: String) {
    WELDS("Recherche & Filtres"),
    DUPLICATES("Doublons WDB"),
    SYNC("Synchro WDB")
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
    var currentScreen by remember { mutableStateOf(AppScreen.WELDS) }
    var selectedWeldForDetail by remember { mutableStateOf<WeldJoint?>(null) }

    val allWelds by viewModel.allWelds.collectAsStateWithLifecycle()
    val duplicateCount by viewModel.duplicateCount.collectAsStateWithLifecycle()

    // Handle system back gesture
    BackHandler(enabled = currentScreen != AppScreen.WELDS) {
        currentScreen = AppScreen.WELDS
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "WeldTrack Light",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF86EFAC).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "v2.1-light",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF86EFAC),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (allWelds.size <= 16) "Recherche & Doublons • 16 joints (Démo d'attente)" else "Recherche & Doublons • ${allWelds.size} joints réels",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                actions = {
                    // Quick Duplicate counter chip in Top Bar
                    if (duplicateCount > 0) {
                        Surface(
                            onClick = { currentScreen = AppScreen.DUPLICATES },
                            shape = RoundedCornerShape(12.dp),
                            color = RejectRedContainer,
                            modifier = Modifier.padding(end = 4.dp).testTag("top_bar_duplicate_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = RejectRedDark, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$duplicateCount doublon(s)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RejectRedDark
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ApprovedGreen.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "✅ 0 doublon",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF86EFAC),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { currentScreen = AppScreen.SYNC },
                        modifier = Modifier.testTag("top_bar_sync_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Synchronisation WDB",
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
                // 1. Recherche & Filtres (Direct Working Screen)
                NavigationBarItem(
                    selected = currentScreen == AppScreen.WELDS,
                    onClick = { currentScreen = AppScreen.WELDS },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Recherche & Filtres") },
                    label = { Text("Recherche & Filtres", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_welds"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndustrialNavy900,
                        selectedTextColor = ElectricCyanLight,
                        indicatorColor = ElectricCyanLight,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                // 2. Doublons WDB (Dedicated Duplicate Detection & Management)
                NavigationBarItem(
                    selected = currentScreen == AppScreen.DUPLICATES,
                    onClick = { currentScreen = AppScreen.DUPLICATES },
                    icon = {
                        if (duplicateCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = RejectRed,
                                        contentColor = Color.White
                                    ) {
                                        Text("$duplicateCount", fontSize = 9.sp)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Doublons WDB")
                            }
                        } else {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Doublons WDB")
                        }
                    },
                    label = { Text("Doublons WDB", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_duplicates"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndustrialNavy900,
                        selectedTextColor = ElectricCyanLight,
                        indicatorColor = ElectricCyanLight,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                // 3. Synchro WDB (Câble USB, OneDrive, IP)
                NavigationBarItem(
                    selected = currentScreen == AppScreen.SYNC,
                    onClick = { currentScreen = AppScreen.SYNC },
                    icon = { Icon(Icons.Default.Sync, contentDescription = "Synchro WDB") },
                    label = { Text("Synchro WDB", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_sync"),
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
                AppScreen.WELDS -> WeldsListScreen(
                    viewModel = viewModel,
                    onSelectWeld = { selectedWeldForDetail = it }
                )

                AppScreen.DUPLICATES -> WdbDuplicatesScreen(
                    viewModel = viewModel,
                    onNavigateToWelds = { statusFilter ->
                        viewModel.setStatusFilter(statusFilter)
                        currentScreen = AppScreen.WELDS
                    },
                    onSelectWeld = { selectedWeldForDetail = it }
                )

                AppScreen.SYNC -> GoogleSyncScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    // Fiche de consultation & exploitation technique WDB (Lecture seule)
    selectedWeldForDetail?.let { weld ->
        WeldDetailDialog(
            weld = weld,
            onDismiss = { selectedWeldForDetail = null }
        )
    }
}
