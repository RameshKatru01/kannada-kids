package com.example.data

/** A word shown in a theme video (and spoken by the app's Kannada voice). */
data class ThemeWord(
    val kannada: String,
    val english: String,
    val picture: String,
    /** A short sentence using the word, e.g. "ಇದು ಸೇಬು" (This is an apple). */
    val sentence: String
)

/** A theme of 10 words, one Fun Videos folder. */
data class WordTheme(val folder: String, val kannada: String, val words: List<ThemeWord>)

/**
 * Words for the theme videos in Fun Videos: Fruits, Vegetables, Body Parts and Family.
 * tools/theme_videos makes one short video per word from this list.
 */
object ThemeWords {
    /** Said in every video. */
    const val SAY_IT = "ನೀನೂ ಹೇಳು!"
    const val WELL_DONE = "ಶಭಾಷ್!"

    val fruits = WordTheme("Fruits", "ಹಣ್ಣುಗಳು", listOf(
        ThemeWord("ಸೇಬು", "Apple", "🍎", "ಇದು ಸೇಬು"),
        ThemeWord("ಬಾಳೆಹಣ್ಣು", "Banana", "🍌", "ಇದು ಬಾಳೆಹಣ್ಣು"),
        ThemeWord("ಮಾವಿನಹಣ್ಣು", "Mango", "🥭", "ಇದು ಮಾವಿನಹಣ್ಣು"),
        ThemeWord("ದ್ರಾಕ್ಷಿ", "Grapes", "🍇", "ಇದು ದ್ರಾಕ್ಷಿ"),
        ThemeWord("ಕಿತ್ತಳೆ", "Orange", "🍊", "ಇದು ಕಿತ್ತಳೆ"),
        ThemeWord("ಕಲ್ಲಂಗಡಿ", "Watermelon", "🍉", "ಇದು ಕಲ್ಲಂಗಡಿ"),
        ThemeWord("ಅನಾನಸ್", "Pineapple", "🍍", "ಇದು ಅನಾನಸ್"),
        ThemeWord("ನಿಂಬೆಹಣ್ಣು", "Lemon", "🍋", "ಇದು ನಿಂಬೆಹಣ್ಣು"),
        ThemeWord("ತೆಂಗಿನಕಾಯಿ", "Coconut", "🥥", "ಇದು ತೆಂಗಿನಕಾಯಿ"),
        ThemeWord("ಸ್ಟ್ರಾಬೆರಿ", "Strawberry", "🍓", "ಇದು ಸ್ಟ್ರಾಬೆರಿ"),
    ))

    val vegetables = WordTheme("Vegetables", "ತರಕಾರಿಗಳು", listOf(
        ThemeWord("ಕ್ಯಾರೆಟ್", "Carrot", "🥕", "ಇದು ಕ್ಯಾರೆಟ್"),
        ThemeWord("ಟೊಮೆಟೊ", "Tomato", "🍅", "ಇದು ಟೊಮೆಟೊ"),
        ThemeWord("ಆಲೂಗಡ್ಡೆ", "Potato", "🥔", "ಇದು ಆಲೂಗಡ್ಡೆ"),
        ThemeWord("ಈರುಳ್ಳಿ", "Onion", "🧅", "ಇದು ಈರುಳ್ಳಿ"),
        ThemeWord("ಬೆಳ್ಳುಳ್ಳಿ", "Garlic", "🧄", "ಇದು ಬೆಳ್ಳುಳ್ಳಿ"),
        ThemeWord("ಬದನೆಕಾಯಿ", "Brinjal", "🍆", "ಇದು ಬದನೆಕಾಯಿ"),
        ThemeWord("ಮೆಣಸಿನಕಾಯಿ", "Chilli", "🌶️", "ಇದು ಮೆಣಸಿನಕಾಯಿ"),
        ThemeWord("ಸೌತೆಕಾಯಿ", "Cucumber", "🥒", "ಇದು ಸೌತೆಕಾಯಿ"),
        ThemeWord("ಮೆಕ್ಕೆಜೋಳ", "Corn", "🌽", "ಇದು ಮೆಕ್ಕೆಜೋಳ"),
        ThemeWord("ದೊಣ್ಣೆಮೆಣಸಿನಕಾಯಿ", "Capsicum", "🫑", "ಇದು ದೊಣ್ಣೆಮೆಣಸಿನಕಾಯಿ"),
    ))

    val bodyParts = WordTheme("Body Parts", "ದೇಹದ ಭಾಗಗಳು", listOf(
        ThemeWord("ಮುಖ", "Face", "🙂", "ಇದು ನನ್ನ ಮುಖ"),
        ThemeWord("ಕಣ್ಣು", "Eye", "👁️", "ಇದು ನನ್ನ ಕಣ್ಣು"),
        ThemeWord("ಕಿವಿ", "Ear", "👂🏽", "ಇದು ನನ್ನ ಕಿವಿ"),
        ThemeWord("ಮೂಗು", "Nose", "👃🏽", "ಇದು ನನ್ನ ಮೂಗು"),
        ThemeWord("ಬಾಯಿ", "Mouth", "👄", "ಇದು ನನ್ನ ಬಾಯಿ"),
        ThemeWord("ಹಲ್ಲು", "Tooth", "🦷", "ಇದು ನನ್ನ ಹಲ್ಲು"),
        ThemeWord("ನಾಲಿಗೆ", "Tongue", "👅", "ಇದು ನನ್ನ ನಾಲಿಗೆ"),
        ThemeWord("ಕೈ", "Hand", "✋🏽", "ಇದು ನನ್ನ ಕೈ"),
        ThemeWord("ಬೆರಳು", "Finger", "☝🏽", "ಇದು ನನ್ನ ಬೆರಳು"),
        ThemeWord("ಕಾಲು", "Leg", "🦵🏽", "ಇದು ನನ್ನ ಕಾಲು"),
    ))

    val family = WordTheme("Family", "ನನ್ನ ಕುಟುಂಬ", listOf(
        ThemeWord("ಅಮ್ಮ", "Mother", "👩🏽", "ಇವರು ನನ್ನ ಅಮ್ಮ"),
        ThemeWord("ಅಪ್ಪ", "Father", "👨🏽", "ಇವರು ನನ್ನ ಅಪ್ಪ"),
        ThemeWord("ಅಜ್ಜಿ", "Grandmother", "👵🏽", "ಇವರು ನನ್ನ ಅಜ್ಜಿ"),
        ThemeWord("ಅಜ್ಜ", "Grandfather", "👴🏽", "ಇವರು ನನ್ನ ಅಜ್ಜ"),
        ThemeWord("ಅಣ್ಣ", "Big brother", "👦🏽", "ಇವನು ನನ್ನ ಅಣ್ಣ"),
        ThemeWord("ಅಕ್ಕ", "Big sister", "👧🏽", "ಇವಳು ನನ್ನ ಅಕ್ಕ"),
        ThemeWord("ತಮ್ಮ", "Little brother", "👦🏽", "ಇವನು ನನ್ನ ತಮ್ಮ"),
        ThemeWord("ತಂಗಿ", "Little sister", "👧🏽", "ಇವಳು ನನ್ನ ತಂಗಿ"),
        ThemeWord("ಮಗು", "Baby", "👶🏽", "ಇದು ಪುಟ್ಟ ಮಗು"),
        ThemeWord("ಕುಟುಂಬ", "Family", "👨‍👩‍👧‍👦", "ಇದು ನನ್ನ ಕುಟುಂಬ"),
    ))

    val all = listOf(fruits, vegetables, bodyParts, family)

    // ---- Hindi ----
    const val SAY_IT_HI = "अब तुम बोलो!"
    const val WELL_DONE_HI = "शाबाश!"

    val hindiFruits = WordTheme("Fruits", "फल", listOf(
        ThemeWord("सेब", "Apple", "🍎", "यह सेब है"),
        ThemeWord("केला", "Banana", "🍌", "यह केला है"),
        ThemeWord("आम", "Mango", "🥭", "यह आम है"),
        ThemeWord("अंगूर", "Grapes", "🍇", "ये अंगूर हैं"),
        ThemeWord("संतरा", "Orange", "🍊", "यह संतरा है"),
        ThemeWord("तरबूज़", "Watermelon", "🍉", "यह तरबूज़ है"),
        ThemeWord("अनानास", "Pineapple", "🍍", "यह अनानास है"),
        ThemeWord("नींबू", "Lemon", "🍋", "यह नींबू है"),
        ThemeWord("नारियल", "Coconut", "🥥", "यह नारियल है"),
        ThemeWord("स्ट्रॉबेरी", "Strawberry", "🍓", "यह स्ट्रॉबेरी है"),
    ))

    val hindiBirds = WordTheme("Birds", "पक्षी", listOf(
        ThemeWord("कबूतर", "Pigeon", "🕊️", "यह कबूतर है"),
        ThemeWord("चील", "Eagle", "🦅", "यह चील है"),
        ThemeWord("उल्लू", "Owl", "🦉", "यह उल्लू है"),
        ThemeWord("हंस", "Swan", "🦢", "यह हंस है"),
        ThemeWord("बगुला", "Crane", "🪶", "यह बगुला है"),
        ThemeWord("तोता", "Parrot", "🦜", "यह तोता है"),
        ThemeWord("मुर्गी", "Hen", "🐔", "यह मुर्गी है"),
        ThemeWord("मोर", "Peacock", "🦚", "यह मोर है"),
        ThemeWord("गौरैया", "Sparrow", "🐦", "यह गौरैया है"),
        ThemeWord("कौआ", "Crow", "🐦‍⬛", "यह कौआ है"),
    ))

    val allHindi = listOf(hindiFruits, hindiBirds)

    /** Everything the videos say, for the voice clips. */
    fun phrases(): List<String> = buildList {
        add(SAY_IT); add(WELL_DONE)
        all.forEach { t -> add(t.kannada); t.words.forEach { add(it.kannada); add(it.sentence) } }
        add(SAY_IT_HI); add(WELL_DONE_HI)
        allHindi.forEach { t -> add(t.kannada); t.words.forEach { add(it.kannada); add(it.sentence) } }
    }
}
