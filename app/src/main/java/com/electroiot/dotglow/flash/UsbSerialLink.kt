package com.electroiot.dotglow.flash

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.CompletableDeferred

/** A board on the phone's USB port (USB-OTG adapter or USB-C cable). */
class UsbSerialLink private constructor(private val port: UsbSerialPort, override val isUsbJtag: Boolean) : SerialLink {

    override fun write(data: ByteArray) = port.write(data, 2000)
    override fun read(buffer: ByteArray, timeoutMs: Int): Int = port.read(buffer, timeoutMs.coerceAtLeast(1))
    override fun setBaudRate(baud: Int) = port.setParameters(baud, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
    override fun setDtr(on: Boolean) { port.dtr = on }
    override fun setRts(on: Boolean) { port.rts = on }
    fun close() = runCatching { port.close() }

    companion object {
        private const val ESPRESSIF = 0x303A
        private const val PERMISSION = "com.electroiot.dotglow.USB_PERMISSION"

        /** Boards the stock driver list misses: the S3's own USB port, WCH CH9102 / CH343. */
        private val extra = UsbSerialProber(ProbeTable().apply {
            addProduct(ESPRESSIF, 0x1001, CdcAcmSerialDriver::class.java)   // ESP32-S3 / C3 USB-Serial-JTAG
            addProduct(ESPRESSIF, 0x0002, CdcAcmSerialDriver::class.java)   // ESP32-S2 / S3 ROM USB download
            addProduct(0x1A86, 0x55D4, CdcAcmSerialDriver::class.java)      // CH9102
            addProduct(0x1A86, 0x55D3, CdcAcmSerialDriver::class.java)      // CH343
        })

        fun findDrivers(usb: UsbManager): List<UsbSerialDriver> {
            val known = UsbSerialProber.getDefaultProber().findAllDrivers(usb)
            return known + extra.findAllDrivers(usb).filter { d -> known.none { it.device == d.device } }
        }

        /** USB adapter name for the screen, e.g. "CP2102 (Silicon Labs)". */
        fun describe(d: UsbDevice): String = when (d.vendorId) {
            ESPRESSIF -> "ESP32-S3 built-in USB"
            0x10C4 -> "CP210x USB adapter"
            0x1A86 -> "CH340 / CH9102 USB adapter"
            0x0403 -> "FTDI USB adapter"
            else -> d.productName ?: "USB serial adapter"
        }

        /** Asks the user to allow USB access (once per device, unless "always" is ticked). */
        suspend fun requestPermission(context: Context, usb: UsbManager, device: UsbDevice): Boolean {
            if (usb.hasPermission(device)) return true
            val result = CompletableDeferred<Boolean>()
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, i: Intent) {
                    result.complete(i.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                }
            }
            ContextCompat.registerReceiver(context, receiver, IntentFilter(PERMISSION), ContextCompat.RECEIVER_NOT_EXPORTED)
            try {
                val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
                val intent = PendingIntent.getBroadcast(context, 0, Intent(PERMISSION).setPackage(context.packageName), flags)
                usb.requestPermission(device, intent)
                return result.await()
            } finally {
                context.unregisterReceiver(receiver)
            }
        }

        fun open(usb: UsbManager, driver: UsbSerialDriver): UsbSerialLink {
            val conn = usb.openDevice(driver.device) ?: throw FlashException("Could not open the USB device")
            val port = driver.ports.first()
            port.open(conn)
            port.setParameters(EspFlasher.ROM_BAUD, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            return UsbSerialLink(port, driver.device.vendorId == ESPRESSIF)
        }
    }
}
