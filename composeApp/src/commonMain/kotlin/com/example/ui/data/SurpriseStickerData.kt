package com.example.ui.data

import com.example.platform.*

import androidx.compose.ui.graphics.Color

enum class StickerRarity(
    val label: String,
    val color: Color,
    val bgGradient: List<Color>,
    val defaultBonusCoins: Int = 15,
    val defaultPrimaryColor: Color = Color(0xFF4CAF50),
    val defaultSecondaryColor: Color = Color(0xFF1B5E20)
) {
    COMMON(
        label = "COMMON",
        color = Color(0xFF2E7D32),
        bgGradient = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)),
        defaultBonusCoins = 15,
        defaultPrimaryColor = Color(0xFF4CAF50),
        defaultSecondaryColor = Color(0xFF1B5E20)
    ),
    RARE(
        label = "RARE",
        color = Color(0xFF0277BD),
        bgGradient = listOf(Color(0xFFE1F5FE), Color(0xFFB3E5FC)),
        defaultBonusCoins = 25,
        defaultPrimaryColor = Color(0xFF0288D1),
        defaultSecondaryColor = Color(0xFF01579B)
    ),
    EPIC(
        label = "EPIC",
        color = Color(0xFF7B1FA2),
        bgGradient = listOf(Color(0xFFF3E5F5), Color(0xFFE1BEE7)),
        defaultBonusCoins = 35,
        defaultPrimaryColor = Color(0xFF8E24AA),
        defaultSecondaryColor = Color(0xFF4A148C)
    ),
    LEGENDARY(
        label = "LEGENDARY",
        color = Color(0xFFFF8F00),
        bgGradient = listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3)),
        defaultBonusCoins = 50,
        defaultPrimaryColor = Color(0xFFFFB300),
        defaultSecondaryColor = Color(0xFFFF6F00)
    )
}

data class SurpriseSticker(
    val id: String,
    val nameKannada: String,
    val nameEnglish: String,
    val descriptionKannada: String = "",
    val descriptionEnglish: String = "",
    val emoji: String,
    val rarity: StickerRarity = StickerRarity.COMMON,
    val bonusCoins: Int = rarity.defaultBonusCoins,
    val primaryColor: Color = rarity.defaultPrimaryColor,
    val secondaryColor: Color = rarity.defaultSecondaryColor
)

/**
 * Convenient builder to generate a new SurpriseSticker simply.
 */
fun createSticker(
    id: String,
    nameKannada: String,
    nameEnglish: String,
    emoji: String,
    rarity: StickerRarity = StickerRarity.COMMON,
    descriptionKannada: String = "",
    descriptionEnglish: String = "",
    bonusCoins: Int = rarity.defaultBonusCoins,
    primaryColor: Color = rarity.defaultPrimaryColor,
    secondaryColor: Color = rarity.defaultSecondaryColor
): SurpriseSticker {
    return SurpriseSticker(
        id = id,
        nameKannada = nameKannada,
        nameEnglish = nameEnglish,
        descriptionKannada = descriptionKannada,
        descriptionEnglish = descriptionEnglish,
        emoji = emoji,
        rarity = rarity,
        bonusCoins = bonusCoins,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor
    )
}

object SurpriseStickerManager {
    private const val PREFS_NAME = "AppProgress"
    private const val STICKERS_PREFIX = "surprise_stickers_"

    val allStickers: List<SurpriseSticker> = listOf(
        SurpriseSticker(
            id = "golden_crown",
            nameKannada = "ಚಿನ್ನದ ಕಿರೀಟ",
            nameEnglish = "Golden Crown",
            descriptionKannada = "ರಾಜೋಚಿತ ಜಯ! ನೀವು ಅಕ್ಷರಗಳ ರಾಜ!",
            descriptionEnglish = "Royal Victory! You are the King of Letters!",
            emoji = "👑",
            rarity = StickerRarity.LEGENDARY,
            bonusCoins = 50,
            primaryColor = Color(0xFFFFB300),
            secondaryColor = Color(0xFFFF6F00)
        ),
        SurpriseSticker(
            id = "karnataka_ratna",
            nameKannada = "ಕರ್ನಾಟಕ ರತ್ನ",
            nameEnglish = "Karnataka Gem",
            descriptionKannada = "ನಾಡಿನ ಹೆಮ್ಮೆಯ ಕಲಿ! ಅದ್ಭುತ ಪ್ರತಿಭೆ!",
            descriptionEnglish = "Pride of the Land! Outstanding Talent!",
            emoji = "💎",
            rarity = StickerRarity.LEGENDARY,
            bonusCoins = 45,
            primaryColor = Color(0xFFE91E63),
            secondaryColor = Color(0xFF880E4F)
        ),
        SurpriseSticker(
            id = "flying_airavata",
            nameKannada = "ಹಾರುವ ಐರಾವತ",
            nameEnglish = "Flying Airavata",
            descriptionKannada = "ಅತಿ ಬಲಶಾಲಿ ಸಾಧನೆ! ಮೇಲಕ್ಕೆ ಹಾರಿ!",
            descriptionEnglish = "Mighty achievement! Soar high!",
            emoji = "🐘",
            rarity = StickerRarity.EPIC,
            bonusCoins = 35,
            primaryColor = Color(0xFF3F51B5),
            secondaryColor = Color(0xFF1A237E)
        ),
        SurpriseSticker(
            id = "mysore_palace",
            nameKannada = "ಮೈಸೂರು ಅರಮನೆ",
            nameEnglish = "Mysore Palace",
            descriptionKannada = "ಭವ್ಯ ಕೌಶಲ್ಯ! ಜ್ಞಾನದ ಭವನ ನಿರ್ಮಿಸಿದ್ದೀರಿ!",
            descriptionEnglish = "Grand skill! You built a palace of knowledge!",
            emoji = "🏰",
            rarity = StickerRarity.EPIC,
            bonusCoins = 35,
            primaryColor = Color(0xFFFF7043),
            secondaryColor = Color(0xFFD84315)
        ),
        SurpriseSticker(
            id = "akshara_veera",
            nameKannada = "ಅಕ್ಷರ ವೀರ",
            nameEnglish = "Akshara Hero",
            descriptionKannada = "ಧೈರ್ಯಶಾಲಿ ಕಲಿ! ಪ್ರತಿಯೊಂದು ಸವಾಲು ಜಯಿಸಿದ್ದೀರಿ!",
            descriptionEnglish = "Brave learner! Conquered every challenge!",
            emoji = "🦸‍♂️",
            rarity = StickerRarity.EPIC,
            bonusCoins = 30,
            primaryColor = Color(0xFF7E57C2),
            secondaryColor = Color(0xFF4527A0)
        ),
        SurpriseSticker(
            id = "jnana_jyothi",
            nameKannada = "ಜ್ಞಾನ ಜ್ಯೋತಿ",
            nameEnglish = "Flame of Wisdom",
            descriptionKannada = "ನಿಮ್ಮ ಬುದ್ಧಿ ಬೆಳಕಿನಂತೆ ಪ್ರಕಾಶಿಸುತ್ತಿದೆ!",
            descriptionEnglish = "Your intellect shines like a bright lamp!",
            emoji = "🪔",
            rarity = StickerRarity.EPIC,
            bonusCoins = 30,
            primaryColor = Color(0xFFFFA000),
            secondaryColor = Color(0xFFFF6D00)
        ),
        SurpriseSticker(
            id = "geluvina_navilu",
            nameKannada = "ಗೆಲುವಿನ ನವಿಲು",
            nameEnglish = "Peacock of Victory",
            descriptionKannada = "ಸುಂದರ ಆಟ! ನವಿಲಿನಂತೆ ಗರಿ ಬಿಚ್ಚಿ ನಲಿಯಿರಿ!",
            descriptionEnglish = "Graceful play! Dance with pride like a peacock!",
            emoji = "🦚",
            rarity = StickerRarity.RARE,
            bonusCoins = 25,
            primaryColor = Color(0xFF009688),
            secondaryColor = Color(0xFF004D40)
        ),
        SurpriseSticker(
            id = "kalika_taare",
            nameKannada = "ಕಲಿಕಾ ತಾರೆ",
            nameEnglish = "Super Star",
            descriptionKannada = "ಆಕಾಶದ ತಾರೆಯಂತೆ ಹೊಳೆಯುತ್ತಿರುವ ಜಾಣ!",
            descriptionEnglish = "Bright spark shining like a star in the sky!",
            emoji = "⭐",
            rarity = StickerRarity.RARE,
            bonusCoins = 25,
            primaryColor = Color(0xFFFFCA28),
            secondaryColor = Color(0xFFFF8F00)
        ),
        SurpriseSticker(
            id = "chanaksha_gini",
            nameKannada = "ಚಾಣಾಕ್ಷ ಗಿಣಿ",
            nameEnglish = "Clever Parrot",
            descriptionKannada = "ಸ್ಪಷ್ಟ ಉಚ್ಚಾರಣೆ! ಗಿಣಿಯಂತೆ ಚುರುಕು!",
            descriptionEnglish = "Sharp pronunciation! Clever like a talking parrot!",
            emoji = "🦜",
            rarity = StickerRarity.RARE,
            bonusCoins = 20,
            primaryColor = Color(0xFF4CAF50),
            secondaryColor = Color(0xFF1B5E20)
        ),
        SurpriseSticker(
            id = "dheera_kesari",
            nameKannada = "ಧೀರ ಕೇಸರಿ",
            nameEnglish = "Brave Lion",
            descriptionKannada = "ಸಿಂಹದ ಶಕ್ತಿ! ಯಾವುದೇ ಮಟ್ಟಕ್ಕೂ ಹಿಂಜರಿಯುವುದಿಲ್ಲ!",
            descriptionEnglish = "Lion's courage! Fearless in every level!",
            emoji = "🦁",
            rarity = StickerRarity.RARE,
            bonusCoins = 20,
            primaryColor = Color(0xFFFF9800),
            secondaryColor = Color(0xFFE65100)
        ),
        SurpriseSticker(
            id = "kamala_kuvara",
            nameKannada = "ಕಮಲದ ಕುವರ",
            nameEnglish = "Lotus Wonder",
            descriptionKannada = "ಶಾಂತ ಮತ್ತು ಶುದ್ಧ ಕಲಿಕೆ! ಕಮಲದಂತೆ ಅರಳಿದ್ದೀರಿ!",
            descriptionEnglish = "Pure learning! Blooming brightly like a lotus!",
            emoji = "🪷",
            rarity = StickerRarity.RARE,
            bonusCoins = 20,
            primaryColor = Color(0xFFEC407A),
            secondaryColor = Color(0xFFAD1457)
        ),
        SurpriseSticker(
            id = "akshara_champion",
            nameKannada = "ಅಕ್ಷರ ಚಾಂಪಿಯನ್",
            nameEnglish = "Letter Champion",
            descriptionKannada = "ಉತ್ತಮ ಪ್ರಯತ್ನ! ಪ್ರತಿಯೊಂದು ಅಕ್ಷರದಲ್ಲೂ ಯಶಸ್ಸು!",
            descriptionEnglish = "Great effort! Success in every single letter!",
            emoji = "🏆",
            rarity = StickerRarity.COMMON,
            bonusCoins = 15,
            primaryColor = Color(0xFF26A69A),
            secondaryColor = Color(0xFF00695C)
        ),
        SurpriseSticker(
            id = "minchina_otagara",
            nameKannada = "ಮಿಂಚಿನ ಓಟಗಾರ",
            nameEnglish = "Lightning Runner",
            descriptionKannada = "ಮಿಂಚಿನ ವೇಗ! ಕ್ಷಣಾರ್ಧದಲ್ಲಿ ಉತ್ತರ ನೀಡಿದ್ದೀರಿ!",
            descriptionEnglish = "Lightning speed! Solved in a flash!",
            emoji = "⚡",
            rarity = StickerRarity.COMMON,
            bonusCoins = 15,
            primaryColor = Color(0xFFFDD835),
            secondaryColor = Color(0xFFF57F17)
        ),
        SurpriseSticker(
            id = "sagarada_muttu",
            nameKannada = "ಸಾಗರದ ಮುತ್ತು",
            nameEnglish = "Ocean Pearl",
            descriptionKannada = "ಅಮೂಲ್ಯ ಜ್ಞಾನ! ಮುತ್ತಿನಂತೆ ಹೊಳೆಯುವ ಬುದ್ಧಿ!",
            descriptionEnglish = "Precious pearl of wisdom! Keep shining!",
            emoji = "🦪",
            rarity = StickerRarity.COMMON,
            bonusCoins = 15,
            primaryColor = Color(0xFF42A5F5),
            secondaryColor = Color(0xFF1565C0)
        ),
        SurpriseSticker(
            id = "haduva_kogile",
            nameKannada = "ಹಾಡುವ ಕೋಗಿಲೆ",
            nameEnglish = "Sweet Nightingale",
            descriptionKannada = "ಇಂಪಾದ ಧ್ವನಿ! ಕನ್ನಡದ ಹಾಡಿನಂತ ಕಲಿಕೆ!",
            descriptionEnglish = "Sweet melodies of learning Kannada!",
            emoji = "🎵",
            rarity = StickerRarity.COMMON,
            bonusCoins = 15,
            primaryColor = Color(0xFFAB47BC),
            secondaryColor = Color(0xFF6A1B9A)
        ),
        SurpriseSticker(
            id = "geleyara_aane",
            nameKannada = "ಗೆಳೆಯ ಆನೆ",
            nameEnglish = "Friendly Gaja",
            descriptionKannada = "ಪ್ರೀತಿಯ ಸ್ನೇಹಿತ! ಸದಾ ಜೊತೆಯಿರುವ ಆನೆ ಮರಿ!",
            descriptionEnglish = "Warm companion! Always joyful along your journey!",
            emoji = "🎈",
            rarity = StickerRarity.COMMON,
            bonusCoins = 15,
            primaryColor = Color(0xFF29B6F6),
            secondaryColor = Color(0xFF0277BD)
        )
    )

    fun getUnlockedStickerIds(context: Context, userEmail: String): Set<String> {
        val prefs = getSharedPreferences(PREFS_NAME)
        return prefs.getStringSet("${STICKERS_PREFIX}$userEmail", emptySet()) ?: emptySet()
    }

    fun unlockSticker(context: Context, userEmail: String, stickerId: String): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME)
        val currentSet = prefs.getStringSet("${STICKERS_PREFIX}$userEmail", emptySet())?.toMutableSet() ?: mutableSetOf()
        val isNew = !currentSet.contains(stickerId)
        currentSet.add(stickerId)
        prefs.edit().putStringSet("${STICKERS_PREFIX}$userEmail", currentSet).apply()

        // Also credit bonus coins
        val sticker = allStickers.find { it.id == stickerId }
        if (sticker != null) {
            try {
                ScoreManager.addCoins(context, "sticker_reward", sticker.bonusCoins)
            } catch (e: Exception) {
                // ignore
            }
        }
        return isNew
    }

    /**
     * Simplified, direct sticker generation:
     * Returns a locked sticker to guarantee ongoing progress and reward excitement.
     * When all stickers are collected, returns a random sticker from the full album.
     */
    fun generateSticker(context: Context, userEmail: String): SurpriseSticker {
        val unlocked = getUnlockedStickerIds(context, userEmail)
        val lockedStickers = allStickers.filter { it.id !in unlocked }
        return lockedStickers.randomOrNull() ?: allStickers.random()
    }

    /**
     * Backward-compatible alias for existing callers.
     */
    fun getRandomSticker(context: Context, userEmail: String, seedModifier: Int = 0): SurpriseSticker {
        return generateSticker(context, userEmail)
    }

    /**
     * 1-Step generation and unlocking for simplified workflows.
     */
    fun generateAndUnlockSticker(context: Context, userEmail: String): Pair<SurpriseSticker, Boolean> {
        val sticker = generateSticker(context, userEmail)
        val isNew = unlockSticker(context, userEmail, sticker.id)
        return Pair(sticker, isNew)
    }
}
