package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.platform.AppContextHolder
import com.google.firebase.FirebaseApp
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AppContextHolder.isInitialized) {
            AppContextHolder.context = applicationContext
        }

        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Firebase init failed", e)
        }

        val crashFile = File(getExternalFilesDir(null), "crash_log.txt")
        if (crashFile.exists()) {
            crashFile.delete()
        }

        setContent {
            App()
        }

        // Home-screen widget: keep today's word up to date, and say the word if the widget was tapped
        com.example.widget.WordWidgetProvider.refreshAll(applicationContext)
        sayWidgetWord(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sayWidgetWord(intent)
    }

    private fun sayWidgetWord(intent: Intent?) {
        val word = intent?.getStringExtra(com.example.widget.WordWidgetProvider.EXTRA_SAY_WORD) ?: return
        intent.removeExtra(com.example.widget.WordWidgetProvider.EXTRA_SAY_WORD)
        android.os.Handler(mainLooper).postDelayed({
            try { com.example.ui.audio.SpeechBank.play(applicationContext, word) } catch (e: Exception) {}
        }, 900)
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.ui.data.AudioFeedbackManager.shutdown()
    }
}
