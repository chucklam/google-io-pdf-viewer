/**
 * MainActivity.kt - Prime Entrance for PDF Web Browser Application.
 *
 * This file implements the main Activity and Jetpack Compose scaffold for the PDF Web Browser app.
 * Use cases:
 * - Boot the application framework under full edge-to-edge support.
 * - Manage active tab selection (Built-in Browser vs. Offline Saved Library).
 * - React to global notification broadcasts (Toast indicators) asynchronously.
 * - Override hardware back button triggers to exit full-screen offline document sessions.
 * - Handle active PDF reader modal interfaces over active view layers.
 */
package com.example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BrowserView
import com.example.ui.components.OfflineLibraryView
import com.example.ui.components.OfflinePdfViewer
import com.example.ui.components.OfflineMarkdownViewer
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BrowserViewModel

/**
 * Entry Activity configuring screen metrics and setting the core compose layout.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppConfig.logCall(AppConfig.LOG_TAG_UI, "MainActivity:onCreate")

        // Enable standard edge-to-edge immersive views
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppScaffold()
            }
        }
    }
}

/**
 * Core layout of the PDF Web Browser app coordinating Views, Overlays, and shared dialog notifications.
 */
@Composable
fun MainAppScaffold(
    viewModel: BrowserViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val activeOfflinePdf by viewModel.activeOfflinePdf.collectAsState()
    val activeOfflineMarkdown by viewModel.activeOfflineMarkdown.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val context = LocalContext.current

    // Observe global Toast messages launched from backend routines reactively
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            if (message.isNotBlank()) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                Log.d(AppConfig.LOG_TAG_UI, "Dispatched Toast Notification: $message")
            }
        }
    }

    // Intercept hardware Back events to safely collapse active viewer overlays first
    BackHandler(enabled = activeOfflinePdf != null || activeOfflineMarkdown != null) {
        if (activeOfflinePdf != null) {
            viewModel.setActiveOfflineDocForReading(null)
        } else if (activeOfflineMarkdown != null) {
            viewModel.setActiveOfflineMarkdownForReading(null)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // MAIN MULTI-TAB SWITCHING INTERFACE
            Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFDF8F6))) {
                
                // TAB SELECTION BAR (custom Sleek Interface look/feel)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF7F2F9),
                    contentColor = Color(0xFF1D1B1E),
                    indicator = { tabPositions ->
                        // Hide standard indicator line to let the custom selected pills draw focus cleanly
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(0.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("main_tabs")
                ) {
                    // Browser Tab selector
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selectedTab == 0) Color(0xFFE8DEF8) else Color.Transparent)
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) Color(0xFF1D192B) else Color(0xFF49454F).copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Browser",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 0) Color(0xFF1D192B) else Color(0xFF49454F)
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_browser")
                    )

                    // Offline Documents Tab selector
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selectedTab == 1) Color(0xFFE8DEF8) else Color.Transparent)
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = if (selectedTab == 1) Color(0xFF1D192B) else Color(0xFF49454F).copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Offline Library",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 1) Color(0xFF1D192B) else Color(0xFF49454F)
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_offline_library")
                    )
                }

                // SCREEN PORTAL CONTENT
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (selectedTab == 0) {
                        BrowserView(viewModel = viewModel)
                    } else {
                        OfflineLibraryView(viewModel = viewModel)
                    }
                }
            }

            // OVERLAY: OFFLINE PDF VIEWER PANEL
            AnimatedVisibility(
                visible = activeOfflinePdf != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                activeOfflinePdf?.let { doc ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black) // Dark cinematic overlay blocker
                    ) {
                        OfflinePdfViewer(
                            doc = doc,
                            onBack = { viewModel.setActiveOfflineDocForReading(null) }
                        )
                    }
                }
            }

            // OVERLAY: OFFLINE MARKDOWN VIEWER PANEL
            AnimatedVisibility(
                visible = activeOfflineMarkdown != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                activeOfflineMarkdown?.let { doc ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background) // Eye-safe background
                    ) {
                        OfflineMarkdownViewer(
                            doc = doc,
                            onBack = { viewModel.setActiveOfflineMarkdownForReading(null) }
                        )
                    }
                }
            }

            // TRANSITIONAL DOWNLOAD LOADER OVERLAY INDICATOR
            AnimatedVisibility(
                visible = isDownloading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)), // Dim overall viewport
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Downloading PDF...",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
