package com.example.ui.screens

import com.example.ui.components.readableWidth
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.resources.Res
import com.example.resources.thumb_animals
import com.example.resources.thumb_fruits
import com.example.resources.thumb_vegetables
import com.example.resources.thumb_body_parts
import com.example.resources.thumb_family
import com.example.resources.thumb_birds
import com.example.resources.thumb_classroom
import com.example.resources.thumb_colours
import com.example.resources.thumb_consonants
import com.example.resources.thumb_how_many
import com.example.resources.thumb_letter_quiz
import com.example.resources.thumb_number_quiz
import com.example.resources.thumb_numbers
import com.example.resources.thumb_picture_quiz
import com.example.resources.thumb_playtime
import com.example.resources.thumb_vowels
import com.example.ui.components.LetterTag
import com.example.ui.components.PolaroidCard
import com.example.ui.components.ScrapbookHeader
import com.example.ui.components.ScrapbookInk
import com.example.ui.components.ScrapbookInkSoft
import com.example.ui.components.ScrapbookPaper
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** How a video folder looks in the scrapbook: photo (from one of its videos), letter tag, names. */
private data class FolderLook(
    val kannada: String,
    val glyph: String,
    val color: Color,
    val ink: Color,
    val photo: DrawableResource? = null
)

private val Teal = Color(0xFF2EC4B6)
private val Coral = Color(0xFFFF6B6B)
private val Violet = Color(0xFF6C63FF)
private val Sun = Color(0xFFFFC93C)
private val Tangerine = Color(0xFFFF9F1C)
private val Berry = Color(0xFFE84393)
private val Sky = Color(0xFF4DA8FF)
private val Leaf = Color(0xFF59C173)

private fun folderLook(name: String): FolderLook = when (name.lowercase()) {
    "vowels" -> FolderLook(if (com.example.data.Lang.isHindi) "स्वर" else "ಸ್ವರಗಳು", if (com.example.data.Lang.isHindi) "अ" else "ಅ", Teal, ScrapbookInk, Res.drawable.thumb_vowels)
    "consonants" -> FolderLook("ವ್ಯಂಜನಗಳು", "ಕ", Violet, Color.White, Res.drawable.thumb_consonants)
    "birds" -> FolderLook(if (com.example.data.Lang.isHindi) "पक्षी" else "ಪಕ್ಷಿಗಳು", if (com.example.data.Lang.isHindi) "प" else "ನ", Sky, ScrapbookInk, Res.drawable.thumb_birds)
    "animals" -> FolderLook("ಪ್ರಾಣಿಗಳು", "ಸಿಂ", Coral, ScrapbookInk, Res.drawable.thumb_animals)
    "colours", "colors" -> FolderLook("ಬಣ್ಣಗಳು", "ಬ", Berry, Color.White, Res.drawable.thumb_colours)
    "numbers" -> FolderLook("ಅಂಕೆಗಳು", "೩", Sun, ScrapbookInk, Res.drawable.thumb_numbers)
    "fruits" -> FolderLook(if (com.example.data.Lang.isHindi) "फल" else "ಹಣ್ಣುಗಳು", if (com.example.data.Lang.isHindi) "फ" else "ಹ", Tangerine, ScrapbookInk, Res.drawable.thumb_fruits)
    "vegetables" -> FolderLook("ತರಕಾರಿಗಳು", "ತ", Leaf, ScrapbookInk, Res.drawable.thumb_vegetables)
    "body parts" -> FolderLook("ದೇಹದ ಭಾಗಗಳು", "ಕೈ", Sky, ScrapbookInk, Res.drawable.thumb_body_parts)
    "family" -> FolderLook("ನನ್ನ ಕುಟುಂಬ", "ಅ", Violet, Color.White, Res.drawable.thumb_family)
    "letter quiz" -> FolderLook("ಅಕ್ಷರ ರಸಪ್ರಶ್ನೆ", "?", Sun, ScrapbookInk, Res.drawable.thumb_letter_quiz)
    "picture quiz" -> FolderLook("ಚಿತ್ರ ರಸಪ್ರಶ್ನೆ", "?", Tangerine, ScrapbookInk, Res.drawable.thumb_picture_quiz)
    "number quiz" -> FolderLook("ಸಂಖ್ಯೆ ರಸಪ್ರಶ್ನೆ", "?", Teal, ScrapbookInk, Res.drawable.thumb_number_quiz)
    "how many?" -> FolderLook("ಎಷ್ಟು?", "೧", Coral, ScrapbookInk, Res.drawable.thumb_how_many)
    "class room" -> FolderLook("ತರಗತಿ", "ತ", Teal, ScrapbookInk, Res.drawable.thumb_classroom)
    "play time" -> FolderLook("ಆಟದ ಸಮಯ", "ಆ", Tangerine, ScrapbookInk, Res.drawable.thumb_playtime)
    "getting ready to school" -> FolderLook("ಶಾಲೆಗೆ ಸಿದ್ಧ", "ಶಾ", Leaf, ScrapbookInk)
    "shopping mall" -> FolderLook("ಶಾಪಿಂಗ್ ಮಾಲ್", "ಮಾ", Berry, Color.White)
    "hospital" -> FolderLook("ಆಸ್ಪತ್ರೆ", "ಆ", Sky, ScrapbookInk)
    "vacation" -> FolderLook("ರಜೆ", "ರ", Sun, ScrapbookInk)
    else -> {
        val number = name.filter { it.isDigit() }
        val colors = listOf(Teal, Coral, Violet, Sun, Tangerine, Berry, Sky, Leaf)
        val c = colors[(number.toIntOrNull() ?: kotlin.math.abs(name.hashCode())) % colors.size]
        if (name.lowercase().startsWith("class") && number.isNotEmpty()) {
            FolderLook("ತರಗತಿ $number", number, c, ScrapbookInk)
        } else {
            FolderLook("", name.take(1).uppercase(), c, ScrapbookInk)
        }
    }
}

@Composable
fun VideoFoldersScreen(
    title: String,
    folders: List<String>,
    onFolderClick: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookPaper)
            .safeDrawingPadding()
            .readableWidth(1000.dp)
    ) {
        ScrapbookHeader(title = title, onBack = onBack)
        if (folders.isEmpty()) {
            Text(
                "Hindi videos are coming soon! Switch to Kannada in Parent corner to watch the Kannada videos.",
                modifier = Modifier.padding(24.dp),
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScrapbookInk
            )
            return@Column
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(folders) { index, folder ->
                VideoFolderPolaroid(index, folder) {
                    com.example.ui.audio.SoundEffectsHelper.playButtonTap()
                    onFolderClick(folder)
                }
            }
        }
    }
}

@Composable
private fun VideoFolderPolaroid(index: Int, folder: String, onClick: () -> Unit) {
    val look = folderLook(folder)
    PolaroidCard(
        index = index,
        onClick = onClick,
        modifier = Modifier.testTag("video_folder_$index"),
        picture = {
            AccessBadge(com.example.ui.data.Access.videoFolder(folder), Modifier.align(Alignment.TopEnd).padding(6.dp).zIndex(1f), size = 32.dp)
            val photo = look.photo
            if (photo != null) {
                Image(
                    painter = painterResource(photo),
                    contentDescription = "$folder videos",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                LetterTag(look.glyph, look.color, look.ink)
            } else {
                // No picture yet: a big letter on the folder's colour
                Box(
                    modifier = Modifier.fillMaxSize().background(look.color.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(84.dp).background(look.color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(look.glyph, color = look.ink, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp)
                    }
                }
            }
        },
        caption = {
            if (look.kannada.isNotEmpty()) {
                Text(
                    look.kannada,
                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = ScrapbookInk,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                folder.replaceFirstChar { it.uppercase() },
                fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ScrapbookInkSoft,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    )
}
