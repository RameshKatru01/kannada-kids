#!/usr/bin/env python3
"""Hindi vowel videos in the Kannada vowel style: big yellow letter at the top, the word in white at
the bottom (both with a thick black outline), over the Flow clip with its own sound.
Usage: vowel_overlay.py <clips dir> <out dir> [--only N]"""
import argparse, os, subprocess
from playwright.sync_api import sync_playwright

ITEMS = [("Pomegranate", "अ", "अनार"), ("Animated_mango", "आ", "आम"), ("Tamarind", "इ", "इमली"),
         ("Animated_sugarcane", "ई", "ईख"), ("Baby_owl_waving", "उ", "उल्लू"), ("Wool", "ऊ", "ऊन"),
         ("Sage", "ऋ", "ऋषि"), ("Toddler", "ए", "एड़ी"), ("Animated_spectacles", "ऐ", "ऐनक"),
         ("Animated_mortar", "ओ", "ओखली"), ("Woman", "औ", "औरत"), ("Smiling_grape", "अं", "अंगूर")]
HTML = """<!DOCTYPE html><html><head><meta charset="utf-8"><style>
html,body{margin:0;width:1080px;height:1920px;background:transparent;font-family:'Baloo 2',sans-serif}
.t{position:absolute;left:0;right:0;text-align:center;font-weight:800;paint-order:stroke fill;line-height:1.3}
#l{top:190px;font-size:250px;color:#FFD400;-webkit-text-stroke:26px #111}
.t:last-child{top:1540px}
#w{font-size:200px;color:#fff;-webkit-text-stroke:24px #111}
</style></head><body><div class="t" id="l"></div><div class="t"><span id="w"></span></div></body></html>"""

ap = argparse.ArgumentParser(); ap.add_argument("clips"); ap.add_argument("out"); ap.add_argument("--only", type=int)
a = ap.parse_args(); os.makedirs(a.out, exist_ok=True)
with sync_playwright() as p:
    b = p.chromium.launch(); page = b.new_page(viewport={"width": 1080, "height": 1920})
    for i, (clip, letter, word) in enumerate(ITEMS, 1):
        if a.only and a.only != i: continue
        page.set_content(HTML); page.evaluate("document.fonts.ready")
        page.evaluate("([l,w]) => {document.getElementById('l').textContent=l; document.getElementById('w').textContent=w}", [letter, word])
        # keep the word inside the frame
        page.evaluate("""() => { const w = document.getElementById('w'); let fs = 200;
            while (w.getBoundingClientRect().width > 980 && fs > 80) { fs -= 6; w.style.fontSize = fs + 'px'; } }""")
        page.wait_for_timeout(200)
        ov = os.path.join(a.out, f"_ov{i}.png"); page.screenshot(path=ov, omit_background=True)
        src = os.path.join(a.clips, clip + ".mp4"); dst = os.path.join(a.out, f"vowel_hi_{i:02d}.mp4"); tmp = dst + ".part.mp4"
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", src, "-i", ov, "-filter_complex",
                        "[0:v]scale=1080:1920[bg];[bg][1:v]overlay,format=yuv420p[v]", "-map", "[v]", "-map", "0:a?",
                        "-c:v", "libx264", "-preset", "medium", "-crf", "20", "-c:a", "aac", "-b:a", "160k",
                        "-movflags", "+faststart", tmp], check=True)
        os.replace(tmp, dst); os.remove(ov); print(dst, letter, word, flush=True)
    b.close()
