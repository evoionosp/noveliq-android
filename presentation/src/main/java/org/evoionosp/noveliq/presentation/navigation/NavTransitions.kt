package org.evoionosp.noveliq.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

private fun routeBase(route: String?): String? {
    return route?.substringBefore('/')
}

private fun rootIndex(route: String?): Int? {
    return when (routeBase(route)) {
        AppRoute.Home.route -> 0
        AppRoute.Library.route -> 1
        AppRoute.Authors.route -> 2
        else -> null
    }
}

private fun isAudiobookDetailRoute(route: String?): Boolean {
    return routeBase(route) == AppRoute.AudiobookDetail.route.substringBefore('/')
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.rootEnterTransition(): EnterTransition {
    if (isAudiobookDetailRoute(initialState.destination.route) || isAudiobookDetailRoute(targetState.destination.route)) {
        return EnterTransition.None
    }
    val initialIndex = rootIndex(initialState.destination.route)
    val targetIndex = rootIndex(targetState.destination.route)
    val forward = initialIndex != null && targetIndex != null && targetIndex > initialIndex
    return slideInHorizontally(
        animationSpec = tween(durationMillis = 300),
        initialOffsetX = { fullWidth -> if (forward) fullWidth else -fullWidth }
    )
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.rootExitTransition(): ExitTransition {
    if (isAudiobookDetailRoute(initialState.destination.route) || isAudiobookDetailRoute(targetState.destination.route)) {
        return ExitTransition.None
    }
    val initialIndex = rootIndex(initialState.destination.route)
    val targetIndex = rootIndex(targetState.destination.route)
    val forward = initialIndex != null && targetIndex != null && targetIndex > initialIndex
    return slideOutHorizontally(
        animationSpec = tween(durationMillis = 300),
        targetOffsetX = { fullWidth -> if (forward) -fullWidth else fullWidth }
    )
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardEnterTransition(): EnterTransition {
    return slideInHorizontally(
        animationSpec = tween(durationMillis = 300),
        initialOffsetX = { fullWidth -> fullWidth }
    )
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardExitTransition(): ExitTransition {
    return slideOutHorizontally(
        animationSpec = tween(durationMillis = 300),
        targetOffsetX = { fullWidth -> -fullWidth }
    )
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.backEnterTransition(): EnterTransition {
    return slideInHorizontally(
        animationSpec = tween(durationMillis = 300),
        initialOffsetX = { fullWidth -> -fullWidth }
    )
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.backExitTransition(): ExitTransition {
    return slideOutHorizontally(
        animationSpec = tween(durationMillis = 300),
        targetOffsetX = { fullWidth -> fullWidth }
    )
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.modalEnterTransition(): EnterTransition {
    return slideInVertically(
        animationSpec = tween(durationMillis = 320),
        initialOffsetY = { fullHeight -> fullHeight }
    )
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.modalExitTransition(): ExitTransition {
    return slideOutVertically(
        animationSpec = tween(durationMillis = 320),
        targetOffsetY = { fullHeight -> fullHeight }
    )
}

internal fun modalPopEnterTransition(): EnterTransition {
    return EnterTransition.None
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.modalPopExitTransition(): ExitTransition {
    return slideOutVertically(
        animationSpec = tween(durationMillis = 320),
        targetOffsetY = { fullHeight -> fullHeight }
    )
}
