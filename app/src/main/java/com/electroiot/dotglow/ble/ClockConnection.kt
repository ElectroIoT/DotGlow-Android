package com.electroiot.dotglow.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

sealed interface ConnState {
    data object Connecting : ConnState
    data object Pairing : ConnState                 // waiting for the user to type the PIN shown on the clock
    data class Ready(val info: ClockInfo) : ConnState
    data class Failed(val reason: String) : ConnState
    data object Disconnected : ConnState
}

data class ClockInfo(val name: String, val firmware: String, val board: String, val protocol: Int)

/**
 * One BLE connection to a clock: connect -> discover -> MTU -> read INFO -> pair (PIN on the
 * clock's display) -> enable notifications -> ready for [call].
 *
 * Android's GATT API runs one operation at a time and reports results via callbacks, so every
 * operation here goes through [gattOp]: start it, then suspend until its callback arrives.
 */
@SuppressLint("MissingPermission")   // created only after the Bluetooth permissions are granted
class ClockConnection(private val context: Context, private val device: BluetoothDevice) {

    private val _state = MutableStateFlow<ConnState>(ConnState.Connecting)
    val state: StateFlow<ConnState> = _state.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var mtu = 23
    private val opLock = Mutex()
    private var pending: CompletableDeferred<Any?>? = null
    private var connected = CompletableDeferred<Boolean>()

    private val nextId = AtomicInteger(1)
    private val waiting = ConcurrentHashMap<Int, CompletableDeferred<RpcResult>>()
    private val rxLine = StringBuilder()

    // ---- GATT callbacks (binder thread) ------------------------------------------------

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                connected.complete(true)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connected.complete(false)
                pending?.completeExceptionally(RpcException("Disconnected"))
                waiting.values.forEach { it.completeExceptionally(RpcException("Disconnected")) }
                waiting.clear()
                if (_state.value !is ConnState.Failed) _state.value = ConnState.Disconnected
                g.close()
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) = finish(status)
        override fun onMtuChanged(g: BluetoothGatt, newMtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) mtu = newMtu
            finish(status)
        }

        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray, status: Int) =
            finish(status, value)

        @Deprecated("Android 12 and older")
        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            if (Build.VERSION.SDK_INT < 33) @Suppress("DEPRECATION") finish(status, c.value)
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) = finish(status)
        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) = finish(status)

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) =
            received(value)

        @Deprecated("Android 12 and older")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < 33) @Suppress("DEPRECATION") received(c.value)
        }
    }

    private fun finish(status: Int, value: Any? = null) {
        val p = pending ?: return
        if (status == BluetoothGatt.GATT_SUCCESS) p.complete(value)
        else p.completeExceptionally(GattStatusException(status))
    }

    private class GattStatusException(val status: Int) : Exception("Bluetooth error $status")

    /** Runs one GATT operation: [start] must return true if the operation was queued by Android. */
    private suspend fun gattOp(timeoutMs: Long = 10_000, start: (BluetoothGatt) -> Boolean): Any? = opLock.withLock {
        val g = gatt ?: throw RpcException("Not connected")
        val d = CompletableDeferred<Any?>()
        pending = d
        try {
            if (!start(g)) throw RpcException("Bluetooth is busy")
            withTimeout(timeoutMs) { d.await() }
        } finally {
            pending = null
        }
    }

    // ---- connect / pair ------------------------------------------------------------

    suspend fun connect() {
        try {
            _state.value = ConnState.Connecting
            connected = CompletableDeferred()
            gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
            if (!withTimeout(15_000) { connected.await() }) throw RpcException("Could not connect")

            gattOp { it.discoverServices() }
            runCatching { gattOp { it.requestMtu(517) } }   // falls back to 23-byte packets
            val service = gatt?.getService(DotGlowProtocol.SERVICE) ?: throw RpcException("Not a DotGlow clock")

            val infoBytes = gattOp { g -> g.readCharacteristic(service.getCharacteristic(DotGlowProtocol.INFO)) } as ByteArray
            val info = parseInfo(String(infoBytes))

            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                _state.value = ConnState.Pairing
                pair()
            }

            val tx = service.getCharacteristic(DotGlowProtocol.TX)
            gatt?.setCharacteristicNotification(tx, true)
            val cccd = tx.getDescriptor(DotGlowProtocol.CCCD)
            gattOp { g -> writeDescriptor(g, cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) }

            _state.value = ConnState.Ready(info)
        } catch (e: Exception) {
            _state.value = ConnState.Failed(e.message ?: "Connection failed")
            disconnect()
        }
    }

    /** Starts bonding; Android shows its pairing dialog and the clock shows the PIN. */
    private suspend fun pair() {
        val bonded = CompletableDeferred<Boolean>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val d: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33)
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (d?.address != device.address) return
                when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)) {
                    BluetoothDevice.BOND_BONDED -> bonded.complete(true)
                    BluetoothDevice.BOND_NONE -> bonded.complete(false)
                }
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED,
        )
        try {
            if (!device.createBond()) throw RpcException("Could not start pairing")
            if (!withTimeout(90_000) { bonded.await() }) throw RpcException("Pairing failed - check the PIN on the clock")
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    fun disconnect() {
        gatt?.let { runCatching { it.disconnect() } }
    }

    // ---- requests --------------------------------------------------------------------

    /** Sends one request and waits for its response. */
    suspend fun call(op: String, args: Map<String, Any?> = emptyMap(), timeoutMs: Long = 15_000): RpcResult {
        val service = gatt?.getService(DotGlowProtocol.SERVICE) ?: throw RpcException("Not connected")
        val id = nextId.getAndIncrement()
        val req = buildJsonObject {
            put("id", id)
            put("op", op)
            put("args", buildJsonObject { args.forEach { (k, v) -> put(k, v?.toString()) } })
        }
        val reply = CompletableDeferred<RpcResult>()
        waiting[id] = reply
        try {
            val bytes = (req.toString() + "\n").toByteArray()
            val rx = service.getCharacteristic(DotGlowProtocol.RX)
            val chunk = (mtu - 3).coerceAtLeast(20)
            for (i in bytes.indices step chunk) {
                val part = bytes.copyOfRange(i, minOf(i + chunk, bytes.size))
                gattOp { g -> writeCharacteristic(g, rx, part) }
            }
            return withTimeout(timeoutMs) { reply.await() }
        } finally {
            waiting.remove(id)
        }
    }

    private fun received(value: ByteArray) {
        rxLine.append(String(value))
        while (true) {
            val nl = rxLine.indexOf("\n")
            if (nl < 0) break
            val line = rxLine.substring(0, nl)
            rxLine.delete(0, nl + 1)
            runCatching {
                val obj = DotGlowProtocol.json.parseToJsonElement(line).jsonObject
                val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@runCatching
                val code = obj["code"]?.jsonPrimitive?.int ?: 500
                waiting[id]?.complete(RpcResult(code, obj["body"] ?: JsonNull))
            }
        }
    }

    // ---- helpers -----------------------------------------------------------------------

    private fun parseInfo(text: String): ClockInfo {
        val o: JsonObject = DotGlowProtocol.json.parseToJsonElement(text).jsonObject
        fun s(k: String) = (o[k] as? JsonPrimitive)?.content ?: ""
        return ClockInfo(s("name"), s("fw"), s("board"), s("proto").toIntOrNull() ?: 0)
    }

    private fun writeCharacteristic(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            g.writeCharacteristic(c, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            run {
                c.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                c.value = value
                g.writeCharacteristic(c)
            }
        }

    private fun writeDescriptor(g: BluetoothGatt, d: BluetoothGattDescriptor, value: ByteArray): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            g.writeDescriptor(d, value) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            run {
                d.value = value
                g.writeDescriptor(d)
            }
        }
}
