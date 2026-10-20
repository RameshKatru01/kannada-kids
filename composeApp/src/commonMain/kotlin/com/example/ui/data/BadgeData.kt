package com.example.ui.data

import com.example.platform.*


data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: (SharedPreferences, String, String) -> Boolean
)

object BadgeManager {
    val badges = listOf(
        Badge("first_vowel", "First Steps", "Complete your first vowel.", "🌱") { prefs, user, lang ->
            prefs.getInt("${user}_${lang}_vowels_unlocked", 0) >= 1
        },
        Badge("half_vowels", "Vowel Adept", "Learn half of the vowels.", "🌿") { prefs, user, lang ->
            prefs.getInt("${user}_${lang}_vowels_unlocked", 0) >= 7
        },
        Badge("vowel_master", "Vowel Master", "Complete all vowels.", "🌟") { prefs, user, lang ->
            prefs.getInt("${user}_${lang}_vowels_unlocked", 0) >= 14
        },
        Badge("first_consonant", "Consonant Beginner", "Complete your first consonant.", "🚀") { prefs, user, lang ->
            prefs.getInt("${user}_${lang}_consonants_unlocked", 0) >= 1
        },
        Badge("consonant_master", "Consonant Master", "Complete all consonants.", "👑") { prefs, user, lang ->
            prefs.getInt("${user}_${lang}_consonants_unlocked", 0) >= 33
        },
        Badge("first_number", "Number Novice", "Learn your first number.", "1️⃣") { prefs, user, lang ->
            prefs.getInt("${user}_${lang}_numbers_unlocked", 0) >= 1
        },
        Badge("number_master", "Number Master", "Complete all numbers.", "💯") { prefs, user, lang ->
            prefs.getInt("${user}_${lang}_numbers_unlocked", 0) >= 9
        },
        Badge("gold_member", "Gold VIP", "Upgrade to the Gold plan.", "🏆") { prefs, user, lang ->
            prefs.getString("${lang}_subscriptionPlan_${user}", "Free") == "Gold"
        }
    )
}
