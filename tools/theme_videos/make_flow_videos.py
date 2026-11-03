#!/usr/bin/env python3
"""Adds the word, voice and "say it" pause on top of AI (Google Flow) clips, in Kannada or Hindi.

Usage: make_flow_videos.py <lang kn|hi> <clips dir> <voice dir> <out dir> [--only Apple] [--still t ...]
Clips are 720x1280 (9:16) and named Apple.mp4, Banana.mp4 ...
"""
import argparse, os, subprocess, sys, wave
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from make_videos import fnv, load_clip, tone, chime, pop_sfx, soothing, cheerful, SR

HERE = os.path.dirname(os.path.abspath(__file__))
W, H, FPS = 720, 1280, 30

FRUITS = [  # clip, kannada, kannada sentence, hindi, hindi sentence, english
    ("Apple", "ಸೇಬು", "ಇದು ಸೇಬು", "सेब", "यह सेब है", "Apple"),
    ("Banana", "ಬಾಳೆಹಣ್ಣು", "ಇದು ಬಾಳೆಹಣ್ಣು", "केला", "यह केला है", "Banana"),
    ("Mango", "ಮಾವಿನಹಣ್ಣು", "ಇದು ಮಾವಿನಹಣ್ಣು", "आम", "यह आम है", "Mango"),
    ("Grapes", "ದ್ರಾಕ್ಷಿ", "ಇದು ದ್ರಾಕ್ಷಿ", "अंगूर", "ये अंगूर हैं", "Grapes"),
    ("Orange", "ಕಿತ್ತಳೆ", "ಇದು ಕಿತ್ತಳೆ", "संतरा", "यह संतरा है", "Orange"),
    ("Watermelon", "ಕಲ್ಲಂಗಡಿ", "ಇದು ಕಲ್ಲಂಗಡಿ", "तरबूज़", "यह तरबूज़ है", "Watermelon"),
    ("Golden_pineapple", "ಅನಾನಸ್", "ಇದು ಅನಾನಸ್", "अनानास", "यह अनानास है", "Pineapple"),
    ("Lemons", "ನಿಂಬೆಹಣ್ಣು", "ಇದು ನಿಂಬೆಹಣ್ಣು", "नींबू", "यह नींबू है", "Lemon"),
    ("Coconut", "ತೆಂಗಿನಕಾಯಿ", "ಇದು ತೆಂಗಿನಕಾಯಿ", "नारियल", "यह नारियल है", "Coconut"),
    ("Strawberry", "ಸ್ಟ್ರಾಬೆರಿ", "ಇದು ಸ್ಟ್ರಾಬೆರಿ", "स्ट्रॉबेरी", "यह स्ट्रॉबेरी है", "Strawberry"),
]
BIRDS = [  # clip name starts with, kannada, kannada sentence, hindi, hindi sentence, english
    ("BIRDAnimated_pigeon", "", "", "कबूतर", "यह कबूतर है", "Pigeon"),
    ("BIRD_cBaby_eagle", "", "", "चील", "यह चील है", "Eagle"),
    ("BIRD_cBaby_owl", "", "", "उल्लू", "यह उल्लू है", "Owl"),
    ("BIRD_cBaby_swan", "", "", "हंस", "यह हंस है", "Swan"),
    ("BIRD_cCrane", "", "", "बगुला", "यह बगुला है", "Crane"),
    ("BIRD_cGreen_parrot", "", "", "तोता", "यह तोता है", "Parrot"),
    ("BIRD_cHen", "", "", "मुर्गी", "यह मुर्गी है", "Hen"),
    ("BIRD_cPeacock", "", "", "मोर", "यह मोर है", "Peacock"),
    ("BIRD_cSparrow", "", "", "गौरैया", "यह गौरैया है", "Sparrow"),
    ("BIRD_crow", "", "", "कौआ", "यह कौआ है", "Crow"),
]
SETS = {
    "fruits": dict(items=FRUITS, theme=dict(en="Fruits", ink="#7A2E0E", ink2="#B5541C", accent="#FF6B3D"),
                   native={"kn": "ಹಣ್ಣುಗಳು", "hi": "फल"}, cover=False),
    "birds": dict(items=BIRDS, theme=dict(en="Birds", ink="#0E4A73", ink2="#2378B0", accent="#1E90E0"),
                  native={"kn": "ಪಕ್ಷಿಗಳು", "hi": "पक्षी"}, cover=True),
}
LANG = {
    "kn": dict(native="ಹಣ್ಣುಗಳು", say="ನೀನೂ ಹೇಳು!", bravo="ಶಭಾಷ್!"),
    "hi": dict(native="फल", say="अब तुम बोलो!", bravo="शाबाश!"),
}
THEME = dict(en="Fruits", ink="#7A2E0E", ink2="#B5541C", accent="#FF6B3D")


def clip_audio(path, dur):
    raw = subprocess.run(["ffmpeg", "-v", "error", "-stream_loop", "3", "-i", path, "-t", f"{dur:.3f}",
                          "-f", "s16le", "-ac", "1", "-ar", str(SR), "-"], capture_output=True, check=True).stdout
    return np.frombuffer(raw, np.int16).astype(np.float32) / 32768


def plan(word, sentence, say, bravo, voice_dir):
    vw, vs = (load_clip(voice_dir, x) for x in (word, sentence))
    L = lambda a: len(a) / SR
    T = dict(word1=0.9)
    T["sentence"] = T["word1"] + L(vw) + 0.6
    # No "say it" part: ends calmly with the clip (those parts are kept off screen)
    T["end"] = max(T["sentence"] + L(vs) + 1.8, 6.0)
    T["say"] = T["pause"] = T["word2"] = T["bravo"] = 999.0
    T["pauseLen"] = 3.0
    tl = [(T["word1"] - 0.15, pop_sfx() * 0.6, "sfx"), (T["word1"], vw, "voice"), (T["sentence"], vs, "voice")]
    return T, tl


# Music for each fruit: the soundtrack Flow made for the Colours video of a matching colour
MUSIC = {"Apple": "Red", "Banana": "Yellow", "Mango": "Orange", "Grapes": "Purple", "Orange": "Pink",
         "Watermelon": "Green", "Golden_pineapple": "Blue", "Lemons": "White", "Coconut": "Fuzzy_brown", "Strawberry": "Black"}
BIRD_MUSIC = {"BIRDAnimated_pigeon": "White", "BIRD_cBaby_eagle": "Fuzzy_brown", "BIRD_cBaby_owl": "Purple", "BIRD_cBaby_swan": "Blue",
              "BIRD_cCrane": "Pink", "BIRD_cGreen_parrot": "Green", "BIRD_cHen": "Orange", "BIRD_cPeacock": "Yellow",
              "BIRD_cSparrow": "Red", "BIRD_crow": "Black"}
MUSIC_DIR = os.environ.get("FLOW_MUSIC_DIR", "")


def flow_music(clip, dur):
    name = os.path.splitext(os.path.basename(clip))[0]
    want = MUSIC.get(name) or next((v for k, v in BIRD_MUSIC.items() if name.startswith(k)), None)
    files = [f for f in sorted(os.listdir(MUSIC_DIR)) if f.startswith(f"COLOR_{want}_")] if want and MUSIC_DIR else []
    if not files:
        return cheerful(dur, len(clip))
    raw = subprocess.run(["ffmpeg", "-v", "error", "-stream_loop", "3", "-i", os.path.join(MUSIC_DIR, files[0]), "-t", f"{dur:.3f}",
                          "-f", "s16le", "-ac", "1", "-ar", str(SR), "-"], capture_output=True, check=True).stdout
    a = np.frombuffer(raw, np.int16).astype(np.float32) / 32768
    a = a * (0.12 / max(1e-6, np.sqrt(np.mean(a ** 2))))     # same loudness for every track
    out = np.zeros(int(dur * SR) + SR); out[:len(a)] = a
    return out


def build_audio(path, clip, tl, dur, amb_level=0.1):
    n = int(dur * SR)
    mix, voice = np.zeros(n + SR), np.zeros(n + SR)
    for t, a, kind in tl:
        s = int(t * SR); e = min(len(mix), s + len(a))
        (voice if kind == "voice" else mix)[s:e] += a[:e - s]
    amb = np.zeros(n + SR); a = clip_audio(clip, dur); amb[:len(a)] = a
    env = np.convolve(np.abs(voice), np.ones(4410) / 4410, mode="same")
    duck = 1 - 0.5 * np.clip(env * 25, 0, 1)
    fade = np.clip((dur - np.arange(len(mix)) / SR) / 0.8, 0, 1)
    # Only the clip's own (original Flow) sound under the voice: no added music
    m = np.zeros(len(mix))
    total = (mix + voice + amb * amb_level * duck + m * (1 - 0.55 * np.clip(env * 25, 0, 1))) * fade
    total = total * (0.9 / max(1e-6, np.max(np.abs(total))))
    with wave.open(path, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((total[:n] * 32767).astype(np.int16).tobytes())


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("lang", choices=["kn", "hi"]); ap.add_argument("clips"); ap.add_argument("voice"); ap.add_argument("out")
    ap.add_argument("--only"); ap.add_argument("--still", type=float, nargs="*"); ap.add_argument("--set", default="fruits", choices=list(SETS))
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    L = LANG[a.lang]
    from playwright.sync_api import sync_playwright
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={"width": W, "height": H})
        SET = SETS[a.set]
        for i, (clip, kn, kns, hi, his, en) in enumerate(SET["items"]):
            if a.only and not clip.startswith(a.only):
                continue
            word, sentence = (hi, his) if a.lang == "hi" else (kn, kns)
            T, tl = plan(word, sentence, L["say"], L["bravo"], a.voice)
            th = dict(SET["theme"], native=SET["native"][a.lang])
            spec = dict(theme=th, seed=i + 3, word=word, en=en, sentence=sentence, sayIt=L["say"], bravo=L["bravo"],
                        hindi=a.lang == "hi", cover=SET["cover"], times=T)
            page.goto("file://" + os.path.join(HERE, "overlay.html"))
            page.evaluate("document.fonts.ready")
            page.evaluate("s => setup(s)", spec)
            page.wait_for_timeout(300)
            src = os.path.join(a.clips, clip + ".mp4")
            if not os.path.exists(src):
                src = os.path.join(a.clips, next(f for f in sorted(os.listdir(a.clips)) if f.startswith(clip + "_") and f.endswith(".mp4")))
            name = f"{a.set}_{a.lang}_{i + 1:02d}"
            if a.still is not None:
                for t in a.still:
                    page.evaluate(f"render({t})")
                    ov = os.path.join(a.out, "_ov.png")
                    page.screenshot(path=ov, omit_background=True)
                    subprocess.run(["ffmpeg", "-v", "error", "-y", "-stream_loop", "3", "-i", src, "-i", ov, "-filter_complex",
                                    f"[0:v]trim=start={t % 6:.2f},setpts=PTS-STARTPTS[v];[v][1:v]overlay", "-frames:v", "1",
                                    os.path.join(a.out, f"{name}_{t:.1f}.png")], check=True)
                continue
            wav = os.path.join(a.out, name + ".wav")
            # Background: only the clip's original sound (no added music)
            build_audio(wav, src, tl, T["end"], 1.0)
            tmp = os.path.join(a.out, name + ".part.mp4")
            dur = T["end"]
            ff = subprocess.Popen([
                "ffmpeg", "-v", "error", "-y", "-stream_loop", "3", "-i", src,
                "-f", "image2pipe", "-framerate", str(FPS), "-c:v", "png", "-i", "-", "-i", wav,
                "-filter_complex",
                f"[0:v]fps={FPS},scale={W}:{H},trim=duration={dur:.3f},setpts=PTS-STARTPTS[bg];"
                f"[bg][1:v]overlay=format=auto,fade=t=out:st={dur - 0.5:.3f}:d=0.5,format=yuv420p[v]",
                "-map", "[v]", "-map", "2:a", "-c:v", "libx264", "-preset", "medium", "-crf", "22",
                "-c:a", "aac", "-b:a", "128k", "-ac", "2", "-t", f"{dur:.3f}", "-movflags", "+faststart", tmp],
                stdin=subprocess.PIPE)
            for f in range(int(dur * FPS)):
                page.evaluate(f"render({f / FPS})")
                ff.stdin.write(page.screenshot(type="png", omit_background=True))
            ff.stdin.close()
            if ff.wait() != 0:
                sys.exit("ffmpeg failed for " + name)
            os.replace(tmp, os.path.join(a.out, name + ".mp4"))
            os.remove(wav)
            print(f"{name}.mp4  {dur:.1f}s  {word}", flush=True)
        browser.close()


if __name__ == "__main__":
    main()
