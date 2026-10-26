package com.example.platform

import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.FileOutputStream

actual fun printPdfs(context: Context, files: List<PdfFile>, joinedName: String, title: String) {
    val manager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? PrintManager ?: return
    printFrom(manager, files, 0, title)
}

/** Prints files[index], then the next one once that print dialog has closed. */
private fun printFrom(manager: PrintManager, files: List<PdfFile>, index: Int, title: String) {
    val file = files.getOrNull(index) ?: return
    val adapter = object : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder(file.name)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build()
            callback.onLayoutFinished(info, oldAttributes != newAttributes)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            try {
                // The PDF is already laid out for A4, so it is written as it is.
                FileOutputStream(destination.fileDescriptor).use { it.write(file.bytes) }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }

        override fun onFinish() {
            printFrom(manager, files, index + 1, title)
        }
    }
    val attributes = PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
    val jobName = if (files.size > 1) "$title (${index + 1} of ${files.size})" else title
    manager.print(jobName, adapter, attributes)
}
