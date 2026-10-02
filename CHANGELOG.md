# Changelog

## Unreleased (0.1.0, versionCode 1)

### Added
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
