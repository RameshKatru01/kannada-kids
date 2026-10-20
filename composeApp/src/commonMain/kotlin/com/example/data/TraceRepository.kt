package com.example.data

import com.example.platform.*

import androidx.compose.ui.geometry.Offset

object TraceRepository {
    // Map of letter character to a list of traces (each trace is a list of strokes)
    val userTraces = mutableMapOf<String, MutableList<List<List<Offset>>>>()

    fun addTrace(letter: String, strokes: List<List<Offset>>) {
        if (!userTraces.containsKey(letter)) {
            userTraces[letter] = mutableListOf()
        }
        userTraces[letter]?.add(strokes)
    }
}
