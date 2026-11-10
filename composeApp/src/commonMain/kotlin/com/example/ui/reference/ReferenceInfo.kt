package com.example.ui.reference

import com.example.data.KannadaLetter
import com.example.data.LetterData
import com.example.platform.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** An example word for a letter: Kannada, English meaning and a picture (emoji, works offline). */
data class RefWord(val kannada: String, val english: String, val picture: String)

/** A group of consonants (ವರ್ಗ) as taught in school. */
data class ConsonantGroup(val kannada: String, val english: String, val letters: List<KannadaLetter>)

enum class RefTab(private val kn: String, private val hi: String, private val en: String, private val enHi: String = en) {
    Vowels("ಸ್ವರ", "स्वर", "Vowels"),
    Consonants("ವ್ಯಂಜನ", "व्यंजन", "Consonants"),
    KaKaa("ಕಾಗುಣಿತ", "बारहखड़ी", "Ka-kaa", "Barakhadi"),
    Numbers("ಅಂಕೆ", "अंक", "Numbers");

    /** The tab's name in the script being learned. */
    val kannada: String get() = if (com.example.data.Lang.isHindi) hi else kn
    val english: String get() = if (com.example.data.Lang.isHindi) enHi else en

    fun letters(): List<KannadaLetter> = when (this) {
        Vowels -> LetterData.vowels
        Consonants -> LetterData.consonants
        KaKaa -> LetterData.kaGuninthakshari
        Numbers -> LetterData.numbers
    }
}

object ReferenceInfo {

    /** How to say each letter, in English letters (clearer than the ids used inside the games). */
    private val sounds = mapOf(
        "ಅ" to "a", "ಆ" to "aa", "ಇ" to "i", "ಈ" to "ii", "ಉ" to "u", "ಊ" to "uu", "ಋ" to "ru",
        "ಎ" to "e", "ಏ" to "ee", "ಐ" to "ai", "ಒ" to "o", "ಓ" to "oo", "ಔ" to "au", "ಅಂ" to "am", "ಅಃ" to "aha",
        "ಕ" to "ka", "ಖ" to "kha", "ಗ" to "ga", "ಘ" to "gha", "ಙ" to "nga",
        "ಚ" to "cha", "ಛ" to "chha", "ಜ" to "ja", "ಝ" to "jha", "ಞ" to "nya",
        "ಟ" to "Ta", "ಠ" to "Tha", "ಡ" to "Da", "ಢ" to "Dha", "ಣ" to "Na",
        "ತ" to "ta", "ಥ" to "tha", "ದ" to "da", "ಧ" to "dha", "ನ" to "na",
        "ಪ" to "pa", "ಫ" to "pha", "ಬ" to "ba", "ಭ" to "bha", "ಮ" to "ma",
        "ಯ" to "ya", "ರ" to "ra", "ಲ" to "la", "ವ" to "va", "ಶ" to "sha", "ಷ" to "Sha",
        "ಸ" to "sa", "ಹ" to "ha", "ಳ" to "La"
    )

    private val vowelTips = mapOf(
        "ಅ" to "Short “a”, like the “u” in “up”",
        "ಆ" to "Long “aa”, like in “father”",
        "ಇ" to "Short “i”, like in “sit”",
        "ಈ" to "Long “ee”, like in “see”",
        "ಉ" to "Short “u”, like in “put”",
        "ಊ" to "Long “oo”, like in “moon”",
        "ಋ" to "“ru”, a quick r with a u",
        "ಎ" to "Short “e”, like in “bed”",
        "ಏ" to "Long “ay”, like in “say”",
        "ಐ" to "“ai”, like in “eye”",
        "ಒ" to "Short “o”, a quick “o”",
        "ಓ" to "Long “o”, like in “go”",
        "ಔ" to "“au”, like in “cow”",
        "ಅಂ" to "“am”, with a hum at the end",
        "ಅಃ" to "“aha”, with a soft breath at the end",
        "अ" to "Short “a”, like the “u” in “up”",
        "आ" to "Long “aa”, like in “father”",
        "इ" to "Short “i”, like in “sit”",
        "ई" to "Long “ee”, like in “see”",
        "उ" to "Short “u”, like in “put”",
        "ऊ" to "Long “oo”, like in “moon”",
        "ऋ" to "“ri”, a quick r with an i",
        "ए" to "“e”, like in “they”",
        "ऐ" to "“ai”, like in “bat”, said long",
        "ओ" to "“o”, like in “go”",
        "औ" to "“au”, like in “awe”",
        "अं" to "“am”, with a hum at the end",
        "अः" to "“aha”, with a soft breath at the end"
    )

    private val words = mapOf(
        "ಅ" to RefWord("ಅಮ್ಮ", "mother", "👩‍👧"),
        "ಆ" to RefWord("ಆನೆ", "elephant", "🐘"),
        "ಇ" to RefWord("ಇಲಿ", "mouse", "🐭"),
        "ಈ" to RefWord("ಈರುಳ್ಳಿ", "onion", "🧅"),
        "ಉ" to RefWord("ಉಗಿಬಂಡಿ", "steam train", "🚂"),
        "ಊ" to RefWord("ಊಟ", "meal", "🍛"),
        "ಋ" to RefWord("ಋಷಿ", "sage", "🧘"),
        "ಎ" to RefWord("ಎಲೆ", "leaf", "🍃"),
        "ಏ" to RefWord("ಏಣಿ", "ladder", "🪜"),
        "ಐ" to RefWord("ಐದು", "five", "🖐️"),
        "ಒ" to RefWord("ಒಂಟೆ", "camel", "🐫"),
        "ಓ" to RefWord("ಓಟ", "race", "🏃"),
        "ಔ" to RefWord("ಔಷಧ", "medicine", "💊"),
        "ಅಂ" to RefWord("ಅಂಗಿ", "shirt", "👕"),
        "ಕ" to RefWord("ಕಮಲ", "lotus", "🪷"),
        "ಖ" to RefWord("ಖಡ್ಗ", "sword", "⚔️"),
        "ಗ" to RefWord("ಗಡಿಯಾರ", "clock", "🕰️"),
        "ಘ" to RefWord("ಘಂಟೆ", "bell", "🔔"),
        "ಚ" to RefWord("ಚಮಚ", "spoon", "🥄"),
        "ಛ" to RefWord("ಛತ್ರಿ", "umbrella", "☂️"),
        "ಜ" to RefWord("ಜಿಂಕೆ", "deer", "🦌"),
        "ಝ" to RefWord("ಝರಿ", "stream", "🏞️"),
        "ಟ" to RefWord("ಟೊಮೆಟೊ", "tomato", "🍅"),
        "ಡ" to RefWord("ಡಬ್ಬ", "box", "📦"),
        "ಢ" to RefWord("ಢಕ್ಕೆ", "drum", "🥁"),
        "ತ" to RefWord("ತರಕಾರಿ", "vegetables", "🥕"),
        "ದ" to RefWord("ದೋಣಿ", "boat", "⛵"),
        "ಧ" to RefWord("ಧ್ವಜ", "flag", "🚩"),
        "ನ" to RefWord("ನವಿಲು", "peacock", "🦚"),
        "ಪ" to RefWord("ಪುಸ್ತಕ", "book", "📖"),
        "ಫ" to RefWord("ಫಲ", "fruit", "🍇"),
        "ಬ" to RefWord("ಬಾಳೆಹಣ್ಣು", "banana", "🍌"),
        "ಭ" to RefWord("ಭೂಮಿ", "earth", "🌍"),
        "ಮ" to RefWord("ಮರ", "tree", "🌳"),
        "ಯ" to RefWord("ಯಂತ್ರ", "machine", "⚙️"),
        "ರ" to RefWord("ರೈಲು", "train", "🚆"),
        "ಲ" to RefWord("ಲಾರಿ", "lorry", "🚚"),
        "ವ" to RefWord("ವಿಮಾನ", "aeroplane", "✈️"),
        "ಶ" to RefWord("ಶಂಖ", "conch shell", "🐚"),
        "ಸ" to RefWord("ಸೇಬು", "apple", "🍎"),
        "ಹ" to RefWord("ಹಸು", "cow", "🐄"),
        // Numbers: the number's name
        "೧" to RefWord("ಒಂದು", "one", "☝️"),
        "೨" to RefWord("ಎರಡು", "two", "✌️"),
        "೩" to RefWord("ಮೂರು", "three", "🎈🎈🎈"),
        "೪" to RefWord("ನಾಲ್ಕು", "four", "🍎🍎🍎🍎"),
        "೫" to RefWord("ಐದು", "five", "🖐️"),
        "೬" to RefWord("ಆರು", "six", "⭐⭐⭐⭐⭐⭐"),
        "೭" to RefWord("ಏಳು", "seven", "🌈"),
        "೮" to RefWord("ಎಂಟು", "eight", "🐙"),
        "೯" to RefWord("ಒಂಬತ್ತು", "nine", "9️⃣"),
        "೧೦" to RefWord("ಹತ್ತು", "ten", "🔟"),
        // Hindi
        "अ" to RefWord("अनानास", "pineapple", "🍍"),
        "आ" to RefWord("आम", "mango", "🥭"),
        "इ" to RefWord("इंजन", "engine", "🚂"),
        "ई" to RefWord("ईख", "sugarcane", "🎋"),
        "उ" to RefWord("उल्लू", "owl", "🦉"),
        "ऊ" to RefWord("ऊन", "wool", "🧶"),
        "ऋ" to RefWord("ऋषि", "sage", "🧘"),
        "ए" to RefWord("एक", "one", "☝️"),
        "ऐ" to RefWord("ऐनक", "glasses", "👓"),
        "ओ" to RefWord("ओला", "hailstone", "🌨️"),
        "औ" to RefWord("औरत", "woman", "👩"),
        "अं" to RefWord("अंगूर", "grapes", "🍇"),
        "क" to RefWord("कबूतर", "pigeon", "🕊️"),
        "ख" to RefWord("खरगोश", "rabbit", "🐇"),
        "ग" to RefWord("गमला", "flower pot", "🪴"),
        "घ" to RefWord("घड़ी", "watch", "⌚"),
        "च" to RefWord("चम्मच", "spoon", "🥄"),
        "छ" to RefWord("छतरी", "umbrella", "☂️"),
        "ज" to RefWord("जहाज़", "ship", "🚢"),
        "झ" to RefWord("झंडा", "flag", "🚩"),
        "ट" to RefWord("टमाटर", "tomato", "🍅"),
        "ठ" to RefWord("ठेला", "handcart", "🛒"),
        "ड" to RefWord("डमरू", "small drum", "🪘"),
        "ढ" to RefWord("ढोल", "drum", "🥁"),
        "त" to RefWord("तरबूज़", "watermelon", "🍉"),
        "थ" to RefWord("थैला", "bag", "👜"),
        "द" to RefWord("दरवाज़ा", "door", "🚪"),
        "ध" to RefWord("धनुष", "bow", "🏹"),
        "न" to RefWord("नल", "tap", "🚰"),
        "प" to RefWord("पतंग", "kite", "🪁"),
        "फ" to RefWord("फूल", "flower", "🌸"),
        "ब" to RefWord("बत्तख", "duck", "🦆"),
        "भ" to RefWord("भालू", "bear", "🐻"),
        "म" to RefWord("मछली", "fish", "🐟"),
        "य" to RefWord("योग", "yoga", "🧘"),
        "र" to RefWord("रस्सी", "rope", "🪢"),
        "ल" to RefWord("लालटेन", "lantern", "🏮"),
        "व" to RefWord("वन", "forest", "🌳"),
        "श" to RefWord("शेर", "lion", "🦁"),
        "स" to RefWord("सेब", "apple", "🍎"),
        "ह" to RefWord("हाथी", "elephant", "🐘"),
        "त्र" to RefWord("त्रिशूल", "trident", "🔱"),
        "१" to RefWord("एक", "one", "☝️"),
        "२" to RefWord("दो", "two", "✌️"),
        "३" to RefWord("तीन", "three", "🎈🎈🎈"),
        "४" to RefWord("चार", "four", "🍎🍎🍎🍎"),
        "५" to RefWord("पाँच", "five", "🖐️"),
        "६" to RefWord("छह", "six", "⭐⭐⭐⭐⭐⭐"),
        "७" to RefWord("सात", "seven", "🌈"),
        "८" to RefWord("आठ", "eight", "🐙"),
        "९" to RefWord("नौ", "nine", "9️⃣"),
        "१०" to RefWord("दस", "ten", "🔟")
    )

    private val aspirated = setOf("ಖ", "ಘ", "ಛ", "ಝ", "ಠ", "ಢ", "ಥ", "ಧ", "ಫ", "ಭ", "ख", "घ", "छ", "झ", "ठ", "ढ", "थ", "ध", "फ", "भ")
    private val curled = setOf("ಟ", "ಠ", "ಡ", "ಢ", "ಣ", "ಷ", "ಳ", "ट", "ठ", "ड", "ढ", "ण", "ष")
    private val nasal = setOf("ಙ", "ಞ", "ಣ", "ನ", "ಮ", "ङ", "ञ", "ण", "न", "म")

    fun sound(letter: KannadaLetter): String = sounds[letter.character] ?: letter.transliteration

    fun word(letter: KannadaLetter): RefWord? = words[letter.character]

    /** Every example word (for the voice clips). */
    fun allWords(): List<RefWord> = words.values.toList()

    /** A short tip on how to say the letter. */
    fun tip(letter: KannadaLetter, tab: RefTab): String {
        val c = letter.character
        return when (tab) {
            RefTab.Vowels -> vowelTips[c] ?: "Say “${sound(letter)}”"
            RefTab.Consonants -> {
                val parts = mutableListOf<String>()
                if (c in aspirated) parts += "with a puff of air"
                if (c in curled) parts += "tongue curled back"
                if (c in nasal) parts += "through the nose"
                "Say “${sound(letter)}”" + if (parts.isEmpty()) "" else ", " + parts.joinToString(", ")
            }
            RefTab.KaKaa -> {
                val i = LetterData.kaGuninthakshari.indexOfFirst { it.id == letter.id }
                val vowel = LetterData.vowels.getOrNull(i)?.character
                if (vowel != null) "${LetterData.consonants.first().character} + $vowel = $c" else "Say “${letter.transliteration}”"
            }
            RefTab.Numbers -> "The number ${letter.transliteration}"
        }
    }

    /** Consonants arranged in the traditional ವರ್ಗ groups. */
    fun consonantGroups(): List<ConsonantGroup> {
        val all = LetterData.consonants
        fun pick(vararg chars: String) = chars.mapNotNull { ch -> all.firstOrNull { it.character == ch } }
        if (com.example.data.Lang.isHindi) return listOf(
            ConsonantGroup("क वर्ग", "ka group", pick("क", "ख", "ग", "घ", "ङ")),
            ConsonantGroup("च वर्ग", "cha group", pick("च", "छ", "ज", "झ", "ञ")),
            ConsonantGroup("ट वर्ग", "Ta group", pick("ट", "ठ", "ड", "ढ", "ण")),
            ConsonantGroup("त वर्ग", "ta group", pick("त", "थ", "द", "ध", "न")),
            ConsonantGroup("प वर्ग", "pa group", pick("प", "फ", "ब", "भ", "म")),
            ConsonantGroup("अन्य", "other letters", pick("य", "र", "ल", "व", "श", "ष", "स", "ह")),
            ConsonantGroup("संयुक्त", "joined letters", pick("क्ष", "त्र", "ज्ञ"))
        ).filter { it.letters.isNotEmpty() }
        return listOf(
            ConsonantGroup("ಕ ವರ್ಗ", "ka group", pick("ಕ", "ಖ", "ಗ", "ಘ", "ಙ")),
            ConsonantGroup("ಚ ವರ್ಗ", "cha group", pick("ಚ", "ಛ", "ಜ", "ಝ", "ಞ")),
            ConsonantGroup("ಟ ವರ್ಗ", "Ta group", pick("ಟ", "ಠ", "ಡ", "ಢ", "ಣ")),
            ConsonantGroup("ತ ವರ್ಗ", "ta group", pick("ತ", "ಥ", "ದ", "ಧ", "ನ")),
            ConsonantGroup("ಪ ವರ್ಗ", "pa group", pick("ಪ", "ಫ", "ಬ", "ಭ", "ಮ")),
            ConsonantGroup("ಅವರ್ಗೀಯ", "other letters", pick("ಯ", "ರ", "ಲ", "ವ", "ಶ", "ಷ", "ಸ", "ಹ", "ಳ"))
        ).filter { it.letters.isNotEmpty() }
    }
}

/** Which letters the child has practised on the Reference page (per player). */
object ReferenceProgress {
    private const val PREFS = "ReferenceProgress"
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    /** Screens redraw (after ProgressSync brought in progress from another device). */
    fun notifyChanged() { _version.value += 1 }

    private fun user() = getSharedPreferences("AppProgress").getString("last_active_user", "guest") ?: "guest"

    fun isPractised(letter: KannadaLetter): Boolean =
        getSharedPreferences(PREFS).getBoolean("${user()}_${letter.id}", false)

    fun markPractised(letter: KannadaLetter) {
        if (isPractised(letter)) return
        getSharedPreferences(PREFS).edit().putBoolean("${user()}_${letter.id}", true).apply()
        _version.value += 1
    }

    fun count(letters: List<KannadaLetter>): Int = letters.count { isPractised(it) }
}
