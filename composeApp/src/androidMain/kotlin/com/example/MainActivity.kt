package com.example

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
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.ui.data.AudioFeedbackManager.shutdown()
    }
}
