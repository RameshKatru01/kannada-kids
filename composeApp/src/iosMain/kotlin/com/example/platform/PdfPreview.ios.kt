package com.example.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFPage
import platform.PDFKit.PDFView
import platform.PDFKit.kPDFDisplayDirectionVertical
import platform.PDFKit.kPDFDisplaySinglePageContinuous
import platform.UIKit.UIColor

/** Joins the wanted pages of every doc into one PDFDocument for the viewer. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun buildDocument(docs: List<PreviewPdf>): PDFDocument {
    val out = PDFDocument()
    for (d in docs) {
        val data = d.bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = d.bytes.size.toULong()) }
        val src = PDFDocument(data = data)
        val count = minOf(src.pageCount.toInt(), d.maxPages)
        for (i in 0 until count) {
            // A page belongs to one document, so a copy is inserted.
            val page = src.pageAtIndex(i.toULong())?.copy() as? PDFPage ?: continue
            out.insertPage(page, atIndex = out.pageCount)
        }
    }
    return out
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PdfPagesView(docs: List<PreviewPdf>, modifier: Modifier) {
    val document = remember(docs) { buildDocument(docs) }
    UIKitView(
        factory = {
            PDFView().apply {
                displayMode = kPDFDisplaySinglePageContinuous
                displayDirection = kPDFDisplayDirectionVertical
                displaysPageBreaks = true
                autoScales = true
                backgroundColor = UIColor(white = 0.92, alpha = 1.0)
                this.document = document
            }
        },
        modifier = modifier,
        update = { view ->
            if (view.document != document) {
                view.document = document
                view.autoScales = true
            }
        },
        properties = UIKitInteropProperties(isInteractive = true, isNativeAccessibilityEnabled = true)
    )
}
