#!/usr/bin/env python3
"""
Makes the app's Kannada voice clips with Google Cloud Text-to-Speech (licensed for use in apps).

Input : composeApp/build/voice/phrases.tsv  (key<TAB>text, exported by the SpeechCatalogExport test)
Output: composeApp/src/commonMain/composeResources/files/voice/v_<key>.mp3
        tools/voice-index.tsv (which file says what)

Needs the environment variable GOOGLE_TTS_API_KEY (an API key allowed to use the Cloud
Text-to-Speech API). Clips that already exist are kept unless --force is given.
"""
import argparse, base64, json, os, sys, time, urllib.request, urllib.error

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PHRASES = os.path.join(ROOT, "composeApp/build/voice/phrases.tsv")
OUT = os.path.join(ROOT, "composeApp/src/commonMain/composeResources/files/voice")
INDEX = os.path.join(ROOT, "tools/voice-index.tsv")
API = "https://texttospeech.googleapis.com/v1"


def fnv1a64(text: str) -> str:
    h = 0xcbf29ce484222325
    for b in text.strip().encode("utf-8"):
        h ^= b
        h = (h * 0x100000001b3) & 0xFFFFFFFFFFFFFFFF
    return f"{h:016x}"


def call(url, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"})
    for attempt in range(5):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                return json.loads(r.read())
        except urllib.error.HTTPError as e:
            msg = e.read().decode(errors="replace")
            if e.code in (429, 500, 503) and attempt < 4:
                time.sleep(2 ** attempt)
                continue
            sys.exit(f"Google TTS error {e.code}: {msg}")


def language_of(text):
    """kn-IN for Kannada script, hi-IN for Devanagari (Hindi), en-IN for plain English."""
    if any("\u0900" <= c <= "\u097F" for c in text):
        return "hi-IN"
    if any("\u0C80" <= c <= "\u0CFF" for c in text):
        return "kn-IN"
    return "en-IN"


def pick_voice(key, wanted, lang="kn-IN"):
    voices = call(f"{API}/voices?languageCode={lang}&key={key}").get("voices", [])
    names = [v["name"] for v in voices]
    print(f"{lang} voices available:", ", ".join(sorted(names)) or "none")
    if wanted != "auto":
        if wanted not in names:
            sys.exit(f"Voice {wanted} is not available")
        return wanted
    # Prefer a natural female voice: WaveNet, then Neural2, then Standard, then anything
    def rank(v):
        n, g = v["name"], v.get("ssmlGender", "")
        kind = 0 if "Wavenet" in n else 1 if "Neural2" in n else 2 if "Standard" in n else 3
        return (kind, 0 if g == "FEMALE" else 1, n)
    if not voices:
        sys.exit(f"No {lang} voice found for this key")
    return sorted(voices, key=rank)[0]["name"]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--voice", default="auto")
    ap.add_argument("--rate", type=float, default=0.85)
    ap.add_argument("--force", action="store_true")
    ap.add_argument("--prune", action="store_true", help="delete clips no longer used")
    a = ap.parse_args()
    key = os.environ.get("GOOGLE_TTS_API_KEY", "").strip()
    if not key:
        sys.exit("GOOGLE_TTS_API_KEY is not set")

    rows = []
    with open(PHRASES, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line:
                continue
            k, text = line.split("\t", 1)
            if fnv1a64(text) != k:
                sys.exit(f"Hash mismatch for {text!r}: app {k}, script {fnv1a64(text)}")
            rows.append((k, text))
    os.makedirs(OUT, exist_ok=True)
    langs = sorted({language_of(t) for _, t in rows})
    voices = {l: pick_voice(key, a.voice if l == "kn-IN" else "auto", l) for l in langs}
    voice = ", ".join(voices.values())
    print(f"Using voices {voice} at speed {a.rate} for {len(rows)} phrases")

    made = kept = 0
    chars = 0
    for k, text in rows:
        path = os.path.join(OUT, f"v_{k}.mp3")
        if os.path.exists(path) and not a.force:
            kept += 1
            continue
        res = call(f"{API}/text:synthesize?key={key}", {
            "input": {"text": text},
            "voice": {"languageCode": language_of(text), "name": voices[language_of(text)]},
            "audioConfig": {"audioEncoding": "MP3", "speakingRate": a.rate, "sampleRateHertz": 24000},
        })
        with open(path, "wb") as out:
            out.write(base64.b64decode(res["audioContent"]))
        made += 1
        chars += len(text)
    if a.prune:
        wanted = {f"v_{k}.mp3" for k, _ in rows}
        for name in os.listdir(OUT):
            if name.startswith("v_") and name.endswith(".mp3") and name not in wanted:
                os.remove(os.path.join(OUT, name))
    with open(INDEX, "w", encoding="utf-8") as f:
        f.write(f"# voice: {voice}, speed {a.rate}\n")
        for k, text in rows:
            f.write(f"v_{k}.mp3\t{text}\n")
    print(f"Made {made} clips ({chars} characters), kept {kept}")


if __name__ == "__main__":
    main()
