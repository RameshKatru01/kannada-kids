package com.example.platform

/**
 * Cross-platform key/value storage with the same API shape as Android's SharedPreferences.
 *
 * Android: backed by the real SharedPreferences (so existing users keep their progress).
 * iOS: backed by NSUserDefaults.
 */
interface SharedPreferences {
    val all: Map<String, Any?>

    fun getString(key: String, defValue: String?): String?
    fun getStringSet(key: String, defValues: Set<String>?): Set<String>?
    fun getInt(key: String, defValue: Int): Int
    fun getLong(key: String, defValue: Long): Long
    fun getFloat(key: String, defValue: Float): Float
    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun contains(key: String): Boolean
    fun edit(): Editor

    interface Editor {
        fun putString(key: String, value: String?): Editor
        fun putStringSet(key: String, values: Set<String>?): Editor
        fun putInt(key: String, value: Int): Editor
        fun putLong(key: String, value: Long): Editor
        fun putFloat(key: String, value: Float): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun remove(key: String): Editor
        fun clear(): Editor
        fun commit(): Boolean
        fun apply()
    }
}

/** Equivalent of `context.getSharedPreferences(name, Context.MODE_PRIVATE)`. */
expect fun getSharedPreferences(name: String): SharedPreferences
