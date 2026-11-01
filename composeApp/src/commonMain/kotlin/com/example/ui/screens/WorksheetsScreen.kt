package com.example.ui.screens

import com.example.platform.*
import com.example.ui.components.readableWidth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ScrapbookInk
import com.example.ui.components.ScrapbookInkSoft
import com.example.ui.components.ScrapbookPaper
import com.example.ui.data.Access
import com.example.ui.data.ActivityTracker
import com.example.ui.data.Worksheets
import kotlinx.coroutines.launch

private val LearnedGreen = Color(0xFFD9F6F2)
private val LearnedInk = Color(0xFF0F6B62)
private val SelectBlue = Color(0xFF2E86DE)
private val SelectSoft = Color(0xFFE3F0FF)
private val TileGray = Color(0xFFF0F4F8)

/**
 * Letter Box → (any tab) → Printable worksheets (behind the grown-up sum), for one [section].
 * Tap letters (and the cover or review sheets) to select or unselect them; for vowels the pair
 * chips select two look-alike vowels at once. The bar at the bottom previews or prints everything
 * selected together. Learned letters are green; printed sheets get a printer; Gold sheets a crown.
 */
@Composable
fun WorksheetsScreen(section: Worksheets.Section = Worksheets.Section.Vowels, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accessVersion by Access.version.collectAsState()
    val trackerVersion by ActivityTracker.version.collectAsState()
    val learned = remember(trackerVersion) { ActivityTracker.learned(section.learnedType) }
    val items = remember(section) { Worksheets.itemSheets(section) }
    val kit = remember(section) { Worksheets.kit(section) }
    val extras = remember(section) { Worksheets.extras(section) }
    val isVowels = section == Worksheets.Section.Vowels

    var selected by remember { mutableStateOf(setOf<String>()) }
    // Bumped when ticks change so they redraw.
    var printedVersion by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<List<PreviewPdf>?>(null) }

    val chosen = Worksheets.all(section).filter { it.id in selected }
    val anyLocked = remember(accessVersion, selected) { chosen.any { !Worksheets.isOpen(it) } }

    fun toggle(sheet: Worksheets.Sheet) {
        selected = if (sheet.id in selected) selected - sheet.id else selected + sheet.id
    }

    fun failed() = Toast.makeText(context, "Could not download the worksheets. Check the internet and try again.", Toast.LENGTH_SHORT).show()

    fun print() {
        if (busy || chosen.isEmpty()) return
        if (anyLocked) {
            Worksheets.askForGold(section)
            return
        }
        busy = true
        scope.launch {
            if (!Worksheets.print(context, chosen, "Kannada ${section.plural}")) failed()
            printedVersion++
            busy = false
        }
    }

    fun openPreview() {
        if (busy || chosen.isEmpty()) return
        busy = true
        scope.launch {
            val docs = Worksheets.loadPreview(chosen)
            if (docs == null) failed() else preview = docs
            busy = false
        }
    }

    Box(Modifier.fillMaxSize().background(ScrapbookPaper)) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .readableWidth(720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp)
                // Room for the selection bar so the last card is never hidden behind it.
                .padding(bottom = if (selected.isEmpty()) 0.dp else 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BackHeader(section.title, "Letter Box · printable worksheets", onBack)

            WhiteCard {
                Text("Print and learn on paper", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                Text(
                    intro(section),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft
                )
            }

            key(accessVersion, printedVersion) {
                WhiteCard {
                    Text("Start here", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                    SheetRow(kit, icon = "📘", selected = kit.id in selected) { toggle(it) }
                }

                WhiteCard {
                    Text(section.plural.replaceFirstChar { it.uppercase() }, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                    Text("Tap to select · tap again to unselect", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val learnedIds = learned.sorted().mapNotNull { Worksheets.item(section, it)?.id }
                        if (learnedIds.isNotEmpty()) {
                            SmallChip("Select learned (${learnedIds.size})", testTag = "worksheets_select_learned") {
                                selected = selected + learnedIds
                            }
                        }
                        SmallChip("Select all", testTag = "worksheets_select_all") {
                            selected = selected + items.map { it.id }
                        }
                        val anyVowel = items.any { it.id in selected }
                        if (anyVowel) {
                            SmallChip("Clear", testTag = "worksheets_clear_vowels") {
                                selected = selected - items.map { it.id }.toSet()
                            }
                        }
                    }
                    items.chunked(5).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { sheet ->
                                VowelTile(
                                    sheet,
                                    learned = sheet.items.first() in learned,
                                    selected = sheet.id in selected,
                                    modifier = Modifier.weight(1f)
                                ) { toggle(it) }
                            }
                        }
                    }
                }

                if (isVowels) {
                    WhiteCard {
                        Text("Look-alike pairs", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                        Text(
                            "Select both vowels of a pair at once (8 pages)",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft
                        )
                        Worksheets.pairIndexes.chunked(3).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (a, b) ->
                                    val ids = setOf(Worksheets.vowel(a).id, Worksheets.vowel(b).id)
                                    val on = selected.containsAll(ids)
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (on) SelectSoft else TileGray)
                                            .border(if (on) 2.5.dp else 0.dp, if (on) SelectBlue else Color.Transparent, RoundedCornerShape(16.dp))
                                            .clickable { selected = if (on) selected - ids else selected + ids }
                                            .padding(vertical = 10.dp)
                                            .testTag("worksheet_pair_${a}_$b"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${Worksheets.glyph(a)} + ${Worksheets.glyph(b)}",
                                            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                                            color = if (on) SelectBlue else ScrapbookInk
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                extras.forEach { extra ->
                    WhiteCard {
                        Text("After a few vowels", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                        SheetRow(extra, icon = "🌼", selected = extra.id in selected) { toggle(it) }
                    }
                }
            }

            Text(
                "On Android, choose a printer or \"Save as PDF\". On iPhone and iPad, tap Print, or save to Files.",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }

        if (selected.isNotEmpty()) {
            SelectionBar(
                section = section,
                chosen = chosen,
                anyLocked = anyLocked,
                busy = busy,
                allPrinted = remember(printedVersion, selected) { chosen.all { Worksheets.printed(it) } },
                onClear = { selected = emptySet() },
                onPreview = { openPreview() },
                onPrint = { print() },
                onUntick = {
                    chosen.forEach { Worksheets.setPrinted(it, false) }
                    printedVersion++
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        preview?.let { docs ->
            PreviewOverlay(
                docs = docs,
                title = chosen.joinToString("  ") { it.title },
                anyLocked = anyLocked,
                onClose = { preview = null },
                onPrint = {
                    preview = null
                    print()
                }
            )
        }
    }
}

/** Bottom bar: what is selected, and Preview / Print. */
@Composable
private fun SelectionBar(
    section: Worksheets.Section,
    chosen: List<Worksheets.Sheet>,
    anyLocked: Boolean,
    busy: Boolean,
    allPrinted: Boolean,
    onClear: () -> Unit,
    onPreview: () -> Unit,
    onPrint: () -> Unit,
    onUntick: () -> Unit,
    modifier: Modifier
) {
    val pages = chosen.sumOf { it.pages }
    val vowelCount = chosen.count { it.items.size == 1 }
    val extras = chosen.filter { it.items.size != 1 }.map { it.title.lowercase() }
    val summary = buildList {
        if (vowelCount > 0) add(if (vowelCount == 1) "1 ${section.singular}" else "$vowelCount ${section.plural}")
        addAll(extras)
    }.joinToString(" + ")

    Column(
        modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .readableWidth(720.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("worksheets_selection_bar"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$summary · $pages pages", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                Text(
                    chosen.filter { it.items.size == 1 }.joinToString(" ") { it.title }.ifEmpty { "Ready to print" },
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft, maxLines = 1
                )
            }
            Text(
                "Clear",
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onClear() }.padding(8.dp).testTag("worksheets_clear"),
                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInkSoft
            )
        }
        if (anyLocked) {
            Text(
                "👑 Some of these are in the Gold pack. You can preview their first page.",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A6B00)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BarButton(if (busy) "Opening…" else "Preview", filled = false, Modifier.weight(1f), testTag = "worksheets_preview", onClick = onPreview)
            BarButton(if (anyLocked) "👑 Print" else "🖨️ Print", filled = true, Modifier.weight(1f), testTag = "worksheets_print", onClick = onPrint)
        }
        if (allPrinted) {
            Text(
                "Printed already · remove tick",
                modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(12.dp)).clickable { onUntick() }
                    .padding(horizontal = 10.dp, vertical = 4.dp).testTag("worksheets_untick"),
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LearnedInk
            )
        }
    }
}

/** Full-screen preview of the selected sheets, with Print. */
@Composable
private fun PreviewOverlay(
    docs: List<PreviewPdf>,
    title: String,
    anyLocked: Boolean,
    onClose: () -> Unit,
    onPrint: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ScrapbookPaper)
            // Swallow taps so they don't reach the list underneath.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .safeDrawingPadding()
            .testTag("worksheets_preview_screen")
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { onClose() }.testTag("worksheets_preview_close"),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            }
            Column(Modifier.weight(1f)) {
                Text("Preview", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft, maxLines = 1)
            }
            BarButton(if (anyLocked) "👑 Print" else "🖨️ Print", filled = true, Modifier.width(120.dp), testTag = "worksheets_preview_print", onClick = onPrint)
        }
        if (anyLocked) {
            Text(
                "Gold sheets show their first page only.",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A6B00)
            )
        }
        PdfPagesView(docs, Modifier.fillMaxWidth().weight(1f))
    }
}

/** One letter: the letter and its sound; outline + check when selected, tick when printed, crown when Gold. */
@Composable
private fun VowelTile(
    sheet: Worksheets.Sheet,
    learned: Boolean,
    selected: Boolean,
    modifier: Modifier,
    onClick: (Worksheets.Sheet) -> Unit
) {
    val open = Worksheets.isOpen(sheet)
    val printed = Worksheets.printed(sheet)
    val index = sheet.items.first()
    val shape = RoundedCornerShape(16.dp)
    Box(modifier.aspectRatio(0.9f)) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    when {
                        selected -> SelectSoft
                        learned -> LearnedGreen
                        else -> TileGray
                    }
                )
                .border(if (selected) 3.dp else 0.dp, if (selected) SelectBlue else Color.Transparent, shape)
                .clickable { onClick(sheet) }
                .padding(4.dp)
                .testTag("worksheet_item_$index"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                sheet.title, fontSize = if (sheet.title.length > 1) 22.sp else 26.sp, fontWeight = FontWeight.ExtraBold,
                color = when {
                    selected -> SelectBlue
                    learned -> LearnedInk
                    else -> ScrapbookInk
                }
            )
            Text(
                sheet.caption, maxLines = 1,
                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft
            )
        }
        if (selected) SelectedCheck(Modifier.align(Alignment.TopStart).padding(4.dp))
        when {
            !open -> AccessBadge(Access.State.NeedsPremium, Modifier.align(Alignment.TopEnd).padding(2.dp), size = 20.dp)
            printed -> PrintedTick(Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
    }
}

/** A full-width row for the starter kit or the review sheets. */
@Composable
private fun SheetRow(sheet: Worksheets.Sheet, icon: String, selected: Boolean, onClick: (Worksheets.Sheet) -> Unit) {
    val open = Worksheets.isOpen(sheet)
    val printed = Worksheets.printed(sheet)
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) SelectSoft else TileGray)
            .border(if (selected) 3.dp else 0.dp, if (selected) SelectBlue else Color.Transparent, shape)
            .clickable { onClick(sheet) }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("worksheet_${sheet.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(48.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
            Text(icon, fontSize = 22.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(sheet.title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) SelectBlue else ScrapbookInk)
            Text("${sheet.subtitle} · ${sheet.pages} pages", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                !open -> AccessBadge(Access.State.NeedsPremium, size = 24.dp)
                printed -> PrintedTick()
            }
            if (selected) SelectedCheck() else EmptyCheck()
        }
    }
}

@Composable
private fun SmallChip(label: String, testTag: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TileGray)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag(testTag),
        fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk
    )
}

@Composable
private fun BarButton(label: String, filled: Boolean, modifier: Modifier, testTag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(48.dp)
            .clip(shape)
            .background(if (filled) SelectBlue else Color.White)
            .border(2.dp, SelectBlue, shape)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (filled) Color.White else SelectBlue)
    }
}

@Composable
private fun SelectedCheck(modifier: Modifier = Modifier) {
    Box(modifier.size(22.dp).background(SelectBlue, CircleShape).border(1.5.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
        Text("✓", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

@Composable
private fun EmptyCheck(modifier: Modifier = Modifier) {
    Box(modifier.size(22.dp).border(2.dp, Color(0xFFB8C7D4), CircleShape))
}

/** Shown on sheets already printed (a printer, so it can't be mistaken for the blue selection check). */
@Composable
private fun PrintedTick(modifier: Modifier = Modifier) {
    Text("🖨️", modifier = modifier, fontSize = 13.sp)
}

/** What the section's sheets contain, for the card at the top. */
private fun intro(section: Worksheets.Section): String = when (section) {
    Worksheets.Section.Vowels ->
        "Tap the vowels you want, then preview or print them together. Each vowel is 4 A4 pages: " +
            "tracing, colouring, find-and-circle, and picture-and-word. Learned vowels are green."
    Worksheets.Section.Consonants ->
        "Tap the consonants you want, then preview or print them together. Each consonant is 4 A4 pages: " +
            "tracing, colouring, find-and-circle, and picture-and-word. Learned consonants are green."
    Worksheets.Section.KaKaa ->
        "Each consonant has 2 A4 pages: trace all 15 forms (ಕ ಕಾ ಕಿ …), then join each vowel to its form and " +
            "fill in the missing ones. Families your child has learned are green."
    Worksheets.Section.Numbers ->
        "Tap the numbers you want, then preview or print them together. Each number is 4 A4 pages: " +
            "tracing, colouring, find-and-circle, and count-and-write. Learned numbers are green."
}

/**
 * Preview of one sheet (a letter's worksheets) with Print, used on the letter screens. Gold sheets
 * show their first page and ask for Gold when Print is tapped.
 */
@Composable
internal fun SingleWorksheetPreview(section: Worksheets.Section, sheet: Worksheets.Sheet, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var docs by remember(sheet.id) { mutableStateOf<List<PreviewPdf>?>(null) }
    var failed by remember(sheet.id) { mutableStateOf(false) }
    LaunchedEffect(sheet.id) {
        val loaded = Worksheets.loadPreview(listOf(sheet))
        if (loaded == null) failed = true else docs = loaded
    }
    val locked = !Worksheets.isOpen(sheet)
    val title = "${sheet.title} · ${sheet.pages} pages"

    val loadedDocs = docs
    if (loadedDocs != null) {
        PreviewOverlay(
            docs = loadedDocs,
            title = title,
            anyLocked = locked,
            onClose = onClose,
            onPrint = {
                if (locked) {
                    Worksheets.askForGold(section)
                } else {
                    scope.launch {
                        if (!Worksheets.print(context, listOf(sheet), "Kannada ${section.plural}")) {
                            Toast.makeText(context, "Could not open the worksheet. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                        onClose()
                    }
                }
            }
        )
    } else {
        // Loading (or failed): a light screen with a close button.
        Box(
            Modifier
                .fillMaxSize()
                .background(ScrapbookPaper)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .safeDrawingPadding()
                .padding(20.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ScrapbookInk)
            }
            Text(
                if (failed) "Could not open the worksheet. Please try again." else "Opening the worksheet…",
                modifier = Modifier.align(Alignment.Center),
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScrapbookInkSoft
            )
        }
    }
}

/** The worksheet for letter [index] of a Letter Box tab (Ka-kaa forms of ಕ all use the ಕ family). */
internal fun worksheetFor(tab: com.example.ui.reference.RefTab, index: Int): Pair<Worksheets.Section, Worksheets.Sheet>? {
    val section = when (tab) {
        com.example.ui.reference.RefTab.Vowels -> Worksheets.Section.Vowels
        com.example.ui.reference.RefTab.Consonants -> Worksheets.Section.Consonants
        com.example.ui.reference.RefTab.KaKaa -> Worksheets.Section.KaKaa
        com.example.ui.reference.RefTab.Numbers -> Worksheets.Section.Numbers
    }
    val itemIndex = if (tab == com.example.ui.reference.RefTab.KaKaa) 0 else index
    return Worksheets.item(section, itemIndex)?.let { section to it }
}
