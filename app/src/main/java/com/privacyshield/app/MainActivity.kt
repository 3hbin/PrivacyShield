package com.privacyshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.privacyshield.app.ui.screens.AccessHistoryScreen
import com.privacyshield.app.ui.screens.IndicatorSettingsScreen
import com.privacyshield.app.ui.screens.AssistantScreen
import com.privacyshield.app.ui.screens.EmergencyScreen
import com.privacyshield.app.ui.screens.LogScreen
import com.privacyshield.app.ui.screens.ProtectionScreen
import com.privacyshield.app.ui.screens.UpdateScreen
import com.privacyshield.app.ui.screens.VaultScreen
import com.privacyshield.app.ui.screens.AboutScreen
import com.privacyshield.app.ui.screens.AppLockScreen
import com.privacyshield.app.ui.screens.DashboardScreen
import com.privacyshield.app.ui.screens.MoreScreen
import com.privacyshield.app.ui.screens.PinSetupScreen
import com.privacyshield.app.ui.screens.PowerScreen
import com.privacyshield.app.ui.screens.PrivacyScreen
import com.privacyshield.app.ui.screens.SettingsScreen
import com.privacyshield.app.ui.screens.SuspiciousScreen
import com.privacyshield.app.ui.theme.PrivacyShieldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ShieldViewModel = viewModel()
            val settings by viewModel.preferences.settings.collectAsState()
            LaunchedEffect(Unit) { viewModel.refresh() }
            PrivacyShieldTheme(settings.themeMode, settings.seedColor) {
                AppNav(viewModel)
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("dashboard", "Tổng quan", Icons.Filled.Home),
    Tab("privacy", "Riêng tư", Icons.Filled.Visibility),
    Tab("applock", "Khóa", Icons.Filled.Lock),
    Tab("more", "Thêm", Icons.Filled.Menu)
)

@Composable
private fun AppNav(viewModel: ShieldViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = current in tabs.map { it.route }
    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onOpenPrivacy = { navController.navigate("privacy") },
                    onOpenLock = { navController.navigate("applock") }
                )
            }
            composable("privacy") {
                PrivacyScreen(viewModel) { navController.popBackStack() }
            }
            composable("applock") {
                AppLockScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSetupPin = { navController.navigate("pin/setup") }
                )
            }
            composable("more") {
                MoreScreen(
                    onSuspicious = { navController.navigate("suspicious") },
                    onPower = { navController.navigate("power") },
                    onSettings = { navController.navigate("settings") },
                    onAbout = { navController.navigate("about") },
                    onProtection = { navController.navigate("protection") },
                    onLog = { navController.navigate("log") },
                    onVault = { navController.navigate("vault") },
                    onEmergency = { navController.navigate("emergency") },
                    onAssistant = { navController.navigate("assistant") },
                    onUpdate = { navController.navigate("update") },
                    onIndicators = { navController.navigate("indicators") },
                    onAccessHistory = { navController.navigate("access_history") }
                )
            }
            composable("suspicious") { SuspiciousScreen(viewModel) { navController.popBackStack() } }
            composable("power") { PowerScreen { navController.popBackStack() } }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onChangePin = {
                        val route = if (viewModel.preferences.settings.value.hasPin) "pin/change" else "pin/setup"
                        navController.navigate(route)
                    }
                )
            }
            composable("about") { AboutScreen { navController.popBackStack() } }
            composable("protection") { ProtectionScreen(viewModel) { navController.popBackStack() } }
            composable("log") { LogScreen { navController.popBackStack() } }
            composable("vault") { VaultScreen(viewModel) { navController.popBackStack() } }
            composable("emergency") { EmergencyScreen(viewModel) { navController.popBackStack() } }
            composable("assistant") { AssistantScreen(viewModel) { navController.popBackStack() } }
            composable("update") { UpdateScreen(viewModel) { navController.popBackStack() } }
            composable("access_history") { AccessHistoryScreen { navController.popBackStack() } }
            composable("indicators") { IndicatorSettingsScreen(viewModel) { navController.popBackStack() } }
            composable("pin/setup") {
                PinSetupScreen(viewModel, onBack = { navController.popBackStack() }, changing = false)
            }
            composable("pin/change") {
                PinSetupScreen(viewModel, onBack = { navController.popBackStack() }, changing = true)
            }
        }
    }
}
