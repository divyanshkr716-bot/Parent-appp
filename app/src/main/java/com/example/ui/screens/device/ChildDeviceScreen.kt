package com.example.ui.screens.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.commands.CommandSender
import com.example.data.model.Device
import com.example.device.DeviceManager
import com.example.device.DeviceRepository
import com.example.files.RemoteFileRepository
import com.example.telegram.TelegramMediaRepository
import com.example.ui.components.ConnectionBadge
import com.example.ui.screens.device.tabs.ActivityTelegramTab
import com.example.ui.screens.device.tabs.AudioTab
import com.example.ui.screens.device.tabs.CameraTab
import com.example.ui.screens.device.tabs.CommandsTab
import com.example.ui.screens.device.tabs.FilesTab
import com.example.ui.screens.device.tabs.LocationTab
import com.example.ui.screens.device.tabs.OverviewTab
import com.example.ui.screens.device.tabs.ScreenShareTab
import com.example.webrtc.ParentWebRtcManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDeviceScreen(
    deviceId: String,
    initialTab: String,
    deviceManager: DeviceManager,
    deviceRepository: DeviceRepository,
    commandSender: CommandSender,
    webRtcManager: ParentWebRtcManager,
    fileRepository: RemoteFileRepository,
    telegramRepository: TelegramMediaRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboardState by deviceManager.dashboardState.collectAsState()
    val device = dashboardState.devices.find { it.id == deviceId }
        ?: Device(id = deviceId, childName = "Child Device")

    val tabs = listOf(
        "Overview" to Icons.Default.Info,
        "Screen" to Icons.Default.ScreenShare,
        "Camera" to Icons.Default.CameraAlt,
        "Audio" to Icons.Default.GraphicEq,
        "Location" to Icons.Default.LocationOn,
        "Files" to Icons.Default.Folder,
        "Activity" to Icons.Default.Notifications,
        "Commands" to Icons.Default.ListAlt
    )

    val initialIndex = when (initialTab.lowercase()) {
        "screen" -> 1
        "camera" -> 2
        "audio" -> 3
        "location" -> 4
        "files" -> 5
        "activity" -> 6
        "commands" -> 7
        else -> 0
    }

    var selectedTabIndex by remember { mutableIntStateOf(initialIndex) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                text = device.childName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = device.model,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        ConnectionBadge(status = device.status)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("child_screen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Scrollable Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, (label, icon) ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(label, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        modifier = Modifier.testTag("tab_$label")
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> OverviewTab(
                        device = device,
                        commandSender = commandSender,
                        deviceManager = deviceManager,
                        onUnpaired = onNavigateBack
                    )
                    1 -> ScreenShareTab(
                        device = device,
                        webRtcManager = webRtcManager,
                        commandSender = commandSender
                    )
                    2 -> CameraTab(
                        device = device,
                        webRtcManager = webRtcManager,
                        commandSender = commandSender
                    )
                    3 -> AudioTab(
                        device = device,
                        webRtcManager = webRtcManager,
                        commandSender = commandSender
                    )
                    4 -> LocationTab(
                        device = device,
                        commandSender = commandSender
                    )
                    5 -> FilesTab(
                        device = device,
                        fileRepository = fileRepository
                    )
                    6 -> ActivityTelegramTab(
                        device = device,
                        deviceRepository = deviceRepository,
                        telegramRepository = telegramRepository
                    )
                    7 -> CommandsTab(
                        device = device,
                        commandSender = commandSender
                    )
                }
            }
        }
    }
}
