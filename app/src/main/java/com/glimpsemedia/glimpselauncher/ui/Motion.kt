package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** How long a full-screen panel takes to come in and to go, in milliseconds. */
const val OVERLAY_IN_MS = 320
const val OVERLAY_OUT_MS = 240

/** How long a page turn takes when it is not following a finger. */
const val PAGE_TURN_MS = 520

/** The eased movement for page turns not following a finger (Home, dragging to the edge): it starts gently and settles. */
fun <T> pageTurnSpec() = tween<T>(PAGE_TURN_MS, easing = FastOutSlowInEasing)

/**
 * How a swiped page settles after the finger lets go: a spring that carries on at the finger's speed and slows
 * to a stop without bouncing. (A timed glide here started from a standstill, so the page paused before moving.)
 * Softer than the standard snap.
 */
fun <T> pageSwipeSpec() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = PAGE_SWIPE_STIFFNESS)

/** Between Spring.StiffnessLow (200) and StiffnessMediumLow (400, the standard): unhurried but not sluggish. */
const val PAGE_SWIPE_STIFFNESS = 260f

private val overlayEnter = fadeIn(tween(OVERLAY_IN_MS)) + slideInVertically(tween(OVERLAY_IN_MS, easing = FastOutSlowInEasing)) { it / 12 }
private val overlayExit = fadeOut(tween(OVERLAY_OUT_MS)) + slideOutVertically(tween(OVERLAY_OUT_MS, easing = FastOutSlowInEasing)) { it / 12 }

/** A full-screen panel (All apps, a folder, Settings...) that fades and slides in and out instead of appearing at once. */
@Composable
fun Overlay(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, enter = overlayEnter, exit = overlayExit) { content() }
}

/** The same for a panel shown while [value] is set: it keeps the last value while it slides away. */
@Composable
fun <T : Any> OverlayFor(value: T?, content: @Composable (T) -> Unit) {
    var last by remember { mutableStateOf(value) }
    if (value != null) last = value
    AnimatedVisibility(value != null, enter = overlayEnter, exit = overlayExit) { last?.let { content(it) } }
}
