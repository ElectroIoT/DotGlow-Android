# DotGlow user guide

DotGlow is the app for DotGlow LED matrix clocks (ESP32 / ESP32-S3 + MAX7219 display). This guide covers building
the clock, first setup and every screen of the app.

---

## 1. What you need

| Part | Notes |
|---|---|
| ESP32-S3 DevKitC-1 **or** ESP32 DevKit | ESP32-S2 is not supported (no Bluetooth). ESP8266 NodeMCU runs the WiFi-only Matrix Clock |
| MAX7219 4-in-1 LED matrix (8x32) | Chain more boards for 8x64 ... 8x128 |
| 5 jumper wires | |
| **5 V / 1 A (or stronger) phone charger** | **Required.** A computer USB port is too weak: the display stays dim, all LEDs stay on, the board restarts |
| 470-1000 µF capacitor | Across the display's VCC and GND (+ leg to VCC) |
| Optional: LDR + 10 kΩ, push button | Auto brightness, stopwatch button |

## 2. Wiring

Always connect to the display's **IN** end (pins marked **DIN**).

| Display | ESP32-S3 | ESP32 DevKit |
|---|---|---|
| VCC | 5V | 5V / VIN |
| GND | GND | GND |
| DIN | GPIO11 | D23 |
| CS | GPIO10 | D5 |
| CLK | GPIO12 | D18 |
| LDR (optional) | GPIO1 | GPIO34 |
| Stopwatch button (optional, to GND) | GPIO4 | GPIO13 |

> ESP32 DevKit: never use the pins labelled **CLK, SD0-SD3, CMD** (flash pins). The board will not start.

The app shows all of this with pictures: **Setup guide** (first screen → *New here?*).

## 3. Install the firmware

1. Plug the board into the phone with a USB-OTG adapter (or a USB-C data cable). Unplug the display while flashing.
2. App: **Install firmware → Find my board → allow USB → Install** (20-60 s).
3. The app detects ESP32 / ESP32-S3 (DotGlow) or ESP8266 (Matrix Clock) by itself. *Advanced* lets you pick a `.bin` file.

## 4. First setup

1. Power the clock from the charger. It shows a start-up animation and "manoranjan.dev".
2. Open DotGlow, tap **DotGlow-XXXX** in *Nearby clocks*.
3. The clock shows a **6-digit PIN** on its LEDs. Type it in the phone's pairing request (ignore "usually 0000 or 1234").
4. Tap **Set up** next to "This clock isn't on WiFi yet", pick your network, type the password, **Save & connect**.
   The clock restarts, gets the internet time and the app reconnects.

## 5. The dashboard

- **Live matrix**: a copy of what the clock shows right now.
- **Rotate button** (top right): turns the display upside down and back.
- **Brightness** slider (0-15).
- **Play now**: tap a card to play that effect on the clock.
- **Settings** and **Help** lists open the pages below.

## 6. Settings pages

| Page | What you can change |
|---|---|
| **WiFi** | Current network and IP; scan, pick and join another network |
| **Clock & time** | 12 / 24 hour, time zone, re-sync, blinking colon, seconds bar, leading zero, show date (live preview at the top) |
| **Brightness & night** | Brightness, night mode (from / until / night brightness or display off), auto brightness with a light sensor |
| **Display** | Number of modules (4-16, restarts the clock), module type, show module numbers, **Rotate 180°**, mirror, flip, show seconds (6+ modules) |
| **Animations** | Digit-change style (6 styles + random + instant), effect every hour and at power-on; ▶ plays an effect now |
| **Message** | Your own scrolling text (up to 64 characters), how often it repeats, show now |
| **Special days** | Birthdays / anniversaries: text and animation, shown every 15 minutes on that day |
| **Stopwatch** | Start / pause / resume / reset, back to the clock; optional button on the clock |
| **Updates** | Firmware version; update over WiFi from the internet, or **over Bluetooth** (no WiFi needed, ~30-60 s); automatic updates |
| **Security** | App lock (PIN + fingerprint), web page password, forget all paired phones |
| **System** | Firmware, mode, uptime; show IP, restart, factory reset |

## 7. Buttons on the clock

| Button | Action |
|---|---|
| BOOT, short press | Scroll the IP address |
| BOOT, **double press** | Rotate the display 180° and back |
| BOOT, hold 5 s | Forget WiFi, start setup |
| BOOT, hold 10 s | Remove the web page password |
| Stopwatch button, tap | Start / pause (from the clock: open the stopwatch) |
| Stopwatch button, hold | Reset; at 0: back to the clock |

## 8. Web page

On the same WiFi, open `http://<clock IP>` (shown on the WiFi page and when you press BOOT) or `http://dotglow.local`.
All settings are there too. Set a password under *Security* in the app or on the page.

## 9. Troubleshooting

| Problem | Fix |
|---|---|
| Display dim, all LEDs stuck on, garbage | **Power:** 5 V / 1 A charger, not a computer USB port. Add the capacitor |
| Board keeps restarting / drops off USB | Same: power. Use a stronger supply |
| Only the first module works | Broken link between module 1 and 2: re-solder the joints, or the wires are on the OUT end |
| ESP32 doesn't start after wiring | A wire is on CMD / SD0-SD3 / CLK: move it |
| No PIN on the display | The display gets no data: check wiring and power first |
| Can't pair / wrong PIN | Use the 6 digits on the display within 30 s; after a factory reset the app re-pairs by itself |
| "Getting time..." | The WiFi has no internet, or the time zone is wrong |
| Upside down / mirrored | Display → Rotate 180° (or double-press BOOT), mirror / flip |
| Install firmware fails | Use a data cable; hold BOOT, tap RESET, release BOOT, try again |

No clock yet? On the first screen tap **Try the demo clock** to explore every page.

Questions: electroiot.in@gmail.com · https://manoranjan.dev
