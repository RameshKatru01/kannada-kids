package com.example.ui.data

import com.example.data.LetterData
import com.example.platform.*
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The printable worksheets for vowels, consonants, Ka-kaa and numbers. The PDFs are made by
 * tools/worksheets/VowelPack.swift into tools/worksheets/pdf (keep the file names in step), uploaded
 * to Firebase Storage (worksheets/<file>) by the "Upload videos & worksheets" GitHub action, and
 * downloaded by the app the first time a parent previews or prints them, then kept on the phone.
 * Parents select any mix of sheets and they print together.
 */
object Worksheets {

    /** A worksheet section, matching a Letter Box tab and a learning island. */
    enum class Section(
        val route: String,
        /** ActivityTracker type, for highlighting what the child has learned. */
        val learnedType: String,
        val title: String,
        val singular: String,
        val plural: String
    ) {
        Vowels("vowels", "vowels", "Vowel worksheets", "vowel", "vowels"),
        Consonants("consonants", "consonants", "Consonant worksheets", "consonant", "consonants"),
        KaKaa("kakaa", "guninthakshari", "Ka-kaa worksheets", "letter family", "letter families"),
        Numbers("numbers", "numbers", "Number worksheets", "number", "numbers");

        companion object {
            fun fromRoute(route: String?): Section = entries.firstOrNull { it.route == route } ?: Vowels

            /** The section for a letter-flow type ("vowels", "consonants", "guninthakshari", "numbers"). */
            fun fromLearnedType(type: String): Section? =
                if (!com.example.data.Lang.hasWorksheets) null else entries.firstOrNull { it.learnedType == type }
        }
    }

    data class Sheet(
        val id: String,
        val file: String,
        val title: String,
        val subtitle: String,
        /** Items covered (indexes into the section's letters); decides the plan lock. */
        val items: List<Int>,
        val pages: Int,
        /** Small line under the letter on its tile (the sound, or the number's value). */
        val caption: String = ""
    )

    /** Sound names used in the vowel file names, in vowel order. */
    private val vowelSounds = listOf("a", "aa", "i", "ee", "u", "oo", "ru", "e", "ae", "ai", "o", "oa", "au", "am", "aha")

    /** Vowels that look alike and are good to practise together (shortcuts for selecting both). */
    val pairIndexes = listOf(0 to 1, 2 to 3, 4 to 5, 7 to 8, 10 to 11, 9 to 12)

    private fun two(n: Int) = n.toString().padStart(2, '0')
    fun glyph(i: Int) = LetterData.vowels.getOrNull(i)?.character ?: ""

    // ---- Vowels ----

    val starterKit = Sheet(
        "starter", "starter-kit.pdf", "Starter kit",
        "Cover, how-to guide for parents, and a certificate", emptyList(), 3
    )

    val review = Sheet(
        "review", "review.pdf", "Review sheets",
        "Join the pairs, picture matching, colour by letter, missing vowels", vowelSounds.indices.toList(), 5
    )

    fun vowel(i: Int) = Sheet(
        "vowel_$i", "vowel-${two(i + 1)}-${vowelSounds[i]}.pdf", glyph(i),
        "Trace, colour, find, and picture-and-word", listOf(i), 4,
        caption = LetterData.vowels.getOrNull(i)?.transliteration ?: ""
    )

    // ---- Consonants, Ka-kaa, numbers ----

    private fun consonant(i: Int) = Sheet(
        "consonant_$i", "consonant-${two(i + 1)}.pdf", LetterData.consonants.getOrNull(i)?.character ?: "",
        "Trace, colour, find, and picture-and-word", listOf(i), 4,
        caption = LetterData.consonants.getOrNull(i)?.transliteration ?: ""
    )

    private fun kakaa(i: Int): Sheet {
        val c = LetterData.consonants.getOrNull(i)?.character ?: ""
        return Sheet(
            "kakaa_$i", "kakaa-${two(i + 1)}.pdf", c, "All 15 forms of $c: trace, join and fill in", listOf(i), 2,
            caption = (LetterData.consonants.getOrNull(i)?.transliteration ?: "") + " family"
        )
    }

    private fun number(i: Int) = Sheet(
        "number_$i", "number-${two(i + 1)}.pdf", LetterData.numbers.getOrNull(i)?.character ?: "",
        "Trace, colour, find, and count-and-write", listOf(i), 4,
        caption = LetterData.numbers.getOrNull(i)?.transliteration ?: ""
    )

    /** Cover and certificate (and, for vowels, the parent guide) for a section. Always free. */
    fun kit(section: Section): Sheet = when (section) {
        Section.Vowels -> starterKit
        Section.Consonants -> Sheet("consonants_kit", "consonants-kit.pdf", "Cover and certificate",
            "A cover to colour, and a certificate with a star for each consonant", emptyList(), 2)
        Section.KaKaa -> Sheet("kakaa_kit", "kakaa-kit.pdf", "Cover and certificate",
            "A cover to colour, and a Ka-kaa certificate", emptyList(), 2)
        Section.Numbers -> Sheet("numbers_kit", "numbers-kit.pdf", "Cover and certificate",
            "A cover to colour, and a certificate with a star for each number", emptyList(), 2)
    }

    /** One sheet per letter / letter family / number, in order. */
    fun itemSheets(section: Section): List<Sheet> = when (section) {
        Section.Vowels -> vowelSounds.indices.map { vowel(it) }
        Section.Consonants -> LetterData.consonants.indices.map { consonant(it) }
        Section.KaKaa -> LetterData.consonants.indices.map { kakaa(it) }
        Section.Numbers -> LetterData.numbers.indices.map { number(it) }
    }

    /** Extra sheets after the letters (vowels only). */
    fun extras(section: Section): List<Sheet> = if (section == Section.Vowels) listOf(review) else emptyList()

    /** Every sheet of a section in print order: kit, letters, extras. */
    fun all(section: Section): List<Sheet> = listOf(kit(section)) + itemSheets(section) + extras(section)

    /** A section's sheet for one letter, or null. */
    fun item(section: Section, index: Int): Sheet? = itemSheets(section).getOrNull(index)

    // Kept for the vowel screen code paths.
    val vowelSheets: List<Sheet> get() = itemSheets(Section.Vowels)

    fun isOpen(sheet: Sheet): Boolean = Access.worksheet(sheet.items) == Access.State.Open

    // ---- Printed ticks (per child account) ----

    private fun prefs() = getSharedPreferences("AppProgress")
    private fun user(): String = prefs().getString("last_active_user", "guest") ?: "guest"
    private fun key(sheet: Sheet) = "${user()}_worksheet_printed_${sheet.id}"

    fun printed(sheet: Sheet): Boolean = prefs().getBoolean(key(sheet), false)

    /** Lets a parent take the "printed" tick off again (for example after cancelling the print). */
    fun setPrinted(sheet: Sheet, value: Boolean) {
        prefs().edit().putBoolean(key(sheet), value).apply()
    }

    // ---- Preview ----

    /**
     * Loads [sheets] (in the given order) for the in-app preview. Sheets not in the plan show
     * only their first page, as a taste. Returns null when a file could not be opened.
     */
    suspend fun loadPreview(sheets: List<Sheet>): List<PreviewPdf>? = try {
        val bytes = download(sheets) ?: return null
        sheets.map { s -> PreviewPdf(bytes.getValue(s.file), if (isOpen(s)) Int.MAX_VALUE else 1) }
    } catch (e: Exception) {
        null
    }

    // ---- Downloading (Firebase Storage, kept on the phone) ----

    private const val DEFAULT_BASE =
        "https://firebasestorage.googleapis.com/v0/b/kannadakids-fd92d.firebasestorage.app/o/{object}?alt=media"
    private val http by lazy { createHttpClient() }

    private fun url(file: String): String {
        val base = getSharedPreferences("AppSettings").getString("files_base", null)?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE
        val obj = "worksheets/$file"
        return if ("{object}" in base) base.replace("{object}", encodeAll(obj)) else base.trimEnd('/') + "/" + obj
    }

    private fun encodeAll(s: String): String = buildString {
        for (b in s.encodeToByteArray()) {
            val u = b.toInt() and 0xFF
            val ch = u.toChar()
            if (u < 128 && (ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == '.' || ch == '~')) append(ch)
            else append('%').append(u.toString(16).uppercase().padStart(2, '0'))
        }
    }

    /** True when every sheet is already on the phone (no internet needed). */
    fun downloaded(sheets: List<Sheet>): Boolean = sheets.all { FileCache.read("ws_${it.file}") != null }

    /**
     * The PDF bytes of [sheets] by file name: from the phone when already downloaded, otherwise from
     * Firebase Storage (then kept). Null when one could not be downloaded (e.g. no internet).
     */
    suspend fun download(sheets: List<Sheet>): Map<String, ByteArray>? = withContext(Dispatchers.Default) {
        val out = HashMap<String, ByteArray>()
        for (s in sheets.distinctBy { it.file }) {
            val cached = FileCache.read("ws_${s.file}")
            if (cached != null) { out[s.file] = cached; continue }
            val bytes = try {
                val response = http.get(url(s.file))
                if (!response.status.isSuccess()) null else response.readRawBytes()
            } catch (e: Exception) {
                null
            }
            if (bytes == null || bytes.size < 100) return@withContext null
            FileCache.write("ws_${s.file}", bytes)
            out[s.file] = bytes
        }
        out
    }

    // ---- Printing ----

    /** Tells the parent a sheet is in the Gold pack. */
    fun askForGold(section: Section = Section.Vowels) {
        val first = itemSheets(section).firstOrNull()?.title ?: ""
        Access.askGrownUp("All the ${section.singular} worksheets are in the Gold pack. The $first sheets and the cover are free.")
    }

    /**
     * Prints [sheets] together (in the given order) through the system print / share sheet and
     * ticks them as printed. Locked sheets are skipped. Returns false when a file could not be opened.
     */
    suspend fun print(context: Context, sheets: List<Sheet>, label: String = "Kannada worksheets"): Boolean {
        val chosen = sheets.filter { isOpen(it) }
        if (chosen.isEmpty()) return true
        val bytes = download(chosen) ?: return false
        val files = chosen.map { PdfFile(it.file, bytes.getValue(it.file)) }
        val name = if (chosen.size == 1) chosen.first().file else "kannada-worksheets-${chosen.size}-sets.pdf"
        val title = "$label: " + chosen.joinToString(", ") { it.title }
        withContext(Dispatchers.Main) { printPdfs(context, files, name, title) }
        chosen.forEach { setPrinted(it, true) }
        return true
    }
}
