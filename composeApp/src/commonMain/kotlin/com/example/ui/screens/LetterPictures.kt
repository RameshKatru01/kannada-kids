package com.example.ui.screens

import com.example.resources.Res
import com.example.resources.pic_arasa
import com.example.resources.pic_aane
import com.example.resources.pic_ili
import com.example.resources.pic_eerulli
import com.example.resources.pic_ugibandi
import com.example.resources.pic_oota
import com.example.resources.pic_rushi
import com.example.resources.pic_ele
import com.example.resources.pic_eeni
import com.example.resources.pic_aidu
import com.example.resources.pic_onte
import com.example.resources.pic_odu
import com.example.resources.pic_aushadhi
import com.example.resources.pic_angadi
import com.example.resources.pic_karadi
import com.example.resources.pic_khadga
import com.example.resources.pic_gadiyara
import com.example.resources.pic_ghante
import com.example.resources.pic_chamacha
import com.example.resources.pic_chhatri
import com.example.resources.pic_jahaju
import com.example.resources.pic_jhari
import com.example.resources.pic_tagaru
import com.example.resources.pic_damaru
import com.example.resources.pic_dhakka
import com.example.resources.pic_takkadi
import com.example.resources.pic_dana
import com.example.resources.pic_dhanassu
import com.example.resources.pic_navilu
import com.example.resources.pic_pata
import com.example.resources.pic_phala
import com.example.resources.pic_bassu
import com.example.resources.pic_bharatanatya
import com.example.resources.pic_mane
import com.example.resources.pic_yantra
import com.example.resources.pic_ravi
import com.example.resources.pic_laddu
import com.example.resources.pic_vajra
import com.example.resources.pic_shankha
import com.example.resources.pic_shatkona
import com.example.resources.pic_sarpa
import com.example.resources.pic_hamsa
import org.jetbrains.compose.resources.DrawableResource

/** A letter's picture word, as shown in its letter video (ಅ → ಅರಸ, the king). */
data class LetterPic(val word: String, val english: String, val image: DrawableResource)

/**
 * Pictures for the letter games (Memory Flip, Match the Letter), cut from the app's own letter
 * videos, so the games show the same characters the children see in the videos.
 */
val letterPictures: Map<String, LetterPic> = mapOf(
    "ಅ" to LetterPic("ಅರಸ", "king", Res.drawable.pic_arasa),
    "ಆ" to LetterPic("ಆನೆ", "elephant", Res.drawable.pic_aane),
    "ಇ" to LetterPic("ಇಲಿ", "mouse", Res.drawable.pic_ili),
    "ಈ" to LetterPic("ಈರುಳ್ಳಿ", "onion", Res.drawable.pic_eerulli),
    "ಉ" to LetterPic("ಉಗಿಬಂಡಿ", "steam train", Res.drawable.pic_ugibandi),
    "ಊ" to LetterPic("ಊಟ", "meal", Res.drawable.pic_oota),
    "ಋ" to LetterPic("ಋಷಿ", "sage", Res.drawable.pic_rushi),
    "ಎ" to LetterPic("ಎಲೆ", "leaf", Res.drawable.pic_ele),
    "ಏ" to LetterPic("ಏಣಿ", "ladder", Res.drawable.pic_eeni),
    "ಐ" to LetterPic("ಐದು", "five", Res.drawable.pic_aidu),
    "ಒ" to LetterPic("ಒಂಟೆ", "camel", Res.drawable.pic_onte),
    "ಓ" to LetterPic("ಓದು", "read", Res.drawable.pic_odu),
    "ಔ" to LetterPic("ಔಷಧಿ", "medicine", Res.drawable.pic_aushadhi),
    "ಅಂ" to LetterPic("ಅಂಗಡಿ", "shop", Res.drawable.pic_angadi),
    "ಕ" to LetterPic("ಕರಡಿ", "bear", Res.drawable.pic_karadi),
    "ಖ" to LetterPic("ಖಡ್ಗ", "sword", Res.drawable.pic_khadga),
    "ಗ" to LetterPic("ಗಡಿಯಾರ", "clock", Res.drawable.pic_gadiyara),
    "ಘ" to LetterPic("ಘಂಟೆ", "bell", Res.drawable.pic_ghante),
    "ಚ" to LetterPic("ಚಮಚ", "spoon", Res.drawable.pic_chamacha),
    "ಛ" to LetterPic("ಛತ್ರಿ", "umbrella", Res.drawable.pic_chhatri),
    "ಜ" to LetterPic("ಜಹಜು", "ship", Res.drawable.pic_jahaju),
    "ಝ" to LetterPic("ಝರಿ", "waterfall", Res.drawable.pic_jhari),
    "ಟ" to LetterPic("ಟಗರು", "ram", Res.drawable.pic_tagaru),
    "ಡ" to LetterPic("ಡಮರು", "damaru drum", Res.drawable.pic_damaru),
    "ಢ" to LetterPic("ಢಕ್ಕ", "big drum", Res.drawable.pic_dhakka),
    "ತ" to LetterPic("ತಕ್ಕಡಿ", "weighing scale", Res.drawable.pic_takkadi),
    "ದ" to LetterPic("ದನ", "cow", Res.drawable.pic_dana),
    "ಧ" to LetterPic("ಧನಸ್ಸು", "bow", Res.drawable.pic_dhanassu),
    "ನ" to LetterPic("ನವಿಲು", "peacock", Res.drawable.pic_navilu),
    "ಪ" to LetterPic("ಪಟ", "kite", Res.drawable.pic_pata),
    "ಫ" to LetterPic("ಫಲ", "fruit", Res.drawable.pic_phala),
    "ಬ" to LetterPic("ಬಸ್ಸು", "bus", Res.drawable.pic_bassu),
    "ಭ" to LetterPic("ಭರತನಾಟ್ಯ", "classical dance", Res.drawable.pic_bharatanatya),
    "ಮ" to LetterPic("ಮನೆ", "house", Res.drawable.pic_mane),
    "ಯ" to LetterPic("ಯಂತ್ರ", "machine", Res.drawable.pic_yantra),
    "ರ" to LetterPic("ರವಿ", "sun", Res.drawable.pic_ravi),
    "ಲ" to LetterPic("ಲಡ್ಡು", "laddu", Res.drawable.pic_laddu),
    "ವ" to LetterPic("ವಜ್ರ", "diamond", Res.drawable.pic_vajra),
    "ಶ" to LetterPic("ಶಂಖ", "conch shell", Res.drawable.pic_shankha),
    "ಷ" to LetterPic("ಷಟ್ಕೋನ", "hexagon", Res.drawable.pic_shatkona),
    "ಸ" to LetterPic("ಸರ್ಪ", "snake", Res.drawable.pic_sarpa),
    "ಹ" to LetterPic("ಹಂಸ", "swan", Res.drawable.pic_hamsa)
)

/** Letters whose picture is hard to recognise on its own (an action, a number, a shape): left out of picture games. */
private val unclearPictures = setOf("ಏ", "ಐ", "ಓ", "ಧ", "ಯ", "ಷ")

/** The picture for [letter] in the picture games, or null (no picture, or learning Hindi). */
fun gamePicture(letter: String): LetterPic? =
    if (com.example.data.Lang.isHindi || letter in unclearPictures) null else letterPictures[letter]

/** Every word the letter pictures use, for the voice clips (see SpeechCatalog). */
fun letterPicturePhrases(): List<String> = letterPictures.values.map { it.word }
