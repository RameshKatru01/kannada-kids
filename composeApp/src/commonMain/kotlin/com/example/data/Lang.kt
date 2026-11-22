package com.example.data

import com.example.ui.data.LanguageManager

/**
 * The language the child is learning (Kannada or Hindi), for everything that changes with it:
 * island names, letter glyphs on cards, saved progress and which games and videos exist.
 *
 * Kannada and Devanagari (Hindi) letters sit in matching places in Unicode, so a decorative
 * Kannada letter (a game card's "ಕ", "ಕ ಕಾ ಕಿ") can be shown in Hindi with [script].
 */
object Lang {
    val code: String get() = LanguageManager.currentLanguage.value.code
    val isHindi: Boolean get() = code == "hi"

    /** Added to saved-progress keys. Empty for Kannada, so existing progress is kept. */
    val keyPrefix: String get() = if (isHindi) "hi_" else ""

    /** The Kannada-script text [kn] in the current script (unchanged for Kannada). */
    fun script(kn: String): String = if (!isHindi) kn else buildString {
        kn.forEach { c ->
            append(
                when (c) {
                    'ಎ' -> 'ए'; 'ಒ' -> 'ओ'          // Hindi has one e and one o
                    'ೆ' -> 'े'; 'ೊ' -> 'ो'
                    'ಳ' -> 'ल'
                    in 'ಀ'..'೿' -> (c.code - 0x380).toChar()
                    else -> c
                }
            )
        }
    }

    /** A list of Kannada letters in the current script, without repeats. */
    fun letters(kn: List<String>): List<String> = kn.map { script(it) }.distinct()

    /** Island (planet) names. */
    fun islandName(type: String): String = when (type) {
        "vowels" -> "Vowels"
        "consonants" -> "Consonants"
        "guninthakshari" -> if (isHindi) "Barakhadi" else "Ka-kaa"
        "numbers" -> "Numbers"
        "ottakshara" -> if (isHindi) "Sanyuktakshar" else "Ottakshara"
        else -> type
    }

    /** The island's own name in the script, e.g. ಸ್ವರಗಳು / स्वर. */
    fun islandNative(type: String): String = when (type) {
        "vowels" -> if (isHindi) "स्वर" else "ಸ್ವರಗಳು"
        "consonants" -> if (isHindi) "व्यंजन" else "ವ್ಯಂಜನಗಳು"
        "guninthakshari" -> if (isHindi) "बारहखड़ी" else "ಕಾಗುಣಿತ"
        "numbers" -> if (isHindi) "अंक" else "ಅಂಕೆಗಳು"
        "ottakshara" -> if (isHindi) "संयुक्ताक्षर" else "ಒತ್ತಕ್ಷರ"
        else -> ""
    }

    /** "Hello" in the language. */
    val hello: String get() = if (isHindi) "नमस्ते!" else "ನಮಸ್ಕಾರ!"

    /** Games that need words not yet written in Hindi. */
    private val kannadaOnlyGames = setOf(8, 9, 20, 24, 25, 26, 27)   // Scrambled Words, Word Search, Swara Adventure, Tap & Learn, Memory Flip, Write My Name

    fun hasGame(id: Int): Boolean = !isHindi || id !in kannadaOnlyGames

    /** Printable worksheets are Kannada only for now. */
    val hasWorksheets: Boolean get() = !isHindi
}
