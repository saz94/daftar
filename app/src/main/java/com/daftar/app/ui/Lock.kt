package com.daftar.app.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.daftar.app.R
import kotlinx.coroutines.delay

private const val MIN_PIN = 4
private const val MAX_PIN = 8

// ------------------------------------------------------------------ fingerprint / face

/** Fingerprint and face unlock both count; which one appears is up to the phone. */
private const val BIOMETRICS = BiometricManager.Authenticators.BIOMETRIC_WEAK

fun canUseBiometric(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(BIOMETRICS) == BiometricManager.BIOMETRIC_SUCCESS

private fun Context.findActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}

/** Shows the phone's own fingerprint / face sheet. Nothing about the fingerprint reaches the app. */
fun askBiometric(context: Context, title: String, usePinLabel: String, onSuccess: () -> Unit) {
    val activity = context.findActivity() ?: return
    try {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setNegativeButtonText(usePinLabel)
                .setAllowedAuthenticators(BIOMETRICS)
                .build(),
        )
    } catch (e: Exception) {
        // the phone refused to show the sheet; the PIN still works
    }
}

// ------------------------------------------------------------------ keypad

@Composable
private fun PinDots(entered: Int, total: Int, error: Boolean) {
    val on = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until total) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (i < entered) on else Color.Transparent)
                    .border(1.5.dp, if (i < entered) on else MaterialTheme.colorScheme.outline, CircleShape),
            )
        }
    }
}

@Composable
private fun Key(label: String? = null, icon: ImageVector? = null, description: String? = null, quiet: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (quiet) Color.Transparent else MaterialTheme.colorScheme.surface,
        modifier = Modifier.size(76.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (label != null) {
                Text(label, style = MaterialTheme.typography.headlineMedium.tabular())
            } else if (icon != null) {
                Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
            }
        }
    }
}

/** A phone-style number pad. Laid out left to right in every language, like a phone's own keypad. */
@Composable
private fun PinPad(onDigit: (Char) -> Unit, onDelete: () -> Unit, onBiometric: (() -> Unit)?) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            for (row in listOf("123", "456", "789")) {
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    for (digit in row) Key(label = digit.toString()) { onDigit(digit) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                if (onBiometric != null) {
                    Key(icon = Icons.Outlined.Fingerprint, description = stringResource(R.string.lock_use_biometric), quiet = true, onClick = onBiometric)
                } else {
                    Spacer(Modifier.size(76.dp))
                }
                Key(label = "0") { onDigit('0') }
                Key(icon = Icons.AutoMirrored.Outlined.Backspace, description = stringResource(R.string.lock_delete_digit), quiet = true, onClick = onDelete)
            }
        }
    }
}

// ------------------------------------------------------------------ lock screen

/** Covers the whole app until the PIN, a fingerprint or a face is accepted. */
@Composable
fun LockScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val preview = vm.lockPreview && !vm.locked
    val length = if (preview) 4 else vm.prefs.pinLength.coerceIn(MIN_PIN, MAX_PIN)
    val biometric = !preview && vm.prefs.biometricUnlock && canUseBiometric(context)
    var entered by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val biometricTitle = stringResource(R.string.lock_biometric_title)
    val usePin = stringResource(R.string.lock_use_pin)
    val showBiometric = { askBiometric(context, biometricTitle, usePin) { vm.unlockWithBiometric() } }

    // put the keyboard away: whatever was being typed underneath must not keep receiving keys
    val focus = LocalFocusManager.current
    // offer the fingerprint or face straight away, so most unlocks need no typing
    LaunchedEffect(Unit) {
        focus.clearFocus(force = true)
        if (biometric) showBiometric()
    }
    // while tries are paused, tick so the countdown text updates
    LaunchedEffect(vm.lockedOutUntil) {
        while (System.currentTimeMillis() < vm.lockedOutUntil) {
            now = System.currentTimeMillis()
            delay(500)
        }
        now = System.currentTimeMillis()
    }
    // "back" leaves the app instead of slipping past the lock
    BackHandler { context.findActivity()?.moveTaskToBack(true) }

    val waitSeconds = ((vm.lockedOutUntil - now + 999) / 1000).coerceAtLeast(0)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.lock_enter_pin), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    waitSeconds > 0 -> stringResource(R.string.lock_wait, waitSeconds.toString())
                    wrong -> stringResource(R.string.lock_wrong)
                    else -> stringResource(R.string.lock_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (wrong || waitSeconds > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            PinDots(entered.length, length, wrong)
            Spacer(Modifier.weight(1f))
            PinPad(
                onDigit = { digit ->
                    if (waitSeconds == 0L && entered.length < length) {
                        wrong = false
                        entered += digit
                        if (entered.length == length) {
                            val attempt = entered
                            if (preview) {
                                entered = ""
                            } else {
                                vm.tryPin(attempt) {
                                    wrong = true
                                    entered = ""
                                }
                            }
                        }
                    }
                },
                onDelete = { entered = entered.dropLast(1) },
                onBiometric = if (biometric) showBiometric else null,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ------------------------------------------------------------------ choosing a PIN

/** Two steps: type a new PIN, then type it again. */
@Composable
fun PinSetupScreen(vm: AppViewModel, go: Go) {
    var first by rememberSaveable { mutableStateOf("") }
    var confirming by rememberSaveable { mutableStateOf(false) }
    var entered by rememberSaveable { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    AppScreen(
        title = stringResource(if (vm.lockEnabled) R.string.lock_change_pin else R.string.lock_set_pin),
        onBack = { go.back() },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(if (confirming) R.string.lock_confirm_title else R.string.lock_choose_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(if (mismatch) R.string.lock_mismatch else if (confirming) R.string.lock_confirm_hint else R.string.lock_choose_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = if (mismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            PinDots(entered.length, if (confirming) first.length else maxOf(entered.length, MIN_PIN), mismatch)
            Spacer(Modifier.height(8.dp))
            // a PIN can be 4 to 8 digits, so the first step needs an explicit "next"
            Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                if (!confirming && entered.length >= MIN_PIN) {
                    TextButton(onClick = {
                        first = entered
                        entered = ""
                        confirming = true
                    }) { Text(stringResource(R.string.next)) }
                }
            }
            Spacer(Modifier.weight(1f))
            PinPad(
                onDigit = { digit ->
                    mismatch = false
                    val limit = if (confirming) first.length else MAX_PIN
                    if (entered.length < limit) entered += digit
                    if (confirming && entered.length == first.length) {
                        if (entered == first) {
                            vm.setPin(first) { go.back() }
                        } else {
                            mismatch = true
                            entered = ""
                            first = ""
                            confirming = false
                        }
                    }
                },
                onDelete = { entered = entered.dropLast(1) },
                onBiometric = null,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
