# Video

Large files, **never committed** (see `.gitignore`).

| Folder | What |
|---|---|
| `raw/` | Original phone recordings (copied with `adb pull` from `/sdcard/DCIM/Camera/`) |
| `edited/` | Your finished video(s) before uploading to YouTube |

Thumbnails: `../docs/youtube/` (made by `tools/make_thumbnails.py`).
After uploading to YouTube as Unlisted, put the link in `guide.json` on manoranjan.dev (`videoUrl`).

## HyperFrames (HTML to MP4 editor) — `studio/`

Open-source (Apache-2.0) video framework: https://github.com/heygen-com/hyperframes . Needs Node 22+ and FFmpeg (both installed).

```bash
cd video/studio
npm run dev        # preview in the browser with live reload
npm run check      # lint + runtime + layout + contrast gate
npm run render     # MP4 into renders/ (git-ignored)   e.g. npx hyperframes render -o renders/demo.mp4 --quality delivery
```

- Put footage in `studio/assets/` (or reference `../raw/...`); compositions are plain HTML in `studio/index.html` and `studio/compositions/`.
- The 10 HyperFrames skills live in `C:\Users\<you>\.claude\skills` (global): start a Claude Code session in `video/studio` and ask with `/hyperframes`.
- `npx hyperframes doctor` shows what is installed. Optional, not installed: whisper-cpp (transcription / captions), Kokoro TTS, MusicGen.
- Telemetry (anonymous usage data) is on by default: turn it off with `npx hyperframes telemetry disable`.
- Plain FFmpeg 9.0 (Gyan full build, winget) is on the PATH: `ffmpeg`, `ffprobe`; encoders x264, x265, AAC, Opus, NVENC / QSV / AMF.

## Whisper (speech to captions) — installed 2026-10-07

- Program: official whisper.cpp prebuilt (`whisper-cli.exe`) in `~/.cache/hyperframes/whisper/bin/Release/`; the user environment variable `HYPERFRAMES_WHISPER_PATH` points to it (HyperFrames reads it).
- Models in `~/.cache/hyperframes/whisper/models/`: `ggml-small.en.bin` (English, fast) and `ggml-large-v3.bin` (multilingual, Hindi; slow on CPU).
- Use: `npx hyperframes transcribe <audio-or-video> -m small.en -l en` (English) or `-m large-v3 -l hi` (Hindi).
- Whisper invents text on silent or noisy audio (e.g. soldering): only trust it where someone really speaks.
- Short clips for the YouTube Short go in `raw/short/` (names `01-hook.mp4` ... `07-end.mp4`).
