package com.example.ui.screens.device.tabs

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.commands.CommandSender
import com.example.data.model.Command
import com.example.data.model.Device
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.SapphirePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun LocationTab(
    device: Device,
    commandSender: CommandSender,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isRequesting by remember { mutableStateOf(false) }

    // Real Coordinates state (null until received from real child device)
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var accuracyMeters by remember { mutableStateOf<Float?>(null) }
    var provider by remember { mutableStateOf<String?>(null) }
    var lastUpdated by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("No location fix reported yet by child device.") }

    val commands by commandSender.commands.collectAsState()

    // Listen to command responses for real location
    val lastLocationCommand = commands.firstOrNull { it.type == Command.TYPE_REQUEST_LOCATION && it.isCompleted }
    if (lastLocationCommand != null && lastLocationCommand.result != null && latitude == null) {
        try {
            val json = JSONObject(lastLocationCommand.result)
            latitude = json.optDouble("latitude", Double.NaN).takeIf { !it.isNaN() }
            longitude = json.optDouble("longitude", Double.NaN).takeIf { !it.isNaN() }
            accuracyMeters = json.optDouble("accuracy", 0.0).toFloat()
            provider = json.optString("provider", "GPS")
            lastUpdated = lastLocationCommand.completedAt ?: lastLocationCommand.createdAt
            statusText = "Live GPS fix verified from child daemon"
        } catch (_: Exception) {
            // Ignore parse errors
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Map Preview Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (latitude != null && longitude != null) {
                    // Real Coordinate Display
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(EmeraldOnline.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldOnline),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Pin",
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${String.format("%.5f", latitude)}, ${String.format("%.5f", longitude)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        accuracyMeters?.let { acc ->
                            Text(
                                text = "Accuracy: ±${acc.toInt()} meters (${provider ?: "GPS"})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Top Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, tint = EmeraldOnline, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ACTIVE GPS FIX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldOnline)
                        }
                    }
                } else {
                    // Waiting for real location fix
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsNotFixed,
                            contentDescription = "No location fix",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Awaiting Real GPS Fix",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    isRequesting = true
                    statusText = "Dispatching REQUEST_LOCATION to child daemon..."
                    coroutineScope.launch {
                        val result = commandSender.requestLocation(device.id)
                        isRequesting = false
                        result.onSuccess { cmd ->
                            statusText = "Command sent (ID: ${cmd.id.take(8)}). Polling child device for GPS fix..."
                            Toast.makeText(context, "Location request dispatched to child", Toast.LENGTH_SHORT).show()

                            // Poll for result
                            for (i in 0..10) {
                                delay(2000)
                                val history = commandSender.commands.value.firstOrNull { it.id == cmd.id }
                                if (history?.isCompleted == true && history.result != null) {
                                    try {
                                        val json = JSONObject(history.result)
                                        latitude = json.getDouble("latitude")
                                        longitude = json.getDouble("longitude")
                                        accuracyMeters = json.optDouble("accuracy", 0.0).toFloat()
                                        provider = json.optString("provider", "GPS")
                                        lastUpdated = "Just now"
                                        statusText = "Received real GPS fix from child"
                                        break
                                    } catch (_: Exception) {}
                                }
                            }
                        }.onFailure { err ->
                            statusText = "Failed to dispatch: ${err.localizedMessage}"
                            Toast.makeText(context, "Failed: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isRequesting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("request_location_button")
            ) {
                if (isRequesting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Request Location", fontWeight = FontWeight.Bold)
                }
            }

            if (latitude != null && longitude != null) {
                OutlinedButton(
                    onClick = {
                        val lat = latitude ?: return@OutlinedButton
                        val lng = longitude ?: return@OutlinedButton
                        val mapUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(device.childName)})")
                        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                        try {
                            context.startActivity(mapIntent)
                        } catch (_: Exception) {
                            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("open_maps_button")
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open in Maps")
                }
            }
        }

        // Location Info Card
        if (latitude != null && longitude != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Real GPS Telemetry",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LocationDataRow(
                        icon = Icons.Default.Navigation,
                        label = "Latitude / Longitude",
                        value = "$latitude, $longitude"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LocationDataRow(
                        icon = Icons.Default.GpsFixed,
                        label = "Accuracy Radius",
                        value = "±${accuracyMeters?.toInt() ?: 0} m (${provider ?: "GPS lock"})"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LocationDataRow(
                        icon = Icons.Default.Schedule,
                        label = "Last Fix Timestamp",
                        value = lastUpdated ?: "Recent"
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationDataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
