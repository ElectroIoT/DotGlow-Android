"""Setup-guide images for the app: app/src/main/res/drawable-nodpi/guide_*.png

    python tools/make_guide_images.py
"""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'drawable-nodpi')
os.makedirs(OUT, exist_ok=True)
F = 'C:/Windows/Fonts/'
W, H = 1600, 900
BG, PANEL, LINE = (20, 22, 28), (31, 35, 46), (58, 63, 75)
TXT, MUT, RED, OFF = (236, 238, 243), (155, 161, 175), (255, 59, 59), (52, 16, 18)
WIRE = {'VCC': (255, 77, 77), 'GND': (160, 165, 178), 'DIN': (53, 208, 127), 'CS': (255, 197, 61), 'CLK': (77, 163, 255)}


def font(size, bold=False):
    return ImageFont.truetype(F + ('seguisb.ttf' if bold else 'segoeui.ttf'), size)


def canvas(title, subtitle=''):
    im = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(im)
    d.text((60, 40), title, font=font(52, True), fill=TXT)
    if subtitle:
        d.text((60, 108), subtitle, font=font(30), fill=MUT)
    return im, d


def matrix(d, x, y, modules=4, label='MAX7219 4-in-1 (8x32)', w=150):
    d.rounded_rectangle((x, y, x + modules * (w + 12) + 28, y + 250), 22, fill=(18, 50, 79), outline=(42, 93, 138), width=3)
    for m in range(modules):
        mx = x + 20 + m * (w + 12)
        d.rounded_rectangle((mx, y + 22, mx + w, y + 22 + w), 8, fill=(11, 11, 13))
        p = w / 8
        for r in range(8):
            for c in range(8):
                lit = (r * 3 + c + m * 2) % 5 == 0
                cx, cy = mx + p * c + p / 2, y + 22 + p * r + p / 2
                d.ellipse((cx - p * .32, cy - p * .32, cx + p * .32, cy + p * .32), fill=RED if lit else OFF)
    d.text((x + 30, y + 196), label, font=font(30, True), fill=TXT)


def board(d, x, y, name, sub, pins, color=(16, 53, 58), outline=(31, 107, 114)):
    """Dev board with labelled pins on its right edge; returns {pin label: (x, y)}."""
    h = 120 + len(pins) * 64
    d.rounded_rectangle((x, y, x + 380, y + h), 24, fill=color, outline=outline, width=3)
    d.rounded_rectangle((x + 140, y - 34, x + 240, y + 6), 8, fill=(201, 204, 212))
    d.text((x + 30, y + 30), name, font=font(40, True), fill=TXT)
    d.text((x + 30, y + 80), sub, font=font(26), fill=MUT)
    spots = {}
    for i, (lab, pin) in enumerate(pins):
        py = y + 150 + i * 64
        d.text((x + 360, py - 18), pin, font=font(30, True), fill=TXT, anchor='ra')
        d.ellipse((x + 368, py - 9, x + 386, py + 9), fill=(230, 200, 90))
        spots[lab] = (x + 386, py)
    return spots


def wire(d, a, b, color, lane=0, width=9):
    """Right-angle wire; each wire gets its own vertical lane so they don't overlap."""
    (x1, y1), (x2, y2) = a, b
    mid = x1 + 70 + lane * 55
    pts = [(x1, y1), (mid, y1), (mid, y2), (x2, y2)]
    d.line(pts, fill=color, width=width, joint='curve')


def wiring(name, sub, pins, fname, title):
    im, d = canvas(title, 'Match each wire colour - the pin names are printed on both boards')
    order = ['VCC', 'GND', 'DIN', 'CS', 'CLK']
    spots = board(d, 80, 230, name, sub, [(k, pins[k]) for k in order])
    matrix(d, 820, 240)
    # matrix input header (left edge of the module board)
    for i, k in enumerate(order):
        py = 520 + i * 58
        d.ellipse((800, py - 9, 818, py + 9), fill=(230, 200, 90))
        d.text((830, py - 20), k, font=font(30, True), fill=TXT)
        wire(d, spots[k], (800, py), WIRE[k], lane=len(order) - 1 - i)   # top wire outermost: no crossings
    d.text((790, 480), 'IN', font=font(26, True), fill=MUT)
    # legend
    y = 520
    for i, k in enumerate(order):
        d.rounded_rectangle((1060, y + i * 58 - 18, 1100, y + i * 58 + 18), 6, fill=WIRE[k])
        d.text((1120, y + i * 58 - 20), f'{k}  ->  {pins[k]}', font=font(32), fill=TXT)
    im.save(os.path.join(OUT, fname), optimize=True)


wiring('ESP32-S3', 'DevKitC-1 (N16R8 etc.)',
       {'VCC': '5V', 'GND': 'GND', 'DIN': 'GPIO11', 'CS': 'GPIO10', 'CLK': 'GPIO12'},
       'guide_wiring_esp32s3.png', 'Wiring: ESP32-S3')
wiring('ESP32', 'DevKit (WROOM-32)',
       {'VCC': 'VIN (5V)', 'GND': 'GND', 'DIN': 'GPIO23', 'CS': 'GPIO5', 'CLK': 'GPIO18'},
       'guide_wiring_esp32.png', 'Wiring: ESP32 DevKit')


# ---- more modules: chain + power + capacitors ----------------------------------------
im, d = canvas('More modules (8x64 and up)', 'OUT of one board -> IN of the next  ·  separate 5 V supply  ·  capacitor at each board')
matrix(d, 70, 200, label='Board 2 (modules 5-8)', w=130)
matrix(d, 850, 200, label='Board 1 (modules 1-4)', w=130)
for i, k in enumerate(['DIN', 'CS', 'CLK']):
    y = 290 + i * 40
    d.line((850, y, 650, y), fill=WIRE[k], width=8)
d.text((700, 240), 'OUT -> IN', font=font(28, True), fill=MUT)
d.line((60, 560, 1540, 560), fill=WIRE['VCC'], width=12)
d.line((60, 690, 1540, 690), fill=WIRE['GND'], width=12)
d.text((70, 515), '5 V', font=font(30, True), fill=WIRE['VCC'])
d.text((70, 700), 'GND', font=font(30, True), fill=WIRE['GND'])
for bx in (300, 1080):
    d.line((bx - 60, 450, bx - 60, 560), fill=WIRE['VCC'], width=7)
    d.line((bx + 60, 450, bx + 60, 690), fill=WIRE['GND'], width=7)
    # electrolytic capacitor from 5 V to GND, next to the board's power taps
    cx = bx + 150
    d.line((cx, 560, cx, 610), fill=WIRE['VCC'], width=6)
    d.rectangle((cx - 34, 610, cx + 34, 620), fill=TXT)
    d.arc((cx - 34, 628, cx + 34, 668), 200, 340, fill=TXT, width=8)
    d.line((cx, 640, cx, 690), fill=WIRE['GND'], width=6)
    d.text((cx + 46, 588), '+', font=font(34, True), fill=(255, 197, 61))
    d.text((cx + 46, 628), '1000 µF', font=font(28, True), fill=(255, 197, 61))
d.rounded_rectangle((540, 760, 860, 870), 18, fill=PANEL, outline=LINE, width=3)
d.text((700, 790), '5 V supply', font=font(34, True), fill=TXT, anchor='ma')
d.text((700, 832), '2 A for 8 modules', font=font(26), fill=MUT, anchor='ma')
d.line((660, 760, 660, 560), fill=WIRE['VCC'], width=8)   # clear of board 2's capacitor label
d.line((780, 760, 780, 690), fill=WIRE['GND'], width=8)
d.rounded_rectangle((900, 760, 1300, 870), 18, fill=(16, 53, 58), outline=(31, 107, 114), width=3)
d.text((1100, 785), 'ESP32 / ESP32-S3', font=font(32, True), fill=TXT, anchor='ma')
d.text((1100, 828), 'VIN from the 5 V rail, GND shared', font=font(24), fill=MUT, anchor='ma')
d.line((960, 760, 960, 560), fill=WIRE['VCC'], width=7)
d.line((1000, 760, 1000, 690), fill=WIRE['GND'], width=7)
im.save(os.path.join(OUT, 'guide_chain.png'), optimize=True)


# ---- light sensor ------------------------------------------------------------------------
im, d = canvas('Optional: light sensor', 'For automatic brightness. The clock works fine without it.')
x = 520
d.text((x - 40, 245), '3V3', font=font(40, True), fill=WIRE['VCC'], anchor='rm')
d.line((x, 245, x + 120, 245), fill=WIRE['VCC'], width=8)
d.rounded_rectangle((x + 120, 205, x + 330, 285), 40, fill=(70, 50, 20), outline=(230, 190, 90), width=4)
d.text((x + 225, 245), 'LDR', font=font(36, True), fill=(255, 220, 140), anchor='mm')
d.line((x + 330, 245, x + 520, 245), fill=TXT, width=8)
d.ellipse((x + 505, 230, x + 535, 260), fill=(230, 200, 90))
d.text((x + 560, 245), 'GPIO1 (S3)  /  GPIO34 (ESP32)', font=font(36, True), fill=TXT, anchor='lm')
d.line((x + 520, 260, x + 520, 420), fill=TXT, width=8)
d.rectangle((x + 490, 420, x + 550, 560), outline=(200, 200, 210), width=6)
d.text((x + 580, 490), '10 kΩ', font=font(38, True), fill=TXT, anchor='lm')
d.line((x + 520, 560, x + 520, 680), fill=WIRE['GND'], width=8)
d.text((x + 520, 720), 'GND', font=font(40, True), fill=WIRE['GND'], anchor='mm')
d.text((80, 800), 'Then in the app: Brightness & night  ->  Auto brightness  ->  on', font=font(32), fill=MUT)
im.save(os.path.join(OUT, 'guide_ldr.png'), optimize=True)

for f in sorted(os.listdir(OUT)):
    print(f, os.path.getsize(os.path.join(OUT, f)) // 1024, 'KB')
