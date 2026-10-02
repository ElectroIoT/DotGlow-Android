package com.electroiot.dotglow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.electroiot.dotglow.ble.ConnState
import com.electroiot.dotglow.ui.AnimationsScreen
import com.electroiot.dotglow.ui.BrightnessScreen
import com.electroiot.dotglow.ui.ClockSettingsScreen
import com.electroiot.dotglow.ui.ConnectingScreen
import com.electroiot.dotglow.ui.DashboardScreen
import com.electroiot.dotglow.ui.DisplayScreen
import com.electroiot.dotglow.ui.DotGlowTheme
import com.electroiot.dotglow.ui.GuideScreen
import com.electroiot.dotglow.ui.LockScreen
import com.electroiot.dotglow.ui.MessageScreen
import com.electroiot.dotglow.ui.Page
import com.electroiot.dotglow.ui.ScanScreen
import com.electroiot.dotglow.ui.SecurityScreen
import com.electroiot.dotglow.ui.SetPinDialog
import com.electroiot.dotglow.ui.SpecialDaysScreen
import com.electroiot.dotglow.ui.SystemScreen
import com.electroiot.dotglow.ui.UpdatesScreen
import com.electroiot.dotglow.ui.WifiScreen

class MainActivity : FragmentActivity() {
    private val vm: ClockViewModel by viewModels()
    private lateinit var lock: AppLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lock = AppLock(this)
        setContent { DotGlowTheme { App(vm) } }
    }

    private val blePermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)   // BLE scanning needs it on Android 8-11
        }

    private fun hasBlePermissions() =
        blePermissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    private fun askBiometric(onOk: () -> Unit) {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onOk()
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock DotGlow")
                .setAllowedAuthenticators(BIOMETRIC_WEAK)
                .setNegativeButtonText("Use PIN")
                .build(),
        )
    }

    @Composable
    private fun App(vm: ClockViewModel) {
        var unlocked by rememberSaveable { mutableStateOf(!lock.enabled) }
        if (!unlocked) {
            LockScreen(check = lock::check, biometric = lock.biometric && lock.biometricAvailable,
                onBiometric = { askBiometric { unlocked = true } }, onUnlocked = { unlocked = true })
            return
        }

        var granted by remember { mutableStateOf(hasBlePermissions()) }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            granted = hasBlePermissions()
            if (granted) vm.startScan()
        }
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
        LaunchedEffect(granted) { if (granted && vm.bluetoothOn) vm.startScan() }

        val conn by vm.connState.collectAsStateWithLifecycle()
        val found by vm.found.collectAsStateWithLifecycle()
        val scanning by vm.scanning.collectAsStateWithLifecycle()
        val status by vm.status.collectAsStateWithLifecycle()
        val config by vm.config.collectAsStateWithLifecycle()
        val reconnecting by vm.reconnecting.collectAsStateWithLifecycle()
        var page by rememberSaveable { mutableStateOf(Page.Home) }

        // app-lock settings live here (they belong to the phone, not the clock)
        var lockOn by remember { mutableStateOf(lock.enabled) }
        var bioOn by remember { mutableStateOf(lock.biometric) }
        var settingPin by remember { mutableStateOf(false) }
        if (settingPin) SetPinDialog(onSet = { lock.setPin(it); lockOn = true; settingPin = false }, onDismiss = { settingPin = false })

        val back = { page = Page.Home }
        BackHandler(enabled = page != Page.Home) { back() }

        // The setup guide works without a clock (it explains how to build one).
        if (page == Page.Guide) { GuideScreen(snackbar, onBack = back); return }

        val c = conn
        val ready = c is ConnState.Ready || reconnecting
        when {
            !ready && (c == null || c == ConnState.Disconnected) -> {
                page = Page.Home
                ScanScreen(
                    hasPermission = granted,
                    bluetoothOn = vm.bluetoothOn,
                    scanning = scanning,
                    clocks = found,
                    snackbar = snackbar,
                    onGrant = { launcher.launch(blePermissions) },
                    onRescan = { vm.stopScan(); vm.startScan() },
                    onPick = vm::connect,
                    onGuide = { page = Page.Guide },
                )
            }
            !ready -> ConnectingScreen(c, onRetry = vm::retry) { vm.disconnect(); vm.startScan() }
            else -> when (page) {
                Page.Wifi -> WifiScreen(vm, snackbar, back)
                Page.Clock -> ClockSettingsScreen(vm, snackbar, back)
                Page.Brightness -> BrightnessScreen(vm, snackbar, back)
                Page.Display -> DisplayScreen(vm, snackbar, back)
                Page.Animations -> AnimationsScreen(vm, snackbar, back)
                Page.Message -> MessageScreen(vm, snackbar, back)
                Page.SpecialDays -> SpecialDaysScreen(vm, snackbar, back)
                Page.Updates -> UpdatesScreen(vm, snackbar, back)
                Page.System -> SystemScreen(vm, snackbar, back)
                Page.Security -> SecurityScreen(
                    vm, snackbar,
                    appLockOn = lockOn,
                    biometricAvailable = lock.biometricAvailable,
                    biometricOn = bioOn,
                    onSetAppPin = { settingPin = true },
                    onRemoveAppPin = { lock.remove(); lockOn = false; bioOn = false },
                    onBiometric = { lock.biometric = it; bioOn = it },
                    onBack = back,
                )
                else -> DashboardScreen(
                    info = (c as? ConnState.Ready)?.info ?: com.electroiot.dotglow.ble.ClockInfo("DotGlow", "", "", 0),
                    status = status,
                    brightness = config?.int("br", 4) ?: 4,
                    reconnecting = reconnecting,
                    snackbar = snackbar,
                    onBrightness = vm::setBrightness,
                    onAnimation = vm::playAnimation,
                    onDigits = vm::previewDigits,
                    onOpen = { page = it },
                    onDisconnect = { vm.disconnect(); vm.startScan() },
                )
            }
        }
    }
}
