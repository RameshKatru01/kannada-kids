// Works out how each letter is written (stroke order and direction) from the app's writing
// videos, where red ink appears as the letter is drawn on a white background.
//
// Run:  swift ExtractStrokes.swift <out.json> [debug-folder]
// For each video frame, the red pixels that were not inked before show where the pen is. Their
// centre, frame after frame, is the pen's path. A pause with no new ink, or a jump, is a pen lift
// (a new stroke). Points are saved relative to the letter's ink box (0…1), so the app can lay
// them over its own letter.

import AVFoundation
import AppKit
import Foundation

let base = "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/main"
/// The same videos the tracing game plays (LetterGames.kt).
let videos: [(String, String)] = [
    ("ಅ", "Final_A.mp4"), ("ಆ", "Final%20AA.mp4"), ("ಇ", "Final_E.mp4"), ("ಈ", "Final_EE.mp4"),
    ("ಉ", "Final_u.mp4"), ("ಊ", "Final_uu.mp4"), ("ಋ", "Final_RU.mp4"), ("ಎ", "Final_ye.mp4"),
    ("ಏ", "Final_yeee.mp4"), ("ಐ", "Final_I.mp4"), ("ಒ", "Final_O.mp4"), ("ಓ", "Final_OO.mp4"),
    ("ಔ", "Final_ouh.mp4"), ("ಅಂ", "Final_AUM.mp4"), ("ಅಃ", "Final_AHA.mp4"),
    ("ಕ", "Vowel_KA.mp4"), ("ಖ", "Vowel_KHA.mp4"), ("ಗ", "Vowel_GA.mp4"), ("ಘ", "Vowel_GHHHA.mp4"),
    ("ಙ", "Vowle_Inya.mp4"), ("ಚ", "Vowel_cha.mp4"), ("ಛ", "Vowel_CHHHA.mp4"), ("ಜ", "Vowel_JA.mp4"),
    ("ಝ", "Vowel_JJJAA.mp4"), ("ಞ", "Vowel_INI.mp4"), ("ಟ", "Vowel_Ta.mp4"), ("ಠ", "Vowel_tttta.mp4"),
    ("ಡ", "Vowle_Da.mp4"), ("ಢ", "Vowel_DDDAA.mp4"), ("ಣ", "Vowel_ANA.mp4"), ("ತ", "Vowel_Tha.mp4"),
    ("ಥ", "Vowel_THA2.mp4"), ("ದ", "Vowel_Dha.mp4"), ("ಧ", "Vowel_DHA2.mp4"), ("ನ", "Vowels_Na.mp4"),
    ("ಪ", "Vowel_Pa.mp4"), ("ಫ", "Vowel_Pha2.mp4"), ("ಬ", "Vowel_Ba.mp4"), ("ಭ", "Vowel_Bha2.mp4"),
    ("ಮ", "Vowel_ma.mp4"), ("ಯ", "Vowel_Ya.mp4"), ("ರ", "Vowel_Ra.mp4"), ("ಲ", "Vowel_La.mp4"),
    ("ವ", "Vowel_va.mp4"), ("ಶ", "Vowel%20SSA.mp4"), ("ಷ", "Vowel_Sha.mp4"), ("ಸ", "Vowel_Sa.mp4"),
    ("ಹ", "Vowel_ha.mp4"), ("ಳ", "Vowel_lha.mp4"),
]

struct Pt { var x: Double; var y: Double }

/// Red ink: strong red, little green and blue.
@inline(__always) func isInk(_ r: UInt8, _ g: UInt8, _ b: UInt8) -> Bool { r > 150 && g < 130 && b < 130 && Int(r) - Int(g) > 70 }

func extract(_ file: URL) -> (strokes: [[Pt]], box: CGRect, size: CGSize, final: CGImage?)? {
    let asset = AVURLAsset(url: file)
    guard let track = asset.tracks(withMediaType: .video).first,
          let reader = try? AVAssetReader(asset: asset) else { return nil }
    let out = AVAssetReaderTrackOutput(track: track, outputSettings: [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA])
    reader.add(out)
    reader.startReading()
    let fps = Double(track.nominalFrameRate > 0 ? track.nominalFrameRate : 30)

    var seen: [Bool] = []
    var w = 0, h = 0
    var samples: [(t: Double, c: Pt, n: Int)] = []
    var frame = 0
    var peak = 0
    var finalImage: CGImage?
    // Ink that is already there when the first inked frame appears (some videos start mid-stroke).
    var firstChunk: [Pt] = []
    while let sb = out.copyNextSampleBuffer(), let pb = CMSampleBufferGetImageBuffer(sb) {
        defer { frame += 1 }
        CVPixelBufferLockBaseAddress(pb, .readOnly)
        w = CVPixelBufferGetWidth(pb); h = CVPixelBufferGetHeight(pb)
        let row = CVPixelBufferGetBytesPerRow(pb)
        let p = CVPixelBufferGetBaseAddress(pb)!.assumingMemoryBound(to: UInt8.self)
        if seen.isEmpty { seen = [Bool](repeating: false, count: w * h) }
        var count = 0, nx = 0.0, ny = 0.0, nn = 0
        for y in 0..<h {
            let r0 = p + y * row
            for x in 0..<w {
                let px = r0 + x * 4
                if isInk(px[2], px[1], px[0]) {
                    count += 1
                    let i = y * w + x
                    if !seen[i] {
                        seen[i] = true; nx += Double(x); ny += Double(y); nn += 1
                        if samples.isEmpty { firstChunk.append(Pt(x: Double(x), y: Double(y))) }
                    }
                }
            }
        }
        // Keep a picture of the most complete frame for the debug overlay.
        if count > peak {
            peak = count
            let ci = CIImage(cvPixelBuffer: pb)
            finalImage = CIContext().createCGImage(ci, from: ci.extent)
        }
        CVPixelBufferUnlockBaseAddress(pb, .readOnly)
        // The video is over once most of the ink has gone (it clears at the end).
        if peak > 500 && count < peak / 3 { break }
        if nn >= 6 {
            samples.append((Double(frame) / fps, Pt(x: nx / Double(nn), y: ny / Double(nn)), nn))
        } else if samples.isEmpty {
            firstChunk.removeAll()
        }
    }
    guard !samples.isEmpty, w > 0 else { return nil }

    // If the first inked frame already holds a long piece of stroke, rebuild its path: the pen
    // continues from the end nearest the next new ink, so the farthest pixels were drawn first.
    if samples.count > 1, samples[0].n > 400 {
        let anchor = samples[1].c
        let sorted = firstChunk.sorted {
            let da = ($0.x - anchor.x) * ($0.x - anchor.x) + ($0.y - anchor.y) * ($0.y - anchor.y)
            let db = ($1.x - anchor.x) * ($1.x - anchor.x) + ($1.y - anchor.y) * ($1.y - anchor.y)
            return da > db
        }
        let bins = 14
        let size = max(1, sorted.count / bins)
        var rebuilt: [(t: Double, c: Pt, n: Int)] = []
        var k = 0
        while k < sorted.count {
            let part = sorted[k..<min(sorted.count, k + size)]
            let c = Pt(x: part.map(\.x).reduce(0, +) / Double(part.count), y: part.map(\.y).reduce(0, +) / Double(part.count))
            rebuilt.append((samples[0].t, c, part.count))
            k += size
        }
        samples = rebuilt + samples.dropFirst()
    }

    // The letter's ink box (from every inked pixel).
    var minX = w, minY = h, maxX = 0, maxY = 0
    for y in 0..<h { for x in 0..<w where seen[y * w + x] {
        minX = min(minX, x); maxX = max(maxX, x); minY = min(minY, y); maxY = max(maxY, y)
    } }
    let box = CGRect(x: minX, y: minY, width: max(1, maxX - minX), height: max(1, maxY - minY))
    let diag = (box.width * box.width + box.height * box.height).squareRoot()

    // Split into strokes at pauses or jumps.
    var strokes: [[Pt]] = []
    var cur: [Pt] = []
    var lastT = -1.0
    for s in samples {
        if let last = cur.last {
            let d = ((s.c.x - last.x) * (s.c.x - last.x) + (s.c.y - last.y) * (s.c.y - last.y)).squareRoot()
            // A pen lift shows as a pause; a fast stroke can move far between frames, so only a
            // very big jump without a pause counts as a lift.
            if s.t - lastT > 0.3 || d > Double(diag) * 0.35 {
                strokes.append(cur); cur = []
            }
        }
        cur.append(s.c)
        lastT = s.t
    }
    if !cur.isEmpty { strokes.append(cur) }

    // Smooth, drop specks, and space points evenly.
    let step = Double(diag) * 0.035
    strokes = strokes.compactMap { st -> [Pt]? in
        // A very short stroke is a dot (as in ಅಃ, ಸ): keep its centre as a single point.
        if st.count < 3 {
            return [Pt(x: st.map(\.x).reduce(0, +) / Double(st.count), y: st.map(\.y).reduce(0, +) / Double(st.count))]
        }
        var sm: [Pt] = []
        for i in st.indices {
            let a = st[max(0, i - 2)...min(st.count - 1, i + 2)]
            sm.append(Pt(x: a.map(\.x).reduce(0, +) / Double(a.count), y: a.map(\.y).reduce(0, +) / Double(a.count)))
        }
        var even: [Pt] = [sm[0]]
        var acc = 0.0
        for i in 1..<sm.count {
            let d = ((sm[i].x - sm[i - 1].x) * (sm[i].x - sm[i - 1].x) + (sm[i].y - sm[i - 1].y) * (sm[i].y - sm[i - 1].y)).squareRoot()
            acc += d
            if acc >= step { even.append(sm[i]); acc = 0 }
        }
        if let l = sm.last, let e = even.last, (l.x != e.x || l.y != e.y) { even.append(l) }
        // Ignore tiny strokes (noise), unless they are dots such as in ಅಃ
        var len = 0.0
        for i in 1..<even.count { len += ((even[i].x - even[i - 1].x) * (even[i].x - even[i - 1].x) + (even[i].y - even[i - 1].y) * (even[i].y - even[i - 1].y)).squareRoot() }
        if len <= Double(diag) * 0.04 {
            return [Pt(x: even.map(\.x).reduce(0, +) / Double(even.count), y: even.map(\.y).reduce(0, +) / Double(even.count))]
        }
        return even
    }
    // A real dot sits on ink; a "dot" in empty space comes from a loop closing on both sides.
    func onInk(_ p: Pt) -> Bool {
        let cx = Int(p.x), cy = Int(p.y)
        for dy in -3...3 { for dx in -3...3 {
            let x = cx + dx, y = cy + dy
            if x >= 0, y >= 0, x < w, y < h, seen[y * w + x] { return true }
        } }
        return false
    }
    strokes = strokes.filter { $0.count > 1 || onInk($0[0]) }
    // Dots right after each other belong together (a short tick drawn in two bursts).
    var merged: [[Pt]] = []
    for st in strokes {
        if st.count == 1, let last = merged.last, last.count <= 2, let e = last.last,
           ((st[0].x - e.x) * (st[0].x - e.x) + (st[0].y - e.y) * (st[0].y - e.y)).squareRoot() < Double(diag) * 0.12 {
            if last.count == 1 { merged[merged.count - 1] = last + st } else { merged[merged.count - 1] = [last[0], st[0]] }
        } else {
            merged.append(st)
        }
    }
    strokes = merged.map { st in
        // Two identical points are just a dot.
        if st.count == 2, abs(st[0].x - st[1].x) + abs(st[0].y - st[1].y) < Double(diag) * 0.03 { return [st[0]] }
        return st
    }
    // Drop "dots" that sit on another stroke (the start or end of a line, not a real dot).
    let near = Double(diag) * 0.07
    strokes = strokes.enumerated().filter { n, st in
        guard st.count == 1 else { return true }
        let d = st[0]
        return !strokes.enumerated().contains { m, o in
            m != n && o.count > 1 && o.contains { ($0.x - d.x) * ($0.x - d.x) + ($0.y - d.y) * ($0.y - d.y) < near * near }
        }
    }.map { $0.1 }
    return (strokes, box, CGSize(width: w, height: h), finalImage)
}

/// Draws the strokes over the letter: numbered green start dots, arrows along the way.
func debugImage(_ img: CGImage, _ strokes: [[Pt]], to url: URL) {
    let w = img.width, h = img.height
    guard let ctx = CGContext(data: nil, width: w, height: h, bitsPerComponent: 8, bytesPerRow: 0,
                              space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { return }
    ctx.draw(img, in: CGRect(x: 0, y: 0, width: w, height: h))
    ctx.setAlpha(1)
    func P(_ p: Pt) -> CGPoint { CGPoint(x: p.x, y: Double(h) - p.y) }
    for (n, s) in strokes.enumerated() {
        ctx.setStrokeColor(CGColor(red: 0.1, green: 0.2, blue: 0.8, alpha: 1)); ctx.setLineWidth(3)
        if s.count > 1 { ctx.move(to: P(s[0])); s.dropFirst().forEach { ctx.addLine(to: P($0)) }; ctx.strokePath() }
        for i in stride(from: 2, to: s.count, by: 4) {
            let a = P(s[i - 1]), b = P(s[i])
            let ang = atan2(b.y - a.y, b.x - a.x)
            ctx.move(to: b)
            ctx.addLine(to: CGPoint(x: b.x - 12 * cos(ang - 0.5), y: b.y - 12 * sin(ang - 0.5)))
            ctx.move(to: b)
            ctx.addLine(to: CGPoint(x: b.x - 12 * cos(ang + 0.5), y: b.y - 12 * sin(ang + 0.5)))
            ctx.strokePath()
        }
        ctx.setFillColor(CGColor(red: 0.1, green: 0.7, blue: 0.3, alpha: 1))
        let s0 = P(s[0]); ctx.fillEllipse(in: CGRect(x: s0.x - 9, y: s0.y - 9, width: 18, height: 18))
        let attrs: [NSAttributedString.Key: Any] = [.font: NSFont.boldSystemFont(ofSize: 14), .foregroundColor: NSColor.white]
        let gc = NSGraphicsContext(cgContext: ctx, flipped: false)
        NSGraphicsContext.saveGraphicsState(); NSGraphicsContext.current = gc
        NSAttributedString(string: "\(n + 1)", attributes: attrs).draw(at: NSPoint(x: s0.x - 4, y: s0.y - 8))
        NSGraphicsContext.restoreGraphicsState()
        ctx.setFillColor(CGColor(red: 0.85, green: 0.1, blue: 0.1, alpha: 1))
        let e = P(s[s.count - 1]); ctx.fill(CGRect(x: e.x - 6, y: e.y - 6, width: 12, height: 12))
    }
    if let cg = ctx.makeImage() {
        try? NSBitmapImageRep(cgImage: cg).representation(using: .png, properties: [:])?.write(to: url)
    }
}

let outPath = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "strokes.json"
let debugDir = CommandLine.arguments.count > 2 ? URL(fileURLWithPath: CommandLine.arguments[2]) : nil
if let d = debugDir { try? FileManager.default.createDirectory(at: d, withIntermediateDirectories: true) }
let cache = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("kannada-stroke-videos")
try? FileManager.default.createDirectory(at: cache, withIntermediateDirectories: true)

var result: [String: Any] = [:]
for (n, (letter, file)) in videos.enumerated() {
    let local = cache.appendingPathComponent(file.replacingOccurrences(of: "%20", with: "_"))
    if !FileManager.default.fileExists(atPath: local.path),
       let data = try? Data(contentsOf: URL(string: "\(base)/\(file)")!) {
        try? data.write(to: local)
    }
    guard let r = extract(local) else { print("  \(letter): could not read \(file)"); continue }
    let b = r.box
    let norm = r.strokes.map { s in s.map { [((($0.x - b.minX) / b.width) * 1000).rounded() / 1000, ((($0.y - b.minY) / b.height) * 1000).rounded() / 1000] } }
    result[letter] = ["aspect": (Double(b.width / b.height) * 1000).rounded() / 1000, "strokes": norm]
    print("  \(letter): \(r.strokes.count) stroke(s), \(r.strokes.map(\.count)) points")
    if let d = debugDir, let img = r.final {
        debugImage(img, r.strokes, to: d.appendingPathComponent(String(format: "%02d.png", n + 1)))
    }
}
let json = try! JSONSerialization.data(withJSONObject: result, options: [.sortedKeys])
try! json.write(to: URL(fileURLWithPath: outPath))
print("Wrote \(result.count) letters to \(outPath)")
