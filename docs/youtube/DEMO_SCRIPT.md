# DotGlow demo video: script (split screen, about 4.5 minutes)

Layout: clock footage on the left, phone screen recording in the phone frame on the right
(see `overlay/`; positions in `tools/make_demo_overlay.py`). Narration is in simple English; read it in your own words.

## Before you film

- [ ] Clock powered, display on its **own 5 V / 1 A charger**, grounds joined. Brightness about 3-6 (not full) so the camera does not blow out the digits.
- [ ] **Daylight** next to a window, clock straight on, phone camera steady (tripod or lean it on a book). One take per scene.
- [ ] Phone: Do Not Disturb on, battery above 50 %, notifications cleared, **Developer options → Show taps** on, screen recorder ready (9:20, portrait).
- [ ] **Forget the clock** in the phone's Bluetooth settings first, so the pairing scene is real (Security page also has "forget paired phones").
- [ ] **WiFi:** use the phone's hotspot with a throwaway password. Turn **Show taps off** while typing a password, and never film neighbours' WiFi names.
- [ ] Sync: start both recordings, **clap once** in front of the camera and **tap the phone screen once** at the same time. Line the two up in Filmora.
- [ ] Record each scene 2 times; say the first words of each scene after a one-second pause (easy to cut).

## Script

| # | Time | Phone (screen recording) | Clock (camera) | Narration | On-screen text |
|---|---|---|---|---|---|
| 0 | 0:00 | (intro animation) | | | **DotGlow** · ElectroIoT.in |
| 1 | 0:06 | Home screen, open **DotGlow** | Clock showing the time, digits rolling to the next minute | "This is DotGlow: an LED matrix clock you build yourself, and control from your phone." | Build it · Control it · No account |
| 2 | 0:25 | **Nearby clocks** → tap **DotGlow-XXXX** | Clock shows a **6-digit PIN** on its LEDs | "The app finds the clock over Bluetooth. For security, the PIN appears on the clock's display." | Secure pairing |
| 3 | 0:45 | Type the 6 digits in the pairing request → **Pair** | Clock back to the time | "Type the PIN, and you're paired. No account, no cloud." | Paired in 20 seconds |
| 4 | 1:00 | **Dashboard**: the live LED preview, drag the **Brightness** slider down and up | Clock dims and brightens in sync | "The preview is a live copy of the clock. Move the slider, and the clock follows." | Live LED preview |
| 5 | 1:20 | **Play now**: tap **Digit change**, **Sparkle**, **Wipe**, **Rain**, **Boxes**, **Pac-Man** | Each effect plays on the clock | "Tap any effect to play it on the clock. There are six ways to change the digits, and five effects." | Tap to play |
| 6 | 2:00 | **Clock & time**: switch **12 / 24 hour**, toggle **Blinking colon**, **Seconds bar**, **Show date** | Clock changes after each switch | "Everything has a switch: 12 or 24 hour, the colon, the seconds bar, the date, your time zone." | Every setting in the app |
| 7 | 2:25 | **Brightness & night**: set night mode times, show Auto brightness | Display goes dim | "At night it dims or turns off. With a small light sensor it adjusts by itself." | Night mode · Auto brightness |
| 8 | 2:45 | **Message**: type "Hello from DotGlow" → **Show now** | Text scrolls across the display | "Type your own message and send it. It scrolls on the clock." | Your own message |
| 9 | 3:05 | **Special days**: add a birthday with an animation | (optional) show the animation | "Add birthdays and anniversaries with their own animation." | Special days |
| 10 | 3:20 | **Stopwatch**: Start, Pause, Reset | Display shows the running stopwatch | "There's a stopwatch too. Control it from the app, or with a button on the clock." | Stopwatch |
| 11 | 3:40 | Dashboard → **Rotate** button | Display flips upside down and back | "Mounted upside down? One tap rotates the display 180 degrees." | Rotate 180° |
| 12 | 3:55 | **Updates**: show the installed version | | "Update the firmware over Bluetooth, no WiFi needed." | Updates over Bluetooth |
| 13 | 4:10 | **Setup guide** and **Troubleshooting** | | "The built-in guide has the wiring pictures and fixes for the common problems." | Wiring guide inside the app |
| 14 | 4:25 | Back to the dashboard | Wide shot of the finished clock | "Build your own, and control it from your phone. Links are in the description." | mtpcode.com/apps/dotglow.html |

## End screen (last 10 seconds)

- "Subscribe for more electronics builds" + your channel logo.
- Links in the description: app page, web flasher (https://mtpcode.com/flash/), setup guide, source (after the repos go public).
- The app is in **closed testing** on Google Play for now: join the tester group (see the app page) until it is public.

## Description (paste into YouTube)

```text
DotGlow: build a smart LED matrix clock (ESP32 + MAX7219) and control it from your phone.

Chapters
0:00 Intro
0:06 What is DotGlow
0:25 Find and pair the clock
1:00 Live preview and brightness
1:20 Effects and animations
2:00 Clock and time settings
2:25 Night mode and auto brightness
2:45 Your own scrolling message
3:05 Special days
3:20 Stopwatch
3:40 Rotate 180
3:55 Bluetooth updates
4:10 Built-in setup guide

App and guide: https://mtpcode.com/apps/dotglow.html
Flash the firmware from your browser: https://mtpcode.com/flash/
Made by ElectroIoT.in
```

## Tips

- Keep each narration line under 10 seconds. Cut every pause longer than a second.
- Zoom (Filmora Pan and Zoom) on the clock when an effect plays, and on the phone when you tap small buttons.
- Put the step title (last column) in the top-left for 3 seconds at the start of each scene.
- Music: soft, 15-20 % volume under your voice.
