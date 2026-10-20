package com.example.data

import com.example.platform.*

import com.example.ui.data.LanguageManager

data class KannadaLetter(
    val id: Int,
    val character: String,
    val transliteration: String,
    val isVowel: Boolean,
    val videoUrl: String? = null
)

object LetterData {
    private const val ASSETS_BASE = "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/main"

    val kannadaVowels = listOf(
        KannadaLetter(1, "ಅ", "a", true, "$ASSETS_BASE/Final_A.mp4"),
        KannadaLetter(2, "ಆ", "aa", true, "$ASSETS_BASE/Final%20AA.mp4"),
        KannadaLetter(3, "ಇ", "i", true, "$ASSETS_BASE/Final_E.mp4"),
        KannadaLetter(4, "ಈ", "ee", true, "$ASSETS_BASE/Final_EE.mp4"),
        KannadaLetter(5, "ಉ", "u", true, "$ASSETS_BASE/Final_u.mp4"),
        KannadaLetter(6, "ಊ", "oo", true, "$ASSETS_BASE/Final_uu.mp4"),
        KannadaLetter(7, "ಋ", "ru", true, "$ASSETS_BASE/Final_RU.mp4"),
        KannadaLetter(8, "ಎ", "e", true, "$ASSETS_BASE/Final_ye.mp4"),
        KannadaLetter(9, "ಏ", "ea", true, "$ASSETS_BASE/Final_yeee.mp4"),
        KannadaLetter(10, "ಐ", "ai", true, "$ASSETS_BASE/Final_I.mp4"),
        KannadaLetter(11, "ಒ", "o", true, "$ASSETS_BASE/Final_O.mp4"),
        KannadaLetter(12, "ಓ", "oa", true, "$ASSETS_BASE/Final_OO.mp4"),
        KannadaLetter(13, "ಔ", "au", true, "$ASSETS_BASE/Final_ouh.mp4"),
        KannadaLetter(14, "ಅಂ", "am", true, "$ASSETS_BASE/Final_AUM.mp4"),
        KannadaLetter(15, "ಅಃ", "aha", true, "$ASSETS_BASE/Final_AHA.mp4")
    )

    val kannadaConsonants = listOf(
        KannadaLetter(16, "ಕ", "ka", false, "$ASSETS_BASE/Vowel_KA.mp4"),
        KannadaLetter(17, "ಖ", "kha", false, "$ASSETS_BASE/Vowel_KHA.mp4"),
        KannadaLetter(18, "ಗ", "ga", false, "$ASSETS_BASE/Vowel_GA.mp4"),
        KannadaLetter(19, "ಘ", "gha", false, "$ASSETS_BASE/Vowel_GHHHA.mp4"),
        KannadaLetter(20, "ಙ", "nga", false, "$ASSETS_BASE/Vowle_Inya.mp4"),
        
        KannadaLetter(21, "ಚ", "cha", false, "$ASSETS_BASE/Vowel_cha.mp4"),
        KannadaLetter(22, "ಛ", "chha", false, "$ASSETS_BASE/Vowel_CHHHA.mp4"),
        KannadaLetter(23, "ಜ", "ja", false, "$ASSETS_BASE/Vowel_JA.mp4"),
        KannadaLetter(24, "ಝ", "jha", false, "$ASSETS_BASE/Vowel_JJJAA.mp4"),
        KannadaLetter(25, "ಞ", "nya", false, "$ASSETS_BASE/Vowel_INI.mp4"),
        
        KannadaLetter(26, "ಟ", "ta", false, "$ASSETS_BASE/Vowel_Ta.mp4"),
        KannadaLetter(27, "ಠ", "tha", false, "$ASSETS_BASE/Vowel_tttta.mp4"),
        KannadaLetter(28, "ಡ", "da", false, "$ASSETS_BASE/Vowle_Da.mp4"),
        KannadaLetter(29, "ಢ", "dha", false, "$ASSETS_BASE/Vowel_DDDAA.mp4"),
        KannadaLetter(30, "ಣ", "na", false, "$ASSETS_BASE/Vowel_ANA.mp4"),
        
        KannadaLetter(31, "ತ", "ta", false, "$ASSETS_BASE/Vowel_Tha.mp4"),
        KannadaLetter(32, "ಥ", "tha", false, "$ASSETS_BASE/Vowel_THA2.mp4"),
        KannadaLetter(33, "ದ", "da", false, "$ASSETS_BASE/Vowel_Dha.mp4"),
        KannadaLetter(34, "ಧ", "dha", false, "$ASSETS_BASE/Vowel_DHA2.mp4"),
        KannadaLetter(35, "ನ", "na", false, "$ASSETS_BASE/Vowels_Na.mp4"),
        
        KannadaLetter(36, "ಪ", "pa", false, "$ASSETS_BASE/Vowel_Pa.mp4"),
        KannadaLetter(37, "ಫ", "pha", false, "$ASSETS_BASE/Vowel_Pha2.mp4"),
        KannadaLetter(38, "ಬ", "ba", false, "$ASSETS_BASE/Vowel_Ba.mp4"),
        KannadaLetter(39, "ಭ", "bha", false, "$ASSETS_BASE/Vowel_Bha2.mp4"),
        KannadaLetter(40, "ಮ", "ma", false, "$ASSETS_BASE/Vowel_ma.mp4"),
        
        KannadaLetter(41, "ಯ", "ya", false, "$ASSETS_BASE/Vowel_Ya.mp4"),
        KannadaLetter(42, "ರ", "ra", false, "$ASSETS_BASE/Vowel_Ra.mp4"),
        KannadaLetter(43, "ಲ", "la", false, "$ASSETS_BASE/Vowel_La.mp4"),
        KannadaLetter(44, "ವ", "va", false, "$ASSETS_BASE/Vowel_va.mp4"),
        KannadaLetter(45, "ಶ", "sha", false, "$ASSETS_BASE/Vowel%20SSA.mp4"),
        KannadaLetter(46, "ಷ", "sha", false, "$ASSETS_BASE/Vowel_Sha.mp4"),
        KannadaLetter(47, "ಸ", "sa", false, "$ASSETS_BASE/Vowel_Sa.mp4"),
        KannadaLetter(48, "ಹ", "ha", false, "$ASSETS_BASE/Vowel_ha.mp4"),
        KannadaLetter(49, "ಳ", "la", false, "$ASSETS_BASE/Vowel_lha.mp4")
    )

    val kannadaNumbers = listOf(
        KannadaLetter(101, "೧", "1", false),
        KannadaLetter(102, "೨", "2", false),
        KannadaLetter(103, "೩", "3", false),
        KannadaLetter(104, "೪", "4", false),
        KannadaLetter(105, "೫", "5", false),
        KannadaLetter(106, "೬", "6", false),
        KannadaLetter(107, "೭", "7", false),
        KannadaLetter(108, "೮", "8", false),
        KannadaLetter(109, "೯", "9", false),
        KannadaLetter(110, "೧೦", "10", false)
    )
    
    val kannadaKaGuninthakshari = listOf(
        KannadaLetter(201, "ಕ", "ka", false),
        KannadaLetter(202, "ಕಾ", "kaa", false),
        KannadaLetter(203, "ಕಿ", "ki", false),
        KannadaLetter(204, "ಕೀ", "kee", false),
        KannadaLetter(205, "ಕು", "ku", false),
        KannadaLetter(206, "ಕೂ", "koo", false),
        KannadaLetter(207, "ಕೃ", "kru", false),
        KannadaLetter(208, "ಕೆ", "ke", false),
        KannadaLetter(209, "ಕೇ", "kea", false),
        KannadaLetter(210, "ಕೈ", "kai", false),
        KannadaLetter(211, "ಕೊ", "ko", false),
        KannadaLetter(212, "ಕೋ", "koa", false),
        KannadaLetter(213, "ಕೌ", "kau", false),
        KannadaLetter(214, "ಕಂ", "kam", false),
        KannadaLetter(215, "ಕಃ", "kaha", false)
    )

    fun getVowels(langCode: String): List<KannadaLetter> = kannadaVowels
    fun getConsonants(langCode: String): List<KannadaLetter> = kannadaConsonants
    fun getNumbers(langCode: String): List<KannadaLetter> = kannadaNumbers
    fun getKaGuninthakshari(langCode: String): List<KannadaLetter> = kannadaKaGuninthakshari

    val vowels get() = getVowels(LanguageManager.currentLanguage.value.code)
    val consonants get() = getConsonants(LanguageManager.currentLanguage.value.code)
    val numbers get() = getNumbers(LanguageManager.currentLanguage.value.code)
    val kaGuninthakshari get() = getKaGuninthakshari(LanguageManager.currentLanguage.value.code)
    val allLetters get() = vowels + consonants + numbers
}
