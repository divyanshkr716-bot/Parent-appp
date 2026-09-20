package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Command
import com.example.data.model.Device
import com.example.data.model.Pairing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Game Centre Parent", appName)
    }

    @Test
    fun `test pairing model validation`() {
        val pairing = Pairing(
            id = "p-1",
            parentId = "parent-123",
            pairingCode = "12345678",
            childDeviceId = "device_xyz_123",
            status = "waiting"
        )
        assertEquals("12345678", pairing.pairingCode)
        assertEquals("device_xyz_123", pairing.childDeviceId)
        assertTrue(pairing.isWaiting)
    }

    @Test
    fun `test device model properties`() {
        val device = Device(
            id = "test-device-1",
            childName = "Alex's Tablet",
            model = "Pixel Tablet",
            osVersion = "Android 14",
            batteryLevel = 85,
            isCharging = true,
            status = "online"
        )
        assertTrue(device.isOnline)
        assertEquals("Alex's Tablet", device.childName)
        assertEquals(85, device.batteryLevel)
    }

    @Test
    fun `test command model state`() {
        val cmd = Command(
            id = "cmd-001",
            parentId = "parent-123",
            childId = "test-device-1",
            type = Command.TYPE_START_SCREEN_SHARE,
            payload = mapOf("session_id" to "sess-123"),
            status = "pending"
        )
        assertTrue(cmd.isPending)
        assertEquals(Command.TYPE_START_SCREEN_SHARE, cmd.type)
    }
}
