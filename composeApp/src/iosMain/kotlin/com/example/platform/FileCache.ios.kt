package com.example.platform

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

actual object FileCache {
    private val dir: String? by lazy {
        val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: return@lazy null
        val d = "$base/downloads"
        NSFileManager.defaultManager.createDirectoryAtPath(d, true, null, null)
        d
    }

    actual fun read(name: String): ByteArray? {
        val path = dir?.let { "$it/$name" } ?: return null
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        val size = data.length.toInt()
        if (size == 0) return null
        val out = ByteArray(size)
        out.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        return out
    }

    actual fun write(name: String, bytes: ByteArray) {
        val path = dir?.let { "$it/$name" } ?: return
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        data.writeToFile(path, atomically = true)
    }
}
