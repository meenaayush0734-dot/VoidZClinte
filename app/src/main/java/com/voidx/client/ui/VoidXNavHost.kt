package com.voidx.client.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.voidx.client.ui.screens.DownloadsScreen
import com.voidx.client.ui.screens.HomeScreen
import com.voidx.client.ui.screens.PacksScreen
import com.voidx.client.ui.screens.SettingsScreen

private enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Play", Icons.Filled.Home),
    PACKS("packs", "Packs", Icons.Filled.Inventory2),
    DOWNLOADS("downloads", "Downloads", Icons.Filled.Download),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}

@Composable
fun VoidXNavHost(vm: AppViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val message by vm.message.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(m.text, duration = SnackbarDuration.Long)
        if (result == SnackbarResult.Dismissed || result == SnackbarResult.ActionPerformed) {
            vm.consumeMessage(m.id)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = current == dest.route,
                        onClick = {
                            nav.navigate(dest.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Destination.HOME.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.HOME.route) { HomeScreen(vm) }
            composable(Destination.PACKS.route) { PacksScreen(vm) }
            composable(Destination.DOWNLOADS.route) { DownloadsScreen(vm) }
            composable(Destination.SETTINGS.route) { SettingsScreen(vm) }
        }
    }
}
