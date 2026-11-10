package com.glimpsemedia.glimpselauncher.ui

import android.app.KeyguardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glimpsemedia.glimpselauncher.data.Pin
import com.glimpsemedia.glimpselauncher.data.PinTries
import kotlinx.coroutines.delay

/**
 * Big number buttons for the padlock PIN. Dots show how many digits are in, never the digits.
 * [onPin] says whether the PIN is accepted; a refused one counts towards the five-try block.
 */
@Composable
fun PinPad(title: String, tries: PinTries, onPin: (String) -> Boolean, onForgotten: (() -> Unit)?, onCancel: () -> Unit) {
    var entry by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var blockedMs by remember { mutableLongStateOf(tries.blockedForMs()) }
    LaunchedEffect(Unit) {
        while (true) { blockedMs = tries.blockedForMs(); delay(250) }
    }
    val blocked = blockedMs > 0
    MenuDialog(title = title, onClose = onCancel, dismissible = false) {
        val note = when {
            blocked -> "Try again in ${(blockedMs + 999) / 1000} seconds"
            wrong -> "That PIN is not right"
            else -> "4 to 8 digits"
        }
        Text(note, color = if (blocked || wrong) LocalAccent.current else LocalMenu.current.dimText, fontSize = 18.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Row(
            Modifier.fillMaxWidth().height(28.dp).semantics { contentDescription = "${entry.length} digits entered" },
            horizontalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.CenterHorizontally),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            repeat(entry.length) { Box(Modifier.size(18.dp).clip(CircleShape).background(LocalMenu.current.text)) }
        }
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("Delete", "0", "OK"))
        for (row in rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (key in row) {
                    val enabled = !blocked && when (key) {
                        "Delete" -> entry.isNotEmpty()
                        "OK" -> Pin.isValid(entry)
                        else -> entry.length < 8
                    }
                    BigButton(
                        key,
                        {
                            if (!enabled) return@BigButton
                            when (key) {
                                "Delete" -> entry = entry.dropLast(1)
                                "OK" -> {
                                    if (onPin(entry)) { tries.succeeded(); wrong = false } else { tries.failed(); wrong = true }
                                    entry = ""
                                    blockedMs = tries.blockedForMs()
                                }
                                else -> { entry += key; wrong = false }
                            }
                        },
                        Modifier.weight(1f).alpha(if (enabled) 1f else 0.4f),
                        primary = key == "OK",
                        fontSize = if (key.length > 1) 18.sp else 26.sp,
                    )
                }
            }
        }
        if (onForgotten != null) BigButton("Forgotten PIN?", onForgotten)
        BigButton("Cancel", onCancel)
    }
}

/** Where the PIN screens are up to. */
sealed interface PinStep {
    /** The padlock was held and there is a PIN. */
    data object Unlock : PinStep
    /** Entering a new PIN the first time; [note] replaces the title after a mismatch. */
    data class NewFirst(val unlockAfter: Boolean, val note: String? = null) : PinStep
    data class NewSecond(val first: String, val unlockAfter: Boolean) : PinStep
    /** The current PIN, before changing or removing it. */
    data class Current(val thenRemove: Boolean) : PinStep
    /** Forgotten PIN on a phone with no screen lock to fall back on. */
    data object NoPhoneLock : PinStep
    /** The padlock was held on the locked notes screen. */
    data object UnlockNotes : PinStep
    /** The PIN was right on the notes screen: unlock the screens, or only the notes. */
    data object UnlockChoice : PinStep
}

/**
 * Runs the PIN screens: unlocking, setting, changing and removing it, and the forgotten-PIN route through
 * the phone's own lock. [onSave] receives the new hash and salt (base64), or nulls to remove the PIN.
 */
@Composable
fun PinSteps(
    step: PinStep?,
    onStep: (PinStep?) -> Unit,
    pinHash: String?,
    pinSalt: String?,
    tries: PinTries,
    onSave: (hash: String?, salt: String?) -> Unit,
    onUnlocked: () -> Unit,
    onConfirmPhoneLock: (onOk: () -> Unit) -> Unit,
    toast: (String) -> Unit,
    onNotesUnlocked: () -> Unit = {},
) {
    val context = LocalContext.current
    val freeTries = remember { PinTries { 0L } }  // entering a new PIN never blocks
    fun matches(pin: String) = pinHash != null && pinSalt != null && Pin.matches(pin, pinHash, pinSalt)
    when (step) {
        null -> {}
        PinStep.Unlock -> PinPad(
            title = "Enter your PIN",
            tries = tries,
            onPin = { pin -> matches(pin).also { if (it) { onStep(null); onUnlocked() } } },
            onForgotten = {
                val secure = context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
                if (secure) onConfirmPhoneLock { onStep(PinStep.NewFirst(unlockAfter = true)) } else onStep(PinStep.NoPhoneLock)
            },
            onCancel = { onStep(null) },
        )
        is PinStep.NewFirst -> key(step) {
            PinPad(
                title = step.note ?: "Enter the new PIN",
                tries = freeTries,
                onPin = { pin -> onStep(PinStep.NewSecond(pin, step.unlockAfter)); true },
                onForgotten = null,
                onCancel = { onStep(null) },
            )
        }
        is PinStep.NewSecond -> key(step) {
            PinPad(
                title = "Enter the new PIN again",
                tries = freeTries,
                onPin = { pin ->
                    if (pin == step.first) {
                        val salt = Pin.newSalt()
                        onSave(Pin.hash(pin, salt), java.util.Base64.getEncoder().encodeToString(salt))
                        onStep(null)
                        toast("PIN saved")
                        if (step.unlockAfter) onUnlocked()
                    } else {
                        onStep(PinStep.NewFirst(step.unlockAfter, "The two PINs are different. Enter the new PIN"))
                    }
                    true
                },
                onForgotten = null,
                onCancel = { onStep(null) },
            )
        }
        is PinStep.Current -> PinPad(
            title = "Enter the current PIN",
            tries = tries,
            onPin = { pin ->
                matches(pin).also {
                    if (it && step.thenRemove) { onSave(null, null); onStep(null); toast("PIN removed") }
                    else if (it) onStep(PinStep.NewFirst(unlockAfter = false))
                }
            },
            onForgotten = null,
            onCancel = { onStep(null) },
        )
        PinStep.UnlockNotes -> PinPad(
            title = "Enter your PIN",
            tries = tries,
            onPin = { pin -> matches(pin).also { if (it) onStep(PinStep.UnlockChoice) } },
            onForgotten = {
                val secure = context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
                if (secure) onConfirmPhoneLock { onStep(PinStep.NewFirst(unlockAfter = true)) } else onStep(PinStep.NoPhoneLock)
            },
            onCancel = { onStep(null) },
        )
        PinStep.UnlockChoice -> MenuDialog(title = "Unlock", onClose = { onStep(null) }) {
            Text(
                "Unlock notes opens only the notes. Edit mode unlocks all editing, and the notes.",
                color = LocalMenu.current.dimText, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            BigButton("Unlock notes", { onStep(null); onNotesUnlocked() })
            BigButton("Edit mode", { onStep(null); onUnlocked() }, primary = true)
        }
        PinStep.NoPhoneLock -> MenuDialog(title = "Forgotten PIN", onClose = { onStep(null) }) {
            Text(
                "This phone has no screen lock, so the PIN can't be reset here. To start again, clear Glimpse Launcher's storage in the phone's Settings > Apps.",
                color = LocalMenu.current.text, fontSize = 20.sp,
            )
        }
    }
}

/** What is left of the PIN screens when the padlock locks: only the unlock pad and the no-screen-lock note survive. */
fun pinStepAfterLock(step: PinStep?): PinStep? = when (step) {
    PinStep.Unlock, PinStep.NoPhoneLock, PinStep.UnlockNotes -> step
    else -> null
}

/** Finishing the welcome unlocks only if there is no PIN, or the padlock was already open. */
fun welcomeMayUnlock(pinHash: String?, unlocked: Boolean): Boolean = pinHash == null || unlocked

/** The notes show unless "Notes need the PIN" is on and neither the screens nor the notes are unlocked. */
fun notesOpen(needPin: Boolean, unlocked: Boolean, notesUnlocked: Boolean): Boolean = !needPin || unlocked || notesUnlocked

/**
 * What holding the padlock asks for: null unlocks at once (no PIN). With "Notes need the PIN" and the notes still
 * locked, the PIN leads to the choice between unlocking the notes and edit mode, from any screen.
 */
fun pinStepForPadlock(hasPin: Boolean, notesNeedPin: Boolean, notesUnlocked: Boolean): PinStep? = when {
    !hasPin -> null
    notesNeedPin && !notesUnlocked -> PinStep.UnlockNotes
    else -> PinStep.Unlock
}
