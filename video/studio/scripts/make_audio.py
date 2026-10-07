"""Original music + sound effects for the DotGlow Short (numpy only, so nothing is copyrighted by anyone else).

    python scripts/make_audio.py      ->  assets/audio/music.wav, sfx-*.wav   (44.1 kHz stereo 16-bit)

Music: 120 BPM, A minor (Am F C G), 32 s: arp + pad intro, riser into a drop at 3.0 s (kick, bass), hats, claps, fade-out.
"""
import os
import wave

import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'assets', 'audio')
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(7)


def save(name, x, peak=0.89):
    x = np.asarray(x, np.float64)
    if x.ndim == 1:
        x = np.stack([x, x], 1)
    m = np.abs(x).max()
    if m > 0:
        x = x / m * peak
    pcm = (np.clip(x, -1, 1) * 32767).astype('<i2')
    with wave.open(os.path.join(OUT, name), 'wb') as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def tt(d):
    return np.arange(int(d * SR)) / SR


def hp_noise(n):                      # crude high-pass noise (first difference)
    z = rng.standard_normal(n + 1)
    return np.diff(z) * 0.7


def add(buf, start, sig, pan=0.0, gain=1.0):
    i = int(start * SR)
    if i >= len(buf):
        return
    e = min(len(buf), i + len(sig))
    l, r = np.cos((pan + 1) * np.pi / 4), np.sin((pan + 1) * np.pi / 4)
    buf[i:e, 0] += sig[:e - i] * l * gain
    buf[i:e, 1] += sig[:e - i] * r * gain


def saw(f, t, harmonics=8):
    return sum(np.sin(2 * np.pi * f * k * t) / k for k in range(1, harmonics + 1)) * 0.6


# ---------------------------------------------------------------- sound effects

def sfx_whoosh(d=0.5, up=True):
    n = int(d * SR)
    x = rng.standard_normal(n)
    out = np.zeros(n)
    a = 0.0
    for i in range(n):                # time-varying one-pole low-pass: sweeps up (or down)
        p = i / n
        c = 0.02 + 0.55 * (p if up else 1 - p) ** 1.5
        a += c * (x[i] - a)
        out[i] = a
    env = np.sin(np.pi * np.linspace(0, 1, n)) ** 1.6
    return out * env


def sfx_pop(f0=900, f1=260, d=0.14):
    t = tt(d)
    f = f1 + (f0 - f1) * np.exp(-t * 40)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 28)


def sfx_click():
    t = tt(0.06)
    return (np.sin(2 * np.pi * 2400 * t) * np.exp(-t * 160) + hp_noise(len(t)) * np.exp(-t * 220) * 0.4)


def sfx_ding(f=880, d=1.4):
    t = tt(d)
    return (np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * f * 2.76 * t) + 0.25 * np.sin(2 * np.pi * f * 5.4 * t)) * np.exp(-t * 3.2)


def sfx_powerup(d=0.9):
    t = tt(d)
    f = 180 * (1 + 5.5 * (t / d) ** 2)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.6 + saw(f, t, 4) * 0.25
    return x * np.minimum(1, t / 0.05) * np.exp(-np.maximum(0, t - 0.6) * 12)


def sfx_zap(d=0.3):
    n = int(d * SR)
    t = tt(d)
    f = rng.choice([300, 520, 880, 1400, 2100], size=12)
    steps = np.repeat(f, n // 12 + 1)[:n]
    return np.sign(np.sin(2 * np.pi * np.cumsum(steps) / SR)) * 0.4 * np.exp(-t * 7)


def sfx_impact(d=1.2):
    t = tt(d)
    f = 48 + 90 * np.exp(-t * 14)
    return (np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 3.2) + hp_noise(len(t)) * np.exp(-t * 18) * 0.5)


def sfx_swipe(d=0.35):
    return sfx_whoosh(d, True) * 0.9


for name, sig in [('sfx-whoosh.wav', sfx_whoosh(0.55, True)), ('sfx-whoosh-down.wav', sfx_whoosh(0.5, False)),
                  ('sfx-pop.wav', sfx_pop()), ('sfx-click.wav', sfx_click()), ('sfx-ding.wav', sfx_ding()),
                  ('sfx-powerup.wav', sfx_powerup()), ('sfx-zap.wav', sfx_zap()), ('sfx-impact.wav', sfx_impact()),
                  ('sfx-swipe.wav', sfx_swipe())]:
    save(name, sig)

# ---------------------------------------------------------------- music
BPM = 120
BEAT = 60 / BPM
BAR = 4 * BEAT
TOTAL = 32.0
n = int(TOTAL * SR)
mix = np.zeros((n, 2))
pad_b = np.zeros((n, 2))
low_b = np.zeros((n, 2))
arp_b = np.zeros((n, 2))
dr_b = np.zeros((n, 2))

CHORDS = [  # pad tones (Hz), bass root (Hz)
    ([220.0, 261.63, 329.63], 110.0),     # Am
    ([174.61, 220.0, 261.63], 87.31),     # F
    ([261.63, 329.63, 392.0], 130.81),    # C
    ([196.0, 246.94, 293.66], 98.0),      # G
]
KICK_FROM, HAT_FROM, CLAP_FROM = 3.0, 5.0, 7.0

kicks = []
for bar in range(int(TOTAL / BAR)):
    tones, root = CHORDS[bar % 4]
    t0 = bar * BAR
    # pad: three detuned saws per tone, slow attack
    d = BAR + 0.5
    tp = tt(d)
    env = np.minimum(1, tp / 0.45) * np.minimum(1, (d - tp) / 0.4)
    pad = sum(saw(f * c, tp, 6) for f in tones for c in (0.997, 1.0, 1.004)) / 9.0 * env
    add(pad_b, t0, pad, 0.0, 0.5)
    # arp: 16th plucks across the chord, octave up
    seq = [0, 1, 2, 1, 0, 1, 2, 1, 0, 2, 1, 2, 0, 1, 2, 1]
    for k, idx in enumerate(seq):
        ts = t0 + k * BEAT / 4
        f = tones[idx] * 2
        tp = tt(0.35)
        pl = (np.sin(2 * np.pi * f * tp) + 0.35 * np.sin(2 * np.pi * f * 2 * tp)) * np.exp(-tp * 13)
        add(arp_b, ts, pl, -0.4 + 0.8 * ((k % 4) / 3), 0.55 if t0 >= 4 else 0.35)
    for beat in range(4):
        tb = t0 + beat * BEAT
        if tb >= KICK_FROM:                                   # kick
            tk = tt(0.28)
            f = 45 + 95 * np.exp(-tk * 30)
            add(dr_b, tb, np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tk * 11), 0, 1.0)
            kicks.append(tb)
        if tb >= CLAP_FROM and beat in (1, 3):                # clap
            tc = tt(0.22)
            cl = hp_noise(len(tc)) * (np.exp(-tc * 28) + 0.6 * np.exp(-np.maximum(0, tc - 0.02) * 60) * (tc > 0.02))
            add(dr_b, tb, cl, 0.1, 0.38)
        for half in (0, 1):                                   # off-beat hat + bass
            th = tb + half * BEAT / 2
            if th >= HAT_FROM and half == 1:
                ts_ = tt(0.07)
                add(dr_b, th, hp_noise(len(ts_)) * np.exp(-ts_ * 75), 0.25, 0.20)
            if th >= KICK_FROM and not (beat == 3 and half == 1):
                tb_ = tt(0.4)
                fb = root * (2 if (beat == 2 and half == 1) else 1)
                bs = (np.sin(2 * np.pi * fb * tb_) + 0.3 * np.sin(2 * np.pi * fb * 2 * tb_)) * np.exp(-tb_ * 4.5)
                add(low_b, th, bs, 0, 0.55)

# riser into the drop (1.4 s -> 3.0 s) and an impact on it
tr = tt(1.6)
ramp = (tr / 1.6) ** 2.2
riser = hp_noise(len(tr)) * ramp * 0.5 + np.sin(2 * np.pi * np.cumsum(220 + 1500 * ramp) / SR) * ramp * 0.18
add(dr_b, 1.4, riser, 0, 0.7)
add(dr_b, 3.0, sfx_impact(1.0), 0, 0.9)

# sidechain: pad / bass / arp duck on every kick
sc = np.ones(n)
for tk in kicks:
    i = int(tk * SR)
    seg = min(int(0.35 * SR), n - i)
    if seg > 0:
        sc[i:i + seg] = np.minimum(sc[i:i + seg], 1 - 0.6 * np.exp(-np.arange(seg) / SR / 0.09))
mix = (pad_b * 0.62 + arp_b * 0.9 + low_b * 0.9) * sc[:, None] + dr_b
mix = np.tanh(mix * 1.15) / 1.15
fade = np.ones(n)
fi, fo = int(0.08 * SR), int(2.6 * SR)
fade[:fi] = np.linspace(0, 1, fi)
fade[-fo:] = np.linspace(1, 0, fo) ** 1.5
save('music.wav', mix * fade[:, None], peak=0.85)
print('audio written to', os.path.abspath(OUT))
