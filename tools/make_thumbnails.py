"""YouTube thumbnails (1280x720 JPG) for the DotGlow videos, built from real photos.

    python tools/make_thumbnails.py <clock-photo.jpg> <app-screenshot.png> [out-dir]

clock-photo: docs/youtube/photos/clock-day-lit.jpg (made by tools/light_up_clock_photo.py from a daylight photo).
app-screenshot: a plain 1080x2400 app screenshot (docs/play-store/raw/); it is put into a drawn phone frame.
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont

W, H = 1280, 720
RED = (255, 59, 59)
WHITE = (255, 255, 255)
BLUE = (66, 133, 244)
FONTS = "C:/Windows/Fonts/"
IMPACT = FONTS + "impact.ttf"
BLACK = FONTS + "seguibl.ttf"
BOLD = FONTS + "segoeuib.ttf"

# crop of the daylight photo with lit digits (docs/youtube/photos/clock-day-lit.jpg, 2000x906): the whole case
CASE_CROP = (372, 104, 1648, 793)
HERE = os.path.dirname(os.path.abspath(__file__))
ICON = os.path.join(HERE, "..", "docs", "play-store", "icon-512.png")


def font(path, size):
    return ImageFont.truetype(path, size)


def background():
    img = Image.new("RGB", (W, H), (10, 12, 17))
    px = ImageDraw.Draw(img)
    for y in range(H):                      # vertical gradient, a hint of red at the bottom
        t = y / H
        px.line([(0, y), (W, y)], fill=(int(10 + 22 * t), int(12 + 4 * t), int(17 + 8 * t)))
    glow = Image.new("RGB", (W, H), (0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([620, 200, 1380, 900], fill=(120, 14, 24))
    gd.ellipse([-260, 380, 460, 980], fill=(70, 8, 16))
    glow = glow.filter(ImageFilter.GaussianBlur(150))
    img = Image.blend(img, Image.composite(glow, img, glow.convert("L")), 1.0)
    dots = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dd = ImageDraw.Draw(dots)
    for y in range(10, H, 20):              # LED dot matrix texture
        for x in range(10, W, 20):
            dd.ellipse([x - 3, y - 3, x + 3, y + 3], fill=(255, 59, 59, 26))
    img = img.convert("RGBA")
    img.alpha_composite(dots)
    return img


def rounded(im, radius):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, im.size[0] - 1, im.size[1] - 1], radius, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def glow_behind(canvas, box, color, blur=40, grow=14, alpha=200):
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    x0, y0, x1, y1 = box
    ImageDraw.Draw(layer).rounded_rectangle([x0 - grow, y0 - grow, x1 + grow, y1 + grow], 40, fill=color + (alpha,))
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(blur)))


def shadow(canvas, box, blur=22, offset=14):
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    x0, y0, x1, y1 = box
    ImageDraw.Draw(layer).rounded_rectangle([x0, y0 + offset, x1, y1 + offset], 30, fill=(0, 0, 0, 190))
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(blur)))


def punch(im, contrast=1.12, color=1.15, bright=1.0):
    im = ImageEnhance.Contrast(im).enhance(contrast)
    im = ImageEnhance.Color(im).enhance(color)
    return ImageEnhance.Brightness(im).enhance(bright)


def text(canvas, xy, s, f, fill, stroke=0, stroke_fill=(0, 0, 0), shadow_px=0, anchor="la"):
    d = ImageDraw.Draw(canvas)
    if shadow_px:
        d.text((xy[0] + shadow_px, xy[1] + shadow_px), s, font=f, fill=(0, 0, 0, 255), anchor=anchor,
               stroke_width=stroke, stroke_fill=(0, 0, 0))
    d.text(xy, s, font=f, fill=fill, stroke_width=stroke, stroke_fill=stroke_fill, anchor=anchor)


def chip(canvas, x, y, label, color=RED, size=34, pad=(26, 12), text_color=WHITE, filled=True):
    f = font(BLACK, size)
    d = ImageDraw.Draw(canvas)
    w = int(d.textlength(label, font=f)) + pad[0] * 2
    h = size + pad[1] * 2 + 6
    if filled:
        d.rounded_rectangle([x, y, x + w, y + h], h // 2, fill=color)
    else:
        d.rounded_rectangle([x, y, x + w, y + h], h // 2, fill=(20, 22, 30, 230), outline=color, width=4)
    d.text((x + pad[0], y + pad[1] - 2), label, font=f, fill=text_color)
    return w, h


def check_row(canvas, x, y, label, w=520):
    d = ImageDraw.Draw(canvas)
    d.rounded_rectangle([x, y, x + w, y + 78], 20, fill=(22, 24, 33, 235))
    d.rounded_rectangle([x, y, x + 12, y + 78], 6, fill=RED)
    d.ellipse([x + 30, y + 16, x + 76, y + 62], fill=RED)
    d.line([(x + 41, y + 40), (x + 50, y + 49), (x + 66, y + 30)], fill=WHITE, width=7, joint="curve")
    d.text((x + 96, y + 10), label, font=font(BLACK, 40), fill=WHITE)


def soldering_iron(canvas, cx, cy, scale=1.0, angle=-35):
    """Stylised soldering iron with a glowing tip and sparks, drawn on its own layer and rotated."""
    s = scale
    layer = Image.new("RGBA", (700, 300), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    # handle, collar, barrel, tip (pointing right)
    d.rounded_rectangle([20, 105, 300, 195], 40, fill=(255, 122, 24))
    for i in range(6):
        d.line([(60 + i * 38, 112), (60 + i * 38, 188)], fill=(214, 96, 14), width=5)
    d.rectangle([300, 118, 340, 182], fill=(70, 74, 84))
    d.rectangle([340, 128, 470, 172], fill=(150, 156, 168))
    d.polygon([(470, 134), (590, 146), (610, 150), (590, 154), (470, 166)], fill=(200, 205, 214))
    glow = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([560, 110, 660, 190], fill=(255, 170, 40, 255))
    layer.alpha_composite(glow.filter(ImageFilter.GaussianBlur(18)))
    d = ImageDraw.Draw(layer)
    d.ellipse([594, 142, 614, 158], fill=(255, 240, 180))
    for ang, ln in [(-60, 46), (-25, 62), (15, 52), (50, 40), (-90, 34)]:   # sparks
        a = math.radians(ang)
        x0, y0 = 618 + math.cos(a) * 14, 150 + math.sin(a) * 14
        d.line([(x0, y0), (x0 + math.cos(a) * ln, y0 + math.sin(a) * ln)], fill=(255, 205, 80), width=5)
    layer = layer.resize((int(700 * s), int(300 * s)), Image.LANCZOS).rotate(angle, expand=True, resample=Image.BICUBIC)
    canvas.alpha_composite(layer, (int(cx - layer.width / 2), int(cy - layer.height / 2)))


def bluetooth(canvas, cx, cy, r=62):
    d = ImageDraw.Draw(canvas)
    ring = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(ring).ellipse([cx - r - 12, cy - r - 12, cx + r + 12, cy + r + 12], fill=BLUE + (255,))
    canvas.alpha_composite(ring.filter(ImageFilter.GaussianBlur(22)))
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=BLUE, outline=WHITE, width=6)
    k = r * 0.42
    pts = [(-k, -k * 0.55), (k * 0.9, k * 0.65), (0, k * 1.5), (0, -k * 1.5), (k * 0.9, -k * 0.65), (-k, k * 0.55)]
    d.line([(cx + x, cy + y) for x, y in pts], fill=WHITE, width=9, joint="curve")


def brand(canvas, x, y, label="DotGlow", sub="ElectroIoT.in"):
    icon = Image.open(ICON).convert("RGBA").resize((84, 84), Image.LANCZOS)
    canvas.alpha_composite(rounded(icon, 20), (x, y))
    text(canvas, (x + 100, y - 2), label, font(BLACK, 44), WHITE)
    text(canvas, (x + 102, y + 46), sub, font(BOLD, 28), (255, 150, 150))


def crop_fit(im, box, size):
    return im.crop(box).resize(size, Image.LANCZOS)


def build_thumb(clock, out):
    c = background()
    # clock photo, bottom right
    photo = punch(crop_fit(clock, CASE_CROP, (770, 416)), contrast=1.04, color=1.05)
    box = (476, 296, 1246, 712)
    glow_behind(c, box, RED, blur=46, grow=22, alpha=230)
    shadow(c, box)
    c.alpha_composite(rounded(photo, 30), (box[0], box[1]))
    ImageDraw.Draw(c).rounded_rectangle(box, 30, outline=(255, 120, 120), width=4)
    # title
    text(c, (56, 34), "BUILD YOUR OWN", font(IMPACT, 96), WHITE, stroke=7, shadow_px=6)
    text(c, (52, 128), "LED CLOCK", font(IMPACT, 200), RED, stroke=9, shadow_px=8)
    text(c, (60, 330), "FROM SCRATCH", font(BLACK, 54), (255, 200, 90))
    # steps
    for i, label in enumerate(["SOLDERING", "WIRING", "APP SETUP"]):
        check_row(c, 56, 400 + i * 92, label, w=390)
    soldering_iron(c, 1085, 200, scale=0.8, angle=212)   # tip points down-left, at the clock
    c.convert("RGB").save(out, quality=93, optimize=True)


def phone_mockup(shot, screen_w=400):
    """Phone frame (bezel, punch-hole camera, side buttons) around an app screenshot. Returns an RGBA image."""
    sh = screen_w * shot.height // shot.width
    bez = 17
    pw, ph = screen_w + 2 * bez, sh + 2 * bez
    out = Image.new("RGBA", (pw + 10, ph), (0, 0, 0, 0))
    d = ImageDraw.Draw(out)
    d.rounded_rectangle([pw - 2, 190, pw + 8, 280], 5, fill=(70, 72, 80))        # power button
    d.rounded_rectangle([pw - 2, 320, pw + 8, 430], 5, fill=(70, 72, 80))        # volume
    d.rounded_rectangle([0, 0, pw - 1, ph - 1], 70, fill=(22, 23, 28), outline=(120, 124, 136), width=4)
    d.rounded_rectangle([5, 5, pw - 6, ph - 6], 66, outline=(44, 46, 54), width=3)
    screen = rounded(shot.convert("RGB").resize((screen_w, sh), Image.LANCZOS), 54)
    out.alpha_composite(screen, (bez, bez))
    d = ImageDraw.Draw(out)
    d.ellipse([pw // 2 - 11, bez + 10, pw // 2 + 11, bez + 32], fill=(10, 10, 12))   # camera hole
    d.ellipse([pw // 2 - 4, bez + 17, pw // 2 + 4, bez + 25], fill=(24, 30, 52))
    return out


def demo_thumb(clock, shot, out):
    c = background()
    shot = shot.convert("RGB").crop((0, 96, shot.width, shot.height))   # drop the real status bar (carrier, notifications)
    padded = Image.new("RGB", (shot.width, shot.height + 100), shot.getpixel((4, 4)))   # clear band for the camera hole
    padded.paste(shot, (0, 100))
    shot = padded
    phone = phone_mockup(shot, 350)
    tilt = phone.rotate(-7, expand=True, resample=Image.BICUBIC)
    pos = (788, 44)
    gl = Image.new("RGBA", c.size, (0, 0, 0, 0))
    ImageDraw.Draw(gl).rounded_rectangle([pos[0] + 20, pos[1] + 30, pos[0] + tilt.width - 20, H + 200], 80, fill=(255, 60, 60, 190))
    c.alpha_composite(gl.filter(ImageFilter.GaussianBlur(60)))
    shadow_layer = Image.new("RGBA", c.size, (0, 0, 0, 0))
    shadow_layer.paste((0, 0, 0, 210), (pos[0] - 14, pos[1] + 12), tilt.getchannel("A"))
    c.alpha_composite(shadow_layer.filter(ImageFilter.GaussianBlur(18)))
    layer = Image.new("RGBA", c.size, (0, 0, 0, 0))
    layer.paste(tilt, pos, tilt)
    c.alpha_composite(layer)
    # clock photo, bottom left
    photo = punch(crop_fit(clock, CASE_CROP, (600, 324)), contrast=1.04, color=1.05)
    box = (46, 382, 646, 706)
    glow_behind(c, box, RED, blur=44, grow=20, alpha=225)
    shadow(c, box)
    c.alpha_composite(rounded(photo, 30), (box[0], box[1]))
    ImageDraw.Draw(c).rounded_rectangle(box, 30, outline=(255, 120, 120), width=4)
    # title
    text(c, (50, 24), "APP + CLOCK", font(IMPACT, 104), WHITE, stroke=7, shadow_px=6)
    text(c, (46, 124), "LIVE DEMO", font(IMPACT, 190), RED, stroke=9, shadow_px=8)
    text(c, (54, 326), "CONTROL IT FROM YOUR PHONE", font(BLACK, 40), (255, 200, 90))
    bluetooth(c, 712, 540, r=54)
    c.convert("RGB").save(out, quality=93, optimize=True)


def main():
    clock = Image.open(sys.argv[1]).convert("RGB")
    phone = Image.open(sys.argv[2]).convert("RGB")
    out = sys.argv[3] if len(sys.argv) > 3 else "."
    os.makedirs(out, exist_ok=True)
    build_thumb(clock, os.path.join(out, "thumbnail-build.jpg"))
    demo_thumb(clock, phone, os.path.join(out, "thumbnail-demo.jpg"))
    print("done")


if __name__ == "__main__":
    main()
