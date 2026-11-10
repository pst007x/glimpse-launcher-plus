package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

const val UNLOCK_HOLD_MS = 3000

/** How long the padlock is held to unlock: 3 seconds, or 1 with Quick unlock. */
fun unlockHoldMs(quick: Boolean): Int = if (quick) 1000 else UNLOCK_HOLD_MS

/**
 * The small padlock. Locked: hold it for three seconds (a ring fills) to unlock, then the PIN if one is set, so a stray touch never does.
 * Unlocked: a tap locks again.
 */
@Composable
fun Padlock(unlocked: Boolean, onUnlock: () -> Unit, onLock: () -> Unit, onHolding: (Boolean) -> Unit, modifier: Modifier = Modifier, holdMs: Int = UNLOCK_HOLD_MS) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(LocalMenu.current.surface)
            .semantics {
                contentDescription = if (unlocked) "Unlocked. Tap to lock." else if (holdMs <= 1000) "Locked. Hold for one second to unlock." else "Locked. Hold for three seconds to unlock."
            }
            .pointerInput(unlocked, holdMs) {
                detectTapGestures(
                    onPress = {
                        if (unlocked) {
                            // A tap locks: handled in onTap below.
                            tryAwaitRelease()
                        } else {
                            onHolding(true)
                            val job = scope.launch {
                                progress.snapTo(0f)
                                progress.animateTo(1f, tween(holdMs, easing = LinearEasing))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onHolding(false)  // this gesture is cancelled once unlocked, so say so here
                                onUnlock()
                            }
                            tryAwaitRelease()
                            job.cancel()
                            onHolding(false)
                            scope.launch { progress.snapTo(0f) }
                        }
                    },
                    onTap = { if (unlocked) onLock() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (unlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
            contentDescription = null,
            tint = if (unlocked) LocalAccent.current else LocalMenu.current.text,
            modifier = Modifier.size(28.dp),
        )
        if (!unlocked && progress.value > 0f) {
            CircularProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.size(52.dp),
                color = LocalAccent.current,
                strokeWidth = 4.dp,
                trackColor = Color.Transparent,
            )
        }
    }
}
