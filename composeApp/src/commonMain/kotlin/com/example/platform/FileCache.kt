package com.example.platform

/**
 * Small files the app downloads and keeps (e.g. printable worksheets), in the app's private storage.
 * Android: files/downloads. iOS: Library/Application Support/downloads.
 */
expect object FileCache {
    fun read(name: String): ByteArray?
    fun write(name: String, bytes: ByteArray)
}
