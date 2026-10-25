package com.example.ui.data

import com.example.platform.*

/**
 * Where the app's videos are played from, changeable without an app update.
 *
 * The app's video links all point at the GitHub assets repo. When the online setting
 * config/app.videoBase (Firestore) is set, those links are moved to that host instead, e.g.
 *   Firebase Storage: https://firebasestorage.googleapis.com/v0/b/<bucket>/o/{object}?alt=media
 *                     ({object} = "videos/<git ref>/<file path>", fully URL-encoded)
 *   Any web host:     https://videos.example.com/   (the "<git ref>/<file path>" is added after it)
 * Until the online setting has been read (a fresh install, the first launch) the built-in
 * [DEFAULT_BASE] (Firebase Storage) is used, so the very first video doesn't come from GitHub.
 * An empty online value = play from GitHub. The last value is remembered for offline starts.
 * The players fall back to the GitHub link if a moved video fails to load.
 */
object VideoHost {
    const val GITHUB_PREFIX = "https://raw.githubusercontent.com/RameshKatru01/kannada-app-assets/"
    private const val PREF = "video_base"

    /** Firebase Storage, used until the online setting is known. */
    const val DEFAULT_BASE = "https://firebasestorage.googleapis.com/v0/b/kannadakids-fd92d.firebasestorage.app/o/{object}?alt=media"

    private val prefs get() = getSharedPreferences("AppSettings")
    private var base: String? = null
    private var loaded = false

    private fun current(): String? {
        if (!loaded) {
            // Nothing saved yet -> the default; a saved empty value means "use GitHub"
            val saved = prefs.getString(PREF, null)
            base = if (saved == null) DEFAULT_BASE else saved.ifBlank { null }
            loaded = true
        }
        return base
    }

    /** Reads the online setting (call at app start). */
    suspend fun refresh() {
        val doc = CloudStore.getPublic("config/app") ?: return
        val value = (doc["videoBase"] as? String)?.trim().orEmpty()
        prefs.edit().putString(PREF, value).apply()
        base = value.ifBlank { null }
        loaded = true
    }

    /** The link to actually play for [url] (unchanged when no other host is set). */
    fun resolve(url: String): String {
        if (!url.startsWith(GITHUB_PREFIX)) return url
        val host = current() ?: return url
        val relative = url.removePrefix(GITHUB_PREFIX)                  // <ref>/<path, %-encoded>
        return when {
            "{object}" in host -> host.replace("{object}", encodeAll("videos/" + percentDecode(relative)))
            else -> host.trimEnd('/') + "/" + relative
        }
    }

    private fun percentDecode(s: String): String {
        val out = ArrayList<Byte>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 < s.length) {
                val v = s.substring(i + 1, i + 3).toIntOrNull(16)
                if (v != null) { out += v.toByte(); i += 3; continue }
            }
            c.toString().encodeToByteArray().forEach { out += it }
            i++
        }
        return out.toByteArray().decodeToString()
    }

    private fun encodeAll(s: String): String {
        val sb = StringBuilder()
        for (b in s.encodeToByteArray()) {
            val u = b.toInt() and 0xFF
            val ch = u.toChar()
            if (ch.isLetterOrDigit() && u < 128 || ch == '-' || ch == '_' || ch == '.' || ch == '~') sb.append(ch)
            else sb.append('%').append(u.toString(16).uppercase().padStart(2, '0'))
        }
        return sb.toString()
    }
}
