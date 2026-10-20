package com.example.platform

actual fun getSharedPreferences(name: String): SharedPreferences =
    AndroidSharedPreferences(
        AppContextHolder.context.getSharedPreferences(name, android.content.Context.MODE_PRIVATE)
    )

private class AndroidSharedPreferences(
    private val prefs: android.content.SharedPreferences
) : SharedPreferences {
    override val all: Map<String, Any?> get() = prefs.all
    override fun getString(key: String, defValue: String?): String? = prefs.getString(key, defValue)
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        prefs.getStringSet(key, defValues)?.toSet()
    override fun getInt(key: String, defValue: Int): Int = prefs.getInt(key, defValue)
    override fun getLong(key: String, defValue: Long): Long = prefs.getLong(key, defValue)
    override fun getFloat(key: String, defValue: Float): Float = prefs.getFloat(key, defValue)
    override fun getBoolean(key: String, defValue: Boolean): Boolean = prefs.getBoolean(key, defValue)
    override fun contains(key: String): Boolean = prefs.contains(key)
    override fun edit(): SharedPreferences.Editor = AndroidEditor(prefs.edit())
}

private class AndroidEditor(
    private val editor: android.content.SharedPreferences.Editor
) : SharedPreferences.Editor {
    override fun putString(key: String, value: String?): SharedPreferences.Editor { editor.putString(key, value); return this }
    override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor { editor.putStringSet(key, values); return this }
    override fun putInt(key: String, value: Int): SharedPreferences.Editor { editor.putInt(key, value); return this }
    override fun putLong(key: String, value: Long): SharedPreferences.Editor { editor.putLong(key, value); return this }
    override fun putFloat(key: String, value: Float): SharedPreferences.Editor { editor.putFloat(key, value); return this }
    override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { editor.putBoolean(key, value); return this }
    override fun remove(key: String): SharedPreferences.Editor { editor.remove(key); return this }
    override fun clear(): SharedPreferences.Editor { editor.clear(); return this }
    override fun commit(): Boolean = editor.commit()
    override fun apply() = editor.apply()
}
