package com.example.ui.audio

import com.example.data.LetterData
import com.example.ui.reference.ReferenceInfo
import com.example.ui.screens.fillerSyllables
import com.example.ui.screens.guninthakshariSigns
import com.example.ui.screens.kidColors
import com.example.ui.screens.pictureWords
import com.example.ui.screens.vowelWords

/**
 * Every Kannada letter, syllable and word the app says out loud. The "Generate voice" GitHub Action
 * exports this list (SpeechCatalogExport test) and makes one voice clip per entry.
 * When you add new letters or words to a screen, add their source here too.
 */
object SpeechCatalog {
    fun phrases(): List<String> {
        val out = LinkedHashSet<String>()
        // Letters and numbers
        LetterData.kannadaVowels.forEach { out += it.character }
        LetterData.kannadaConsonants.forEach { out += it.character }
        LetterData.kannadaNumbers.forEach { out += it.character }
        LetterData.kannadaKaGuninthakshari.forEach { out += it.character }
        // Ka-kaa: every consonant with every vowel sign
        LetterData.kannadaConsonants.forEach { c -> guninthakshariSigns.forEach { (sign, _) -> out += c.character + sign } }
        // Example words (Letter Box, Match the Letter, letter lessons)
        ReferenceInfo.allWords().forEach { out += it.kannada }
        // Picture words and their syllable tiles (Scrambled Words, Word Search)
        pictureWords.forEach { w -> out += w.text; w.syllables.forEach { out += it } }
        fillerSyllables.forEach { out += it }
        // Word-chart video words (Word Search topics) and their syllable tiles, plus ಕೈ from the videos
        com.example.ui.screens.videoWords.forEach { w -> out += w.text; w.syllables.forEach { out += it } }
        out += "ಕೈ"
        // Swara Adventure
        vowelWords.forEach { out += it.word; out += it.vowel }
        // Ottakshara island: the joined letters, their parts and example words
        (com.example.data.OttaksharaData.kannadaLetters + com.example.data.OttaksharaData.hindiLetters)
            .forEach { out += it.glyph; out += it.first; out += it.second; out += it.word }
        // Colour names (Paint the Monster, Colour Sort)
        kidColors.forEach { c -> c.names["kn"]?.let { out += it }; c.names["hi"]?.let { out += it } }
        // Hindi letters, numbers and Barakhadi (every consonant with every vowel sign)
        LetterData.hindiVowels.forEach { out += it.character }
        LetterData.hindiConsonants.forEach { out += it.character }
        LetterData.hindiNumbers.forEach { out += it.character }
        LetterData.hindiKaBarakhadi.forEach { out += it.character }
        LetterData.hindiConsonants.forEach { c -> com.example.ui.screens.barakhadiSigns.forEach { (sign, _) -> out += c.character + sign } }
        // Theme videos (Fruits, Vegetables, Body Parts, Family)
        com.example.data.ThemeWords.phrases().forEach { out += it }
        // Tap & Learn picture scenes (words and short sentences)
        com.example.ui.screens.tapLearnPhrases().forEach { out += it }
        return out.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }
}
