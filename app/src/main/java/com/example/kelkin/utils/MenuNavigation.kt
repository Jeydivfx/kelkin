package com.example.kelkin.utils

import androidx.navigation.NavController
import androidx.navigation.navOptions

/** A menu selection opens the section root, even when its saved stack ends in details. */
fun NavController.openMenuDestination(destinationId: Int) {
    if (currentDestination?.id == destinationId) return
    if (popBackStack(destinationId, false)) return
    navigate(destinationId, null, navOptions {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    })
    // restoreState can restore a section together with its last detail screen.
    if (currentDestination?.id != destinationId) popBackStack(destinationId, false)
}
