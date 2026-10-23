#!/usr/bin/env python3
"""Lists every GitHub video link the app uses (from the Kotlin sources) as
   <url> <TAB> <Firebase Storage object name>   (object = videos/<git ref>/<file path>)."""
import os, re, sys, urllib.parse

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PREFIX = "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/"
MAIN = PREFIX + "main"
urls = set()
for dirpath, _, files in os.walk(os.path.join(ROOT, "composeApp/src")):
    for name in files:
        if not name.endswith(".kt"):
            continue
        text = open(os.path.join(dirpath, name), encoding="utf-8").read()
        # Links written as "$ASSETS_BASE/x.mp4" or "$baseAssets/x.mp4" point at the main branch
        text = text.replace("$ASSETS_BASE", MAIN).replace("$baseAssets", MAIN)
        for m in re.finditer(re.escape(PREFIX) + r'[^"\s$]+?\.mp4', text):
            urls.add(m.group(0))
for u in sorted(urls):
    rel = urllib.parse.unquote(u[len(PREFIX):])
    print(f"{u}\tvideos/{rel}")
print(f"{len(urls)} videos", file=sys.stderr)
