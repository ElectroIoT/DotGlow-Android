# Releasing the DotGlow app

1. Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`. Add the version to `CHANGELOG.md`.
2. Make sure the firmware catalog (`https://manoranjan.dev/dotglow/firmware.json`) lists the firmware this app
   expects (see DotGlow-ESP32 `docs/RELEASE.md`).
3. Signed bundle: `./gradlew bundleRelease` with a release keystore. Keep the keystore and its passwords outside
   the repo (backup in the owner's Dropbox, like the other apps).
4. Smoke test the release build: demo clock, a real clock (pair, WiFi, a setting, Bluetooth update),
   Install firmware on a spare board.

## Play Store checklist

- Name: **DotGlow: LED Matrix Clock**, package `com.electroiot.dotglow`.
- Privacy policy: `https://manoranjan.dev/dotglow/privacy.html`; terms: `https://manoranjan.dev/dotglow/terms.html`.
- Data safety: no data collected or shared. Bluetooth scan is `neverForLocation`.
- Reviewers have no clock: point them to **Try the demo clock** on the first screen.
- Screenshots: `docs/screenshots/` (emulator, demo clock).
- Publishing order (decided 2026-10-02): 1) app live on the Play Store, 2) make DotGlow-ESP32 public + GitHub
  release v<version> (clocks update over WiFi from there), 3) make DotGlow-Android public. Licence stays MIT;
  the README "Name and logo" note keeps the DotGlow name / icon / package for the official app.

## Remote content

- Setup video: set `videoUrl` in `dotglow/guide.json` on manoranjan.dev.
- About links / socials / apps: edit `dotglow/about.json` (fields of `AboutInfo`).
