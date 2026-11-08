package com.example.ui.screens

import com.example.ui.components.readableWidth
import com.example.platform.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KannadaLetter
import com.example.ui.components.ScrapbookHeader
import com.example.ui.components.ScrapbookInk
import com.example.ui.components.ScrapbookInkSoft
import com.example.ui.components.ScrapbookPaper
import com.example.ui.progress.StarGold
import com.example.ui.progress.StarIcon
import com.example.ui.reference.ReferenceInfo
import com.example.ui.reference.ReferenceProgress
import com.example.ui.reference.RefTab

private const val ASSETS_MAIN = "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/main"

/** Stroke-writing video for a vowel or consonant (null for numbers and ka-kaa). */
fun getVideoUrlForLetter(letter: KannadaLetter): String? {
    val base = ASSETS_MAIN
    return when (letter.character) {
        "ಅ" -> "$base/Final_A.mp4"
        "ಆ" -> "$base/Final%20AA.mp4"
        "ಇ" -> "$base/Final_E.mp4"
        "ಈ" -> "$base/Final_EE.mp4"
        "ಉ" -> "$base/Final_u.mp4"
        "ಊ" -> "$base/Final_uu.mp4"
        "ಋ" -> "$base/Final_RU.mp4"
        "ಎ" -> "$base/Final_ye.mp4"
        "ಏ" -> "$base/Final_yeee.mp4"
        "ಐ" -> "$base/Final_I.mp4"
        "ಒ" -> "$base/Final_O.mp4"
        "ಓ" -> "$base/Final_OO.mp4"
        "ಔ" -> "$base/Final_ouh.mp4"
        "ಅಂ" -> "$base/Final_AUM.mp4"
        "ಅಃ" -> "$base/Final_AHA.mp4"
        "ಕ" -> "$base/Vowel_KA.mp4"
        "ಖ" -> "$base/Vowel_KHA.mp4"
        "ಗ" -> "$base/Vowel_GA.mp4"
        "ಘ" -> "$base/Vowel_GHHHA.mp4"
        "ಙ" -> "$base/Vowle_Inya.mp4"
        "ಚ" -> "$base/Vowel_cha.mp4"
        "ಛ" -> "$base/Vowel_CHHHA.mp4"
        "ಜ" -> "$base/Vowel_JA.mp4"
        "ಝ" -> "$base/Vowel_JJJAA.mp4"
        "ಞ" -> "$base/Vowel_INI.mp4"
        "ಟ" -> "$base/Vowel_Ta.mp4"
        "ಠ" -> "$base/Vowel_tttta.mp4"
        "ಡ" -> "$base/Vowle_Da.mp4"
        "ಢ" -> "$base/Vowel_DDDAA.mp4"
        "ಣ" -> "$base/Vowel_ANA.mp4"
        "ತ" -> "$base/Vowel_Tha.mp4"
        "ಥ" -> "$base/Vowel_THA2.mp4"
        "ದ" -> "$base/Vowel_Dha.mp4"
        "ಧ" -> "$base/Vowel_DHA2.mp4"
        "ನ" -> "$base/Vowels_Na.mp4"
        "ಪ" -> "$base/Vowel_Pa.mp4"
        "ಫ" -> "$base/Vowel_Pha2.mp4"
        "ಬ" -> "$base/Vowel_Ba.mp4"
        "ಭ" -> "$base/Vowel_Bha2.mp4"
        "ಮ" -> "$base/Vowel_ma.mp4"
        "ಯ" -> "$base/Vowel_Ya.mp4"
        "ರ" -> "$base/Vowel_Ra.mp4"
        "ಲ" -> "$base/Vowel_La.mp4"
        "ವ" -> "$base/Vowel_va.mp4"
        "ಶ" -> "$base/Vowel%20SSA.mp4"
        "ಷ" -> "$base/Vowel_Sha.mp4"
        "ಸ" -> "$base/Vowel_Sa.mp4"
        "ಹ" -> "$base/Vowel_ha.mp4"
        "ಳ" -> "$base/Vowel_lha.mp4"
        else -> null
    }
}

/** Pastel tile colour + its darker edge, cycled across tiles / consonant groups. */
internal val RefTints = listOf(
    Color(0xFFD9F6F2) to Color(0xFF2EC4B6),
    Color(0xFFFFE1E1) to Color(0xFFFF6B6B),
    Color(0xFFE6E4FF) to Color(0xFF6C63FF),
    Color(0xFFFFF0C2) to Color(0xFFF2B21B),
    Color(0xFFFFE6CC) to Color(0xFFFF9F1C),
    Color(0xFFFBDDEB) to Color(0xFFE84393),
    Color(0xFFDCEEFF) to Color(0xFF4DA8FF)
)

/** Reference: the letter chart. Tapping a letter opens its letter page. */
@Composable
fun ReferenceScreen(
    onBack: () -> Unit,
    onOpenLetter: (tab: Int, index: Int) -> Unit,
    onOpenWorksheets: (com.example.ui.data.Worksheets.Section) -> Unit = {}
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    // Printable worksheets are for grown-ups, so they open behind the Parent corner sum.
    var showWorksheetGate by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
    val tab = RefTab.entries[tabIndex]
    val version by ReferenceProgress.version.collectAsState()
    val letters = tab.letters()
    val practised = remember(version, tab) { ReferenceProgress.count(letters) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookPaper)
            .safeDrawingPadding()
            .readableWidth(820.dp)
    ) {
        ScrapbookHeader(title = "Letter Box", onBack = onBack)
        Text(
            "ಅಕ್ಷರ ಪೆಟ್ಟಿಗೆ · All the letters",
            modifier = Modifier.padding(start = 72.dp, bottom = 8.dp),
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft
        )

        // Tabs
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(22.dp))
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RefTab.entries.forEachIndexed { i, t ->
                val on = i == tabIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (on) ScrapbookInk else Color.Transparent)
                        .clickable { tabIndex = i }
                        .padding(vertical = 6.dp)
                        .testTag("ref_tab_$i"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(t.kannada, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = if (on) Color.White else ScrapbookInk)
                    Text(t.english, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (on) Color.White else ScrapbookInk)
                }
            }
        }

        val hasVideos = (tab == RefTab.Vowels || tab == RefTab.Consonants) && !com.example.data.Lang.isHindi
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(22.dp).background(Color(0xFFFF6B6B), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Text(
                if (hasVideos) "Tap a letter to watch how it is written" else "Tap a letter to hear it",
                modifier = Modifier.weight(1f),
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft
            )
            // This tab's printable worksheets: a small pill in the hint row, so it never takes room from the letters.
            if (com.example.data.Lang.hasWorksheets) WorksheetsChip { showWorksheetGate = true }
        }

        LazyVerticalGrid(
            // Consonants stay in rows of 5 (one ವರ್ಗ per row); other tabs add columns on wide screens
            columns = if (tab == RefTab.Consonants) GridCells.Fixed(5) else GridCells.Adaptive(minSize = 78.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (tab == RefTab.Consonants) 6.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (tab == RefTab.Consonants) 6.dp else 10.dp)
        ) {
            if (tab == RefTab.Consonants) {
                ReferenceInfo.consonantGroups().forEachIndexed { g, group ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "group_$g") {
                        Row(
                            modifier = Modifier.padding(top = if (g == 0) 0.dp else 8.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(group.kannada, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = ScrapbookInk)
                            Text(group.english, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ScrapbookInkSoft, modifier = Modifier.padding(bottom = 2.dp))
                        }
                    }
                    items(group.letters, key = { it.id }) { letter ->
                        LetterTile(
                            letter = letter,
                            sound = ReferenceInfo.sound(letter),
                            tint = RefTints[g % RefTints.size],
                            height = 64.dp,
                            letterSize = 26,
                            hasVideo = true,
                            practised = remember(version, letter.id) { ReferenceProgress.isPractised(letter) },
                            onClick = { onOpenLetter(tabIndex, letters.indexOfFirst { it.id == letter.id }) }
                        )
                    }
                }
            } else {
                items(letters.size, key = { letters[it].id }) { i ->
                    val letter = letters[i]
                    LetterTile(
                        letter = letter,
                        sound = if (tab == RefTab.Vowels) ReferenceInfo.sound(letter) else letter.transliteration,
                        tint = RefTints[i % RefTints.size],
                        height = 84.dp,
                        letterSize = if (letter.character.length > 1 && tab == RefTab.Numbers) 28 else 34,
                        hasVideo = hasVideos,
                        practised = remember(version, letter.id) { ReferenceProgress.isPractised(letter) },
                        onClick = { onOpenLetter(tabIndex, i) }
                    )
                }
            }
        }

        // Practised so far
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StarIcon(20.dp, StarGold)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "$practised of ${letters.size} ${tab.english.lowercase()} practised",
                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk
                )
                Box(
                    Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE3EEF8))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(if (letters.isEmpty()) 0f else practised.toFloat() / letters.size)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2EC4B6))
                    )
                }
            }
        }
    }

        if (showWorksheetGate) {
            ParentGate(
                purpose = "Answer to open the printable worksheets",
                onPass = {
                    showWorksheetGate = false
                    onOpenWorksheets(worksheetSection(tab))
                },
                onCancel = { showWorksheetGate = false }
            )
        }
    }
}

/** The worksheet section for a Letter Box tab. */
private fun worksheetSection(tab: RefTab) = when (tab) {
    RefTab.Vowels -> com.example.ui.data.Worksheets.Section.Vowels
    RefTab.Consonants -> com.example.ui.data.Worksheets.Section.Consonants
    RefTab.KaKaa -> com.example.ui.data.Worksheets.Section.KaKaa
    RefTab.Numbers -> com.example.ui.data.Worksheets.Section.Numbers
}

/** Opens this tab's printable worksheets (for grown-ups: print, trace and colour on paper). */
@Composable
private fun WorksheetsChip(onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("open_worksheets"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("🖨️", fontSize = 14.sp)
        Text("Print & trace", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk, maxLines = 1)
    }
}

@Composable
private fun LetterTile(
    letter: KannadaLetter,
    sound: String,
    tint: Pair<Color, Color>,
    height: Dp,
    letterSize: Int,
    hasVideo: Boolean,
    practised: Boolean,
    onClick: () -> Unit
) {
    val (bg, edge) = tint
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRoundRect(edge, cornerRadius = CornerRadius(16.dp.toPx()))
                drawRoundRect(
                    bg,
                    size = Size(size.width, size.height - 4.dp.toPx()),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
            .clickable { onClick() }
            .testTag("ref_letter_${letter.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Tight line box for the letter so the English sound below has room (it was cut off in the
            // small Consonants tiles)
            Text(
                letter.character, fontSize = letterSize.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk,
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(
                    lineHeight = (letterSize + 4).sp,
                    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                        alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                        trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
                    )
                )
            )
            Text(sound, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft, maxLines = 1, lineHeight = 13.sp)
        }
        if (hasVideo) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(5.dp).size(15.dp).background(edge, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
            }
        }
        if (practised) {
            StarIcon(13.dp, StarGold, Modifier.align(Alignment.TopStart).padding(start = 5.dp, top = 5.dp))
        }
    }
}
