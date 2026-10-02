package com.electroiot.dotglow.ble

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

/**
 * DotGlow Bluetooth protocol v1 (firmware: docs/PROTOCOL.md in ElectroIoT/DotGlow-ESP32).
 *
 * One GATT service with three characteristics:
 *  - INFO  read, open      {"name","fw","board","proto"}: identify a clock before pairing
 *  - RX    write, paired   requests, one JSON object per line: {"id":7,"op":"set","args":{"k":"br","v":"5"}}
 *  - TX    notify, paired  responses, one JSON object per line: {"id":7,"code":200,"body":...}
 *
 * Lines can be longer than one BLE packet: both sides split them into MTU-sized chunks
 * and join them again at the '\n'.
 */
object DotGlowProtocol {
    val SERVICE: UUID = UUID.fromString("6f4a0001-1d2c-4a8e-9c3d-2b7e5f9a1c00")
    val INFO: UUID = UUID.fromString("6f4a0002-1d2c-4a8e-9c3d-2b7e5f9a1c00")
    val RX: UUID = UUID.fromString("6f4a0003-1d2c-4a8e-9c3d-2b7e5f9a1c00")
    val TX: UUID = UUID.fromString("6f4a0004-1d2c-4a8e-9c3d-2b7e5f9a1c00")
    val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val VERSION = 1

    val json = Json { ignoreUnknownKeys = true; isLenient = true }
}

/** Result of one request: code follows HTTP (200 ok, 400 bad request, 401, 404 ...). */
data class RpcResult(val code: Int, val body: JsonElement) {
    val ok get() = code == 200
    /** Plain-text body (error messages, "OK"), or the JSON text for object bodies. */
    val message: String get() = (body as? JsonPrimitive)?.content ?: body.toString()
}

class RpcException(message: String) : Exception(message)
