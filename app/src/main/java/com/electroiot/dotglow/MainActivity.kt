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
import com.electroiot.dotglow.ui.AboutAppScreen
import com.electroiot.dotglow.ui.AboutDeveloperScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.electroiot.dotglow.ui.faceStyle
import com.electroiot.dotglow.ui.BrightnessScreen
import com.electroiot.dotglow.ui.ClockSettingsScreen
import com.electroiot.dotglow.ui.ConnectingScreen
import com.electroiot.dotglow.ui.DashboardScreen
import com.electroiot.dotglow.ui.DisplayScreen
import com.electroiot.dotglow.ui.DotGlowTheme
import com.electroiot.dotglow.flash.FlashViewModel
import com.electroiot.dotglow.ui.FlashScreen
import com.electroiot.dotglow.ui.GUIDE_BOARD_ESP8266
import com.electroiot.dotglow.ui.GuideScreen
import androidx.compose.runtime.mutableIntStateOf
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

/** How deep a screen is, for the direction of the slide animation. */
private fun depth(screen: String) = when (screen) {
    "Scan", "Connecting", "Home" -> 0
    "AboutDev" -> 2
    else -> 1
}

class MainActivity : FragmentActivity() {
    private val vm: ClockViewModel by viewModels()
    private val flashVm: FlashViewModel by viewModels()
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

        var guideBoard by rememberSaveable { mutableIntStateOf(0) }
        val back = { page = if (page == Page.AboutDev) Page.AboutApp else Page.Home }
        BackHandler(enabled = page != Page.Home) {
            if (page == Page.Flash) flashVm.reset()
            back()
        }

        val c = conn
        val ready = c is ConnState.Ready || reconnecting
        val atScan = !ready && (c == null || c == ConnState.Disconnected)
        // Pages that work without a clock: the guide, the USB installer, About.
        val standalone = page in setOf(Page.Guide, Page.Flash, Page.AboutApp, Page.AboutDev)
        LaunchedEffect(atScan) { if (atScan && !standalone) page = Page.Home }
        val screen = when {
            standalone -> page.name
            atScan -> "Scan"
            !ready -> "Connecting"
            else -> page.name
        }

        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                // deeper pages slide in from the right, going back slides the other way
                val dir = if (depth(targetState) >= depth(initialState)) 1 else -1
                (slideInHorizontally(tween(320)) { w -> dir * w / 3 } + fadeIn(tween(320))) togetherWith
                    (slideOutHorizontally(tween(320)) { w -> -dir * w / 4 } + fadeOut(tween(200)))
            },
            label = "screens",
        ) { key ->
            when (key) {
                "Guide" -> GuideScreen(snackbar, initialBoard = guideBoard, onFlash = { page = Page.Flash }, onBack = { guideBoard = 0; back() })
                "Flash" -> FlashScreen(flashVm, snackbar, onBack = back, onGuide8266 = { guideBoard = GUIDE_BOARD_ESP8266; page = Page.Guide })
                "AboutApp" -> AboutAppScreen(snackbar, onDeveloper = { page = Page.AboutDev }, onBack = back)
                "AboutDev" -> AboutDeveloperScreen(snackbar, onBack = back)
                "Scan" -> ScanScreen(
                    hasPermission = granted,
                    bluetoothOn = vm.bluetoothOn,
                    scanning = scanning,
                    clocks = found,
                    snackbar = snackbar,
                    onGrant = { launcher.launch(blePermissions) },
                    onRescan = { vm.stopScan(); vm.startScan() },
                    onPick = vm::connect,
                    onGuide = { page = Page.Guide },
                    onFlash = { page = Page.Flash },
                    onDemo = vm::openDemo,
                    onAbout = { page = Page.AboutApp },
                )
                "Connecting" -> ConnectingScreen(conn, onRetry = vm::retry) { vm.disconnect(); vm.startScan() }
                Page.Wifi.name -> WifiScreen(vm, snackbar, back)
                Page.Clock.name -> ClockSettingsScreen(vm, snackbar, back)
                Page.Brightness.name -> BrightnessScreen(vm, snackbar, back)
                Page.Display.name -> DisplayScreen(vm, snackbar, back)
                Page.Animations.name -> AnimationsScreen(vm, snackbar, back)
                Page.Message.name -> MessageScreen(vm, snackbar, back)
                Page.SpecialDays.name -> SpecialDaysScreen(vm, snackbar, back)
                Page.Updates.name -> UpdatesScreen(vm, snackbar, back)
                Page.System.name -> SystemScreen(vm, snackbar, back)
                Page.Security.name -> SecurityScreen(
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
                    info = (conn as? ConnState.Ready)?.info ?: com.electroiot.dotglow.ble.ClockInfo("DotGlow", "", "", 0),
                    status = status,
                    brightness = config?.int("br", 4) ?: 4,
                    face = config?.let { faceStyle(it) } ?: com.electroiot.dotglow.ui.matrix.FaceStyle(),
                    modules = config?.int("modules", 4) ?: 4,
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
