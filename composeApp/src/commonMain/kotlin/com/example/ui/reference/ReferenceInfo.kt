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

enum class RefTab(val kannada: String, val english: String) {
    Vowels("ಸ್ವರ", "Vowels"),
    Consonants("ವ್ಯಂಜನ", "Consonants"),
    KaKaa("ಕಾಗುಣಿತ", "Ka-kaa"),
    Numbers("ಅಂಕೆ", "Numbers");

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
        "ಅಃ" to "“aha”, with a soft breath at the end"
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
        "೧೦" to RefWord("ಹತ್ತು", "ten", "🔟")
    )

    private val aspirated = setOf("ಖ", "ಘ", "ಛ", "ಝ", "ಠ", "ಢ", "ಥ", "ಧ", "ಫ", "ಭ")
    private val curled = setOf("ಟ", "ಠ", "ಡ", "ಢ", "ಣ", "ಷ", "ಳ")
    private val nasal = setOf("ಙ", "ಞ", "ಣ", "ನ", "ಮ")

    fun sound(letter: KannadaLetter): String = sounds[letter.character] ?: letter.transliteration

    fun word(letter: KannadaLetter): RefWord? = words[letter.character]

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
                if (vowel != null) "ಕ + $vowel = $c" else "Say “${letter.transliteration}”"
            }
            RefTab.Numbers -> "The number ${letter.transliteration}"
        }
    }

    /** Consonants arranged in the traditional ವರ್ಗ groups. */
    fun consonantGroups(): List<ConsonantGroup> {
        val all = LetterData.consonants
        fun pick(vararg chars: String) = chars.mapNotNull { ch -> all.firstOrNull { it.character == ch } }
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
