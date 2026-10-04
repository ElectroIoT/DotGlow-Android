# DotGlow: Play Store listing

Paste these into Play Console → Grow → Store presence → Main store listing.

## App name (max 30)

```
DotGlow: LED Matrix Clock
```
(25 characters)

## Short description (max 80)

```
Set up & control your ESP32 LED matrix clock over Bluetooth. Live LED previews.
```
(79 characters)

## Full description (max 4000)

```
DotGlow is the companion app for DIY LED matrix clocks built with an ESP32 or ESP32-S3 and a MAX7219 8x32 LED display. Set up the clock, change every setting and update its firmware, all from your phone over Bluetooth. No account, no cloud, no ads.

★ SET UP IN TWO MINUTES
• Finds DotGlow clocks near you over Bluetooth
• Secure pairing: the clock shows a 6-digit PIN on its LEDs
• Connect the clock to your WiFi from the app; it gets the exact internet time by itself

★ LIVE LED PREVIEWS
The app draws a real-looking LED matrix that shows exactly what your clock shows: the live clock, every digit animation, every effect, your scrolling message and the stopwatch. Change a setting and see the result instantly.

★ EVERY SETTING
• 12 / 24 hour, time zone, leading zero, blinking colon, seconds bar, date
• 6 digit-change styles: roll down, roll up, dissolve, slide, flip, drop & bounce
• Effects every hour and at power-on: sparkle, wipe, rain, boxes, Pac-Man, random
• Brightness, night mode (dim or display off), automatic brightness with a light sensor
• Your own scrolling message, repeated as often as you like
• Special days: birthdays and anniversaries with their own animation
• 4 to 16 display modules, module type, mirror, flip and one-tap Rotate 180°

★ STOPWATCH
Start, pause and reset the clock's stopwatch from the app, or with an optional push button on the clock.

★ FIRMWARE UPDATES OVER BLUETOOTH
Install new firmware without WiFi, in about a minute.

★ INSTALL FIRMWARE ON A NEW BOARD
Plug a new ESP32, ESP32-S3 or ESP8266 (NodeMCU) board into your phone with a USB-OTG adapter. DotGlow detects the chip and installs the right firmware. No computer needed.

★ STEP-BY-STEP SETUP GUIDE
Parts list, wiring pictures for ESP32-S3, ESP32 DevKit and ESP8266, power tips, chaining more modules, light sensor and stopwatch button, and troubleshooting.

★ PRIVATE AND SECURE
• Encrypted Bluetooth (LE Secure Connections), PIN pairing
• Optional app lock with PIN and fingerprint
• No accounts, no ads, no analytics, no tracking

★ NO CLOCK YET?
Tap "Try the demo clock" and explore every screen.

DotGlow clocks are DIY hardware: you build them from common parts (an ESP32 board, a MAX7219 4-in-1 LED matrix and a 5 V phone charger). The setup guide in the app shows how.

Made with care in India by Manoranjan (manoranjan.dev) · ElectroIoT.
```

## Category and tags

- **App category:** Tools (alternative: Lifestyle)
- **Tags (pick up to 5):** Clock, Bluetooth, Smart home, Electronics / DIY, Tools
- **Contact email:** admin@mtpcode.com
- **Website:** https://mtpcode.com/apps/dotglow.html
- **Privacy policy:** https://mtpcode.com/legal/dotglow/privacy.html

## Graphics (in this folder)

| Asset | File | Play requirement |
|---|---|---|
| App icon | `icon-512.png` | 512 x 512 PNG |
| Feature graphic | `feature-graphic.png` | 1024 x 500 PNG/JPG |
| Phone screenshots | `screenshots/01-...png` to `08-...png` | 2-8, 9:16, 1080 x 1920 |
| Raw screenshots | `raw/*.png` | every screen, unedited (for tablets / website) |

## App content answers

| Section | Answer |
|---|---|
| Privacy policy | https://mtpcode.com/legal/dotglow/privacy.html |
| Ads | No ads |
| App access | All functionality is available without special access. Reviewers without a clock: tap **"Try the demo clock"** on the first screen |
| Content rating | IARC questionnaire: All other app types, every answer No. Result: Everyone / PEGI 3 |
| Target audience | 18+ (not designed for children) |
| News app | No |
| Data safety | **No data collected, no data shared.** The app talks to the clock over Bluetooth / USB on the device; it downloads firmware and setup-guide files from manoranjan.dev and github.com without sending personal data |
| Government app / financial features / health | No |

### Data safety details

- Does your app collect or share any of the required user data types? **No**
- Is all user data encrypted in transit? **Yes** (HTTPS downloads, encrypted Bluetooth)
- Do you provide a way for users to request deletion? Not applicable (nothing is collected); uninstalling removes the app's local settings.

### Permissions to explain if asked

| Permission | Reason |
|---|---|
| BLUETOOTH_SCAN (neverForLocation), BLUETOOTH_CONNECT | Find and control the user's DotGlow clocks |
| ACCESS_FINE_LOCATION (Android 8-11 only, maxSdk 30) | Required by Android for Bluetooth scanning on those versions; never used for location |
| INTERNET | Firmware downloads, setup video and About links |
| USB host (optional feature) | Installing firmware on a board plugged into the phone |
| USE_BIOMETRIC | Optional fingerprint unlock of the app lock |

## Release notes (1.0.0)

```
First release:
• Set up DotGlow clocks over Bluetooth with PIN pairing
• WiFi setup, every clock setting, live LED previews
• Stopwatch, special days, messages, rotate 180°
• Firmware updates over Bluetooth, USB firmware installer for new boards
• Setup guide with wiring pictures, demo clock
```
