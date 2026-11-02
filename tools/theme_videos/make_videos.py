#!/usr/bin/env python3
"""Makes one short video per theme word (Fruits, Vegetables, Body Parts, Family).

Usage: make_videos.py <ThemeWords.kt> <voice dir> <out dir> [--only Fruits:0] [--still t]
Voice clips are the app's own: <voice dir>/v_<fnv1a64(text)>.mp3
"""
import argparse, json, os, re, subprocess, sys, wave
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
W, H, FPS, SR = 1280, 720, 30, 44100

THEMES = {
    "Fruits":     dict(en="Fruits", bg1="#FFF3D6", bg2="#FFC98B", ink="#7A2E0E", ink2="#B5541C", accent="#FF6B3D",
                       blobs=["#FFE08A", "#FFB199", "#FFD1E0"]),
    "Vegetables": dict(en="Vegetables", bg1="#EFFBE3", bg2="#A8E08A", ink="#1F5A1A", ink2="#3D8A2F", accent="#3BAA35",
                       blobs=["#D7F5B5", "#FFF0A8", "#B9EDD5"]),
    "Body Parts": dict(en="Body Parts", bg1="#E6F6FF", bg2="#8FD0F5", ink="#0E4A73", ink2="#2378B0", accent="#1E90E0",
                       blobs=["#CDEBFF", "#FFE3F0", "#E0F7D4"]),
    "Family":     dict(en="My Family", bg1="#F6EEFF", bg2="#C9AEF2", ink="#4A2380", ink2="#7444B5", accent="#9B5DE5",
                       blobs=["#FFD6EC", "#E8DBFF", "#FFF1C1"]),
}


def fnv(text):
    h = 0xcbf29ce484222325
    for b in text.strip().encode():
        h ^= b; h = (h * 0x100000001b3) & 0xFFFFFFFFFFFFFFFF
    return f"{h:016x}"


def parse_words(kt):
    src = open(kt, encoding="utf-8").read()
    themes = []
    for m in re.finditer(r'WordTheme\("([^"]+)", "([^"]+)", listOf\((.*?)\n    \)\)', src, re.S):
        words = re.findall(r'ThemeWord\("([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)"\)', m.group(3))
        themes.append((m.group(1), m.group(2), words))
    return themes


def pic_file(emoji):
    cps = [f"{ord(c):x}" for c in emoji if ord(c) != 0xfe0f]
    return "emoji_u" + "_".join(cps) + ".png"


def layout(english, emoji):
    """Pictures on the left half: (file, size, x, y, tag)."""
    me = pic_file("🧒🏽")
    if english == "Family":
        files = [pic_file(e) for e in ["👨🏽", "👩🏽", "👧🏽", "👦🏽"]]
        return [dict(file=f, size=s, x=x, y=y, tag=None) for f, s, x, y in
                zip(files, [215, 205, 165, 155], [120, 290, 440, 575], [380, 385, 450, 458])]
    if english.startswith("Big "):
        return [dict(file=pic_file(emoji), size=340, x=250, y=360, tag=None),
                dict(file=me, size=190, x=520, y=470, tag="ನಾನು")]
    if english.startswith("Little "):
        return [dict(file=me, size=320, x=240, y=370, tag="ನಾನು"),
                dict(file=pic_file(emoji), size=200, x=510, y=470, tag=None)]
    return [dict(file=pic_file(emoji), size=420, x=340, y=380, tag=None)]


def load_clip(voice_dir, text):
    path = os.path.join(voice_dir, f"v_{fnv(text)}.mp3")
    if not os.path.exists(path):
        sys.exit(f"Missing voice clip for {text}: {path}")
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-f", "s16le", "-ac", "1", "-ar", str(SR), "-"],
                         capture_output=True, check=True).stdout
    a = np.frombuffer(raw, np.int16).astype(np.float32) / 32768
    # trim silence at both ends
    idx = np.where(np.abs(a) > 0.01)[0]
    return a[max(0, idx[0] - 800): idx[-1] + 2000] if len(idx) else a


def tone(freq, dur, vol=0.3, decay=6.0):
    t = np.arange(int(dur * SR)) / SR
    env = np.exp(-decay * t) * np.minimum(1, t * 200)
    return vol * env * (np.sin(2 * np.pi * freq * t) + 0.3 * np.sin(4 * np.pi * freq * t))


def pop_sfx():
    t = np.arange(int(0.18 * SR)) / SR
    f = 300 + 900 * t / 0.18
    return 0.35 * np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-18 * t)


def chime():
    out = np.zeros(int(1.2 * SR))
    for i, f in enumerate([1046.5, 1318.5, 1568.0, 2093.0]):
        s = int(i * 0.09 * SR); n = tone(f, 1.0, 0.16, 5)
        out[s:s + len(n)] += n[:len(out) - s]
    return out


def music(dur, seed):
    """A gentle original plucked tune (C major pentatonic) to sit under the voice."""
    rng = np.random.default_rng(seed)
    out = np.zeros(int(dur * SR) + SR)
    scale = [261.6, 293.7, 329.6, 392.0, 440.0, 523.3]
    beat = 0.375
    chords = [[130.8, 196.0], [110.0, 164.8], [87.3, 130.8], [98.0, 146.8]]
    n = int(dur / beat)
    for i in range(n):
        s = int(i * beat * SR)
        if i % 8 == 0:
            for f in chords[(i // 8) % 4]:
                x = tone(f, 3.0, 0.07, 1.2); out[s:s + len(x)] += x[:len(out) - s]
        if rng.random() < 0.7:
            f = scale[rng.integers(len(scale))]
            x = tone(f, 0.6, 0.05, 7); out[s:s + len(x)] += x[:len(out) - s]
    return out


def soothing(dur, seed=0):
    """Soft, calm original music: a warm pad (C - Am - F - G) with a slow music-box arpeggio."""
    n = int(dur * SR) + SR
    out = np.zeros(n)
    t_all = np.arange(n) / SR
    chords = [[261.6, 329.6, 392.0], [220.0, 261.6, 329.6], [174.6, 220.0, 261.6], [196.0, 246.9, 293.7]]
    chord_len = 2.4
    start = (seed % 4)
    for k in range(int(dur / chord_len) + 2):
        notes = chords[(start + k) % 4]
        s0 = int(k * chord_len * SR); L = int((chord_len + 1.2) * SR)
        if s0 >= n: break
        tt = np.arange(L) / SR
        env = np.minimum(1, tt / 0.6) * np.clip((chord_len + 1.2 - tt) / 1.2, 0, 1)
        pad = sum(np.sin(2 * np.pi * f / 2 * tt) + 0.5 * np.sin(2 * np.pi * f * 1.003 * tt) for f in notes)
        seg = 0.035 * env * pad
        e = min(n, s0 + L); out[s0:e] += seg[:e - s0]
        # music box: chord tones up an octave, one every 0.6 s
        for j, f in enumerate([notes[0] * 2, notes[1] * 2, notes[2] * 2, notes[1] * 2]):
            b0 = s0 + int(j * 0.6 * SR); bl = int(1.6 * SR); bt = np.arange(bl) / SR
            bell = (np.sin(2 * np.pi * f * bt) + 0.25 * np.sin(4 * np.pi * f * bt)) * np.exp(-3.2 * bt) * np.minimum(1, bt * 300)
            e = min(n, b0 + bl)
            if b0 < n: out[b0:e] += 0.045 * bell[:e - b0]
    return out * np.minimum(1, t_all / 0.8)


def build_audio(path, timeline, dur, seed):
    mix = np.zeros(int((dur + 1) * SR))
    voice = np.zeros_like(mix)
    for t, a, kind in timeline:
        s = int(t * SR); e = min(len(mix), s + len(a))
        (voice if kind == "voice" else mix)[s:e] += a[:e - s]
    m = music(dur, seed)[:len(mix)]
    # duck the music under the voice
    env = np.convolve(np.abs(voice), np.ones(4410) / 4410, mode="same")
    duck = 1 - 0.65 * np.clip(env * 25, 0, 1)
    fade = np.clip((dur - np.arange(len(mix)) / SR) / 1.0, 0, 1)
    total = (mix + voice * 1.0 + m * duck) * fade
    total = total * (0.9 / max(1e-6, np.max(np.abs(total))))
    with wave.open(path, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((total[:int(dur * SR)] * 32767).astype(np.int16).tobytes())


def plan(word, sentence, voice_dir):
    vw = load_clip(voice_dir, word)
    vs = load_clip(voice_dir, sentence)
    vy = load_clip(voice_dir, "ನೀನೂ ಹೇಳು!")
    vb = load_clip(voice_dir, "ಶಭಾಷ್!")
    L = lambda a: len(a) / SR
    T = dict(pic=0.5)
    T["word1"] = 1.3
    T["sentence"] = T["word1"] + L(vw) + 0.6
    T["say"] = T["sentence"] + L(vs) + 0.7
    T["pause"] = T["say"] + L(vy) + 0.2
    T["pauseLen"] = 3.0
    T["word2"] = T["pause"] + T["pauseLen"]
    T["bravo"] = T["word2"] + L(vw) + 0.6
    T["end"] = T["bravo"] + L(vb) + 1.8
    tl = [(0.0, chime() * 0.6, "sfx"), (T["pic"], pop_sfx(), "sfx"),
          (T["word1"], vw, "voice"), (T["sentence"], vs, "voice"), (T["say"], vy, "voice"),
          (T["pause"], tone(880, 0.25, 0.12, 12), "sfx"), (T["pause"] + 1, tone(880, 0.25, 0.12, 12), "sfx"),
          (T["pause"] + 2, tone(880, 0.25, 0.12, 12), "sfx"),
          (T["word2"], vw, "voice"), (T["bravo"], vb, "voice"), (T["bravo"] - 0.1, chime(), "sfx")]
    return T, tl


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("kt"); ap.add_argument("voice"); ap.add_argument("out")
    ap.add_argument("--only", help="Theme:index, e.g. Fruits:0")
    ap.add_argument("--still", type=float, nargs="*", help="write PNG frames at these times instead of a video")
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    from playwright.sync_api import sync_playwright
    jobs = []
    for folder, kn_title, words in parse_words(a.kt):
        for i, (kn, en, emoji, sentence) in enumerate(words):
            if a.only and a.only != f"{folder}:{i}":
                continue
            jobs.append((folder, kn_title, i, kn, en, emoji, sentence))
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": W, "height": H})
        for folder, kn_title, i, kn, en, emoji, sentence in jobs:
            T, tl = plan(kn, sentence, a.voice)
            th = dict(THEMES[folder]); th["kn"] = kn_title
            spec = dict(theme=th, seed=i * 7 + len(folder), kn=kn, en=en, sentence=sentence,
                        pics=layout(en, emoji), times=T)
            page.goto("file://" + os.path.join(HERE, "scene.html"))
            page.evaluate("document.fonts.ready")
            page.evaluate("s => setup(s)", spec)
            page.wait_for_timeout(300)
            name = f"{folder.lower().replace(' ', '_')}_{i + 1:02d}"
            if a.still is not None:
                for t in a.still:
                    page.evaluate(f"render({t})")
                    page.screenshot(path=os.path.join(a.out, f"{name}_{t:.1f}.png"))
                continue
            wav = os.path.join(a.out, name + ".wav")
            build_audio(wav, tl, T["end"], i)
            tmp = os.path.join(a.out, name + ".part.mp4")
            ff = subprocess.Popen(["ffmpeg", "-v", "error", "-y", "-f", "image2pipe", "-framerate", str(FPS), "-c:v", "mjpeg",
                                   "-i", "-", "-i", wav, "-c:v", "libx264", "-preset", "medium", "-crf", "23",
                                   "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "128k", "-ac", "2", "-shortest",
                                   "-movflags", "+faststart", tmp], stdin=subprocess.PIPE)
            for f in range(int(T["end"] * FPS)):
                page.evaluate(f"render({f / FPS})")
                ff.stdin.write(page.screenshot(type="jpeg", quality=92))
            ff.stdin.close()
            if ff.wait() != 0:
                sys.exit("ffmpeg failed for " + name)
            os.replace(tmp, os.path.join(a.out, name + ".mp4"))
            os.remove(wav)
            print(f"{name}.mp4  {T['end']:.1f}s  {kn}", flush=True)
        browser.close()


if __name__ == "__main__":
    main()
