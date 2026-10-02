package com.electroiot.dotglow.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class FoundClock(
    val device: BluetoothDevice,
    val name: String,
    val rssi: Int,
    val paired: Boolean,
) {
    val address: String get() = device.address
}

/** Finds nearby DotGlow clocks (filtered by the DotGlow service UUID in their advertising). */
class ClockScanner(context: Context) {
    private val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    val bluetoothOn: Boolean get() = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")   // the UI only starts scanning once the permissions are granted
    fun scan(): Flow<FoundClock> = callbackFlow {
        val scanner = adapter?.bluetoothLeScanner ?: run {
            close(IllegalStateException("Bluetooth is off"))
            return@callbackFlow
        }
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val d = result.device
                val name = result.scanRecord?.deviceName ?: d.name ?: "DotGlow"
                trySend(FoundClock(d, name, result.rssi, d.bondState == BluetoothDevice.BOND_BONDED))
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("Bluetooth scan failed ($errorCode)"))
            }
        }
        val filters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(DotGlowProtocol.SERVICE)).build())
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(filters, settings, callback)
        awaitClose { runCatching { scanner.stopScan(callback) } }
    }
}
