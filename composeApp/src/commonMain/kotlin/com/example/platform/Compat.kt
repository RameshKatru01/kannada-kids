package com.example.platform

import kotlin.random.Random

/** Minimal replacement for android.util.Log. */
object Log {
    fun d(tag: String, msg: String): Int { println("D/$tag: $msg"); return 0 }
    fun i(tag: String, msg: String): Int { println("I/$tag: $msg"); return 0 }
    fun w(tag: String, msg: String, tr: Throwable? = null): Int {
        println("W/$tag: $msg${tr?.let { " - ${it.message}" } ?: ""}"); return 0
    }
    fun e(tag: String, msg: String, tr: Throwable? = null): Int {
        println("E/$tag: $msg${tr?.let { " - ${it.message}" } ?: ""}"); return 0
    }
}

/** Minimal replacement for java.util.Locale (only what the app uses). */
class Locale(val language: String, val country: String = "") {
    fun toLanguageTag(): String = if (country.isEmpty()) language else "$language-$country"

    class Builder {
        private var lang = ""
        private var region = ""
        fun setLanguage(language: String): Builder { lang = language; return this }
        fun setRegion(region: String): Builder { this.region = region; return this }
        fun build(): Locale = Locale(lang, region)
    }

    companion object {
        val US = Locale("en", "US")
        val ENGLISH = Locale("en")
    }
}

/** Minimal replacement for android.net.Uri.encode(). */
object Uri {
    private const val ALLOWED = "_-!.~'()*"
    private const val HEX = "0123456789ABCDEF"

    fun encode(s: String?): String {
        if (s == null) return ""
        val sb = StringBuilder()
        for (b in s.encodeToByteArray()) {
            val c = b.toInt() and 0xFF
            val ch = c.toChar()
            if (c < 0x80 && (ch.isLetterOrDigit() || ALLOWED.indexOf(ch) >= 0)) {
                sb.append(ch)
            } else {
                sb.append('%').append(HEX[c shr 4]).append(HEX[c and 0x0F])
            }
        }
        return sb.toString()
    }
}

/** A short, random identifier (replacement for UUID.randomUUID().toString()). */
fun randomId(): String {
    val hex = "0123456789abcdef"
    val sb = StringBuilder()
    repeat(32) { i ->
        if (i == 8 || i == 12 || i == 16 || i == 20) sb.append('-')
        sb.append(hex[Random.nextInt(16)])
    }
    return sb.toString()
}

