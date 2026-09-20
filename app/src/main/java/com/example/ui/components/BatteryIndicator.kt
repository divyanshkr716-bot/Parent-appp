package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberPending
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.RoseOffline

@Composable
fun BatteryIndicator(
    batteryLevel: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    if (batteryLevel < 0) return

    val batteryColor = when {
        isCharging -> EmeraldOnline
        batteryLevel > 30 -> MaterialTheme.colorScheme.onSurfaceVariant
        batteryLevel > 15 -> AmberPending
        else -> RoseOffline
    }

    val icon = if (isCharging) {
        Icons.Default.BatteryChargingFull
    } else if (batteryLevel > 80) {
        Icons.Default.BatteryFull
    } else {
        Icons.Default.BatteryStd
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Battery: $batteryLevel%",
            tint = batteryColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "$batteryLevel%",
            color = batteryColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
