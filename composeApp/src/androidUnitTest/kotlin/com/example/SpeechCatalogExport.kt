package com.example

import com.example.ui.audio.SpeechBank
import com.example.ui.audio.SpeechCatalog
import org.junit.Test
import java.io.File

/**
 * Writes every phrase the app speaks to build/voice/phrases.tsv (key <TAB> text), for
 * tools/generate_voice.py. Run: ./gradlew :composeApp:testDebugUnitTest --tests com.example.SpeechCatalogExport
 */
class SpeechCatalogExport {
    @Test
    fun export() {
        val phrases = SpeechCatalog.phrases()
        check(phrases.isNotEmpty())
        val out = File("build/voice/phrases.tsv")
        out.parentFile.mkdirs()
        out.writeText(phrases.joinToString("\n") { "${SpeechBank.key(it)}\t$it" } + "\n")
        println("Exported ${phrases.size} phrases to ${out.absolutePath}")
    }
}
