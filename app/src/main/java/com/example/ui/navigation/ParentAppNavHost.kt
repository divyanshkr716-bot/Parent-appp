package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ParentAppContainer
import com.example.auth.AuthState
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.ParentProfileScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.device.ChildDeviceScreen
import com.example.ui.screens.settings.SettingsScreen

@Composable
fun ParentAppNavHost(
    navController: NavHostController,
    container: ParentAppContainer,
    modifier: Modifier = Modifier
) {
    val authState by container.authRepository.authState.collectAsState()
    val startDestination = if (authState is AuthState.Authenticated) {
        NavRoutes.DASHBOARD
    } else {
        NavRoutes.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                authRepository = container.authRepository,
                onNavigateToRegister = { navController.navigate(NavRoutes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(NavRoutes.FORGOT_PASSWORD) },
                onLoginSuccess = {
                    navController.navigate(NavRoutes.DASHBOARD) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.REGISTER) {
            RegisterScreen(
                authRepository = container.authRepository,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(NavRoutes.DASHBOARD) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                authRepository = container.authRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(
                authRepository = container.authRepository,
                deviceManager = container.deviceManager,
                pairingManager = container.pairingManager,
                realtimeManager = container.realtimeManager,
                onNavigateToDevice = { deviceId, initialTab ->
                    navController.navigate(NavRoutes.deviceRoute(deviceId, initialTab))
                },
                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                onNavigateToProfile = { navController.navigate(NavRoutes.PROFILE) }
            )
        }

        composable(
            route = NavRoutes.DEVICE,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType },
                navArgument("initialTab") {
                    type = NavType.StringType
                    defaultValue = "overview"
                }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            val initialTab = backStackEntry.arguments?.getString("initialTab") ?: "overview"

            ChildDeviceScreen(
                deviceId = deviceId,
                initialTab = initialTab,
                deviceManager = container.deviceManager,
                deviceRepository = container.deviceRepository,
                commandSender = container.commandSender,
                webRtcManager = container.parentWebRtcManager,
                fileRepository = container.fileRepository,
                telegramRepository = container.telegramRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                supabaseClient = container.supabaseClient,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.PROFILE) {
            ParentProfileScreen(
                authRepository = container.authRepository,
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
