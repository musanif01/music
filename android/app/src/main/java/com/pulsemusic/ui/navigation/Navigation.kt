package com.pulsemusic.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Search : Screen("search", "Search", Icons.Filled.Search, Icons.Outlined.Search)
    data object Library : Screen("library", "Your Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
    data object Offline : Screen("offline", "Offline", Icons.Filled.PhoneAndroid, Icons.Outlined.PhoneAndroid)
    data object Queue : Screen("queue", "Queue", Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)

    companion object {
        val bottomNavItems = listOf(Home, Search, Library, Offline)
    }
}
