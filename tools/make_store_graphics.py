"""Play Store graphics: docs/play-store/{icon-512.png, feature-graphic.png, screenshots/*.png}

    python tools/make_store_graphics.py

Screenshots are built from the raw phone captures in docs/play-store/raw/ (1080 x 2400,
captured with a real clock connected): status / navigation bars are cut off, the screen is
put in a phone frame on a dark LED-style background with a headline.
"""
import os
import re
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.join(os.path.dirname(__file__), '..')
STORE = os.path.join(ROOT, 'docs', 'play-store')
RAW = os.path.join(STORE, 'raw')
SHOTS = os.path.join(STORE, 'screenshots')
os.makedirs(SHOTS, exist_ok=True)
F = 'C:/Windows/Fonts/'
RED = (255, 59, 59)
TXT, MUT = (242, 243, 247), (160, 166, 180)


def font(size, weight='regular'):
    name = {'regular': 'segoeui.ttf', 'semi': 'seguisb.ttf', 'bold': 'segoeuib.ttf', 'black': 'seguibl.ttf'}[weight]
    return ImageFont.truetype(F + name, size)


def background(w, h):
    """Near-black with a red glow and a faint LED dot grid."""
    im = Image.new('RGB', (w, h), (11, 12, 16))
    glow = Image.new('RGB', (w, h), (0, 0, 0))
    g = ImageDraw.Draw(glow)
    g.ellipse((-w * 0.3, -h * 0.15, w * 0.9, h * 0.45), fill=(120, 18, 24))
    g.ellipse((w * 0.4, h * 0.65, w * 1.4, h * 1.25), fill=(70, 12, 30))
    glow = glow.filter(ImageFilter.GaussianBlur(w * 0.18))
    im = Image.blend(im, glow, 0.55)
    d = ImageDraw.Draw(im)
    step = max(18, w // 54)
    for y in range(0, h, step):
        for x in range(0, w, step):
            d.ellipse((x - 1.6, y - 1.6, x + 1.6, y + 1.6), fill=(34, 20, 24))
    return im


def led_text(text, pitch, color=RED):
    """Text drawn as glowing LED dots with the clock's 5x7 digit font (digits and ':')."""
    DIG = {'0': [14, 17, 19, 21, 25, 17, 14], '1': [4, 12, 4, 4, 4, 4, 14], '2': [14, 17, 1, 2, 4, 8, 31],
           '3': [31, 2, 4, 2, 1, 17, 14], '4': [2, 6, 10, 18, 31, 2, 2], '5': [31, 16, 30, 1, 1, 17, 14],
           '6': [6, 8, 16, 30, 17, 17, 14], '7': [31, 1, 2, 4, 8, 8, 8], '8': [14, 17, 17, 14, 17, 17, 14],
           '9': [14, 17, 17, 15, 1, 2, 12], ':': [0, 6, 6, 0, 6, 6, 0]}
    cols = []
    for ch in text:
        rows = DIG[ch]
        width = 5 if ch != ':' else 4
        for c in range(width):
            cols.append([bool(rows[r] & (0x10 >> c)) for r in range(7)])
        cols.append([False] * 7)
    w, h = int(len(cols) * pitch + pitch), int(8 * pitch + pitch)
    base = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(base)
    r = pitch * 0.38
    for x, col in enumerate(cols):
        for y in range(8):
            cx, cy = pitch * (x + 1), pitch * (y + 1)
            on = y < 7 and col[y]
            d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=color + (255,) if on else (52, 14, 18, 255))
    glow = base.filter(ImageFilter.GaussianBlur(pitch * 0.45))
    out = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    out.alpha_composite(glow)
    out.alpha_composite(glow)
    out.alpha_composite(base)
    return out


# ---- icon --------------------------------------------------------------------------------------

def icon():
    xml = open(os.path.join(ROOT, 'app', 'src', 'main', 'res', 'drawable', 'ic_launcher_foreground.xml')).read()
    dots = re.findall(r'fillColor="#([0-9A-Fa-f]{6})" android:pathData="M([\d.]+),([\d.]+) a([\d.]+)', xml)
    S = 512
    k = S / 108 * 1.25   # adaptive icons show the middle 72 of 108 dp: zoom in like a launcher does
    off = (S - 108 * k) / 2
    im = Image.new('RGBA', (S, S), (18, 10, 12, 255))
    layer = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for col, x, y, r in dots:
        x, y, r = float(x) + float(r), float(y), float(r)
        c = tuple(int(col[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
        cx, cy, rr = off + x * k, off + y * k, r * k
        d.ellipse((cx - rr, cy - rr, cx + rr, cy + rr), fill=c)
    glow = layer.filter(ImageFilter.GaussianBlur(9))
    im.alpha_composite(glow)
    im.alpha_composite(layer)
    im.convert('RGB').save(os.path.join(STORE, 'icon-512.png'), optimize=True)


# ---- feature graphic -------------------------------------------------------------------------------

def feature():
    W, H = 1024, 500
    im = background(W, H).convert('RGBA')
    d = ImageDraw.Draw(im)
    # LED panel with the time
    panel = (52, 120, 52 + 470, 120 + 200)
    d.rounded_rectangle(panel, 24, fill=(6, 6, 8), outline=(46, 50, 60), width=2)
    led = led_text('12:45', 14)
    im.alpha_composite(led, (panel[0] + (470 - led.width) // 2, panel[1] + (200 - led.height) // 2))
    d.text((panel[0] + 235, panel[3] + 26), 'live on your clock and in the app', font=font(22), fill=MUT, anchor='ma')
    d.text((580, 150), 'DotGlow', font=font(84, 'black'), fill=TXT)
    d.text((584, 250), 'LED Matrix Clock', font=font(36, 'semi'), fill=RED)
    for i, t in enumerate(('Bluetooth setup  ·  live previews', 'Stopwatch  ·  firmware updates', 'ESP32 · ESP32-S3 · ESP8266')):
        d.text((584, 312 + i * 38), t, font=font(25), fill=MUT)
    im.convert('RGB').save(os.path.join(STORE, 'feature-graphic.png'), optimize=True)


# ---- screenshots ---------------------------------------------------------------------------------------

def phone(screen, height):
    """The capture without system bars, in a rounded phone frame, scaled to `height`."""
    sw, sh = screen.size
    top, bottom = int(sh * 0.046), int(sh * 0.056)   # status bar / navigation bar on the 1080 x 2400 captures
    screen = screen.crop((0, top, sw, sh - bottom))
    scale = (height - 40) / screen.height
    screen = screen.resize((int(screen.width * scale), int(screen.height * scale)), Image.LANCZOS)
    w, h = screen.width + 40, screen.height + 40
    frame = Image.new('RGBA', (w + 60, h + 60), (0, 0, 0, 0))
    sh_ = Image.new('RGBA', frame.size, (0, 0, 0, 0))
    ImageDraw.Draw(sh_).rounded_rectangle((30, 40, w + 30, h + 40), 70, fill=(0, 0, 0, 170))
    frame.alpha_composite(sh_.filter(ImageFilter.GaussianBlur(22)))
    d = ImageDraw.Draw(frame)
    d.rounded_rectangle((30, 30, w + 30, h + 30), 66, fill=(14, 15, 19), outline=(92, 98, 112), width=4)
    mask = Image.new('L', screen.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, screen.width, screen.height), 48, fill=255)
    frame.paste(screen, (50, 50), mask)
    return frame


SCREENS = [
    # file, headline (first word red), sub line
    ('02-dashboard', 'Your clock, live', 'A real LED preview of your clock, brightness and quick effects in one place'),
    ('08-animations', 'Animations you can see', '6 digit styles and 5 effects, each previewed before your clock plays it'),
    ('01-find', 'Pair in seconds', 'Finds DotGlow clocks nearby. Secure 6-digit PIN shown on the LEDs'),
    ('04-wifi', 'WiFi from your phone', 'Pick your network in the app. The clock fetches the exact time by itself'),
    ('05-clock', 'Every setting, one tap', '12 / 24 h, time zone, colon, seconds bar, date, night mode, rotate 180°'),
    ('12-stopwatch', 'Built-in stopwatch', 'Start, pause, reset from the app, or wire a button on the clock'),
    ('18-install', 'Flash new boards', 'ESP32, ESP32-S3 and ESP8266 over USB-OTG. No computer needed'),
    ('17-guide-wiring', 'Build it yourself', 'Step-by-step guide with wiring pictures, power tips and troubleshooting'),
]


def screenshot(i, name, title, sub):
    W, H = 1080, 1920
    im = background(W, H).convert('RGBA')
    d = ImageDraw.Draw(im)
    first, _, rest = title.partition(' ')
    f = font(78, 'black')
    w1 = d.textlength(first + ' ', font=f)
    total = w1 + d.textlength(rest, font=f)
    x = (W - total) / 2
    d.text((x, 92), first + ' ', font=f, fill=RED)
    d.text((x + w1, 92), rest, font=f, fill=TXT)
    # sub line, wrapped to two lines
    words, lines, cur = sub.split(), [], ''
    for wd in words:
        if d.textlength((cur + ' ' + wd).strip(), font=font(38)) > W - 140:
            lines.append(cur)
            cur = wd
        else:
            cur = (cur + ' ' + wd).strip()
    lines.append(cur)
    for k, ln in enumerate(lines):
        d.text((W / 2, 210 + k * 50), ln, font=font(38), fill=MUT, anchor='ma')
    shot = Image.open(os.path.join(RAW, name + '.png')).convert('RGB')
    ph = phone(shot, 1560)
    im.alpha_composite(ph, ((W - ph.width) // 2, 330))   # top under the headline, bottom runs off the image
    im.convert('RGB').save(os.path.join(SHOTS, f'{i:02d}-{name.split("-", 1)[1]}.png'), optimize=True)


if __name__ == '__main__':
    icon()
    feature()
    for i, (n, t, s) in enumerate(SCREENS, 1):
        if os.path.exists(os.path.join(RAW, n + '.png')):
            screenshot(i, n, t, s)
    for root in (STORE, SHOTS):
        for fn in sorted(os.listdir(root)):
            if fn.endswith('.png'):
                p = os.path.join(root, fn)
                print(os.path.relpath(p, STORE), Image.open(p).size, os.path.getsize(p) // 1024, 'KB')
