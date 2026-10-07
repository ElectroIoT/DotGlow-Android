"""Overlay files for the split-screen demo video (1920x1080), for Filmora / any editor.

    python tools/make_demo_overlay.py [out-dir]       default: docs/youtube/overlay

split-background.png   dark red-dot background with a soft glow behind the phone (bottom track)
phone-frame.png        transparent PNG: phone bezel, camera hole, side buttons, see-through screen (top track)

Layout (pixels in a 1920x1080 project):
    left  : device video, crop/scale to fill x 0-1280 (1280 x 1080)
    right : phone screen recording scaled to 454 x 1008, top-left at x 1373, y 36 (under phone-frame.png)
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from make_thumbnails import background  # noqa: E402  (the same dark dot background as the thumbnails)

W, H = 1920, 1080
SCREEN = (454, 1008)          # 9:20 phone recording
BEZEL = 17
BODY = (SCREEN[0] + 2 * BEZEL, SCREEN[1] + 2 * BEZEL)
BODY_X = 1280 + (640 - BODY[0]) // 2
BODY_Y = (H - BODY[1]) // 2
SCREEN_XY = (BODY_X + BEZEL, BODY_Y + BEZEL)


def make_background(path):
    bg = background().resize((W, H), Image.LANCZOS).convert('RGBA')
    glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(glow).rounded_rectangle([BODY_X - 30, BODY_Y + 10, BODY_X + BODY[0] + 30, BODY_Y + BODY[1] - 10], 120,
                                           fill=(255, 60, 60, 120))
    bg.alpha_composite(glow.filter(ImageFilter.GaussianBlur(90)))
    d = ImageDraw.Draw(bg)
    d.line([(1280, 80), (1280, H - 80)], fill=(255, 255, 255, 40), width=2)   # thin divider
    bg.convert('RGB').save(path, quality=95)


def make_frame(path):
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    x0, y0 = BODY_X, BODY_Y
    d.rounded_rectangle([x0 + BODY[0] - 2, y0 + 220, x0 + BODY[0] + 8, y0 + 320], 5, fill=(70, 72, 80, 255))   # power
    d.rounded_rectangle([x0 + BODY[0] - 2, y0 + 360, x0 + BODY[0] + 8, y0 + 500], 5, fill=(70, 72, 80, 255))   # volume
    d.rounded_rectangle([x0, y0, x0 + BODY[0] - 1, y0 + BODY[1] - 1], 74, fill=(22, 23, 28, 255), outline=(120, 124, 136, 255), width=4)
    d.rounded_rectangle([x0 + 5, y0 + 5, x0 + BODY[0] - 6, y0 + BODY[1] - 6], 70, outline=(44, 46, 54, 255), width=3)
    hole = Image.new('L', (W, H), 0)                                        # cut the screen out: fully transparent
    ImageDraw.Draw(hole).rounded_rectangle([SCREEN_XY[0], SCREEN_XY[1], SCREEN_XY[0] + SCREEN[0] - 1, SCREEN_XY[1] + SCREEN[1] - 1], 56, fill=255)
    alpha = img.getchannel('A')
    alpha.paste(0, mask=hole)
    img.putalpha(alpha)
    d = ImageDraw.Draw(img)                                                 # camera hole sits on the screen, opaque
    cx = x0 + BODY[0] // 2
    d.ellipse([cx - 12, SCREEN_XY[1] + 14, cx + 12, SCREEN_XY[1] + 38], fill=(10, 10, 12, 255))
    d.ellipse([cx - 5, SCREEN_XY[1] + 21, cx + 5, SCREEN_XY[1] + 31], fill=(24, 30, 52, 255))
    img.save(path)


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'docs', 'youtube', 'overlay')
    os.makedirs(out, exist_ok=True)
    make_background(os.path.join(out, 'split-background.png'))
    make_frame(os.path.join(out, 'phone-frame.png'))
    print(f'screen area: {SCREEN[0]}x{SCREEN[1]} at x={SCREEN_XY[0]}, y={SCREEN_XY[1]}')


if __name__ == '__main__':
    main()
