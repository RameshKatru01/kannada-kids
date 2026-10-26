package com.example.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGContextDrawPDFPage
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGDataProviderCreateWithFilename
import platform.CoreGraphics.CGDataProviderRelease
import platform.CoreGraphics.CGPDFDocumentCreateWithProvider
import platform.CoreGraphics.CGPDFDocumentGetNumberOfPages
import platform.CoreGraphics.CGPDFDocumentGetPage
import platform.CoreGraphics.CGPDFDocumentRelease
import platform.CoreGraphics.CGPDFPageGetBoxRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGPDFMediaBox
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginPDFContextToFile
import platform.UIKit.UIGraphicsBeginPDFPageWithInfo
import platform.UIKit.UIGraphicsEndPDFContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun printPdfs(context: Context, files: List<PdfFile>, joinedName: String, title: String) {
    if (files.isEmpty()) return
    // Write each PDF to a real file first; the share sheet needs files to offer Print and Save to Files.
    val paths = files.mapNotNull { f ->
        val path = NSTemporaryDirectory() + f.name
        val data = f.bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = f.bytes.size.toULong()) }
        if (data.writeToFile(path, atomically = true)) path else null
    }
    if (paths.isEmpty()) return
    val sharePath = if (paths.size == 1) paths.first() else joinPdfs(paths, NSTemporaryDirectory() + joinedName) ?: return
    present(NSURL.fileURLWithPath(sharePath))
}

/** Copies every page of [paths], in order, into one new PDF at [outPath]. Pages stay vector. */
@OptIn(ExperimentalForeignApi::class)
private fun joinPdfs(paths: List<String>, outPath: String): String? {
    if (!UIGraphicsBeginPDFContextToFile(outPath, CGRectMake(0.0, 0.0, 595.28, 841.89), null)) return null
    for (path in paths) {
        val provider = CGDataProviderCreateWithFilename(path) ?: continue
        val doc = CGPDFDocumentCreateWithProvider(provider)
        CGDataProviderRelease(provider)
        if (doc == null) continue
        val count = CGPDFDocumentGetNumberOfPages(doc).toInt()
        for (n in 1..count) {
            val page = CGPDFDocumentGetPage(doc, n.toULong()) ?: continue
            val box = CGPDFPageGetBoxRect(page, kCGPDFMediaBox)
            UIGraphicsBeginPDFPageWithInfo(box, null)
            val ctx = UIGraphicsGetCurrentContext() ?: continue
            // UIKit's PDF context is flipped (y down); PDF pages are drawn y up.
            val height = box.useContents { size.height }
            CGContextTranslateCTM(ctx, 0.0, height)
            CGContextScaleCTM(ctx, 1.0, -1.0)
            CGContextDrawPDFPage(ctx, page)
        }
        CGPDFDocumentRelease(doc)
    }
    UIGraphicsEndPDFContext()
    return outPath
}

@OptIn(ExperimentalForeignApi::class)
private fun present(url: NSURL) {
    val app = UIApplication.sharedApplication
    @Suppress("DEPRECATION")
    val window: UIWindow = app.keyWindow ?: (app.windows.firstOrNull() as? UIWindow) ?: return
    var top: UIViewController = window.rootViewController ?: return
    while (true) top = top.presentedViewController ?: break

    val sheet = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
    // On iPad the sheet is a popover and must be anchored, otherwise UIKit throws.
    sheet.popoverPresentationController?.let { popover ->
        val view = top.view
        popover.sourceView = view
        popover.sourceRect = view.bounds.useContents { CGRectMake(size.width / 2, size.height / 2, 1.0, 1.0) }
        popover.permittedArrowDirections = 0u
    }
    top.presentViewController(sheet, animated = true, completion = null)
}
