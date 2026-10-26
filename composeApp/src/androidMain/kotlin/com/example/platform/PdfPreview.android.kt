package com.example.platform

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Open renderers for the docs, plus a flat list of (doc, page) to show. */
private class OpenPdfs(context: android.content.Context, docs: List<PreviewPdf>) {
    private val files = mutableListOf<File>()
    private val descriptors = mutableListOf<ParcelFileDescriptor>()
    val renderers = mutableListOf<PdfRenderer>()
    val pages = mutableListOf<Pair<Int, Int>>()
    /** PdfRenderer allows one page open at a time, so drawing is done one page after another. */
    val lock = Mutex()

    init {
        docs.forEach { d ->
            val f = File.createTempFile("worksheet_preview", ".pdf", context.cacheDir).apply { writeBytes(d.bytes) }
            val pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
            val r = PdfRenderer(pfd)
            files += f; descriptors += pfd; renderers += r
            repeat(minOf(r.pageCount, d.maxPages)) { p -> pages += (renderers.size - 1) to p }
        }
    }

    fun render(doc: Int, page: Int, width: Int): ImageBitmap {
        val p = renderers[doc].openPage(page)
        try {
            val height = width * p.height / p.width
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(AndroidColor.WHITE)
            p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bmp.asImageBitmap()
        } finally {
            p.close()
        }
    }

    fun close() {
        renderers.forEach { runCatching { it.close() } }
        descriptors.forEach { runCatching { it.close() } }
        files.forEach { it.delete() }
    }
}

@Composable
actual fun PdfPagesView(docs: List<PreviewPdf>, modifier: Modifier) {
    val context = LocalContext.current
    val pdfs = remember(docs) { OpenPdfs(context, docs) }
    DisposableEffect(pdfs) { onDispose { pdfs.close() } }

    LazyColumn(
        modifier = modifier.background(Color(0xFFEBEBEB)),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(pdfs.pages) { (doc, page) ->
            // Each page is drawn when it scrolls into view and dropped when it leaves.
            var image by remember(doc, page) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(doc, page) {
                image = withContext(Dispatchers.IO) { pdfs.lock.withLock { pdfs.render(doc, page, 900) } }
            }
            Box(
                Modifier.fillMaxWidth().aspectRatio(595.28f / 841.89f).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                val img = image
                if (img != null) {
                    Image(img, contentDescription = "Worksheet page", modifier = Modifier.fillMaxWidth())
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
