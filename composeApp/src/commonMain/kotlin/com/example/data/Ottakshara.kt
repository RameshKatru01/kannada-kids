package com.example.data

/**
 * Ottakshara (ಒತ್ತಕ್ಷರ): two consonants joined, the second written small underneath (ಕ + ಕ = ಕ್ಕ).
 * 30 of them in 4 groups, each with an example word and picture.
 */
data class OttaLetter(
    val glyph: String,      // ಕ್ಕ
    val first: String,      // ಕ
    val second: String,     // ಕ
    val group: Int,         // index into OttaksharaData.groups
    val word: String,       // ಬೆಕ್ಕು
    val english: String,    // cat
    val picture: String     // 🐱
) {
    /** The word written without the ottakshara (ಬೆಕ್ಕು → ಬೆಕು): a near miss for "Spot it". */
    val withoutOtta: String get() = word.replaceFirst(glyph, first)
}

object OttaksharaData {
    data class Group(val name: String, val kannada: String)

    private val kannadaGroups = listOf(
        Group("Twin letters", "ಒಂದೇ ಅಕ್ಷರ ಎರಡು ಸಲ"),
        Group("With ರ", "ರ ಒತ್ತು"),
        Group("With ಯ and ವ", "ಯ / ವ ಒತ್ತು"),
        Group("Mixed pairs", "ಬೇರೆ ಬೇರೆ ಅಕ್ಷರ")
    )
    private val hindiGroups = listOf(
        Group("Twin letters", "दो एक जैसे अक्षर"),
        Group("With र", "र वाले अक्षर"),
        Group("With य and व", "य / व वाले अक्षर"),
        Group("Mixed pairs", "अलग-अलग अक्षर")
    )
    val groups: List<Group> get() = if (Lang.isHindi) hindiGroups else kannadaGroups

    private fun o(first: String, second: String, group: Int, word: String, english: String, picture: String) =
        OttaLetter(first + (if (first[0] in '\u0900'..'\u097F') "्" else "್") + second, first, second, group, word, english, picture)

    /** The joined letters of the language being learned. */
    val letters: List<OttaLetter> get() = if (Lang.isHindi) hindiLetters else kannadaLetters

    /** Hindi संयुक्ताक्षर (joined letters). */
    val hindiLetters: List<OttaLetter> by lazy { listOf(
        // Twin letters
        o("क", "क", 0, "मक्का", "corn", "🌽"),
        o("च", "च", 0, "बच्चा", "child", "🧒"),
        o("ड", "ड", 0, "हड्डी", "bone", "🦴"),
        o("त", "त", 0, "कुत्ता", "dog", "🐕"),
        o("ल", "ल", 0, "बिल्ली", "cat", "🐱"),
        o("न", "न", 0, "गन्ना", "sugarcane", "🎋"),
        o("म", "म", 0, "चम्मच", "spoon", "🥄"),
        o("स", "स", 0, "रस्सी", "rope", "🪢"),
        o("ब", "ब", 0, "डिब्बा", "box", "📦"),
        // With र
        o("प", "र", 1, "प्रणाम", "greeting", "🙏"),
        o("क", "र", 1, "क्रिकेट", "cricket", "🏏"),
        o("ग", "र", 1, "ग्रह", "planet", "🪐"),
        o("त", "र", 1, "त्रिभुज", "triangle", "🔺"),
        o("द", "र", 1, "चंद्रमा", "moon", "🌙"),
        o("र", "य", 1, "सूर्य", "sun", "☀️"),
        // With य and व
        o("प", "य", 2, "प्यार", "love", "❤️"),
        o("व", "य", 2, "व्यायाम", "exercise", "🏋️"),
        o("स", "व", 2, "स्वाद", "taste", "😋"),
        o("द", "व", 2, "द्वार", "door", "🚪"),
        // Mixed pairs
        o("स", "त", 3, "पुस्तक", "book", "📚"),
        o("क", "ष", 3, "पक्षी", "bird", "🐦"),
        o("ज", "ञ", 3, "ज्ञान", "knowledge", "💡"),
        o("स", "क", 3, "स्कूल", "school", "🏫"),
        o("स", "न", 3, "स्नान", "bath", "🛁"),
        o("ट", "ठ", 3, "चिट्ठी", "letter", "✉️")
    ) }

    val kannadaLetters: List<OttaLetter> = listOf(
        // Twin letters
        o("ಕ", "ಕ", 0, "ಬೆಕ್ಕು", "cat", "🐱"),
        o("ಗ", "ಗ", 0, "ಹಗ್ಗ", "rope", "🪢"),
        o("ಚ", "ಚ", 0, "ಮುಚ್ಚಳ", "lid", "🫙"),
        o("ಜ", "ಜ", 0, "ಅಜ್ಜ", "grandfather", "👴"),
        o("ಟ", "ಟ", 0, "ಮೊಟ್ಟೆ", "egg", "🥚"),
        o("ಡ", "ಡ", 0, "ಗಡ್ಡ", "beard", "🧔"),
        o("ಣ", "ಣ", 0, "ಹಣ್ಣು", "fruit", "🍎"),
        o("ತ", "ತ", 0, "ಕತ್ತೆ", "donkey", "🫏"),
        o("ದ", "ದ", 0, "ಸುದ್ದಿ", "news", "📰"),
        o("ನ", "ನ", 0, "ಕನ್ನಡಿ", "mirror", "🪞"),
        o("ಪ", "ಪ", 0, "ಕಪ್ಪೆ", "frog", "🐸"),
        o("ಬ", "ಬ", 0, "ಹಬ್ಬ", "festival", "🎉"),
        o("ಮ", "ಮ", 0, "ಅಮ್ಮ", "mother", "👩"),
        o("ಲ", "ಲ", 0, "ಕಲ್ಲು", "stone", "🪨"),
        o("ಳ", "ಳ", 0, "ಸೊಳ್ಳೆ", "mosquito", "🦟"),
        o("ಸ", "ಸ", 0, "ಬಸ್ಸು", "bus", "🚌"),
        // With ರ
        o("ಕ", "ರ", 1, "ಚಕ್ರ", "wheel", "🛞"),
        o("ಗ", "ರ", 1, "ಗ್ರಹ", "planet", "🪐"),
        o("ತ", "ರ", 1, "ತ್ರಿಕೋನ", "triangle", "🔺"),
        o("ದ", "ರ", 1, "ದ್ರಾಕ್ಷಿ", "grapes", "🍇"),
        o("ಪ", "ರ", 1, "ಪ್ರಾಣಿ", "animal", "🐾"),
        o("ಶ", "ರ", 1, "ಶ್ರಮ", "hard work", "💪"),
        // With ಯ and ವ
        o("ಕ", "ಯ", 2, "ಕ್ಯಾರೆಟ್", "carrot", "🥕"),
        o("ವ", "ಯ", 2, "ವ್ಯಾಘ್ರ", "tiger", "🐅"),
        o("ಸ", "ವ", 2, "ಸ್ವರ", "vowel / voice", "🎵"),
        o("ದ", "ವ", 2, "ದ್ವಾರ", "doorway", "🚪"),
        // Mixed pairs
        o("ಸ", "ತ", 3, "ಪುಸ್ತಕ", "book", "📚"),
        o("ಷ", "ಟ", 3, "ಕಷ್ಟ", "difficult", "😓"),
        o("ಕ", "ಷ", 3, "ಪಕ್ಷಿ", "bird", "🐦"),
        o("ಜ", "ಞ", 3, "ಜ್ಞಾನ", "knowledge", "💡")
    )
}
