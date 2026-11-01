package com.example.platform

import java.io.File

actual object FileCache {
    private fun dir(): File? =
        if (AppContextHolder.isInitialized) File(AppContextHolder.context.filesDir, "downloads").apply { mkdirs() } else null

    actual fun read(name: String): ByteArray? = try {
        dir()?.let { File(it, name) }?.takeIf { it.exists() && it.length() > 0 }?.readBytes()
    } catch (e: Exception) {
        null
    }

    actual fun write(name: String, bytes: ByteArray) {
        try {
            val d = dir() ?: return
            val tmp = File(d, "$name.part")
            tmp.writeBytes(bytes)
            tmp.renameTo(File(d, name))
        } catch (_: Exception) {
        }
    }
}
