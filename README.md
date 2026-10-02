# DotGlow: LED Matrix Clock (Android)

Companion app for [DotGlow](https://github.com/ElectroIoT/DotGlow-ESP32) clocks (ESP32 / ESP32-S3 +
MAX7219 LED matrix). Set up and control your clock over Bluetooth.

> **Status: in development.** Kotlin + Jetpack Compose, minSdk 26, targetSdk 36.

## What works now (phase 3)

- Finds nearby DotGlow clocks over Bluetooth LE (filtered by the DotGlow service)
- Secure pairing: the clock shows a 6-digit PIN on its LED display, you type it in Android's pairing dialog
- Dashboard: live time and date, brightness slider, animation and digit-change previews, clock info, show IP

## Coming next

WiFi setup, every setting from the web page, special days and messages, app PIN lock (phase 4) ·
firmware updates over Bluetooth (phase 5) · flashing a new ESP32 over USB-OTG (phase 6).

## Protocol

The app follows the clock's Bluetooth protocol, documented in the firmware repo at
`docs/PROTOCOL.md`. UUIDs and message format are in
[`ble/Protocol.kt`](app/src/main/java/com/electroiot/dotglow/ble/Protocol.kt).

## Build

```bash
./gradlew assembleDebug
```

## License

MIT, see [LICENSE](LICENSE). Made by [manoranjan.dev](https://manoranjan.dev) / ElectroIoT.
