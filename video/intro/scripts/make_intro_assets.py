"""Intro assets: LED-dot lettering mask (assets/mask.js) + 5 s sound sting (assets/audio/intro-sting.wav).
    python scripts/make_intro_assets.py ["ElectroIoT"]
Original, synthesised sound: no third-party copyright."""
import os, sys, wave, json
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TEXT = sys.argv[1] if len(sys.argv) > 1 else "ElectroIoT"
COLS, ROWS, CELL = 96, 24, 10

# ---- dot mask: text rendered big, sampled per dot cell
W, H = COLS * CELL, ROWS * CELL
size = 170
while True:
    font = ImageFont.truetype(r"C:\Windows\Fonts\seguibl.ttf", size)
    img = Image.new("L", (W, H), 0); d = ImageDraw.Draw(img)
    bb = d.textbbox((0, 0), TEXT, font=font); tw, th = bb[2] - bb[0], bb[3] - bb[1]
    if tw <= W - 60 and th <= H - 40: break
    size -= 4
d.text(((W - tw) // 2 - bb[0], (H - th) // 2 - bb[1]), TEXT, fill=255, font=font)
rows = []
for r in range(ROWS):
    row = ""
    for c in range(COLS):
        blk = np.asarray(img.crop((c * CELL, r * CELL, (c + 1) * CELL, (r + 1) * CELL)), dtype=np.float32) / 255.0
        row += "1" if blk.mean() > 0.42 else "0"
    rows.append(row)
os.makedirs(os.path.join(ROOT, "assets"), exist_ok=True)
open(os.path.join(ROOT, "assets", "mask.js"), "w", encoding="utf-8").write("window.INTRO_MASK = " + json.dumps(rows) + ";\n")
print("mask:", TEXT, "font", size, "px, lit dots:", sum(r.count("1") for r in rows))

# ---- sound sting
SR = 44100; DUR = 5.0; n = int(DUR * SR); rng = np.random.default_rng(11)
mix = np.zeros((n, 2))
def tt(d): return np.arange(int(d * SR)) / SR
def add(start, sig, pan=0.0, gain=1.0):
    i = int(start * SR); e = min(n, i + len(sig))
    if i >= n: return
    l, r = np.cos((pan + 1) * np.pi / 4), np.sin((pan + 1) * np.pi / 4)
    mix[i:e, 0] += sig[:e - i] * l * gain; mix[i:e, 1] += sig[:e - i] * r * gain
def hp(k): return np.diff(rng.standard_normal(k + 1)) * 0.7
def saw(f, t, h=6): return sum(np.sin(2 * np.pi * f * k * t) / k for k in range(1, h + 1)) * 0.6
# rising arpeggio (A minor), louder as it builds
notes = [220.0, 261.63, 329.63, 440.0, 523.25, 659.25]
for k in range(28):
    t0 = 0.12 + k * 0.05
    if t0 > 1.45: break
    f = notes[k % 6] * (2 if k % 12 > 5 else 1); tp = tt(0.25)
    add(t0, (np.sin(2 * np.pi * f * tp) + .3 * np.sin(2 * np.pi * f * 2 * tp)) * np.exp(-tp * 14), -0.5 + (k % 5) * 0.25, 0.10 + 0.35 * (t0 / 1.45))
# riser 0.3 -> 1.5
tr = tt(1.2); ramp = (tr / 1.2) ** 2.0
add(0.3, hp(len(tr)) * ramp * 0.55 + np.sin(2 * np.pi * np.cumsum(200 + 1800 * ramp) / SR) * ramp * 0.2, 0, 0.8)
# impact + bell chord at 1.5
ti = tt(1.3); fi = 46 + 95 * np.exp(-ti * 13)
add(1.5, np.sin(2 * np.pi * np.cumsum(fi) / SR) * np.exp(-ti * 3.0) + hp(len(ti)) * np.exp(-ti * 16) * 0.5, 0, 1.0)
for f, g in [(440.0, 0.6), (659.25, 0.45), (880.0, 0.4), (1318.5, 0.25)]:
    tb = tt(2.4); add(1.52, (np.sin(2 * np.pi * f * tb) + .45 * np.sin(2 * np.pi * f * 2.76 * tb)) * np.exp(-tb * 2.4), 0.0, g * 0.55)
# pad (Am) 1.5 -> 4.7
tp = tt(3.4); env = np.minimum(1, tp / 0.5) * np.minimum(1, (3.4 - tp) / 1.0)
for f in (110.0, 220.0, 261.63, 329.63):
    add(1.5, saw(f, tp, 5) * env, 0.0, 0.07)
# downward whoosh 3.2 + shimmer ticks
tw_ = tt(0.8); x = rng.standard_normal(len(tw_)); o = np.zeros(len(tw_)); a = 0.0
for i in range(len(tw_)):
    a += (0.02 + 0.5 * (1 - i / len(tw_)) ** 1.6) * (x[i] - a); o[i] = a
add(3.2, o * np.sin(np.pi * np.linspace(0, 1, len(tw_))) ** 1.5, 0, 0.5)
for k in range(10):
    tk = tt(0.12); add(3.35 + k * 0.07, np.sin(2 * np.pi * (1800 + 220 * k) * tk) * np.exp(-tk * 40), -0.6 + 0.13 * k, 0.12)
mix = np.tanh(mix * 1.1) / 1.1
fade = np.ones(n); fo = int(0.7 * SR); fade[-fo:] = np.linspace(1, 0, fo) ** 1.4; fade[:int(0.01 * SR)] = np.linspace(0, 1, int(0.01 * SR))
mix *= fade[:, None]; mix = mix / np.abs(mix).max() * 0.88
pcm = (np.clip(mix, -1, 1) * 32767).astype("<i2")
os.makedirs(os.path.join(ROOT, "assets", "audio"), exist_ok=True)
with wave.open(os.path.join(ROOT, "assets", "audio", "intro-sting.wav"), "wb") as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
print("sting written")
