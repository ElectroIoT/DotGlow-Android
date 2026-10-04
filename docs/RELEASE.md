# Releasing the DotGlow app

1. Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`. Add the version to `CHANGELOG.md`.
2. Make sure the firmware catalog (`https://manoranjan.dev/dotglow/firmware.json`) lists the firmware this app
   expects (see DotGlow-ESP32 `docs/RELEASE.md`).
3. Signed bundle: `./gradlew bundleRelease`. The key is `keystore/release.keystore` + `keystore.properties`
   (both gitignored, alias `dotglow`, created 2026-10-03); backup, passwords and restore steps in
   `D:\My Project\Dropbox\Android\DotGlow\` (README.txt, KEY_DETAILS.txt).
4. Smoke test the release build: demo clock, a real clock (pair, WiFi, a setting, Bluetooth update),
   Install firmware on a spare board.

## Play Store checklist

- Name: **DotGlow: LED Matrix Clock**, package `com.electroiot.dotglow`.
- Privacy policy: `https://mtpcode.com/legal/dotglow/privacy.html`; terms: `https://mtpcode.com/legal/dotglow/terms.html`.
- Data safety: no data collected or shared. Bluetooth scan is `neverForLocation`.
- Reviewers have no clock: point them to **Try the demo clock** on the first screen.
- Screenshots: `docs/screenshots/` (emulator, demo clock).
- Publishing order (decided 2026-10-02): 1) app live on the Play Store, 2) make DotGlow-ESP32 public + GitHub
  release v<version> (clocks update over WiFi from there), 3) make DotGlow-Android public. Licence stays MIT;
  the README "Name and logo" note keeps the DotGlow name / icon / package for the official app.

- Play Console: developer "MTP Code" (8802788467778962488), app ID 4972527762576844971. Store listing, store
  settings (Tools) and every App content section are done (content rating: IARC, Everyone / PEGI 3; target 18+).
  A personal account needs a closed test (12 testers, 14 days) before production access.

## If ads are added later

Change all of these in the same release, or the review rejects the update:

1. Play Console → App content → **Ads**: "Yes, my app contains ads".
2. **Advertising ID** declaration: yes; the AdMob SDK adds the `AD_ID` permission (keep it in the merged manifest).
3. **Data safety**: AdMob collects device / advertising IDs, approximate location, app interactions, diagnostics;
   shared with Google for advertising. Update the form.
4. Privacy policy + terms on mtpcode.com (`legal/dotglow/`): add the AdMob / Google section and the consent text.
5. Consent: Google UMP (User Messaging Platform) dialog for EEA / UK users.
6. Listing (`docs/play-store/listing.md` and Play): remove "no ads" from the description; the store shows "Contains ads".
7. `CLAUDE.md` / this file: drop "no ads" from the rules.

## Remote content

- Setup video: set `videoUrl` in `dotglow/guide.json` on manoranjan.dev.
- About links / socials / apps: edit `dotglow/about.json` (fields of `AboutInfo`).
