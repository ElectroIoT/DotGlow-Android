# Developing the DotGlow app

## Build and install

```bash
./gradlew assembleDebug
adb -s <device> install -r app/build/outputs/apk/debug/app-debug.apk
```

On the dev PC `adb` lives at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`. In Git Bash set
`MSYS_NO_PATHCONV=1` so paths like `/sdcard/ui.xml` are not rewritten.

## Where to test what

| What | Where |
|---|---|
| Layout, previews, every settings screen | Emulator + **Try the demo clock** |
| Bluetooth: scan, pairing, settings, updates | A real phone + a clock (the PC has no Bluetooth). Motorola Edge 40 Neo, adb `ZD222M4292` |
| USB flasher protocol | `FlasherHardwareTest` on the PC (below), then a phone with USB-OTG |

## Pairing in tests

Flash the clock with the firmware's `esp32s3_test` env: the PIN is printed on serial (`BLE: pairing PIN nnnnnn`).
Type it within ~30 s: tap Android's PIN field, `adb shell input text nnnnnn`, tap OK.
Always dump the UI (`uiautomator dump`) and tap exact bounds; never guess coordinates from a screenshot.

## Flasher test on a PC

`EspFlasher` is plain Kotlin. `FlasherHardwareTest` runs it with jSerialComm against a board on this computer:

```bash
./gradlew testDebugUnitTest --tests '*FlasherHardwareTest*' -PflashPort=COM5                      # detect only
./gradlew testDebugUnitTest --tests '*FlasherHardwareTest*' -PflashPort=COM5 -PflashImage=dotglow-esp32s3-full.bin
```

Result on an ESP32-S3 (native USB): 1.3 MB written in 18 s, MD5 verified. The test is skipped without `-PflashPort`.

## Conventions

- Compose only, Material 3, dark theme by design (`ui/Theme.kt`: `LedRed`, `Panel`, `Muted`...).
- Screens take the view model + `SnackbarHostState` + callbacks; state comes from `StateFlow`s.
- New clock op: add it to `ClockViewModel`, to `DemoClock`, and check DotGlow-ESP32 `docs/PROTOCOL.md`.
- New display effect: change `ui/matrix/Scenes.kt` together with the firmware's `display.cpp`.
- Bump `versionCode` / `versionName` before every release build.

## Commits

Author `manoranjan2050 <manoranjan2050@live.com>` with the trailer
`Co-Authored-By: ElectroIoT <electroiot.in@gmail.com>`. No AI co-author trailers. Push with
`gh auth switch -u ElectroIoT`, then switch back to `manoranjan2050`.
