package com.example.ui.screens

/*
 * The words taught by the Kannada word-chart videos (Fun Videos > Fruits, Vegetables, Body Parts,
 * Family), shared by the video folders and Word Search. Adding a new topic here (with its videos)
 * gives Word Search a new topic too.
 */

private const val CHART = "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/54d7d8a9fb07e331bfb123b46e0c9362937aceb5/assets/videos/chart_kn/"

/** A word topic: [folder] is the Fun Videos folder with the same words. */
internal data class WordTopic(val id: String, val label: String, val icon: String, val folder: String?)

internal val wordTopics = listOf(
    WordTopic("fruits", "Fruits", "🍎", "Fruits"),
    WordTopic("vegetables", "Vegetables", "🥕", "Vegetables"),
    WordTopic("body", "Body Parts", "👃", "Body Parts"),
    WordTopic("family", "Family", "👨‍👩‍👧", "Family"),
    WordTopic("mix", "Mix", "🔀", null),
    WordTopic("general", "General", "🌳", null)
)

/** The Word Search topic for a Fun Videos folder, or null. */
internal fun topicForFolder(folder: String): String? = wordTopics.firstOrNull { it.folder == folder }?.id

private fun w(topic: String, file: String, picture: String, meaning: String, vararg syllables: String) =
    PictureWord(syllables.toList(), picture, meaning, video = CHART + file, topic = topic)

internal val videoWords: List<PictureWord> = listOf(
    w("fruits", "fruits/fruit_kn_01_apple.mp4", "🍎", "Sebu (Apple)", "ಸೇ", "ಬು"),
    w("fruits", "fruits/fruit_kn_02_banana.mp4", "🍌", "Baalehannu (Banana)", "ಬಾ", "ಳೆ", "ಹ", "ಣ್ಣು"),
    w("fruits", "fruits/fruit_kn_03_orange.mp4", "🍊", "Kittale (Orange)", "ಕಿ", "ತ್ತ", "ಳೆ"),
    w("fruits", "fruits/fruit_kn_04_mango.mp4", "🥭", "Maavu (Mango)", "ಮಾ", "ವು"),
    w("fruits", "fruits/fruit_kn_05_grapes.mp4", "🍇", "Draakshi (Grapes)", "ದ್ರಾ", "ಕ್ಷಿ"),
    w("fruits", "fruits/fruit_kn_06_pineapple.mp4", "🍍", "Ananas (Pineapple)", "ಅ", "ನಾ", "ನ", "ಸ್"),
    w("fruits", "fruits/fruit_kn_07_watermelon.mp4", "🍉", "Kallangadi (Watermelon)", "ಕ", "ಲ್ಲಂ", "ಗ", "ಡಿ"),
    w("fruits", "fruits/fruit_kn_08_strawberry.mp4", "🍓", "Strawberry (Strawberry)", "ಸ್ಟ್ರಾ", "ಬೆ", "ರಿ"),
    w("fruits", "fruits/fruit_kn_09_pomegranate.mp4", "🔴", "Daalimbe (Pomegranate)", "ದಾ", "ಳಿಂ", "ಬೆ"),
    w("fruits", "fruits/fruit_kn_10_guava.mp4", "🍐", "Seebehannu (Guava)", "ಸೀ", "ಬೆ", "ಹ", "ಣ್ಣು"),

    w("vegetables", "vegetables/veg_kn_01_potato.mp4", "🥔", "Aalugadde (Potato)", "ಆ", "ಲೂ", "ಗ", "ಡ್ಡೆ"),
    w("vegetables", "vegetables/veg_kn_02_brinjal.mp4", "🍆", "Badanekaayi (Brinjal)", "ಬ", "ದ", "ನೆ", "ಕಾ", "ಯಿ"),
    w("vegetables", "vegetables/veg_kn_03_cucumber.mp4", "🥒", "Southekaayi (Cucumber)", "ಸೌ", "ತೆ", "ಕಾ", "ಯಿ"),
    w("vegetables", "vegetables/veg_kn_04_onion.mp4", "🧅", "Eerulli (Onion)", "ಈ", "ರು", "ಳ್ಳಿ"),
    w("vegetables", "vegetables/veg_kn_05_ladysfinger.mp4", "🫛", "Bendekaayi (Lady's finger)", "ಬೆಂ", "ಡೆ", "ಕಾ", "ಯಿ"),
    w("vegetables", "vegetables/veg_kn_06_greens.mp4", "🌿", "Soppu (Greens)", "ಸೊ", "ಪ್ಪು"),
    w("vegetables", "vegetables/veg_kn_07_beans.mp4", "🫘", "Huralikaayi (Beans)", "ಹು", "ರ", "ಳಿ", "ಕಾ", "ಯಿ"),
    w("vegetables", "vegetables/veg_kn_08_radish.mp4", "🌱", "Moolangi (Radish)", "ಮೂ", "ಲಂ", "ಗಿ"),
    w("vegetables", "vegetables/veg_kn_09_cabbage.mp4", "🥬", "Elekosu (Cabbage)", "ಎ", "ಲೆ", "ಕೋ", "ಸು"),
    w("vegetables", "vegetables/veg_kn_10_cauliflower.mp4", "🥦", "Hookosu (Cauliflower)", "ಹೂ", "ಕೋ", "ಸು"),

    // ಕೈ (hand) is one letter block, too short to hide in the grid, so it is not here.
    w("body", "body_parts/body_kn_01_head.mp4", "🙆", "Tale (Head)", "ತ", "ಲೆ"),
    w("body", "body_parts/body_kn_02_eye.mp4", "👁️", "Kannu (Eye)", "ಕ", "ಣ್ಣು"),
    w("body", "body_parts/body_kn_03_ear.mp4", "👂", "Kivi (Ear)", "ಕಿ", "ವಿ"),
    w("body", "body_parts/body_kn_04_nose.mp4", "👃", "Moogu (Nose)", "ಮೂ", "ಗು"),
    w("body", "body_parts/body_kn_05_mouth.mp4", "👄", "Baayi (Mouth)", "ಬಾ", "ಯಿ"),
    w("body", "body_parts/body_kn_07_leg.mp4", "🦵", "Kaalu (Leg)", "ಕಾ", "ಲು"),
    w("body", "body_parts/body_kn_08_tooth.mp4", "🦷", "Hallu (Tooth)", "ಹ", "ಲ್ಲು"),
    w("body", "body_parts/body_kn_09_stomach.mp4", "😋", "Hotte (Stomach)", "ಹೊ", "ಟ್ಟೆ"),
    w("body", "body_parts/body_kn_10_back.mp4", "🎒", "Bennu (Back)", "ಬೆ", "ನ್ನು"),

    w("family", "family/family_kn_01_mother.mp4", "👩", "Taayi (Mother)", "ತಾ", "ಯಿ"),
    w("family", "family/family_kn_02_father.mp4", "👨", "Tande (Father)", "ತಂ", "ದೆ"),
    w("family", "family/family_kn_03_elderbrother.mp4", "🧑", "Anna (Elder brother)", "ಅ", "ಣ್ಣ"),
    w("family", "family/family_kn_04_youngerbrother.mp4", "👦", "Tamma (Younger brother)", "ತ", "ಮ್ಮ"),
    w("family", "family/family_kn_05_eldersister.mp4", "👩‍🦱", "Akka (Elder sister)", "ಅ", "ಕ್ಕ"),
    w("family", "family/family_kn_06_youngersister.mp4", "👧", "Tangi (Younger sister)", "ತಂ", "ಗಿ"),
    w("family", "family/family_kn_07_son.mp4", "👨‍👦", "Maga (Son)", "ಮ", "ಗ"),
    w("family", "family/family_kn_08_daughter.mp4", "👩‍👧", "Magalu (Daughter)", "ಮ", "ಗ", "ಳು"),
    w("family", "family/family_kn_09_grandfather.mp4", "👴", "Ajja (Grandfather)", "ಅ", "ಜ್ಜ"),
    w("family", "family/family_kn_10_grandmother.mp4", "👵", "Ajji (Grandmother)", "ಅ", "ಜ್ಜಿ")
)

/** The words for a Word Search topic. */
internal fun wordsForTopic(topic: String): List<PictureWord> = when (topic) {
    "general" -> pictureWords
    "mix" -> videoWords
    else -> videoWords.filter { it.topic == topic }.ifEmpty { videoWords }
}
