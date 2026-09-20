package com.example

import android.content.Context
import com.example.auth.AuthRepository
import com.example.commands.CommandRepository
import com.example.commands.CommandSender
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseRealtimeClient
import com.example.data.remote.SupabaseRestApi
import com.example.device.DeviceManager
import com.example.device.DeviceRepository
import com.example.files.RemoteFileRepository
import com.example.pairing.PairingManager
import com.example.pairing.PairingRepository
import com.example.realtime.RealtimeManager
import com.example.telegram.TelegramMediaRepository
import com.example.webrtc.ParentWebRtcManager
import com.example.webrtc.WebRtcSignalingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ParentAppContainer(context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val supabaseClient = SupabaseClient(context.applicationContext)

    val restApi = SupabaseRestApi(supabaseClient)
    val authRepository = AuthRepository(context.applicationContext, supabaseClient)
    val pairingRepository = PairingRepository(restApi, authRepository)
    val deviceRepository = DeviceRepository(restApi, authRepository)
    val commandRepository = CommandRepository(restApi, authRepository)
    val webRtcSignalingRepository = WebRtcSignalingRepository(restApi, authRepository)

    val realtimeClient = SupabaseRealtimeClient(supabaseClient, applicationScope)
    val realtimeManager = RealtimeManager(realtimeClient, applicationScope)

    val pairingManager = PairingManager(pairingRepository, applicationScope)
    val deviceManager = DeviceManager(pairingRepository, deviceRepository, applicationScope)
    val commandSender = CommandSender(commandRepository, applicationScope)
    val parentWebRtcManager = ParentWebRtcManager(webRtcSignalingRepository, authRepository, applicationScope)
    val fileRepository = RemoteFileRepository(commandSender)
    val telegramRepository = TelegramMediaRepository(restApi, authRepository)
}
