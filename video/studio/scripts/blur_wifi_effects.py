import os, shutil, subprocess
import cv2, numpy as np

CLIPS = r"D:\My Project\Android\DotGlow\video\studio\assets\clips"
ORIG = os.path.join(CLIPS, "orig")
os.makedirs(ORIG, exist_ok=True)
for n in ("e-effects", "g-message"):
    if not os.path.exists(os.path.join(ORIG, n + ".mp4")):
        shutil.copy(os.path.join(CLIPS, n + ".mp4"), os.path.join(ORIG, n + ".mp4"))

# template: the "WiFi / FTTD2" row of the app (taken from e-effects at 0.5 s, 1080x1920 coordinates)
cap = cv2.VideoCapture(os.path.join(ORIG, "e-effects.mp4"))
cap.set(cv2.CAP_PROP_POS_MSEC, 500)
ok, ref = cap.read(); cap.release()
TX0, TY0, TX1, TY1 = 340, 1455, 440, 1520
tmpl = cv2.cvtColor(ref[TY0:TY1, TX0:TX1], cv2.COLOR_BGR2GRAY)
# FTTD2 text inside the template (blur box, with margin)
BX0, BY0, BX1, BY1 = 12, 24, 96, 62
cv2.imwrite(r"C:\Users\manor\AppData\Local\Temp\claude\D--My-Project\83452c4f-c78b-4895-8187-b3550a09056f\scratchpad\tmpl.png", ref[TY0:TY1, TX0:TX1])

def process(name, thr=0.62):
    src = os.path.join(ORIG, name + ".mp4"); dst = os.path.join(CLIPS, name + ".mp4")
    cap = cv2.VideoCapture(src)
    w, h = int(cap.get(3)), int(cap.get(4)); fps = cap.get(5)
    enc = subprocess.Popen(["ffmpeg","-y","-v","error","-f","rawvideo","-pix_fmt","bgr24","-s",f"{w}x{h}","-r",str(fps),"-i","-",
                            "-an","-c:v","libx264","-crf","18","-preset","medium","-pix_fmt","yuv420p","-movflags","+faststart",dst], stdin=subprocess.PIPE)
    last = None; since = 99; hits = 0; n = 0; blurred = 0
    while True:
        ok, fr = cap.read()
        if not ok: break
        n += 1
        g = cv2.cvtColor(fr, cv2.COLOR_BGR2GRAY)
        roi = g[700:1850, 150:900]
        res = cv2.matchTemplate(roi, tmpl, cv2.TM_CCOEFF_NORMED)
        _, mx, _, loc = cv2.minMaxLoc(res)
        pos = None
        if mx >= thr:
            pos = (loc[0] + 150, loc[1] + 700); last = pos; since = 0; hits += 1; pad = 8
        elif last is not None and since < 10:
            pos = last; since += 1; pad = 26            # lost it for a moment (motion blur / finger): keep covering the last place
        if pos is not None:
            x0, y0 = pos[0] + BX0 - pad, pos[1] + BY0 - pad
            x1, y1 = pos[0] + BX1 + pad, pos[1] + BY1 + pad
            x0, y0, x1, y1 = max(0,x0), max(0,y0), min(w,x1), min(h,y1)
            patch = cv2.GaussianBlur(fr[y0:y1, x0:x1], (0, 0), 11)
            mask = np.zeros((y1-y0, x1-x0), np.float32); cv2.rectangle(mask, (6,6), (x1-x0-7, y1-y0-7), 1.0, -1)
            mask = cv2.GaussianBlur(mask, (0, 0), 5)[..., None]
            fr[y0:y1, x0:x1] = (patch*mask + fr[y0:y1, x0:x1]*(1-mask)).astype(np.uint8)
            blurred += 1
        enc.stdin.write(fr.tobytes())
    enc.stdin.close(); enc.wait(); cap.release()
    print(f"{name}: {n} frames, matched {hits}, blurred {blurred}")

process("e-effects"); process("g-message")
