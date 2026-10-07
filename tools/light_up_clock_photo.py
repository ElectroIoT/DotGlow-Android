"""Light up a photo of an unlit DotGlow clock: draws glowing red digits exactly on the LED dots of the photo.

    python tools/light_up_clock_photo.py <photo.jpg> <out.jpg> --roi x0,y0,x1,y1 --seams x1,x2,x3 [--text 10:55]

roi:    a box around the 4 display modules (a bit larger than the dots).
seams:  x position of the 3 gaps between the 4 modules (pixels in the photo).
The program finds the beige LED lenses, fits each 8x8 module's dot grid (perspective is handled per module),
darkens the unlit lenses and draws the lit ones with a bloom. The top row of dots is usually half hidden by the
case lip, so digits use rows 1-7. Needs: pip install opencv-python numpy.

Example (docs/youtube/photos/clock-day-front.jpg):
    python tools/light_up_clock_photo.py docs/youtube/photos/clock-day-front.jpg docs/youtube/photos/clock-day-lit.jpg \
        --roi 462,362,1545,622 --seams 735,998,1245
"""
import argparse

import cv2
import numpy as np

FONT = {  # 5x7, rows top to bottom, bit 4 = left column
    '0': [14, 17, 19, 21, 25, 17, 14], '1': [4, 12, 4, 4, 4, 4, 14], '2': [14, 17, 1, 2, 4, 8, 31],
    '3': [30, 1, 1, 14, 1, 1, 30], '4': [2, 6, 10, 18, 31, 2, 2], '5': [31, 16, 30, 1, 1, 17, 14],
    '6': [6, 8, 16, 30, 17, 17, 14], '7': [31, 1, 2, 4, 8, 8, 8], '8': [14, 17, 17, 14, 17, 17, 14],
    '9': [14, 17, 17, 15, 1, 2, 12]}


def lit_pixels(text, top=1):
    """Set of (col, row) lit on a 32x8 display: 4 digits (5x7) and the colon; rows start at `top`."""
    lit = set()
    for ch, x0 in zip([c for c in text if c.isdigit()], [1, 7, 19, 25]):
        for r, bits in enumerate(FONT[ch]):
            for c in range(5):
                if bits >> (4 - c) & 1:
                    lit.add((x0 + c, r + top))
    for r in (2, 3, 5, 6):
        lit.add((15, r + top))
        lit.add((16, r + top))
    return lit


def detect_dots(img, roi, amin=160, amax=700):
    """Centres of the fully visible beige LED lenses (local-contrast threshold: the frame is bright too)."""
    x0, y0, x1, y1 = roi
    g = cv2.GaussianBlur(cv2.cvtColor(img[y0:y1, x0:x1], cv2.COLOR_BGR2GRAY), (3, 3), 0)
    m = cv2.adaptiveThreshold(g, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 35, -18)
    m = cv2.morphologyEx(m, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
    n, lab, st, ce = cv2.connectedComponentsWithStats(m)
    pts = []
    for i in range(1, n):
        a, w, h = st[i, cv2.CC_STAT_AREA], st[i, cv2.CC_STAT_WIDTH], st[i, cv2.CC_STAT_HEIGHT]
        if amin <= a <= amax and 0.7 < w / h < 1.5 and 12 <= w <= 34 and 12 <= h <= 34:
            pts.append((ce[i][0] + x0, ce[i][1] + y0))
    return np.array(pts)


def blob_mask(img, roi, amin=60, amax=800):
    """Mask of all beige lenses, including the half-hidden ones."""
    x0, y0, x1, y1 = roi
    g = cv2.GaussianBlur(cv2.cvtColor(img[y0:y1, x0:x1], cv2.COLOR_BGR2GRAY), (3, 3), 0)
    m = cv2.adaptiveThreshold(g, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 35, -18)
    m = cv2.morphologyEx(m, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
    n, lab, st, _ = cv2.connectedComponentsWithStats(m)
    out = np.zeros(img.shape[:2], np.uint8)
    for i in range(1, n):
        a, w, h = st[i, cv2.CC_STAT_AREA], st[i, cv2.CC_STAT_WIDTH], st[i, cv2.CC_STAT_HEIGHT]
        if amin <= a <= amax and w <= 36 and h <= 36:
            out[y0:y1, x0:x1][lab == i] = 255
    return out


def module_lattice(P, n=8):
    """Index one module's dots from their own geometry (two neighbour vectors) and fit local cell -> pixel."""
    P = np.asarray(P, np.float64)
    d = np.linalg.norm(P[:, None] - P[None], axis=2)
    np.fill_diagonal(d, 1e9)
    nn = np.median(d.min(1))
    hv, vv = [], []
    for i in range(len(P)):
        for j in np.where(d[i] < nn * 1.45)[0]:
            v = P[j] - P[i]
            if abs(v[0]) > abs(v[1]):
                hv.append(v if v[0] > 0 else -v)
            else:
                vv.append(v if v[1] > 0 else -v)
    M = np.array([np.median(hv, 0), np.median(vv, 0)]).T
    ref = P[np.argmin(P[:, 0] + P[:, 1])]
    ab = np.linalg.solve(M, (P - ref).T).T
    ab -= ab.min(0)
    idx = np.round(ab)
    ok = (np.abs(ab - idx).max(1) < 0.3) & (idx.max(1) < n)
    H, _ = cv2.findHomography(idx[ok].astype(np.float32), P[ok].astype(np.float32), cv2.RANSAC, 3.0)
    pred = cv2.perspectiveTransform(idx[ok].reshape(-1, 1, 2).astype(np.float32), H).reshape(-1, 2)
    return H, float(np.linalg.norm(pred - P[ok], axis=1).mean()), idx[ok]


def centers(img, roi, seams, cols=32, rows=8, mod=8):
    pts = detect_dots(img, roi)
    edges = [-1e9] + list(seams) + [1e9]
    cen = {}
    for k in range(len(edges) - 1):
        sel = pts[(pts[:, 0] >= edges[k]) & (pts[:, 0] < edges[k + 1])]
        H, err, idx = module_lattice(sel)
        # the lattice starts at the first visible row; the top row is hidden by the lip, so physical row = lattice row + 1
        rows_seen = int(idx[:, 1].max()) + 1
        shift = rows - rows_seen
        print(f'module {k + 1}: {len(sel)} dots, mean fit error {err:.2f} px, {rows_seen} visible rows')
        for c in range(mod):
            for r in range(rows):
                p = cv2.perspectiveTransform(np.float32([[[c, r - shift]]]), H)[0, 0]
                cen[(k * mod + c, r)] = (float(p[0]), float(p[1]))
    return cen


def render(img, cen, lit, blobs, dot_w=20, dot_h=23, dim=0.42, glow=1.0, skip_rows=(0,)):
    """Unlit lenses darkened, lit LEDs saturated red with a hot centre, soft bloom on the case."""
    h, w = img.shape[:2]
    core = np.zeros((h, w, 3), np.float32)
    body = np.zeros((h, w, 3), np.float32)
    litmask = np.zeros((h, w), np.uint8)
    excl = np.zeros((h, w), np.uint8)
    for (c, r), (x, y) in cen.items():
        if r in skip_rows:
            cv2.ellipse(excl, (int(round(x)), int(round(y))), (int(dot_w * .9), int(dot_h * .9)), 0, 0, 360, 255, -1)
    for (c, r) in lit:
        x, y = cen[(c, r)]
        cv2.ellipse(body, (int(round(x)), int(round(y))), (dot_w // 2 + 2, dot_h // 2 + 2), 0, 0, 360, (35, 45, 255), -1, cv2.LINE_AA)
        cv2.ellipse(core, (int(round(x)), int(round(y))), (dot_w // 5, dot_h // 5), 0, 0, 360, (150, 190, 255), -1, cv2.LINE_AA)
        cv2.circle(litmask, (int(round(x)), int(round(y))), int(dot_w * .62), 255, -1)
    off = cv2.dilate(blobs, np.ones((5, 5), np.uint8)).astype(np.float32) / 255.0 * (litmask == 0)
    off[excl > 0] = 0                                                      # the half-hidden top row stays as photographed
    out = img.astype(np.float32)
    om = cv2.GaussianBlur(off, (0, 0), 0.9)[..., None]
    out = out * (1 - om) + (out * dim + np.array([14, 10, 12], np.float32)) * om
    bm = cv2.GaussianBlur((body.max(2) > 0).astype(np.float32), (0, 0), 0.8)[..., None]
    out = out * (1 - bm) + body * bm
    out += (cv2.GaussianBlur(body, (0, 0), 6) * .8 + cv2.GaussianBlur(body, (0, 0), 20) * 1.1
            + cv2.GaussianBlur(body, (0, 0), 55) * .8) * glow
    cm = cv2.GaussianBlur((core.max(2) > 0).astype(np.float32), (0, 0), 1.4)[..., None] * .85
    out = out * (1 - cm) + core * cm
    return np.clip(out, 0, 255).astype(np.uint8)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('photo')
    ap.add_argument('out')
    ap.add_argument('--roi', required=True)
    ap.add_argument('--seams', required=True)
    ap.add_argument('--text', default='10:55')
    a = ap.parse_args()
    img = cv2.imread(a.photo)
    roi = tuple(int(v) for v in a.roi.split(','))
    seams = [int(v) for v in a.seams.split(',')]
    cen = centers(img, roi, seams)
    out = render(img, cen, lit_pixels(a.text), blob_mask(img, roi))
    cv2.imwrite(a.out, out, [cv2.IMWRITE_JPEG_QUALITY, 95])
    print('written', a.out)


if __name__ == '__main__':
    main()
