package com.electroiot.dotglow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.electroiot.dotglow.ble.ConnState
import com.electroiot.dotglow.ui.ConnectingScreen
import com.electroiot.dotglow.ui.DashboardScreen
import com.electroiot.dotglow.ui.DotGlowTheme
import com.electroiot.dotglow.ui.ScanScreen

class MainActivity : ComponentActivity() {
    private val vm: ClockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    @Composable
    private fun App(vm: ClockViewModel) {
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
        val brightness by vm.brightness.collectAsStateWithLifecycle()

        when (val c = conn) {
            null, ConnState.Disconnected -> ScanScreen(
                hasPermission = granted,
                bluetoothOn = vm.bluetoothOn,
                scanning = scanning,
                clocks = found,
                snackbar = snackbar,
                onGrant = { launcher.launch(blePermissions) },
                onRescan = { vm.stopScan(); vm.startScan() },
                onPick = vm::connect,
            )
            is ConnState.Ready -> DashboardScreen(
                info = c.info,
                status = status,
                brightness = brightness,
                snackbar = snackbar,
                onBrightness = vm::setBrightness,
                onAnimation = vm::playAnimation,
                onDigits = vm::previewDigits,
                onShowIp = vm::showIp,
                onDisconnect = { vm.disconnect(); vm.startScan() },
            )
            else -> ConnectingScreen(c) { vm.disconnect(); vm.startScan() }
        }
    }
}
