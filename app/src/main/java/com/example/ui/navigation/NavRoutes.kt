package com.example.ui.navigation

object NavRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val DEVICE = "device/{deviceId}/{initialTab}"

    fun deviceRoute(deviceId: String, initialTab: String = "overview"): String {
        return "device/$deviceId/$initialTab"
    }
}
