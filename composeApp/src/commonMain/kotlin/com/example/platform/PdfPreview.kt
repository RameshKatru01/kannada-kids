package com.example.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A PDF to show in [PdfPagesView]; only its first [maxPages] pages are shown. */
class PreviewPdf(val bytes: ByteArray, val maxPages: Int = Int.MAX_VALUE)

/**
 * Shows the pages of [docs] one under the other, as they will print.
 * iOS: PDFKit's PDFView (pinch to zoom). Android: PdfRenderer, drawing each page as it scrolls in.
 */
@Composable
expect fun PdfPagesView(docs: List<PreviewPdf>, modifier: Modifier)
