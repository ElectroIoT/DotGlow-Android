# Changelog

## 1.0.1 (versionCode 4), 2026-10-05

### Fixed
- Crash on "Allow Bluetooth" after a fresh install: biometric 1.1.0 pulled androidx.fragment 1.2.5, which rejects
  the Activity Result API's request codes. fragment-ktx 1.8.5 is now a direct dependency.
- The first screen scrolls, so the permission button and "New here?" stay reachable in landscape.

## 1.0.0 (versionCode 3), 2026-10-04

- First closed-testing release on Google Play (R8 shrinking on).

## Unreleased (0.1.0, versionCode 1)

### Added
- Rotate 180° (upside down): switch on the Display page and a one-tap button on the dashboard.
- Setup guide: "Important: power" (5 V / 1 A charger required, capacitor, separate supply for 8+ modules),
  ESP32 DevKit flash-pin warning, IN-side hint, new troubleshooting rows.
- LED matrix previews drawn like the real display: live clock mirror on the dashboard, digit styles, effects,
  message, display layout, stopwatch.
- Stopwatch page; optional stopwatch button in the setup guide.
- Demo clock: try every screen without hardware.
- About DotGlow and About the developer (links from `about.json`), privacy policy and terms.
- Install firmware over USB-OTG: ESP32, ESP32-S3 (DotGlow) and ESP8266 NodeMCU (Matrix Clock), or a .bin file.
- Firmware updates over Bluetooth (about 30 s for 1.2 MB).
- Setup guide: wiring for ESP32-S3, ESP32 DevKit and ESP8266, chaining and power, light sensor, USB-OTG, video link.
- All settings, WiFi setup over Bluetooth, special days, messages, app PIN lock with fingerprint.
- Scan and PIN pairing, dashboard.

### Fixed (found on a real phone)
- Reconnect after the app was killed: shorter first attempt, failed connects close their GATT client.
- A clock that was reset or reflashed: the old pairing is removed and the app pairs again by itself.
- GATT cache refreshed on connect, so new firmware features are seen.
