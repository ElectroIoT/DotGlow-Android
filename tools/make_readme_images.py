"""README banner for the app: docs/banner.svg (animated, no external assets).

    python tools/make_readme_images.py

The LED digits are the clock's own 5x7 font (same as app/src/main/java/.../ui/matrix/Scenes.kt).
"""
import os

OUT = os.path.join(os.path.dirname(__file__), '..', 'docs')
os.makedirs(OUT, exist_ok=True)
RED, OFF = '#ff3b3b', '#2a0c0e'
FONT = "font-family='Segoe UI,Roboto,Helvetica,Arial,sans-serif'"
DIGITS = {
    1: [0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x0E], 2: [0x0E, 0x11, 0x01, 0x02, 0x04, 0x08, 0x1F],
    4: [0x02, 0x06, 0x0A, 0x12, 0x1F, 0x02, 0x02], 5: [0x1F, 0x10, 0x1E, 0x01, 0x01, 0x11, 0x0E],
    6: [0x06, 0x08, 0x10, 0x1E, 0x11, 0x11, 0x0E],
}


def px(d):
    return [(c, r) for r in range(7) for c in range(5) if DIGITS[d][r] & (0x10 >> c)]


def banner():
    W, H = 960, 380
    p = 7.2                      # dot pitch inside the phone
    mx, my = 92, 150             # matrix origin
    s = [f"<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 {W} {H}' width='{W}' height='{H}'>",
         "<defs><filter id='g' x='-50%' y='-50%' width='200%' height='200%'><feGaussianBlur stdDeviation='1.6' result='b'/>"
         "<feMerge><feMergeNode in='b'/><feMergeNode in='SourceGraphic'/></feMerge></filter>"
         "<linearGradient id='bg' x1='0' y1='0' x2='1' y2='1'><stop offset='0' stop-color='#13151b'/><stop offset='1' stop-color='#1f1016'/></linearGradient>"
         f"<clipPath id='cell'><rect x='{mx + 25 * p}' y='{my}' width='{5 * p}' height='{7 * p}'/></clipPath></defs>",
         f"<rect width='{W}' height='{H}' rx='22' fill='url(#bg)'/>"]
    # phone
    s.append("<rect x='60' y='30' width='300' height='330' rx='34' fill='#0b0c10' stroke='#5b6170' stroke-width='3'/>")
    s.append("<rect x='76' y='58' width='268' height='290' rx='18' fill='#0f1116'/>")
    s.append(f"<text x='92' y='92' {FONT} font-size='17' font-weight='700' fill='#e9ebf1'>DotGlow-EEA8</text>")
    s.append(f"<circle cx='96' cy='118' r='3.5' fill='{RED}'><animate attributeName='opacity' values='1;0.2;1' dur='1.6s' repeatCount='indefinite'/></circle>")
    s.append(f"<text x='106' y='122' {FONT} font-size='10' letter-spacing='1.4' fill='#8b91a1'>LIVE</text>")
    s.append(f"<rect x='{mx - 8}' y='{my - 8}' width='{32 * p + 16}' height='{8 * p + 16}' rx='9' fill='#050506'/>")
    s += [f"<circle cx='{mx + (x + .5) * p:.1f}' cy='{my + (y + .5) * p:.1f}' r='{p * .36:.2f}' fill='{OFF}'/>" for y in range(8) for x in range(32)]
    s.append("<g filter='url(#g)'>")
    dx = [2, 8, 19, 25]
    for i, d in enumerate([1, 2, 4]):
        s += [f"<circle cx='{mx + (dx[i] + c + .5) * p:.1f}' cy='{my + (r + .5) * p:.1f}' r='{p * .36:.2f}' fill='{RED}'/>" for c, r in px(d)]
    s.append("<g><animate attributeName='opacity' values='1;0' dur='1s' calcMode='discrete' repeatCount='indefinite'/>")
    s += [f"<circle cx='{mx + (15 + c + .5) * p:.1f}' cy='{my + (r + .5) * p:.1f}' r='{p * .36:.2f}' fill='{RED}'/>" for c in (0, 1) for r in (1, 2, 4, 5)]
    s.append("</g><g clip-path='url(#cell)'><g>")
    s.append(f"<animateTransform attributeName='transform' type='translate' values='0 0;0 0;0 {8 * p:.1f};0 {8 * p:.1f}' "
             "keyTimes='0;0.5;0.57;1' dur='6s' repeatCount='indefinite'/>")
    s += [f"<circle cx='{mx + (25 + c + .5) * p:.1f}' cy='{my + (r + .5) * p:.1f}' r='{p * .36:.2f}' fill='{RED}'/>" for c, r in px(5)]
    s += [f"<circle cx='{mx + (25 + c + .5) * p:.1f}' cy='{my + (r - 8 + .5) * p:.1f}' r='{p * .36:.2f}' fill='{RED}'/>" for c, r in px(6)]
    s.append('</g></g></g>')
    # app-like cards under the matrix
    for k, (t, w) in enumerate((('Brightness', 0.55), ('Animations', 0.8), ('Stopwatch', 0.35))):
        y = 238 + k * 34
        s.append(f"<rect x='92' y='{y}' width='236' height='26' rx='8' fill='#171a21'/>")
        s.append(f"<text x='102' y='{y + 17}' {FONT} font-size='11' fill='#cfd3dc'>{t}</text>")
        s.append(f"<rect x='186' y='{y + 10}' width='130' height='6' rx='3' fill='#2a2e38'/>")
        s.append(f"<rect x='186' y='{y + 10}' width='{130 * w:.0f}' height='6' rx='3' fill='{RED}'>"
                 f"<animate attributeName='width' values='{130 * w:.0f};{130 * (1 - w / 2):.0f};{130 * w:.0f}' dur='{3 + k}s' repeatCount='indefinite'/></rect>")
    # Bluetooth waves from the phone
    for k in range(3):
        s.append(f"<path d='M372 {150 + k * 0} q 18 45 0 90' fill='none' stroke='#4da3ff' stroke-width='3' opacity='0'>"
                 f"<animate attributeName='opacity' values='0;0.9;0' dur='1.8s' begin='{k * 0.6}s' repeatCount='indefinite'/>"
                 f"<animateTransform attributeName='transform' type='translate' values='0 0;30 0' dur='1.8s' begin='{k * 0.6}s' repeatCount='indefinite'/></path>")
    # title + chips
    s.append(f"<text x='440' y='130' {FONT} font-size='52' font-weight='800' fill='#f2f3f7'>DotGlow</text>")
    s.append(f"<text x='440' y='168' {FONT} font-size='21' fill='{RED}' font-weight='600'>LED Matrix Clock for Android</text>")
    s.append(f"<text x='440' y='204' {FONT} font-size='16' fill='#9aa0ad'>Set up, control and update ESP32 matrix clocks over Bluetooth.</text>")
    chips = ['Bluetooth setup', 'Live LED previews', 'WiFi from the app', 'USB firmware installer',
             'Updates over BLE', 'Stopwatch', 'App PIN lock', 'Demo clock']
    x, y = 440, 236
    for c in chips:
        w = 16 + len(c) * 7.6
        if x + w > W - 30:
            x, y = 440, y + 40
        s.append(f"<rect x='{x}' y='{y}' width='{w:.0f}' height='30' rx='15' fill='#1c1f28' stroke='#343947'/>")
        s.append(f"<text x='{x + w / 2:.0f}' y='{y + 20}' text-anchor='middle' {FONT} font-size='13' fill='#dfe2ea'>{c}</text>")
        x += w + 10
    s.append('</svg>')
    return '\n'.join(s)


with open(os.path.join(OUT, 'banner.svg'), 'w', encoding='utf-8', newline='\n') as f:
    f.write(banner() + '\n')
print('banner.svg', os.path.getsize(os.path.join(OUT, 'banner.svg')) // 1024, 'KB')
