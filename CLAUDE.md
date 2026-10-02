# DotGlow app: brief for AI assistants

Android app (Kotlin + Jetpack Compose) for DotGlow LED matrix clocks. Package `com.electroiot.dotglow`, app name
"DotGlow: LED Matrix Clock". Firmware repo: `ElectroIoT/DotGlow-ESP32` (local `D:\My Project\ElectroIoT\DotGlow-ESP32`);
its `docs/PROTOCOL.md` is the contract between the two.

Read first: `docs/ARCHITECTURE.md`, `docs/DEVELOPMENT.md`, `docs/RELEASE.md`.

## Commands

```bash
./gradlew assembleDebug
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk        # emulator: use "Try the demo clock"
./gradlew testDebugUnitTest --tests '*FlasherHardwareTest*' -PflashPort=COM5      # USB flasher against a real board
python tools/make_guide_images.py      # guide PNGs in res/drawable-nodpi
python tools/make_readme_images.py     # docs/banner.svg
```

## Rules that matter

- Keep the app, `DemoClock` and the firmware's `api.cpp` / `docs/PROTOCOL.md` in sync (ops, config keys, status fields).
- `ui/matrix/Scenes.kt` mirrors the firmware's `display.cpp` (fonts, roll styles, effects, stopwatch). Change both.
- Bluetooth robustness rules in `docs/ARCHITECTURE.md` came from real-phone bugs; keep them.
- Remote JSON (firmware / guide / about) lives on `https://manoranjan.dev/dotglow/`; only accept `https://` links.
- Before UI taps with adb: dump the UI and use exact bounds.
- Bump `versionCode` / `versionName` before any release build.
- No accounts, analytics or tracking (the privacy policy says so).

## Environment

- Dev PC has **no Bluetooth**: BLE testing needs the user's phone (Motorola Edge 40 Neo, adb `ZD222M4292`) and the
  ESP32-S3 test clock on COM5 (DotGlow-EEA8, `esp32s3_test` firmware prints the pairing PIN on serial).
- Git Bash + adb: `export MSYS_NO_PATHCONV=1`.

## Commits

Author `manoranjan2050 <manoranjan2050@live.com>`, trailer `Co-Authored-By: ElectroIoT <electroiot.in@gmail.com>`,
**no AI co-author trailer**. Commit after each finished step. Push as the `ElectroIoT` gh account, then switch back
to `manoranjan2050`.
