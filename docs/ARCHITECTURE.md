# DotGlow app: architecture

Single-activity Kotlin + Jetpack Compose app. No DI framework, no navigation library: two `AndroidViewModel`s and
a `Page` enum, switched inside an `AnimatedContent` in `MainActivity`.

## Packages (`app/src/main/java/com/electroiot/dotglow/`)

| Path | Job |
|---|---|
| `MainActivity.kt` | App lock gate, Bluetooth permissions, screen switching (`Page` + slide animation), BackHandler |
| `ClockViewModel.kt` | Scan, connect / retry / reconnect, status polling (2 s), config, every setting and action, BLE firmware update, stopwatch |
| `ble/Protocol.kt` | Service + characteristic UUIDs, `RpcResult`, `RpcException` |
| `ble/ClockScanner.kt` | BLE scan filtered by the DotGlow service UUID |
| `ble/ClockConnection.kt` | One GATT connection: connect -> refresh cache -> discover -> MTU 517 -> read INFO -> pair -> secure check -> notifications; `call()`; `sendFirmware()` |
| `ble/DemoClock.kt` | `ClockLink` interface + an in-memory clock that answers like the firmware |
| `flash/EspFlasher.kt` | Espressif ROM bootloader protocol (SLIP, sync, chip detect, deflate writes, MD5 verify, reset sequences). Pure Kotlin, testable on a PC |
| `flash/UsbSerialLink.kt` | `SerialLink` over usb-serial-for-android, USB permission, extra VID/PIDs (S3 native USB, CH9102, CH343) |
| `flash/Firmware.kt` | `firmware.json` catalog, cached + MD5-checked downloads, image header checks |
| `flash/FlashViewModel.kt` | Install-firmware flow: find board -> permission -> detect chip -> pick firmware -> download -> write |
| `AppLock.kt` | Salted SHA-256 app PIN in SharedPreferences, biometric flag |
| `GuideLinks.kt` | Setup video link from `guide.json` (cached) |
| `AboutInfo.kt` | About links from `about.json` (cached, with built-in defaults), developer photo cache |
| `ui/Screens.kt` | Scan, Connecting, Dashboard, `SectionCard`, `InfoRow`, `Page` enum |
| `ui/SettingsScreens.kt` | WiFi, Clock & time, Brightness, Display, Animations, Message, Special days, Updates (+ Bluetooth update), Security, System |
| `ui/StopwatchScreen.kt`, `ui/FlashScreen.kt`, `ui/GuideScreen.kt`, `ui/AboutScreens.kt`, `ui/LockScreens.kt` | Those screens |
| `ui/Components.kt` | `SubPage` (scrolling, max 640 dp wide), rows, dialogs, option lists (animation names, digit styles, time zones) |
| `ui/MatrixWidgets.kt` | `MatrixHero`, preview cards and grids, phone clock, `faceStyle(config)` |
| `ui/matrix/Scenes.kt` | **Port of the firmware's display code**: frame buffer, 5x7 font, `ClockScene` (6 roll styles), effects, scroll, stopwatch |
| `ui/matrix/LedMatrix.kt` | Canvas renderer: glowing dots, module boards, mirror / flip, frame clock |

## Talking to the clock

All requests are JSON lines over BLE (`docs/PROTOCOL.md` in DotGlow-ESP32):
`ClockConnection.call(op, args)` -> `{"id","op","args"}` on RX, response `{"id","code","body"}` on TX notifications,
chunked to MTU - 3. GATT operations are serialized with a `Mutex` + `CompletableDeferred`.

Robustness rules learned on a real phone:
- Refresh Android's GATT cache on connect (`BluetoothGatt.refresh()` via reflection): firmware updates change the service.
- Connect attempts time out after 6 / 10 / 15 s: right after the app was killed, the first connect can hang.
- A failed connect must `close()` its GATT client (leaks otherwise).
- Read TX (encrypted) as a security check: if a "paired" clock refuses, it was reset -> remove the bond and pair again.
- During pairing, only a `BOND_NONE` that follows `BOND_BONDING` counts as failure.

## Remote JSON files (change without an app update)

| URL | Used by | Format |
|---|---|---|
| `https://manoranjan.dev/dotglow/firmware.json` | Install firmware, Bluetooth update | see DotGlow-ESP32 `docs/RELEASE.md` |
| `https://manoranjan.dev/dotglow/guide.json` | Setup guide video card | `{"videoUrl": "https://youtu.be/...", "videoTitle": "..."}` |
| `https://manoranjan.dev/dotglow/about.json` | About pages | fields of `AboutInfo` (links, socials, projects, apps, photo) |

Only `https://` links are accepted. Last good copies are cached in SharedPreferences.

## The LED matrix renderer

`Scene.draw(frame, tMs)` draws into a `Frame` (8 rows x 8*modules). `LedMatrix` advances time with
`withFrameMillis`, loops finite scenes, and draws dots on a `Canvas`. Scenes are deterministic (seeded random,
replayed from the start), so any frame can be drawn. Keep them identical to `src/display.cpp`.

## Guide images

`tools/make_guide_images.py` writes `app/src/main/res/drawable-nodpi/guide_*.png` (wiring per board, chaining,
LDR, USB-OTG, stopwatch button). `tools/make_readme_images.py` writes `docs/banner.svg`.
