// Builds the printable Kannada worksheets (A4 PDF) for parents to print: vowels, consonants,
// Ka-kaa (ಕಾಗುಣಿತ) and numbers.
//
// Run from this folder:   swift VowelPack.swift [output folder]
// With no folder, the files go straight into the app (composeResources/files/worksheets), where
// Letter Box → Vowels → Printable worksheets offers them for printing.
//
// Output:  starter-kit.pdf                    cover, parent guide and certificate (3 pages)
//          vowel-01-a.pdf … vowel-15-aha.pdf  the four sheets for one vowel
//          pair-01-a-aa.pdf … pair-06-ai-au.pdf  two vowels together (only with --pairs; the app
//                                             joins the vowels a parent selects instead)
//          review.pdf                         the six review sheets
//          Kannada-Vowels-Worksheet-Pack.pdf  everything in one file (only with --full)
//          consonants-kit.pdf, consonant-01.pdf … consonant-34.pdf   (cover + certificate; 4 sheets each)
//          kakaa-kit.pdf, kakaa-01.pdf … kakaa-34.pdf               (2 sheets per consonant family)
//          numbers-kit.pdf, number-01.pdf … number-10.pdf           (4 sheets each)
//
// Letters are drawn from the Noto Sans Kannada font as vector outlines, so they print sharply.
// Pictures are the app's emoji turned into black-and-white outlines that children can colour.

import AppKit
import CoreImage
import CoreText
import Foundation

// MARK: - Content

/// One letter or number and its picture word. (Named Vowel because vowels came first; it is used
/// for consonants and numbers too.) An empty [word] means no word page; [contains] means the word
/// has the letter inside rather than at the start.
struct Vowel {
    let letter: String
    let sound: String
    let word: String
    let picture: String
    let english: String
    var contains: Bool = false
}

/// The 15 vowels in the app's order, each with one of the app's picture words.
let vowels: [Vowel] = [
    Vowel(letter: "ಅ", sound: "a", word: "ಅರಸ", picture: "👑", english: "king"),
    Vowel(letter: "ಆ", sound: "aa", word: "ಆನೆ", picture: "🐘", english: "elephant"),
    Vowel(letter: "ಇ", sound: "i", word: "ಇಲಿ", picture: "🐭", english: "mouse"),
    Vowel(letter: "ಈ", sound: "ee", word: "ಈರುಳ್ಳಿ", picture: "🧅", english: "onion"),
    Vowel(letter: "ಉ", sound: "u", word: "ಉಗಿಬಂಡಿ", picture: "🚂", english: "steam train"),
    Vowel(letter: "ಊ", sound: "oo", word: "ಊಟ", picture: "🍛", english: "meal"),
    Vowel(letter: "ಋ", sound: "ru", word: "ಋಷಿ", picture: "🧘", english: "sage"),
    Vowel(letter: "ಎ", sound: "e", word: "ಎಲೆ", picture: "🍃", english: "leaf"),
    Vowel(letter: "ಏ", sound: "ae", word: "ಏಣಿ", picture: "🪜", english: "ladder"),
    Vowel(letter: "ಐ", sound: "ai", word: "ಐದು", picture: "🖐️", english: "five"),
    Vowel(letter: "ಒ", sound: "o", word: "ಒಂಟೆ", picture: "🐫", english: "camel"),
    Vowel(letter: "ಓ", sound: "oa", word: "ಓಟ", picture: "🏃", english: "race"),
    Vowel(letter: "ಔ", sound: "au", word: "ಔಷಧ", picture: "💊", english: "medicine"),
    Vowel(letter: "ಅಂ", sound: "am", word: "ಅಂಗಿ", picture: "👕", english: "shirt"),
    Vowel(letter: "ಅಃ", sound: "aha", word: "ದುಃಖ", picture: "😢", english: "sad", contains: true),
]

/// The 34 consonants in the app's order with the Letter Box picture words. A few letters never
/// start a children's word, so they use a word with the letter inside; ಙ and ಞ have no word page.
let consonants: [Vowel] = [
    Vowel(letter: "ಕ", sound: "ka", word: "ಕಮಲ", picture: "🪷", english: "lotus"),
    Vowel(letter: "ಖ", sound: "kha", word: "ಖಡ್ಗ", picture: "⚔️", english: "sword"),
    Vowel(letter: "ಗ", sound: "ga", word: "ಗಡಿಯಾರ", picture: "🕰️", english: "clock"),
    Vowel(letter: "ಘ", sound: "gha", word: "ಘಂಟೆ", picture: "🔔", english: "bell"),
    Vowel(letter: "ಙ", sound: "nga", word: "", picture: "", english: ""),
    Vowel(letter: "ಚ", sound: "cha", word: "ಚಮಚ", picture: "🥄", english: "spoon"),
    Vowel(letter: "ಛ", sound: "chha", word: "ಛತ್ರಿ", picture: "☂️", english: "umbrella"),
    Vowel(letter: "ಜ", sound: "ja", word: "ಜಿಂಕೆ", picture: "🦌", english: "deer"),
    Vowel(letter: "ಝ", sound: "jha", word: "ಝರಿ", picture: "🏞️", english: "stream"),
    Vowel(letter: "ಞ", sound: "nya", word: "", picture: "", english: ""),
    Vowel(letter: "ಟ", sound: "Ta", word: "ಟೊಮೆಟೊ", picture: "🍅", english: "tomato"),
    Vowel(letter: "ಠ", sound: "Tha", word: "ಮಠ", picture: "🛕", english: "temple", contains: true),
    Vowel(letter: "ಡ", sound: "Da", word: "ಡಬ್ಬ", picture: "📦", english: "box"),
    Vowel(letter: "ಢ", sound: "Dha", word: "ಢಕ್ಕೆ", picture: "🥁", english: "drum"),
    Vowel(letter: "ಣ", sound: "Na", word: "ಬಾಣ", picture: "🏹", english: "arrow", contains: true),
    Vowel(letter: "ತ", sound: "ta", word: "ತರಕಾರಿ", picture: "🥕", english: "vegetables"),
    Vowel(letter: "ಥ", sound: "tha", word: "ಕಥೆ", picture: "📖", english: "story", contains: true),
    Vowel(letter: "ದ", sound: "da", word: "ದೋಣಿ", picture: "⛵", english: "boat"),
    Vowel(letter: "ಧ", sound: "dha", word: "ಧ್ವಜ", picture: "🚩", english: "flag"),
    Vowel(letter: "ನ", sound: "na", word: "ನವಿಲು", picture: "🦚", english: "peacock"),
    Vowel(letter: "ಪ", sound: "pa", word: "ಪುಸ್ತಕ", picture: "📚", english: "book"),
    Vowel(letter: "ಫ", sound: "pha", word: "ಫಲ", picture: "🍇", english: "fruit"),
    Vowel(letter: "ಬ", sound: "ba", word: "ಬಾಳೆಹಣ್ಣು", picture: "🍌", english: "banana"),
    Vowel(letter: "ಭ", sound: "bha", word: "ಭೂಮಿ", picture: "🌍", english: "earth"),
    Vowel(letter: "ಮ", sound: "ma", word: "ಮರ", picture: "🌳", english: "tree"),
    Vowel(letter: "ಯ", sound: "ya", word: "ಯಂತ್ರ", picture: "⚙️", english: "machine"),
    Vowel(letter: "ರ", sound: "ra", word: "ರೈಲು", picture: "🚆", english: "train"),
    Vowel(letter: "ಲ", sound: "la", word: "ಲಾರಿ", picture: "🚚", english: "lorry"),
    Vowel(letter: "ವ", sound: "va", word: "ವಿಮಾನ", picture: "✈️", english: "aeroplane"),
    Vowel(letter: "ಶ", sound: "sha", word: "ಶಂಖ", picture: "🐚", english: "conch shell"),
    Vowel(letter: "ಷ", sound: "Sha", word: "ಮೇಷ", picture: "🐏", english: "ram", contains: true),
    Vowel(letter: "ಸ", sound: "sa", word: "ಸೇಬು", picture: "🍎", english: "apple"),
    Vowel(letter: "ಹ", sound: "ha", word: "ಹಸು", picture: "🐄", english: "cow"),
    Vowel(letter: "ಳ", sound: "La", word: "ಕೋಳಿ", picture: "🐔", english: "hen", contains: true),
]

/// Kannada numbers 1 to 10 with their names; [picture] is the thing to count.
let numbers: [Vowel] = [
    Vowel(letter: "೧", sound: "1", word: "ಒಂದು", picture: "🍎", english: "one"),
    Vowel(letter: "೨", sound: "2", word: "ಎರಡು", picture: "🐟", english: "two"),
    Vowel(letter: "೩", sound: "3", word: "ಮೂರು", picture: "🎈", english: "three"),
    Vowel(letter: "೪", sound: "4", word: "ನಾಲ್ಕು", picture: "🌸", english: "four"),
    Vowel(letter: "೫", sound: "5", word: "ಐದು", picture: "⭐", english: "five"),
    Vowel(letter: "೬", sound: "6", word: "ಆರು", picture: "🐞", english: "six"),
    Vowel(letter: "೭", sound: "7", word: "ಏಳು", picture: "🍌", english: "seven"),
    Vowel(letter: "೮", sound: "8", word: "ಎಂಟು", picture: "🦋", english: "eight"),
    Vowel(letter: "೯", sound: "9", word: "ಒಂಬತ್ತು", picture: "🐥", english: "nine"),
    Vowel(letter: "೧೦", sound: "10", word: "ಹತ್ತು", picture: "🍓", english: "ten"),
]

/// Things that change per section: names, letters, and which letters look alike.
struct Section {
    let singular: String          // "vowel"
    let plural: String            // "vowels"
    let items: [Vowel]
    /// Tall letters of the section, used to give a grid one shared letter size.
    let sample: [String]
    let alike: (String) -> [String]
    var isNumber: Bool = false
}

/// The consonant groups (ವರ್ಗ), used to pick look-alike letters for the find sheets.
let consonantGroups: [[String]] = [
    ["ಕ", "ಖ", "ಗ", "ಘ", "ಙ"], ["ಚ", "ಛ", "ಜ", "ಝ", "ಞ"], ["ಟ", "ಠ", "ಡ", "ಢ", "ಣ"],
    ["ತ", "ಥ", "ದ", "ಧ", "ನ"], ["ಪ", "ಫ", "ಬ", "ಭ", "ಮ"], ["ಯ", "ರ", "ಲ", "ವ", "ಶ", "ಷ", "ಸ", "ಹ", "ಳ"],
]

/// Consonants that children mix up because the shapes are close.
let consonantLookAlikes: [String: [String]] = [
    "ಮ": ["ಯ", "ಘ", "ಝ"], "ಯ": ["ಮ", "ಘ", "ಝ"], "ಘ": ["ಮ", "ಯ", "ಝ"], "ಝ": ["ಘ", "ಮ", "ಯ"],
    "ಪ": ["ಫ", "ಷ", "ಸ"], "ಫ": ["ಪ", "ಷ"], "ಷ": ["ಪ", "ಫ"], "ಬ": ["ಭ", "ಲ", "ಚ"], "ಭ": ["ಬ", "ಲ"],
    "ಲ": ["ಬ", "ಳ", "ಚ"], "ಳ": ["ಲ", "ಟ"], "ದ": ["ಧ", "ಡ", "ಜ"], "ಧ": ["ದ", "ಢ"], "ಡ": ["ಢ", "ದ", "ಜ"],
    "ಢ": ["ಡ", "ಧ"], "ಜ": ["ಡ", "ದ"], "ತ": ["ಥ", "ಠ", "ಕ"], "ಥ": ["ತ", "ಠ"], "ಚ": ["ಛ", "ಬ", "ಲ"],
    "ಛ": ["ಚ", "ಬ"], "ಶ": ["ಸ", "ಕ"], "ಸ": ["ಶ", "ಪ"], "ಕ": ["ಖ", "ತ", "ಶ"], "ಖ": ["ಕ", "ಬ"],
    "ನ": ["ಸ", "ವ"], "ವ": ["ನ", "ಪ"], "ರ": ["ಠ", "ಳ"], "ಠ": ["ರ", "ಥ"], "ಟ": ["ಳ", "ಡ"],
    "ಗ": ["ಸ", "ಘ"], "ಹ": ["ಕ", "ಶ"], "ಣ": ["ಡ", "ಟ"], "ಙ": ["ಜ", "ಞ"], "ಞ": ["ಙ", "ಜ"],
]

func consonantAlike(_ s: String) -> [String] {
    let group = consonantGroups.first { $0.contains(s) } ?? []
    let close = consonantLookAlikes[s] ?? []
    let all = close + group.filter { $0 != s && !close.contains($0) }
    return all.isEmpty ? ["ಕ"] : all
}

let vowelSection = Section(singular: "vowel", plural: "vowels", items: vowels,
                           sample: ["ಅ", "ಇ", "ಈ", "ಓ", "ಔ", "ಋ"], alike: { lookAlikes($0) })
let consonantSection = Section(singular: "consonant", plural: "consonants", items: consonants,
                               sample: ["ಕ", "ಘ", "ಛ", "ಝ", "ಢ", "ಫ", "ಭ", "ಶ"], alike: consonantAlike)
let numberSection = Section(singular: "number", plural: "numbers", items: numbers,
                            sample: ["೧", "೨", "೬", "೭", "೯"],
                            alike: { s in numbers.map(\.letter).filter { $0 != s && $0 != "೧೦" } }, isNumber: true)

let appName = "Fun Learn - ಕನ್ನಡ"

// MARK: - Page geometry (A4 in points, top-left origin used by all helpers)

let pageW: CGFloat = 595.28
let pageH: CGFloat = 841.89
let margin: CGFloat = 36
let contentTop: CGFloat = 132      // below the header and title
let contentBottom: CGFloat = pageH - 58

let ink = CGColor(gray: 0.1, alpha: 1)
let midGray = CGColor(gray: 0.55, alpha: 1)
let lightGray = CGColor(gray: 0.82, alpha: 1)
let paleGray = CGColor(gray: 0.9, alpha: 1)

// MARK: - Fonts and text

func font(_ name: String, _ size: CGFloat) -> CTFont {
    CTFontCreateWithName(name as CFString, size, nil)
}

let kannadaBold = "NotoSansKannada-Bold"
let kannadaSemi = "NotoSansKannada-SemiBold"
let latinBold = "AvenirNext-Bold"
let latinDemi = "AvenirNext-DemiBold"
let latinRegular = "AvenirNext-Regular"

/// A typeset line. CoreText shapes Kannada (conjuncts, vowel signs) and falls back for mixed text.
func makeLine(_ text: String, _ fontName: String, _ size: CGFloat, _ color: CGColor = ink) -> CTLine {
    let attrs: [NSAttributedString.Key: Any] = [
        NSAttributedString.Key(kCTFontAttributeName as String): font(fontName, size),
        NSAttributedString.Key(kCTForegroundColorAttributeName as String): color,
    ]
    return CTLineCreateWithAttributedString(NSAttributedString(string: text, attributes: attrs))
}

enum Align { case left, center, right }

/// Draws one line of text. [y] is the baseline, measured from the top of the page.
func text(_ ctx: CGContext, _ s: String, x: CGFloat, y: CGFloat, font fontName: String = latinDemi,
          size: CGFloat, color: CGColor = ink, align: Align = .left) {
    let line = makeLine(s, fontName, size, color)
    let w = CGFloat(CTLineGetTypographicBounds(line, nil, nil, nil))
    let startX: CGFloat
    switch align {
    case .left: startX = x
    case .center: startX = x - w / 2
    case .right: startX = x - w
    }
    ctx.saveGState()
    ctx.textMatrix = .identity
    ctx.textPosition = CGPoint(x: startX, y: pageH - y)
    CTLineDraw(line, ctx)
    ctx.restoreGState()
}

/// Wraps [s] to [width] and draws it; returns the y below the last line.
@discardableResult
func paragraph(_ ctx: CGContext, _ s: String, x: CGFloat, y: CGFloat, width: CGFloat,
               font fontName: String = latinRegular, size: CGFloat, leading: CGFloat? = nil,
               color: CGColor = ink) -> CGFloat {
    let lh = leading ?? size * 1.45
    var lineY = y
    var current = ""
    func measure(_ t: String) -> CGFloat {
        CGFloat(CTLineGetTypographicBounds(makeLine(t, fontName, size), nil, nil, nil))
    }
    for word in s.split(separator: " ") {
        let candidate = current.isEmpty ? String(word) : current + " " + word
        if measure(candidate) > width && !current.isEmpty {
            text(ctx, current, x: x, y: lineY, font: fontName, size: size, color: color)
            lineY += lh
            current = String(word)
        } else {
            current = candidate
        }
    }
    if !current.isEmpty {
        text(ctx, current, x: x, y: lineY, font: fontName, size: size, color: color)
        lineY += lh
    }
    return lineY
}

// MARK: - Letter shapes

/// The outline of [s] as one path, in a y-up space with the baseline at 0.
func glyphPath(_ s: String, fontName: String = kannadaBold, size: CGFloat = 200) -> CGPath {
    let line = makeLine(s, fontName, size)
    let path = CGMutablePath()
    let runs = CTLineGetGlyphRuns(line) as! [CTRun]
    for run in runs {
        let attrs = CTRunGetAttributes(run) as NSDictionary
        let runFont = attrs[kCTFontAttributeName as String] as! CTFont
        let count = CTRunGetGlyphCount(run)
        var glyphs = [CGGlyph](repeating: 0, count: count)
        var positions = [CGPoint](repeating: .zero, count: count)
        CTRunGetGlyphs(run, CFRange(location: 0, length: count), &glyphs)
        CTRunGetPositions(run, CFRange(location: 0, length: count), &positions)
        for i in 0..<count {
            if let g = CTFontCreatePathForGlyph(runFont, glyphs[i], nil) {
                path.addPath(g, transform: CGAffineTransform(translationX: positions[i].x, y: positions[i].y))
            }
        }
    }
    return path
}

enum LetterStyle {
    case solid                   // black letter
    case trace(gray: CGFloat)    // light grey letter to write over
    case dashed                  // dotted outline to trace along
    case outline(width: CGFloat) // empty outline to colour in
}

/// Draws [s] scaled to fit inside [box] (top-left coordinates), centred, with the given style.
/// [scaleCap] keeps narrow letters from growing larger than their neighbours (1 = 200 pt type).
func letter(_ ctx: CGContext, _ s: String, in box: CGRect, style: LetterStyle,
            fontName: String = kannadaBold, scaleCap: CGFloat? = nil, pattern: Bool = false) {
    let path = glyphPath(s, fontName: fontName)
    let bounds = path.boundingBoxOfPath
    guard bounds.width > 0, bounds.height > 0 else { return }
    var scale = min(box.width / bounds.width, box.height / bounds.height)
    if let scaleCap { scale = min(scale, scaleCap) }
    // Place the bounding box centre at the box centre (convert the box to y-up page space).
    let cx = box.midX
    let cy = pageH - box.midY
    var t = CGAffineTransform(translationX: cx, y: cy)
        .scaledBy(x: scale, y: scale)
        .translatedBy(x: -bounds.midX, y: -bounds.midY)
    guard let placed = path.copy(using: &t) else { return }

    ctx.saveGState()
    ctx.setLineJoin(.round)
    ctx.setLineCap(.round)
    switch style {
    case .solid:
        ctx.addPath(placed); ctx.setFillColor(ink); ctx.fillPath()
    case .trace(let gray):
        ctx.addPath(placed); ctx.setFillColor(CGColor(gray: gray, alpha: 1)); ctx.fillPath()
    case .dashed:
        ctx.addPath(placed)
        ctx.setStrokeColor(midGray)
        let w = max(1.2, min(2.4, box.height / 45))
        ctx.setLineWidth(w)
        ctx.setLineDash(phase: 0, lengths: [0.01, w * 2.6])
        ctx.strokePath()
    case .outline(let width):
        if pattern {
            // Small circles inside the letter make colouring it in more fun.
            ctx.saveGState()
            ctx.addPath(placed); ctx.clip()
            ctx.setStrokeColor(lightGray); ctx.setLineWidth(1)
            let step: CGFloat = 26
            let r = placed.boundingBoxOfPath
            var row = 0
            var y = r.minY
            while y < r.maxY + step {
                var x = r.minX + (row % 2 == 0 ? 0 : step / 2)
                while x < r.maxX + step {
                    ctx.strokeEllipse(in: CGRect(x: x - 5, y: y - 5, width: 10, height: 10))
                    x += step
                }
                y += step * 0.87; row += 1
            }
            ctx.restoreGState()
        }
        ctx.addPath(placed); ctx.setStrokeColor(ink); ctx.setLineWidth(width); ctx.strokePath()
    }
    ctx.restoreGState()
}

/// A scale that fits the tallest vowels into [box], so letters in a grid share one size
/// (wide letters such as ಊ and ಅಂ still shrink to fit their box).
func gridScale(_ box: CGRect, sample: [String] = ["ಅ", "ಇ", "ಈ", "ಓ", "ಔ", "ಋ"]) -> CGFloat {
    sample.map { s -> CGFloat in
        let b = glyphPath(s).boundingBoxOfPath
        return min(box.width / b.width, box.height / b.height)
    }.min() ?? 1
}

// MARK: - Pictures (emoji turned into colouring outlines)

var pictureCache: [String: CGImage] = [:]

func lineArt(_ emoji: String) -> CGImage? {
    if let cached = pictureCache[emoji] { return cached }
    let size: CGFloat = 640
    let px = Int(size)
    guard let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: px, pixelsHigh: px, bitsPerSample: 8,
                                     samplesPerPixel: 4, hasAlpha: true, isPlanar: false,
                                     colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0),
          let emojiFont = NSFont(name: "Apple Color Emoji", size: size * 0.72) else { return nil }
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
    NSColor.white.setFill()
    NSRect(x: 0, y: 0, width: size, height: size).fill()
    let s = NSAttributedString(string: emoji, attributes: [.font: emojiFont])
    let b = s.size()
    s.draw(at: NSPoint(x: (size - b.width) / 2, y: (size - b.height) / 2))
    NSGraphicsContext.restoreGraphicsState()
    guard let cg = rep.cgImage else { return nil }

    let ci = CIImage(cgImage: cg)
    let lines = ci.applyingFilter("CIPhotoEffectNoir").applyingFilter("CILineOverlay", parameters: [
        "inputNRNoiseLevel": 0.07, "inputNRSharpness": 0.71, "inputEdgeIntensity": 1.0,
        "inputThreshold": 0.1, "inputContrast": 50.0,
    ])
    // Trim the edge so the filter's frame line around the image is not printed.
    let inner = ci.extent.insetBy(dx: 12, dy: 12)
    var onWhite = lines.cropped(to: inner).composited(over: CIImage(color: .white).cropped(to: inner))
    // Keep only lines near the emoji itself: the filter leaves specks in empty corners.
    if let shape = emojiShapeMask(emoji, font: emojiFont, size: size) {
        let near = shape.applyingFilter("CIMorphologyMaximum", parameters: ["inputRadius": 10.0])
        onWhite = onWhite.applyingFilter("CIBlendWithMask", parameters: [
            "inputBackgroundImage": CIImage(color: .white).cropped(to: inner),
            "inputMaskImage": near,
        ]).cropped(to: inner)
    }
    let gray = CGColorSpaceCreateDeviceGray()
    guard let rgb = CIContext().createCGImage(onWhite, from: inner),
          let g = CGContext(data: nil, width: rgb.width, height: rgb.height, bitsPerComponent: 8, bytesPerRow: 0,
                            space: gray, bitmapInfo: CGImageAlphaInfo.none.rawValue) else { return nil }
    g.draw(rgb, in: CGRect(x: 0, y: 0, width: rgb.width, height: rgb.height))
    let out = g.makeImage()
    pictureCache[emoji] = out
    return out
}

/// White where the emoji is drawn, black elsewhere (from the emoji's transparency).
func emojiShapeMask(_ emoji: String, font: NSFont, size: CGFloat) -> CIImage? {
    let px = Int(size)
    guard let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: px, pixelsHigh: px, bitsPerSample: 8,
                                     samplesPerPixel: 4, hasAlpha: true, isPlanar: false,
                                     colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0) else { return nil }
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
    NSColor.clear.setFill()
    NSRect(x: 0, y: 0, width: size, height: size).fill()
    let s = NSAttributedString(string: emoji, attributes: [.font: font])
    let b = s.size()
    s.draw(at: NSPoint(x: (size - b.width) / 2, y: (size - b.height) / 2))
    NSGraphicsContext.restoreGraphicsState()
    guard let cg = rep.cgImage else { return nil }
    // Alpha → grey: copy the alpha channel into red, green and blue.
    let alpha = CIVector(x: 0, y: 0, z: 0, w: 1)
    return CIImage(cgImage: cg).applyingFilter("CIColorMatrix", parameters: [
        "inputRVector": alpha, "inputGVector": alpha, "inputBVector": alpha,
        "inputAVector": CIVector(x: 0, y: 0, z: 0, w: 0), "inputBiasVector": CIVector(x: 0, y: 0, z: 0, w: 1),
    ])
}

func picture(_ ctx: CGContext, _ emoji: String, in box: CGRect) {
    guard let img = lineArt(emoji) else { return }
    let side = min(box.width, box.height)
    let r = CGRect(x: box.midX - side / 2, y: pageH - box.midY - side / 2, width: side, height: side)
    ctx.saveGState()
    ctx.interpolationQuality = .high
    ctx.draw(img, in: r)
    ctx.restoreGState()
}

// MARK: - Shapes

func flip(_ r: CGRect) -> CGRect { CGRect(x: r.minX, y: pageH - r.maxY, width: r.width, height: r.height) }

func roundedBox(_ ctx: CGContext, _ r: CGRect, radius: CGFloat = 12, stroke: CGColor = lightGray,
                width: CGFloat = 1.2, dashed: Bool = false, fill: CGColor? = nil) {
    let p = CGPath(roundedRect: flip(r), cornerWidth: radius, cornerHeight: radius, transform: nil)
    ctx.saveGState()
    if let fill { ctx.addPath(p); ctx.setFillColor(fill); ctx.fillPath() }
    ctx.addPath(p)
    ctx.setStrokeColor(stroke); ctx.setLineWidth(width)
    if dashed { ctx.setLineDash(phase: 0, lengths: [5, 4]) }
    ctx.strokePath()
    ctx.restoreGState()
}

func hLine(_ ctx: CGContext, x1: CGFloat, x2: CGFloat, y: CGFloat, color: CGColor = lightGray,
           width: CGFloat = 1, dashed: Bool = false) {
    ctx.saveGState()
    ctx.move(to: CGPoint(x: x1, y: pageH - y)); ctx.addLine(to: CGPoint(x: x2, y: pageH - y))
    ctx.setStrokeColor(color); ctx.setLineWidth(width)
    if dashed { ctx.setLineDash(phase: 0, lengths: [4, 4]) }
    ctx.strokePath()
    ctx.restoreGState()
}

func dot(_ ctx: CGContext, x: CGFloat, y: CGFloat, r: CGFloat = 5) {
    ctx.setFillColor(ink)
    ctx.fillEllipse(in: CGRect(x: x - r, y: pageH - y - r, width: r * 2, height: r * 2))
}

/// A five-pointed star outline centred at (x, y) in top-left coordinates.
func star(_ ctx: CGContext, x: CGFloat, y: CGFloat, r: CGFloat) {
    let p = CGMutablePath()
    for i in 0..<10 {
        let a = CGFloat(i) * .pi / 5 + .pi / 2
        let rr = i % 2 == 0 ? r : r * 0.45
        let pt = CGPoint(x: x + cos(a) * rr, y: pageH - y + sin(a) * rr)
        if i == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
    }
    p.closeSubpath()
    ctx.saveGState()
    ctx.addPath(p); ctx.setStrokeColor(ink); ctx.setLineWidth(2); ctx.setLineJoin(.round); ctx.strokePath()
    ctx.restoreGState()
}

// MARK: - Page frame

/// Header (app name, name line), title, instructions, footer and a soft border.
func frame(_ ctx: CGContext, title: String, titleKannada: String? = nil, instructions: String,
           footer: String, page: Int) {
    roundedBox(ctx, CGRect(x: 18, y: 18, width: pageW - 36, height: pageH - 36), radius: 22,
               stroke: lightGray, width: 2)
    text(ctx, appName, x: margin + 6, y: 52, font: latinBold, size: 12, color: midGray)
    text(ctx, "Name: ____________________", x: pageW - margin - 6, y: 52, font: latinRegular, size: 12,
         color: midGray, align: .right)

    text(ctx, title, x: margin + 6, y: 92, font: latinBold, size: 26)
    if let titleKannada {
        let line = makeLine(title, latinBold, 26)
        let w = CGFloat(CTLineGetTypographicBounds(line, nil, nil, nil))
        text(ctx, titleKannada, x: margin + 6 + w + 10, y: 94, font: kannadaBold, size: 28)
    }
    paragraph(ctx, instructions, x: margin + 6, y: 114, width: pageW - 2 * margin - 12,
              font: latinRegular, size: 12, color: CGColor(gray: 0.3, alpha: 1))

    hLine(ctx, x1: margin, x2: pageW - margin, y: pageH - 46, color: paleGray)
    text(ctx, footer, x: margin + 6, y: pageH - 30, font: latinRegular, size: 10, color: midGray)
    text(ctx, "\(page)", x: pageW - margin - 6, y: pageH - 30, font: latinDemi, size: 10, color: midGray,
         align: .right)
}

// MARK: - Pages

typealias Page = (CGContext, Int) -> Void

func vowelFooter(_ v: Vowel, _ i: Int, _ s: Section = vowelSection) -> String {
    let name = s.singular.prefix(1).uppercased() + s.singular.dropFirst()
    return s.isNumber
        ? "Number \(i + 1) of \(s.items.count):  \(v.letter)  is \(v.english) (\(v.word))"
        : "\(name) \(i + 1) of \(s.items.count):  \(v.letter)  says \u{201C}\(v.sound)\u{201D}"
}

/// 1. Trace the letter: one big grey letter, then rows that fade from grey to dotted to empty.
func tracePage(_ v: Vowel, _ i: Int, _ s: Section = vowelSection) -> Page {
    { ctx, page in
        frame(ctx, title: s.isNumber ? "Trace the number" : "Trace the letter", titleKannada: v.letter,
              instructions: s.isNumber
                ? "Say \u{201C}\(v.word)\u{201D} (\(v.english)) each time. Trace the big number with your finger first, then with a crayon. Then trace the small ones and try one on your own."
                : "Say \u{201C}\(v.sound)\u{201D} each time. Trace the big letter with your finger first, then with a crayon. Then trace the small ones and try one on your own.",
              footer: vowelFooter(v, i, s), page: page)
        let big = CGRect(x: margin + 70, y: 150, width: pageW - 2 * margin - 140, height: 205)
        letter(ctx, v.letter, in: big, style: .trace(gray: 0.86))
        letter(ctx, v.letter, in: big, style: .dashed)

        let cols = 4
        let gap: CGFloat = 14
        let cellW = (pageW - 2 * margin - 12 - gap * CGFloat(cols - 1)) / CGFloat(cols)
        let cellH: CGFloat = 86
        let rows: [[LetterStyle?]] = [
            [.trace(gray: 0.8), .trace(gray: 0.8), .trace(gray: 0.8), .trace(gray: 0.8)],
            [.dashed, .dashed, .dashed, .dashed],
            [.dashed, nil, nil, nil],
            [nil, nil, nil, nil],
        ]
        for (r, row) in rows.enumerated() {
            let y = 378 + CGFloat(r) * (cellH + 12)
            for (c, style) in row.enumerated() {
                let x = margin + 6 + CGFloat(c) * (cellW + gap)
                let cell = CGRect(x: x, y: y, width: cellW, height: cellH)
                roundedBox(ctx, cell)
                hLine(ctx, x1: cell.minX + 8, x2: cell.maxX - 8, y: cell.midY, color: paleGray, dashed: true)
                if let style {
                    let inner = cell.insetBy(dx: 14, dy: 12)
                    letter(ctx, v.letter, in: inner, style: style, scaleCap: gridScale(inner, sample: s.sample))
                }
            }
        }
    }
}

/// 2. Colour the letter and its picture.
func colourPage(_ v: Vowel, _ i: Int, _ s: Section = vowelSection) -> Page {
    { ctx, page in
        if s.isNumber {
            frame(ctx, title: "Colour the number", titleKannada: v.letter,
                  instructions: "\(v.letter) is \(v.word), \(v.english). Colour in the big number, then colour \(v.english) \(i == 0 ? "picture" : "pictures") and count them out loud.",
                  footer: vowelFooter(v, i, s), page: page)
            letter(ctx, v.letter, in: CGRect(x: margin + 30, y: 150, width: pageW - 2 * margin - 60, height: 380),
                   style: .outline(width: 3), pattern: true)
            countingPictures(ctx, v.picture, count: i + 1, in: CGRect(x: margin + 10, y: 560, width: pageW - 2 * margin - 20, height: 210))
            return
        }
        if v.word.isEmpty {
            frame(ctx, title: "Colour the letter", titleKannada: v.letter,
                  instructions: "Colour in the big letter. Say \u{201C}\(v.sound)\u{201D} while you colour. This letter is not used at the start of words, but you will see it in old poems and songs.",
                  footer: vowelFooter(v, i, s), page: page)
            letter(ctx, v.letter, in: CGRect(x: margin + 20, y: 160, width: pageW - 2 * margin - 40, height: 580),
                   style: .outline(width: 3), pattern: true)
            return
        }
        let isFirst = !v.contains
        frame(ctx, title: "Colour the letter", titleKannada: v.letter,
              instructions: isFirst
                ? "\(v.letter) is for \(v.word) (\(v.english)). Colour in the big letter, then colour the picture. Say the word out loud."
                : "You can hear \(v.letter) in \(v.word) (\(v.english)). Colour in the big letter, then colour the picture. Say the word out loud.",
              footer: vowelFooter(v, i, s), page: page)
        letter(ctx, v.letter, in: CGRect(x: margin + 30, y: 150, width: pageW - 2 * margin - 60, height: 400),
               style: .outline(width: 3), pattern: true)

        let picBox = CGRect(x: margin + 20, y: 575, width: 190, height: 190)
        roundedBox(ctx, picBox, radius: 16)
        picture(ctx, v.picture, in: picBox.insetBy(dx: 10, dy: 10))
        let wordBox = CGRect(x: picBox.maxX + 30, y: 590, width: pageW - margin - 20 - picBox.maxX - 30, height: 120)
        letter(ctx, v.word, in: wordBox, style: .outline(width: 2.2))
        text(ctx, v.english, x: wordBox.midX, y: 748, font: latinDemi, size: 16, color: midGray, align: .center)
    }
}

/// Letters children mix up with each vowel, so the search has a real challenge.
func lookAlikes(_ s: String) -> [String] {
    switch s {
    case "ಅ": return ["ಆ", "ಅಂ", "ಅಃ"]
    case "ಆ": return ["ಅ", "ಅಂ"]
    case "ಇ": return ["ಈ", "ಐ"]
    case "ಈ": return ["ಇ", "ಐ"]
    case "ಉ": return ["ಊ", "ಋ"]
    case "ಊ": return ["ಉ", "ಋ"]
    case "ಋ": return ["ಉ", "ಊ"]
    case "ಎ": return ["ಏ", "ಐ"]
    case "ಏ": return ["ಎ", "ಐ"]
    case "ಐ": return ["ಎ", "ಏ"]
    case "ಒ": return ["ಓ", "ಔ"]
    case "ಓ": return ["ಒ", "ಔ"]
    case "ಔ": return ["ಒ", "ಓ"]
    case "ಅಂ": return ["ಅ", "ಅಃ"]
    default: return ["ಅ", "ಅಂ"]
    }
}

/// Repeatable pseudo-random numbers, so the pack is the same every time it is built.
struct Rand {
    var state: UInt64
    mutating func next() -> UInt64 {
        state = state &* 6364136223846793005 &+ 1442695040888963407
        return state >> 33
    }
    mutating func int(_ n: Int) -> Int { Int(next() % UInt64(n)) }
    mutating func shuffled<T>(_ a: [T]) -> [T] {
        var a = a
        for i in stride(from: a.count - 1, to: 0, by: -1) { a.swapAt(i, int(i + 1)) }
        return a
    }
}

/// 3. Find and circle every copy of the vowel in a grid of letters.
func findPage(_ v: Vowel, _ i: Int, _ s: Section = vowelSection) -> Page {
    { ctx, page in
        var rng = Rand(state: UInt64(1000 + i * 77 + s.items.count * 13))
        let cols = 6, rows = 7
        let total = cols * rows
        let targets = 9 + rng.int(4)
        let alike = s.alike(v.letter)
        let others = s.items.map(\.letter).filter { $0 != v.letter }
        var cells = [String](repeating: v.letter, count: targets)
        while cells.count < total {
            // Half of the other letters are look-alikes.
            cells.append(rng.int(2) == 0 ? alike[rng.int(alike.count)] : others[rng.int(others.count)])
        }
        cells = rng.shuffled(cells)

        let thing = s.isNumber ? "number" : "letter"
        frame(ctx, title: "Find and circle every", titleKannada: v.letter,
              instructions: "Look carefully at each \(thing). Draw a circle around every \(v.letter). Some \(thing)s look nearly the same, so check twice!",
              footer: vowelFooter(v, i, s) + "     Answer for grown-ups: \(targets)", page: page)

        let gridTop: CGFloat = 150
        let cellW = (pageW - 2 * margin - 12) / CGFloat(cols)
        let cellH: CGFloat = 74
        let section = s
        for (n, s) in cells.enumerated() {
            let r = n / cols, c = n % cols
            let cell = CGRect(x: margin + 6 + CGFloat(c) * cellW, y: gridTop + CGFloat(r) * cellH, width: cellW, height: cellH)
            // Same size for every letter: fit into a fixed box so wide letters shrink a little.
            let inner = cell.insetBy(dx: 8, dy: 12)
            letter(ctx, s, in: inner, style: .solid, scaleCap: gridScale(inner, sample: section.sample))
        }
        let y = gridTop + CGFloat(rows) * cellH + 34
        text(ctx, "How many did you find?", x: margin + 6, y: y + 8, font: latinBold, size: 18)
        roundedBox(ctx, CGRect(x: margin + 260, y: y - 22, width: 70, height: 48), radius: 10, stroke: midGray, width: 1.5)
    }
}

/// 4. Picture and word: trace the first letter and the whole word.
func wordPage(_ v: Vowel, _ i: Int, _ s: Section = vowelSection) -> Page {
    if s.isNumber { return countPage(v, i, s) }
    if v.word.isEmpty { return practicePage(v, i, s) }
    return { ctx, page in
        let starts = !v.contains
        frame(ctx, title: "Picture and word", titleKannada: v.word,
              instructions: starts
                ? "This is \(v.word), \(v.english). It starts with \(v.letter). Trace the letter, then trace the word and colour the picture."
                : "This is \(v.word), \(v.english). Listen for the \(v.letter) sound inside the word! Trace the word and colour the picture.",
              footer: vowelFooter(v, i, s), page: page)

        let picBox = CGRect(x: margin + 20, y: 150, width: 240, height: 240)
        roundedBox(ctx, picBox, radius: 18)
        picture(ctx, v.picture, in: picBox.insetBy(dx: 12, dy: 12))

        let rightX = picBox.maxX + 30
        let rightW = pageW - margin - 20 - rightX
        text(ctx, starts ? "It starts with" : "The sound", x: rightX + rightW / 2, y: 180, font: latinBold, size: 16,
             align: .center)
        let firstBox = CGRect(x: rightX + 10, y: 196, width: rightW - 20, height: 190)
        roundedBox(ctx, firstBox, radius: 16, stroke: midGray, width: 1.5, dashed: true)
        letter(ctx, v.letter, in: firstBox.insetBy(dx: 24, dy: 24), style: .trace(gray: 0.86))
        letter(ctx, v.letter, in: firstBox.insetBy(dx: 24, dy: 24), style: .dashed)

        let rowW = pageW - 2 * margin - 12
        let rows: [LetterStyle?] = [.trace(gray: 0.8), .dashed, nil]
        for (r, style) in rows.enumerated() {
            let box = CGRect(x: margin + 6, y: 420 + CGFloat(r) * 112, width: rowW, height: 98)
            roundedBox(ctx, box)
            hLine(ctx, x1: box.minX + 12, x2: box.maxX - 12, y: box.midY, color: paleGray, dashed: true)
            if let style {
                letter(ctx, v.word, in: box.insetBy(dx: 60, dy: 12), style: style)
            }
        }
        text(ctx, "Now write it on your own on the last line!", x: pageW / 2, y: 768, font: latinDemi, size: 13,
             color: midGray, align: .center)
    }
}

// MARK: - Numbers, practice and Ka-kaa pages

/// [count] copies of a picture, laid out in rows of up to five, centred in [box].
func countingPictures(_ ctx: CGContext, _ emoji: String, count: Int, in box: CGRect) {
    let perRow = min(5, count)
    let rows = (count + perRow - 1) / perRow
    let size = min((box.width - 12 * CGFloat(perRow - 1)) / CGFloat(perRow), (box.height - 12 * CGFloat(rows - 1)) / CGFloat(rows), 130)
    for n in 0..<count {
        let r = n / perRow, c = n % perRow
        let inRow = r == rows - 1 ? count - r * perRow : perRow
        let rowW = CGFloat(inRow) * size + CGFloat(inRow - 1) * 12
        let x = box.midX - rowW / 2 + CGFloat(c) * (size + 12)
        let totalH = CGFloat(rows) * size + CGFloat(rows - 1) * 12
        let y = box.midY - totalH / 2 + CGFloat(r) * (size + 12)
        picture(ctx, emoji, in: CGRect(x: x, y: y, width: size, height: size))
    }
}

/// Numbers, sheet 4: count the pictures, trace the number word and write the number.
func countPage(_ v: Vowel, _ i: Int, _ s: Section) -> Page {
    { ctx, page in
        frame(ctx, title: "Count and write", titleKannada: v.word,
              instructions: "Count the pictures out loud: \(v.word), \(v.english). Trace the number word, then write the number \(v.letter) in the empty boxes.",
              footer: vowelFooter(v, i, s), page: page)
        let picBox = CGRect(x: margin + 6, y: 150, width: pageW - 2 * margin - 12, height: 230)
        roundedBox(ctx, picBox, radius: 18)
        countingPictures(ctx, v.picture, count: i + 1, in: picBox.insetBy(dx: 18, dy: 16))

        let rowW = pageW - 2 * margin - 12
        for (r, style) in ([LetterStyle.trace(gray: 0.8), .dashed]).enumerated() {
            let box = CGRect(x: margin + 6, y: 400 + CGFloat(r) * 108, width: rowW, height: 94)
            roundedBox(ctx, box)
            hLine(ctx, x1: box.minX + 12, x2: box.maxX - 12, y: box.midY, color: paleGray, dashed: true)
            letter(ctx, v.word, in: box.insetBy(dx: 70, dy: 12), style: style)
        }
        text(ctx, "Write the number", x: margin + 6, y: 640, font: latinBold, size: 16)
        let cells = 4
        let gap: CGFloat = 14
        let cellW = (rowW - gap * CGFloat(cells - 1)) / CGFloat(cells)
        for c in 0..<cells {
            let cell = CGRect(x: margin + 6 + CGFloat(c) * (cellW + gap), y: 654, width: cellW, height: 100)
            roundedBox(ctx, cell, stroke: c == 0 ? midGray : lightGray, dashed: c == 0)
            if c == 0 {
                let inner = cell.insetBy(dx: 16, dy: 14)
                letter(ctx, v.letter, in: inner, style: .trace(gray: 0.86), scaleCap: gridScale(inner, sample: s.sample))
                letter(ctx, v.letter, in: inner, style: .dashed, scaleCap: gridScale(inner, sample: s.sample))
            }
        }
    }
}

/// Sheet 4 for letters with no picture word (ಙ, ಞ): more writing practice in big boxes.
func practicePage(_ v: Vowel, _ i: Int, _ s: Section) -> Page {
    { ctx, page in
        frame(ctx, title: "Write it again", titleKannada: v.letter,
              instructions: "Practice makes perfect! Trace the grey letters, then the dotted ones, then write \(v.letter) on your own. Say \u{201C}\(v.sound)\u{201D} each time.",
              footer: vowelFooter(v, i, s), page: page)
        let cols = 3
        let gap: CGFloat = 14
        let cellW = (pageW - 2 * margin - 12 - gap * CGFloat(cols - 1)) / CGFloat(cols)
        let cellH: CGFloat = 140
        let rows: [[LetterStyle?]] = [
            [.trace(gray: 0.8), .trace(gray: 0.8), .trace(gray: 0.8)],
            [.dashed, .dashed, .dashed],
            [.dashed, nil, nil],
            [nil, nil, nil],
        ]
        for (r, row) in rows.enumerated() {
            for (c, style) in row.enumerated() {
                let cell = CGRect(x: margin + 6 + CGFloat(c) * (cellW + gap), y: 150 + CGFloat(r) * (cellH + 12),
                                  width: cellW, height: cellH)
                roundedBox(ctx, cell)
                hLine(ctx, x1: cell.minX + 8, x2: cell.maxX - 8, y: cell.midY, color: paleGray, dashed: true)
                if let style {
                    let inner = cell.insetBy(dx: 18, dy: 18)
                    letter(ctx, v.letter, in: inner, style: style, scaleCap: gridScale(inner, sample: s.sample))
                }
            }
        }
    }
}

/// Vowel signs added to a consonant to make its Ka-kaa (ಕಾಗುಣಿತ) family, in vowel order.
let vowelSigns = ["", "ಾ", "ಿ", "ೀ", "ು", "ೂ", "ೃ", "ೆ", "ೇ", "ೈ", "ೊ", "ೋ", "ೌ", "ಂ", "ಃ"]

/// The 15 forms of consonant [c], with how each is said (ka, kaa, ki …).
func kakaaForms(_ c: Vowel) -> [(form: String, sound: String)] {
    let base = c.sound.hasSuffix("a") ? String(c.sound.dropLast()) : c.sound
    return vowelSigns.enumerated().map { n, sign in (c.letter + sign, base + vowels[n].sound) }
}

let kakaaSample = ["ಕೀ", "ಕೋ", "ಕೈ", "ಕೃ"]

/// One letter size for a whole Ka-kaa family: the tallest form fills [box] in height, and wide
/// forms (ಕಾ, ಕೋ) only shrink if they would not fit the width.
func kakaaScale(_ c: Vowel, _ box: CGRect) -> CGFloat {
    let tallest = kakaaForms(c).map { glyphPath($0.form).boundingBoxOfPath.height }.max() ?? 1
    return box.height / tallest
}

func kakaaFooter(_ c: Vowel, _ i: Int) -> String {
    "Ka-kaa \(i + 1) of \(consonants.count):  the \(c.letter) family (\(c.sound))"
}

/// Ka-kaa sheet 1: trace all 15 forms of one consonant, each with its vowel.
func kakaaTracePage(_ c: Vowel, _ i: Int) -> Page {
    { ctx, page in
        let forms = kakaaForms(c)
        frame(ctx, title: "Trace the family of", titleKannada: c.letter,
              instructions: "Each vowel sign changes \(c.letter) into a new sound. Say each one as you trace it: \(forms[0].sound), \(forms[1].sound), \(forms[2].sound) and so on.",
              footer: kakaaFooter(c, i), page: page)
        let cols = 5
        let gap: CGFloat = 10
        let cellW = (pageW - 2 * margin - 12 - gap * CGFloat(cols - 1)) / CGFloat(cols)
        let cellH: CGFloat = 132
        for (n, f) in forms.enumerated() {
            let r = n / cols, col = n % cols
            let cell = CGRect(x: margin + 6 + CGFloat(col) * (cellW + gap), y: 148 + CGFloat(r) * (cellH + 10),
                              width: cellW, height: cellH)
            roundedBox(ctx, cell)
            letter(ctx, vowels[n].letter, in: CGRect(x: cell.minX + 7, y: cell.minY + 7, width: 20, height: 18), style: .solid,
                   scaleCap: gridScale(CGRect(x: 0, y: 0, width: 20, height: 18)))
            let inner = CGRect(x: cell.minX + 6, y: cell.minY + 28, width: cell.width - 12, height: cell.height - 54)
            letter(ctx, f.form, in: inner, style: .trace(gray: 0.84), scaleCap: kakaaScale(c, inner))
            letter(ctx, f.form, in: inner, style: .dashed, scaleCap: kakaaScale(c, inner))
            text(ctx, f.sound, x: cell.midX, y: cell.maxY - 10, font: latinDemi, size: 12, color: midGray, align: .center)
        }
        text(ctx, "Now write five on your own:", x: margin + 6, y: 600, font: latinBold, size: 16)
        for col in 0..<cols {
            let cell = CGRect(x: margin + 6 + CGFloat(col) * (cellW + gap), y: 614, width: cellW, height: 110)
            roundedBox(ctx, cell)
            hLine(ctx, x1: cell.minX + 8, x2: cell.maxX - 8, y: cell.midY, color: paleGray, dashed: true)
            let n = [1, 2, 4, 7, 10][col]
            letter(ctx, vowels[n].letter, in: CGRect(x: cell.minX + 7, y: cell.minY + 7, width: 20, height: 18), style: .solid,
                   scaleCap: gridScale(CGRect(x: 0, y: 0, width: 20, height: 18)))
            text(ctx, forms[n].sound, x: cell.midX, y: cell.maxY - 10, font: latinDemi, size: 12, color: lightGray, align: .center)
        }
    }
}

/// Ka-kaa sheet 2: join each vowel to its form, then fill in the missing forms.
func kakaaJoinPage(_ c: Vowel, _ i: Int) -> Page {
    { ctx, page in
        let forms = kakaaForms(c)
        var rng = Rand(state: UInt64(500 + i * 31))
        // Six vowels to match, a different mix for each consonant (always short + long pairs).
        let pickSets = [[1, 2, 4, 7, 9, 10], [0, 3, 5, 8, 11, 12], [1, 3, 6, 7, 10, 13], [2, 4, 5, 9, 11, 14]]
        let picks = pickSets[i % pickSets.count]
        let right = rng.shuffled(picks)
        let blanks: Set<Int> = Set([[2, 5, 9], [1, 4, 8], [3, 6, 10], [2, 7, 9]][i % 4])
        let answers = blanks.sorted().map { forms[$0].form }.joined(separator: " ")
        frame(ctx, title: "Join the vowel to", titleKannada: c.letter + "...",
              instructions: "Each vowel on the left gives \(c.letter) a new sound. Draw a line to the letter it makes. Then fill in the missing letters below.",
              footer: kakaaFooter(c, i) + "     Answers: " + answers, page: page)
        let rowH: CGFloat = 66
        for n in 0..<picks.count {
            let y = 148 + CGFloat(n) * rowH
            let lBox = CGRect(x: margin + 40, y: y, width: 96, height: 56)
            let rBox = CGRect(x: pageW - margin - 136, y: y, width: 96, height: 56)
            roundedBox(ctx, lBox, radius: 14)
            roundedBox(ctx, rBox, radius: 14)
            let lInner = lBox.insetBy(dx: 18, dy: 8), rInner = rBox.insetBy(dx: 14, dy: 8)
            letter(ctx, vowels[picks[n]].letter, in: lInner, style: .solid, scaleCap: gridScale(lInner))
            letter(ctx, forms[right[n]].form, in: rInner, style: .solid, scaleCap: kakaaScale(c, rInner))
            dot(ctx, x: lBox.maxX + 22, y: lBox.midY)
            dot(ctx, x: rBox.minX - 22, y: rBox.midY)
        }
        text(ctx, "Fill in the missing letters", x: margin + 6, y: 570, font: latinBold, size: 16)
        let cols = 6
        let gap: CGFloat = 8
        let cellW = (pageW - 2 * margin - 12 - gap * CGFloat(cols - 1)) / CGFloat(cols)
        for n in 0..<12 {
            let r = n / cols, col = n % cols
            let cell = CGRect(x: margin + 6 + CGFloat(col) * (cellW + gap), y: 586 + CGFloat(r) * 92, width: cellW, height: 82)
            if blanks.contains(n) {
                roundedBox(ctx, cell, radius: 12, stroke: midGray, width: 1.5, dashed: true)
            } else {
                roundedBox(ctx, cell, radius: 12)
                let inner = cell.insetBy(dx: 8, dy: 12)
                letter(ctx, forms[n].form, in: inner, style: .solid, scaleCap: kakaaScale(c, inner))
            }
        }
    }
}

/// Cover for a section kit: title, and the section's letters in outline to colour.
func sectionCoverPage(kannada: String, english: String, subtitle: String, glyphs: [String], sample: [String]) -> Page {
    { ctx, page in
        roundedBox(ctx, CGRect(x: 18, y: 18, width: pageW - 36, height: pageH - 36), radius: 22, stroke: lightGray, width: 2)
        text(ctx, appName, x: pageW / 2, y: 80, font: latinBold, size: 16, color: midGray, align: .center)
        text(ctx, kannada, x: pageW / 2, y: 170, font: kannadaBold, size: 60, align: .center)
        text(ctx, english, x: pageW / 2, y: 225, font: latinBold, size: 30, align: .center)
        text(ctx, subtitle, x: pageW / 2, y: 258, font: latinRegular, size: 15, color: midGray, align: .center)
        let cols = glyphs.count > 15 ? 6 : 5
        let rows = (glyphs.count + cols - 1) / cols
        let cellW = (pageW - 2 * 60) / CGFloat(cols)
        let cellH = min(125, 390 / CGFloat(rows))
        for (n, g) in glyphs.enumerated() {
            let r = n / cols, c = n % cols
            let box = CGRect(x: 60 + CGFloat(c) * cellW + 4, y: 290 + CGFloat(r) * cellH, width: cellW - 8, height: cellH - 8)
            let inner = box.insetBy(dx: 6, dy: 6)
            letter(ctx, g, in: inner, style: .outline(width: cellH > 90 ? 2 : 1.4), scaleCap: gridScale(inner, sample: sample))
        }
        text(ctx, "Colour the letters on this cover too!", x: pageW / 2, y: 710, font: latinDemi, size: 14,
             color: midGray, align: .center)
        text(ctx, "This pack belongs to: ______________________", x: pageW / 2, y: 765, font: latinRegular,
             size: 15, align: .center)
    }
}

/// Certificate for a section, with a star for each letter.
func sectionCertificatePage(champion: String, line: String, glyphs: [String], sample: [String]) -> Page {
    { ctx, page in
        roundedBox(ctx, CGRect(x: 18, y: 18, width: pageW - 36, height: pageH - 36), radius: 22, stroke: ink, width: 3)
        roundedBox(ctx, CGRect(x: 30, y: 30, width: pageW - 60, height: pageH - 60), radius: 16, stroke: midGray, width: 1)
        text(ctx, appName, x: pageW / 2, y: 80, font: latinBold, size: 14, color: midGray, align: .center)
        text(ctx, "ಶಭಾಷ್!", x: pageW / 2, y: 145, font: kannadaBold, size: 50, align: .center)
        text(ctx, champion, x: pageW / 2, y: 198, font: latinBold, size: 32, align: .center)
        text(ctx, "This certificate is for", x: pageW / 2, y: 240, font: latinRegular, size: 15, color: midGray, align: .center)
        hLine(ctx, x1: 140, x2: pageW - 140, y: 288, color: ink, width: 1.2)
        text(ctx, line, x: pageW / 2, y: 316, font: latinDemi, size: 16, align: .center)
        text(ctx, "Colour a star for each one you know.", x: pageW / 2, y: 350, font: latinRegular, size: 13,
             color: midGray, align: .center)
        let cols = glyphs.count > 15 ? 6 : 5
        let rows = (glyphs.count + cols - 1) / cols
        let cellW = (pageW - 2 * 64) / CGFloat(cols)
        let cellH = min(100, 360 / CGFloat(rows))
        let r0: CGFloat = min(44, cellH * 0.46)
        for (n, g) in glyphs.enumerated() {
            let r = n / cols, c = n % cols
            let x = 64 + cellW * (CGFloat(c) + 0.5)
            let y = 400 + CGFloat(r) * cellH
            star(ctx, x: x, y: y, r: r0)
            let inner = CGRect(x: x - r0 * 0.36, y: y - r0 * 0.3, width: r0 * 0.72, height: r0 * 0.62)
            letter(ctx, g, in: inner, style: .solid, scaleCap: gridScale(inner, sample: sample))
        }
        text(ctx, "Date: ______________", x: 70, y: 775, font: latinRegular, size: 13, color: midGray)
        text(ctx, "Signed: ______________", x: pageW - 70, y: 775, font: latinRegular, size: 13, color: midGray, align: .right)
    }
}

// MARK: - Review pages

func reviewFooter() -> String { "Vowel review" }

/// Join each short vowel to its long partner.
func pairsPage() -> Page {
    { ctx, page in
        frame(ctx, title: "Short and long: join the pairs",
              instructions: "Each short vowel on the left has a long partner on the right that looks almost the same. Say both sounds, then draw a line to join them.",
              footer: reviewFooter() + "     Pairs: ಅ-ಆ, ಇ-ಈ, ಉ-ಊ, ಎ-ಏ, ಒ-ಓ", page: page)
        let left = ["ಅ", "ಇ", "ಉ", "ಎ", "ಒ"]
        let right = ["ಈ", "ಓ", "ಆ", "ಊ", "ಏ"]
        let rowH: CGFloat = 120
        for n in 0..<5 {
            let y = 160 + CGFloat(n) * rowH
            let lBox = CGRect(x: margin + 30, y: y, width: 130, height: 100)
            let rBox = CGRect(x: pageW - margin - 160, y: y, width: 130, height: 100)
            roundedBox(ctx, lBox, radius: 16)
            roundedBox(ctx, rBox, radius: 16)
            let cap = gridScale(lBox.insetBy(dx: 22, dy: 16))
            letter(ctx, left[n], in: lBox.insetBy(dx: 22, dy: 16), style: .solid, scaleCap: cap)
            letter(ctx, right[n], in: rBox.insetBy(dx: 22, dy: 16), style: .solid, scaleCap: cap)
            dot(ctx, x: lBox.maxX + 22, y: lBox.midY)
            dot(ctx, x: rBox.minX - 22, y: rBox.midY)
        }
    }
}

/// Join each picture to the vowel it starts with.
func pictureMatchPage(_ items: [Int], part: Int, seed: UInt64) -> Page {
    { ctx, page in
        frame(ctx, title: "Match the picture to its letter",
              instructions: "Say the name of each picture. Which vowel does it start with? Draw a line from the picture to the letter.",
              footer: reviewFooter() + "  (part \(part) of 2)", page: page)
        var rng = Rand(state: seed)
        let shuffled = rng.shuffled(items)
        let rowH = (contentBottom - 150) / CGFloat(items.count)
        for (n, vi) in items.enumerated() {
            let v = vowels[vi]
            let y = 150 + CGFloat(n) * rowH
            let picBox = CGRect(x: margin + 30, y: y + 4, width: rowH - 8, height: rowH - 8)
            picture(ctx, v.picture, in: picBox)
            text(ctx, v.english, x: picBox.maxX + 10, y: picBox.midY + 5, font: latinRegular, size: 12, color: midGray)
            dot(ctx, x: picBox.maxX + 110, y: picBox.midY)

            let lv = vowels[shuffled[n]]
            let lBox = CGRect(x: pageW - margin - 130, y: y + 6, width: 100, height: rowH - 12)
            roundedBox(ctx, lBox, radius: 14)
            let inner = lBox.insetBy(dx: 18, dy: 10)
            letter(ctx, lv.letter, in: inner, style: .solid, scaleCap: gridScale(inner))
            dot(ctx, x: lBox.minX - 22, y: lBox.midY)
        }
    }
}

/// Colour by letter: a flower whose parts are marked with vowels; a key says which colour each is.
func colourByLetterPage() -> Page {
    { ctx, page in
        frame(ctx, title: "Colour by letter",
              instructions: "Look at the letter in each part of the flower. Find it in the colour key at the bottom and colour that part.",
              footer: reviewFooter(), page: page)
        let cx = pageW / 2
        let cy: CGFloat = 350       // top-left coordinates
        let pcy = pageH - cy        // page (y-up) coordinates
        ctx.saveGState()
        ctx.setLineWidth(2.2); ctx.setStrokeColor(ink); ctx.setFillColor(.white)

        // Stem and leaves (ಈ), drawn first so the flower sits on top.
        let stem = CGMutablePath()
        stem.addRect(CGRect(x: cx - 9, y: pageH - 690, width: 18, height: (pageH - cy) - (pageH - 690)))
        ctx.addPath(stem); ctx.drawPath(using: .fillStroke)
        for side in [-1.0, 1.0] {
            let leaf = CGMutablePath()
            let baseY = pageH - 600
            leaf.move(to: CGPoint(x: cx, y: baseY))
            leaf.addQuadCurve(to: CGPoint(x: cx + side * 130, y: baseY + 40), control: CGPoint(x: cx + side * 60, y: baseY + 70))
            leaf.addQuadCurve(to: CGPoint(x: cx, y: baseY), control: CGPoint(x: cx + side * 80, y: baseY - 20))
            ctx.addPath(leaf); ctx.drawPath(using: .fillStroke)
        }
        // Outer petals (ಇ), inner petals (ಆ), centre (ಅ).
        func petals(count: Int, dist: CGFloat, len: CGFloat, wid: CGFloat, offset: CGFloat) -> [CGPoint] {
            var centres: [CGPoint] = []
            for k in 0..<count {
                let a = CGFloat(k) / CGFloat(count) * 2 * .pi + offset
                var t = CGAffineTransform(translationX: cx, y: pcy).rotated(by: a).translatedBy(x: dist, y: 0)
                let p = CGPath(ellipseIn: CGRect(x: -len / 2, y: -wid / 2, width: len, height: wid), transform: &t)
                ctx.addPath(p); ctx.drawPath(using: .fillStroke)
                centres.append(CGPoint(x: cx + cos(a) * dist, y: cy - sin(a) * dist))
            }
            return centres
        }
        let outer = petals(count: 12, dist: 140, len: 112, wid: 58, offset: .pi / 12)
            .map { CGPoint(x: cx + ($0.x - cx) * 1.22, y: cy + ($0.y - cy) * 1.22) }  // label the visible tip
        let inner = petals(count: 8, dist: 88, len: 96, wid: 56, offset: 0)
        ctx.addEllipse(in: CGRect(x: cx - 52, y: pcy - 52, width: 104, height: 104)); ctx.drawPath(using: .fillStroke)
        // Pot (ಉ)
        let pot = CGMutablePath()
        pot.move(to: CGPoint(x: cx - 105, y: pageH - 680))
        pot.addLine(to: CGPoint(x: cx + 105, y: pageH - 680))
        pot.addLine(to: CGPoint(x: cx + 78, y: pageH - 745))
        pot.addLine(to: CGPoint(x: cx - 78, y: pageH - 745))
        pot.closeSubpath()
        ctx.addPath(pot); ctx.drawPath(using: .fillStroke)
        ctx.restoreGState()

        for p in outer { letter(ctx, "ಇ", in: CGRect(x: p.x - 12, y: p.y - 12, width: 24, height: 24), style: .solid) }
        for p in inner { letter(ctx, "ಆ", in: CGRect(x: p.x - 13, y: p.y - 13, width: 26, height: 26), style: .solid) }
        letter(ctx, "ಅ", in: CGRect(x: cx - 20, y: cy - 20, width: 40, height: 40), style: .solid)
        letter(ctx, "ಈ", in: CGRect(x: cx - 88, y: 560, width: 24, height: 24), style: .solid)
        letter(ctx, "ಈ", in: CGRect(x: cx + 64, y: 560, width: 24, height: 24), style: .solid)
        letter(ctx, "ಉ", in: CGRect(x: cx - 18, y: 696, width: 36, height: 30), style: .solid)

        // Key
        let key: [(String, String, CGColor)] = [
            ("ಅ", "yellow", CGColor(red: 0.98, green: 0.82, blue: 0.2, alpha: 1)),
            ("ಆ", "orange", CGColor(red: 0.98, green: 0.55, blue: 0.15, alpha: 1)),
            ("ಇ", "pink", CGColor(red: 0.95, green: 0.45, blue: 0.65, alpha: 1)),
            ("ಈ", "green", CGColor(red: 0.3, green: 0.7, blue: 0.3, alpha: 1)),
            ("ಉ", "brown", CGColor(red: 0.55, green: 0.35, blue: 0.2, alpha: 1)),
        ]
        let keyW = (pageW - 2 * margin - 12) / CGFloat(key.count)
        for (n, k) in key.enumerated() {
            let x = margin + 6 + CGFloat(n) * keyW
            let y: CGFloat = 776
            letter(ctx, k.0, in: CGRect(x: x + 2, y: y - 11, width: 24, height: 22), style: .solid)
            text(ctx, "=", x: x + 30, y: y + 5, font: latinBold, size: 13)
            ctx.setFillColor(k.2)
            ctx.fillEllipse(in: CGRect(x: x + 44, y: pageH - y - 8, width: 16, height: 16))
            text(ctx, k.1, x: x + 64, y: y + 4, font: latinRegular, size: 10)
        }
    }
}

/// Fill in the missing vowels in order.
func missingPage() -> Page {
    { ctx, page in
        let blanks: Set<Int> = [2, 5, 7, 10, 12, 14]
        let answers = blanks.sorted().map { vowels[$0].letter }.joined(separator: " ")
        frame(ctx, title: "Fill in the missing vowels",
              instructions: "Say the vowels in order: ಅ, ಆ, ಇ and so on. When you reach an empty box, write the vowel that is missing.",
              footer: reviewFooter() + "     Answers: " + answers, page: page)
        let cols = 5
        let gap: CGFloat = 12
        let cellW = (pageW - 2 * margin - 12 - gap * CGFloat(cols - 1)) / CGFloat(cols)
        let cellH: CGFloat = 110
        for n in 0..<15 {
            let r = n / cols, c = n % cols
            let cell = CGRect(x: margin + 6 + CGFloat(c) * (cellW + gap), y: 160 + CGFloat(r) * (cellH + 16),
                              width: cellW, height: cellH)
            if blanks.contains(n) {
                roundedBox(ctx, cell, radius: 14, stroke: midGray, width: 1.5, dashed: true)
                text(ctx, "?", x: cell.maxX - 14, y: cell.minY + 22, font: latinBold, size: 14, color: lightGray, align: .right)
            } else {
                roundedBox(ctx, cell, radius: 14)
                let inner = cell.insetBy(dx: 18, dy: 22)
                letter(ctx, vowels[n].letter, in: inner, style: .solid, scaleCap: gridScale(inner))
            }
            text(ctx, "\(n + 1)", x: cell.minX + 10, y: cell.minY + 20, font: latinDemi, size: 11, color: midGray)
        }

        text(ctx, "What comes next?", x: margin + 6, y: 590, font: latinBold, size: 18)
        let seqs = [["ಅ", "ಆ"], ["ಉ", "ಊ"], ["ಎ", "ಏ"], ["ಒ", "ಓ"]]
        for (n, seq) in seqs.enumerated() {
            let x = margin + 6 + CGFloat(n % 2) * 262
            let y = 610 + CGFloat(n / 2) * 82
            for (k, s) in (seq + [""]).enumerated() {
                let cell = CGRect(x: x + CGFloat(k) * 80, y: y, width: 68, height: 68)
                if s.isEmpty {
                    roundedBox(ctx, cell, radius: 12, stroke: midGray, width: 1.5, dashed: true)
                } else {
                    roundedBox(ctx, cell, radius: 12)
                    let inner = cell.insetBy(dx: 12, dy: 12)
                    letter(ctx, s, in: inner, style: .solid, scaleCap: gridScale(inner))
                }
            }
        }
    }
}

/// Certificate with a star for each vowel.
func certificatePage() -> Page {
    { ctx, page in
        roundedBox(ctx, CGRect(x: 18, y: 18, width: pageW - 36, height: pageH - 36), radius: 22, stroke: ink, width: 3)
        roundedBox(ctx, CGRect(x: 30, y: 30, width: pageW - 60, height: pageH - 60), radius: 16, stroke: midGray, width: 1)
        text(ctx, appName, x: pageW / 2, y: 80, font: latinBold, size: 14, color: midGray, align: .center)
        text(ctx, "ಶಭಾಷ್!", x: pageW / 2, y: 150, font: kannadaBold, size: 54, align: .center)
        text(ctx, "Vowel Champion", x: pageW / 2, y: 205, font: latinBold, size: 34, align: .center)
        text(ctx, "This certificate is for", x: pageW / 2, y: 250, font: latinRegular, size: 15, color: midGray, align: .center)
        hLine(ctx, x1: 140, x2: pageW - 140, y: 300, color: ink, width: 1.2)
        text(ctx, "who learned all 15 Kannada vowels!", x: pageW / 2, y: 330, font: latinDemi, size: 16, align: .center)
        text(ctx, "Colour a star for each vowel you know.", x: pageW / 2, y: 372, font: latinRegular, size: 13,
             color: midGray, align: .center)

        let cols = 5
        let cellW = (pageW - 2 * 70) / CGFloat(cols)
        for (n, v) in vowels.enumerated() {
            let r = n / cols, c = n % cols
            let x = 70 + cellW * (CGFloat(c) + 0.5)
            let y = 445 + CGFloat(r) * 100
            star(ctx, x: x, y: y, r: 44)
            let inner = CGRect(x: x - 16, y: y - 13, width: 32, height: 28)
            letter(ctx, v.letter, in: inner, style: .solid, scaleCap: gridScale(inner))
        }
        text(ctx, "Date: ______________", x: 70, y: 770, font: latinRegular, size: 13, color: midGray)
        text(ctx, "Signed: ______________", x: pageW - 70, y: 770, font: latinRegular, size: 13, color: midGray, align: .right)
    }
}

// MARK: - Cover and parent guide

func coverPage() -> Page {
    { ctx, page in
        roundedBox(ctx, CGRect(x: 18, y: 18, width: pageW - 36, height: pageH - 36), radius: 22, stroke: lightGray, width: 2)
        text(ctx, appName, x: pageW / 2, y: 80, font: latinBold, size: 16, color: midGray, align: .center)
        text(ctx, "ಸ್ವರಗಳು", x: pageW / 2, y: 170, font: kannadaBold, size: 64, align: .center)
        text(ctx, "Vowels Worksheet Pack", x: pageW / 2, y: 225, font: latinBold, size: 32, align: .center)
        text(ctx, "Trace, colour, find and match all 15 Kannada vowels", x: pageW / 2, y: 258, font: latinRegular,
             size: 15, color: midGray, align: .center)

        let cols = 5
        let cellW = (pageW - 2 * 60) / CGFloat(cols)
        for (n, v) in vowels.enumerated() {
            let r = n / cols, c = n % cols
            let box = CGRect(x: 60 + CGFloat(c) * cellW + 6, y: 300 + CGFloat(r) * 125, width: cellW - 12, height: 110)
            let inner = box.insetBy(dx: 8, dy: 8)
            letter(ctx, v.letter, in: inner, style: .outline(width: 2), scaleCap: gridScale(inner))
        }
        text(ctx, "Colour the letters on this cover too!", x: pageW / 2, y: 700, font: latinDemi, size: 14,
             color: midGray, align: .center)
        text(ctx, "This pack belongs to: ______________________", x: pageW / 2, y: 760, font: latinRegular,
             size: 15, align: .center)
    }
}

func guidePage() -> Page {
    { ctx, page in
        frame(ctx, title: "For parents", instructions: "How to use this pack with your child.",
              footer: "Print on A4 or Letter paper at 100% scale, in black and white.", page: page)
        var y: CGFloat = 158
        let w = pageW - 2 * margin - 12
        let x = margin + 6
        let sections: [(String, String)] = [
            ("Print one vowel at a time",
             "Each vowel has four sheets: tracing, colouring, find-and-circle, and picture-and-word. In the app, open the Letter Box, then Printable worksheets on the Vowels tab. Tap the vowels your child has learned to select them, then tap Print. They come out together in one file."),
            ("Tracing",
             "Let your child trace the big letter with a finger first, saying the sound out loud. Then use a thick crayon or pencil on the grey letters, then the dotted ones, and finally try one alone in the empty boxes."),
            ("Short and long vowels",
             "Many Kannada vowels come in pairs that look nearly the same, like ಅ and ಆ, or ಇ and ಈ. The find-and-circle sheets mix these on purpose. The review pages at the end practise them together."),
            ("Review sheets and answers",
             "Print the review sheets once your child knows a few vowels. Answers for the find-and-circle and fill-in pages are printed in small text at the bottom of the page, for grown-ups."),
            ("Keep it short and fun",
             "One or two sheets a day is plenty for young children. Praise every try. Colour a star on the certificate in this starter kit each time your child knows a new vowel."),
        ]
        for (title, body) in sections {
            text(ctx, title, x: x, y: y, font: latinBold, size: 15)
            y = paragraph(ctx, body, x: x, y: y + 21, width: w, font: latinRegular, size: 12, leading: 17) + 12
        }

        text(ctx, "The 15 vowels and their sounds", x: x, y: y + 6, font: latinBold, size: 16)
        let cols = 5
        let cellW = w / CGFloat(cols)
        for (n, v) in vowels.enumerated() {
            let r = n / cols, c = n % cols
            let cx = x + cellW * CGFloat(c)
            let cy = y + 22 + CGFloat(r) * 50
            let inner = CGRect(x: cx + 4, y: cy, width: 44, height: 38)
            letter(ctx, v.letter, in: inner, style: .solid, scaleCap: gridScale(inner))
            text(ctx, v.sound, x: cx + 56, y: cy + 25, font: latinDemi, size: 14, color: midGray)
        }
    }
}

// MARK: - Build

func render(_ pages: [Page], to url: URL, title: String) {
    var box = CGRect(x: 0, y: 0, width: pageW, height: pageH)
    let info: [CFString: Any] = [kCGPDFContextTitle: title, kCGPDFContextCreator: appName]
    guard let ctx = CGContext(url as CFURL, mediaBox: &box, info as CFDictionary) else {
        print("Could not create \(url.path)"); exit(1)
    }
    for (n, draw) in pages.enumerated() {
        ctx.beginPDFPage(nil)
        draw(ctx, n + 1)
        ctx.endPDFPage()
    }
    ctx.closePDF()
}

let args = CommandLine.arguments.dropFirst()
let wantFull = args.contains("--full")
let wantPairs = args.contains("--pairs")
let folderArg = args.first { !$0.hasPrefix("--") }
let outDir = URL(fileURLWithPath: folderArg ?? "../../composeApp/src/commonMain/composeResources/files/worksheets",
                 isDirectory: true)
try? FileManager.default.createDirectory(at: outDir, withIntermediateDirectories: true)

func save(_ pages: [Page], _ file: String, _ title: String) {
    render(pages, to: outDir.appendingPathComponent(file), title: title)
    print("  \(file)  (\(pages.count) pages)")
}

let perVowel = vowels.enumerated().map { i, v in [tracePage(v, i), colourPage(v, i), findPage(v, i), wordPage(v, i)] }
let reviewPages: [Page] = [
    pairsPage(),
    pictureMatchPage(Array(0..<7), part: 1, seed: 11),
    pictureMatchPage(Array(7..<14), part: 2, seed: 23),
    colourByLetterPage(),
    missingPage(),
]

print("Writing worksheets to \(outDir.standardizedFileURL.path)")
save([coverPage(), guidePage(), certificatePage()], "starter-kit.pdf", "Kannada vowels: starter kit")
for (i, v) in vowels.enumerated() {
    save(perVowel[i], String(format: "vowel-%02d-%@.pdf", i + 1, v.sound), "Kannada vowel \(v.letter) worksheets")
}
/// Vowels taught together (indexes into [vowels]); keep in step with Worksheets.kt in the app.
let pairs: [(Int, Int)] = [(0, 1), (2, 3), (4, 5), (7, 8), (10, 11), (9, 12)]
for (n, p) in pairs.enumerated() where wantPairs {
    let a = vowels[p.0], b = vowels[p.1]
    save(perVowel[p.0] + perVowel[p.1], String(format: "pair-%02d-%@-%@.pdf", n + 1, a.sound, b.sound),
         "Kannada vowels \(a.letter) and \(b.letter) worksheets")
}
save(reviewPages, "review.pdf", "Kannada vowels: review sheets")
if wantFull {
    save([coverPage(), guidePage()] + perVowel.flatMap { $0 } + reviewPages + [certificatePage()],
         "Kannada-Vowels-Worksheet-Pack.pdf", "Kannada Vowels Worksheet Pack")
}

// Consonants, Ka-kaa and numbers
let two = { (n: Int) in String(format: "%02d", n) }
save([sectionCoverPage(kannada: "ವ್ಯಂಜನಗಳು", english: "Consonants Worksheet Pack",
                       subtitle: "Trace, colour, find and read all 34 Kannada consonants",
                       glyphs: consonants.map(\.letter), sample: consonantSection.sample),
      sectionCertificatePage(champion: "Consonant Champion", line: "who learned all 34 Kannada consonants!",
                             glyphs: consonants.map(\.letter), sample: consonantSection.sample)],
     "consonants-kit.pdf", "Kannada consonants: cover and certificate")
for (i, c) in consonants.enumerated() {
    save([tracePage(c, i, consonantSection), colourPage(c, i, consonantSection), findPage(c, i, consonantSection),
          wordPage(c, i, consonantSection)], "consonant-\(two(i + 1)).pdf", "Kannada consonant \(c.letter) worksheets")
}
save([sectionCoverPage(kannada: "ಕಾಗುಣಿತ", english: "Ka-kaa Worksheet Pack",
                       subtitle: "Every consonant with every vowel sign: ಕ ಕಾ ಕಿ ಕೀ …",
                       glyphs: kakaaForms(consonants[0]).map(\.form), sample: kakaaSample),
      sectionCertificatePage(champion: "Ka-kaa Champion", line: "who can read the Ka-kaa letters!",
                             glyphs: consonants.map(\.letter), sample: consonantSection.sample)],
     "kakaa-kit.pdf", "Kannada Ka-kaa: cover and certificate")
for (i, c) in consonants.enumerated() {
    save([kakaaTracePage(c, i), kakaaJoinPage(c, i)], "kakaa-\(two(i + 1)).pdf", "Kannada Ka-kaa \(c.letter) worksheets")
}
save([sectionCoverPage(kannada: "ಅಂಕೆಗಳು", english: "Numbers Worksheet Pack",
                       subtitle: "Trace, colour and count Kannada numbers 1 to 10",
                       glyphs: numbers.map(\.letter), sample: numberSection.sample),
      sectionCertificatePage(champion: "Number Champion", line: "who can count from ೧ to ೧೦ in Kannada!",
                             glyphs: numbers.map(\.letter), sample: numberSection.sample)],
     "numbers-kit.pdf", "Kannada numbers: cover and certificate")
for (i, n) in numbers.enumerated() {
    save([tracePage(n, i, numberSection), colourPage(n, i, numberSection), findPage(n, i, numberSection),
          wordPage(n, i, numberSection)], "number-\(two(i + 1)).pdf", "Kannada number \(n.letter) worksheets")
}
