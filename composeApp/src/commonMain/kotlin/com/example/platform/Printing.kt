package com.example.platform

/** One PDF to print: its file name (shown in print queues and Files) and its contents. */
class PdfFile(val name: String, val bytes: ByteArray)

/**
 * Hands PDFs to the system so a parent can print them.
 * iOS: the files are joined into one PDF named [joinedName] and shown in the share sheet
 *      (Print, Save to Files, AirDrop, Mail…).
 * Android: the system print dialog (a printer, or "Save as PDF"), one file after another, because
 *      a print job holds a single PDF.
 * Must be called on the main thread; [context] should be the current screen's context.
 */
expect fun printPdfs(context: Context, files: List<PdfFile>, joinedName: String, title: String)
