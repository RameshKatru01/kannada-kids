#!/usr/bin/env python3
"""Hindi vowel videos in the Kannada vowel style: big yellow letter at the top, the word in white at
the bottom (both with a thick black outline), over the Flow clip with its own sound.
Usage: vowel_overlay.py <clips dir> <out dir> [--only N]"""
import argparse, os, subprocess, sys, wave
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from make_videos import load_clip, SR
from playwright.sync_api import sync_playwright

ITEMS = [("Pomegranate", "अ", "अनार", "Pomegranate", "Anaar means pomegranate."),
         ("Animated_mango", "आ", "आम", "Mango", "Aam means mango."),
         ("Tamarind", "इ", "इमली", "Tamarind", "Imli means tamarind."),
         ("Animated_sugarcane", "ई", "ईख", "Sugarcane", "Eekh means sugarcane."),
         ("Baby_owl_waving", "उ", "उल्लू", "Owl", "Ullu means owl."),
         ("Wool", "ऊ", "ऊन", "Wool", "Oon means wool."),
         ("Sage", "ऋ", "ऋषि", "Sage", "Rishi means a wise sage."),
         ("Toddler", "ए", "एड़ी", "Heel", "Edi means heel."),
         ("Animated_spectacles", "ऐ", "ऐनक", "Spectacles", "Ainak means spectacles."),
         ("Animated_mortar", "ओ", "ओखली", "Grinding bowl", "Okhli means a grinding stone bowl."),
         ("Woman", "औ", "औरत", "Woman", "Aurat means woman."),
         ("Smiling_grape", "अं", "अंगूर", "Grapes", "Angoor means grapes.")]
VOICE = os.environ.get("VOICE_DIR", "")


def build_audio(path, clip, hi, en, dur):
    """Flow's own sound, a little quieter under the Hindi line (at 0.5 s) and the English line after it."""
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", clip, "-t", f"{dur:.3f}", "-f", "s16le", "-ac", "1", "-ar", str(SR), "-"],
                         capture_output=True, check=True).stdout
    amb = np.frombuffer(raw, np.int16).astype(np.float32) / 32768
    n = int(dur * SR); bg = np.zeros(n); bg[:min(n, len(amb))] = amb[:n]
    vh, ve = load_clip(VOICE, hi), load_clip(VOICE, en)
    t_hi = 0.5; t_en = t_hi + len(vh) / SR + 0.45
    voice = np.zeros(n)
    for t, a in ((t_hi, vh), (t_en, ve)):
        s0 = int(t * SR); e = min(n, s0 + len(a)); voice[s0:e] += a[:e - s0]
    env = np.convolve(np.abs(voice), np.ones(4410) / 4410, mode="same")
    mix = bg * (1 - 0.6 * np.clip(env * 25, 0, 1)) + voice
    mix = mix * (0.9 / max(1e-6, np.max(np.abs(mix))))
    with wave.open(path, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((mix * 32767).astype(np.int16).tobytes())
    return t_en, t_en + len(ve) / SR
HTML = """<!DOCTYPE html><html><head><meta charset="utf-8"><style>
html,body{margin:0;width:1080px;height:1920px;background:transparent;font-family:'Baloo 2',sans-serif}
.t{position:absolute;left:0;right:0;text-align:center;font-weight:800;paint-order:stroke fill;line-height:1.3}
#l{top:190px;font-size:250px;color:#FFD400;-webkit-text-stroke:26px #111}
#wl{top:1450px}
#e{top:1745px;font-size:84px;color:#fff;-webkit-text-stroke:14px #111}
#w{font-size:200px;color:#fff;-webkit-text-stroke:24px #111}
</style></head><body><div class="t" id="l"></div><div class="t" id="wl"><span id="w"></span></div><div class="t" id="e"></div></body></html>"""

ap = argparse.ArgumentParser(); ap.add_argument("clips"); ap.add_argument("out"); ap.add_argument("--only", type=int)
a = ap.parse_args(); os.makedirs(a.out, exist_ok=True)
with sync_playwright() as p:
    b = p.chromium.launch(); page = b.new_page(viewport={"width": 1080, "height": 1920})
    for i, (clip, letter, word, eng, eng_line) in enumerate(ITEMS, 1):
        if a.only and a.only != i: continue
        page.set_content(HTML); page.evaluate("document.fonts.ready")
        page.evaluate("([l,w]) => {document.getElementById('l').textContent=l; document.getElementById('w').textContent=w}", [letter, word])
        # keep the word inside the frame
        page.evaluate("""() => { const w = document.getElementById('w'); let fs = 200;
            while (w.getBoundingClientRect().width > 980 && fs > 80) { fs -= 6; w.style.fontSize = fs + 'px'; } }""")
        page.wait_for_timeout(200)
        ov = os.path.join(a.out, f"_ov{i}.png"); page.screenshot(path=ov, omit_background=True)
        # the English word, shown while the English line is spoken
        page.evaluate("t => { document.getElementById('l').style.visibility='hidden'; document.getElementById('wl').style.visibility='hidden'; document.getElementById('e').textContent=t }", eng)
        ove = os.path.join(a.out, f"_ove{i}.png"); page.screenshot(path=ove, omit_background=True)
        src = os.path.join(a.clips, clip + ".mp4"); dst = os.path.join(a.out, f"vowel_hi_{i:02d}.mp4"); tmp = dst + ".part.mp4"
        dur = float(subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", src],
                                   capture_output=True, text=True).stdout)
        wav = os.path.join(a.out, f"_a{i}.wav")
        t_en, _ = build_audio(wav, src, f"{letter} से {word}", eng_line, dur)
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", src, "-i", ov, "-loop", "1", "-i", ove, "-i", wav, "-filter_complex",
                        f"[0:v]scale=1080:1920[bg];[bg][1:v]overlay[v1];[2:v]format=rgba,fade=t=in:st={t_en:.2f}:d=0.3:alpha=1[e];"
                        f"[v1][e]overlay=shortest=1,format=yuv420p[v]", "-map", "[v]", "-map", "3:a", "-t", f"{dur:.3f}",
                        "-c:v", "libx264", "-preset", "medium", "-crf", "20", "-c:a", "aac", "-b:a", "160k",
                        "-movflags", "+faststart", tmp], check=True)
        os.replace(tmp, dst); os.remove(ov); os.remove(ove); os.remove(wav); print(dst, letter, word, f"{t_en:.1f}", flush=True)
    b.close()
