package com.example.ui.screens

/** A picture word made of Kannada syllables (aksharas), used by Scrambled Words and Word Search. */
internal data class PictureWord(
    val syllables: List<String>,
    val picture: String,
    val meaning: String,
    /** The word's video (Fun Videos word charts), if it has one. */
    val video: String? = null,
    /** Word Search topic id (see VideoWords.kt); null for the general picture words. */
    val topic: String? = null
) {
    val text: String get() = syllables.joinToString("")
    /** English meaning only, e.g. "King" from "Arasa (King)". */
    val english: String get() = meaning.substringAfter("(", meaning).substringBefore(")")
    val hasConjunct: Boolean get() = syllables.any { it.contains("್") }
}

internal val pictureWords = listOf(
    PictureWord(listOf("ಅ", "ರ", "ಸ"), "👑", "Arasa (King)"),
    PictureWord(listOf("ಆ", "ನೆ"), "🐘", "Aane (Elephant)"),
    PictureWord(listOf("ಇ", "ಲಿ"), "🐁", "Ili (Rat)"),
    PictureWord(listOf("ಮ", "ರ"), "🌳", "Mara (Tree)"),
    PictureWord(listOf("ಮ", "ನೆ"), "🏠", "Mane (House)"),
    PictureWord(listOf("ಕ", "ಮ", "ಲ"), "🪷", "Kamala (Lotus)"),
    PictureWord(listOf("ಹ", "ಡ", "ಗು"), "🚢", "Hadagu (Ship)"),
    PictureWord(listOf("ಬ", "ಸ", "ವ"), "🐂", "Basava (Ox)"),
    PictureWord(listOf("ಹ", "ಸು"), "🐄", "Hasu (Cow)"),
    PictureWord(listOf("ಗಿ", "ಳಿ"), "🦜", "Gili (Parrot)"),
    PictureWord(listOf("ಕ", "ರ", "ಡಿ"), "🐻", "Karadi (Bear)"),
    PictureWord(listOf("ನಾ", "ಯಿ"), "🐕", "Naayi (Dog)"),
    PictureWord(listOf("ಬೆ", "ಕ್ಕು"), "🐈", "Bekku (Cat)"),
    PictureWord(listOf("ಕು", "ರಿ"), "🐑", "Kuri (Sheep)"),
    PictureWord(listOf("ಹು", "ಲಿ"), "🐅", "Huli (Tiger)"),
    PictureWord(listOf("ಸಿಂ", "ಹ"), "🦁", "Simha (Lion)"),
    PictureWord(listOf("ಆ", "ಮೆ"), "🐢", "Aame (Turtle)"),
    PictureWord(listOf("ಕಾ", "ಗೆ"), "🐦‍⬛", "Kaage (Crow)"),
    PictureWord(listOf("ಗೂ", "ಬೆ"), "🦉", "Goobe (Owl)"),
    PictureWord(listOf("ನ", "ವಿ", "ಲು"), "🦚", "Navilu (Peacock)"),
    PictureWord(listOf("ಕ", "ಣ್ಣು"), "👁️", "Kannu (Eye)"),
    PictureWord(listOf("ಕಿ", "ವಿ"), "👂", "Kivi (Ear)"),
    PictureWord(listOf("ಮೂ", "ಗು"), "👃", "Moogu (Nose)"),
    PictureWord(listOf("ಬಾ", "ಯಿ"), "👄", "Baayi (Mouth)"),
    PictureWord(listOf("ಕಾ", "ಲು"), "🦵", "Kaalu (Leg)"),
    PictureWord(listOf("ಕೈ"), "✋", "Kai (Hand)"),
    PictureWord(listOf("ತ", "ಲೆ"), "🧑", "Tale (Head)"),
    PictureWord(listOf("ಕೂ", "ದ", "ಲು"), "💇", "Koodalu (Hair)"),
    PictureWord(listOf("ಸೂ", "ರ್ಯ"), "☀️", "Soorya (Sun)"),
    PictureWord(listOf("ಚಂ", "ದ್ರ"), "🌙", "Chandra (Moon)"),
    PictureWord(listOf("ಭೂ", "ಮಿ"), "🌍", "Bhoomi (Earth)"),
    PictureWord(listOf("ನ", "ಕ್ಷ", "ತ್ರ"), "⭐", "Nakshatra (Star)"),
    PictureWord(listOf("ಸೇ", "ಬು"), "🍎", "Sebu (Apple)"),
    PictureWord(listOf("ಬಾ", "ಳೆ"), "🍌", "Baale (Banana)"),
    PictureWord(listOf("ದ್ರಾ", "ಕ್ಷಿ"), "🍇", "Drakshi (Grapes)"),
    PictureWord(listOf("ಕಿ", "ತ್ತ", "ಳೆ"), "🍊", "Kittale (Orange)"),
    PictureWord(listOf("ಮಾ", "ವು"), "🥭", "Maavu (Mango)"),
    PictureWord(listOf("ಈ", "ರು", "ಳ್ಳಿ"), "🧅", "Eerulli (Onion)"),
    PictureWord(listOf("ಟೊ", "ಮೆ", "ಟೊ"), "🍅", "Tomato"),
    PictureWord(listOf("ಬ", "ದ", "ನೆ"), "🍆", "Badane (Brinjal)"),
    PictureWord(listOf("ಹೂ", "ವು"), "🌸", "Hoovu (Flower)"),
    PictureWord(listOf("ಬೆ", "ಳ", "ಕು"), "💡", "Belaku (Light)"),
    PictureWord(listOf("ಪು", "ಸ್ತ", "ಕ"), "📖", "Pustaka (Book)"),
    PictureWord(listOf("ಲೇ", "ಖ", "ನಿ"), "🖊️", "Lekhani (Pen)"),
    PictureWord(listOf("ಶಾ", "ಲೆ"), "🏫", "Shaale (School)"),
    PictureWord(listOf("ರ", "ಸ್ತೆ"), "🛣️", "Raste (Road)"),
    PictureWord(listOf("ಗಾ", "ಡಿ"), "🚗", "Gaadi (Car)"),
    PictureWord(listOf("ವಿ", "ಮಾ", "ನ"), "✈️", "Vimaana (Airplane)"),
    PictureWord(listOf("ರೈ", "ಲು"), "🚆", "Railu (Train)"),
    PictureWord(listOf("ಕ", "ಡ", "ಲು"), "🌊", "Kadalu (Sea)")
)
