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

    val groups = listOf(
        Group("Twin letters", "ಒಂದೇ ಅಕ್ಷರ ಎರಡು ಸಲ"),
        Group("With ರ", "ರ ಒತ್ತು"),
        Group("With ಯ and ವ", "ಯ / ವ ಒತ್ತು"),
        Group("Mixed pairs", "ಬೇರೆ ಬೇರೆ ಅಕ್ಷರ")
    )

    private fun o(first: String, second: String, group: Int, word: String, english: String, picture: String) =
        OttaLetter(first + "್" + second, first, second, group, word, english, picture)

    val letters: List<OttaLetter> = listOf(
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
