import os, subprocess
import cv2, numpy as np
CLIPS = r"D:\My Project\Android\DotGlow\video\studio\assets\clips"; ORIG = os.path.join(CLIPS, "orig")
cap = cv2.VideoCapture(os.path.join(ORIG, "g-message.mp4")); cap.set(cv2.CAP_PROP_POS_MSEC, 200); ok, ref = cap.read(); cap.release()
TX0, TY0, TX1, TY1 = 350, 1040, 450, 1092                 # "WiFi" label + icon edge + FTTD2
tmpl = cv2.cvtColor(ref[TY0:TY1, TX0:TX1], cv2.COLOR_BGR2GRAY)
BX0, BY0, BX1, BY1 = 28, 25, 80, 46                         # FTTD2 only (the "WiFi" label stays sharp), inside the template
src = os.path.join(ORIG, "g-message.mp4"); dst = os.path.join(CLIPS, "g-message.mp4")
cap = cv2.VideoCapture(src); w, h, fps = int(cap.get(3)), int(cap.get(4)), cap.get(5)
enc = subprocess.Popen(["ffmpeg","-y","-v","error","-f","rawvideo","-pix_fmt","bgr24","-s",f"{w}x{h}","-r",str(fps),"-i","-",
                        "-an","-c:v","libx264","-crf","18","-preset","medium","-pix_fmt","yuv420p","-movflags","+faststart",dst], stdin=subprocess.PIPE)
last = None; since = 99; hits = 0; blurred = 0; n = 0; scores = []
while True:
    ok, fr = cap.read()
    if not ok: break
    n += 1
    g = cv2.cvtColor(fr, cv2.COLOR_BGR2GRAY)
    roi = g[700:1700, 150:900]
    res = cv2.matchTemplate(roi, tmpl, cv2.TM_CCOEFF_NORMED)
    _, mx, _, loc = cv2.minMaxLoc(res); scores.append(round(float(mx), 2))
    pos = None
    if mx >= 0.55:
        pos = (loc[0] + 150, loc[1] + 700); last = pos; since = 0; hits += 1; pad = 7
    elif last is not None and since < 8:
        pos = last; since += 1; pad = 22
    if pos is not None:
        x0, y0 = max(0, pos[0]+BX0-pad), max(0, pos[1]+BY0-pad); x1, y1 = min(w, pos[0]+BX1+pad), min(h, pos[1]+BY1+pad)
        patch = cv2.GaussianBlur(fr[y0:y1, x0:x1], (0, 0), 10)
        m = np.zeros((y1-y0, x1-x0), np.float32); cv2.rectangle(m, (5,5), (x1-x0-6, y1-y0-6), 1.0, -1)
        m = cv2.GaussianBlur(m, (0, 0), 4)[..., None]
        fr[y0:y1, x0:x1] = (patch*m + fr[y0:y1, x0:x1]*(1-m)).astype(np.uint8); blurred += 1
    enc.stdin.write(fr.tobytes())
enc.stdin.close(); enc.wait(); cap.release()
print(f"g-message: {n} frames, matched {hits}, blurred {blurred}"); print("scores:", scores[:40])
