"""Vertical intro: LED-dot lettering mask on a 54 x 36 grid, two stacked lines (assets/mask.js).
    python scripts/make_mask_vertical.py ["Electro" "IoT"]"""
import os, sys, json
import numpy as np
from PIL import Image, ImageDraw, ImageFont
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
LINES = sys.argv[1:] or ["Electro", "IoT"]
COLS, ROWS, CELL = 54, 36, 10
W, H = COLS * CELL, ROWS * CELL
size = 220
while True:
    font = ImageFont.truetype(r"C:\Windows\Fonts\seguibl.ttf", size)
    img = Image.new("L", (W, H), 0); d = ImageDraw.Draw(img)
    boxes = [d.textbbox((0, 0), t, font=font) for t in LINES]
    widths = [b[2] - b[0] for b in boxes]; heights = [b[3] - b[1] for b in boxes]
    gap = int(size * 0.16)
    total = sum(heights) + gap * (len(LINES) - 1)
    if max(widths) <= W - 30 and total <= H - 20: break
    size -= 4
y = (H - total) // 2
for t, b, w, h in zip(LINES, boxes, widths, heights):
    d.text(((W - w) // 2 - b[0], y - b[1]), t, fill=255, font=font); y += h + gap
rows = []
for r in range(ROWS):
    row = ""
    for c in range(COLS):
        blk = np.asarray(img.crop((c * CELL, r * CELL, (c + 1) * CELL, (r + 1) * CELL)), dtype=np.float32) / 255.0
        row += "1" if blk.mean() > 0.42 else "0"
    rows.append(row)
os.makedirs(os.path.join(ROOT, "assets"), exist_ok=True)
open(os.path.join(ROOT, "assets", "mask.js"), "w", encoding="utf-8").write("window.INTRO_MASK = " + json.dumps(rows) + ";\n")
print("mask:", LINES, "font", size, "px, lit dots:", sum(r.count("1") for r in rows))
for r in rows[::2]: print(r.replace("0", ".").replace("1", "#"))