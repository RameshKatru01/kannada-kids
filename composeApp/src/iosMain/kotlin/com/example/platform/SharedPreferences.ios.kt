package com.example.platform

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import platform.Foundation.NSUserDefaults

private val registry = mutableMapOf<String, IosSharedPreferences>()

actual fun getSharedPreferences(name: String): SharedPreferences =
    registry.getOrPut(name) { IosSharedPreferences(name) }

/**
 * Stores one preferences file as a single JSON document in NSUserDefaults.
 * Every value keeps its type ("i" Int, "l" Long, "f" Float, "b" Boolean, "s" String, "S" Set<String>)
 * so that `all` returns properly typed values, exactly like Android.
 */
private class IosSharedPreferences(name: String) : SharedPreferences {
    private val storageKey = "kk_prefs_$name"
    private val values: MutableMap<String, Any> = load()

    private fun load(): MutableMap<String, Any> {
        val raw = NSUserDefaults.standardUserDefaults.stringForKey(storageKey) ?: return mutableMapOf()
        return try {
            val result = mutableMapOf<String, Any>()
            val root = Json.parseToJsonElement(raw).jsonObject
            for ((key, element) in root) {
                val obj = element as? JsonObject ?: continue
                val type = obj["t"]?.jsonPrimitive?.contentOrNull ?: continue
                val v = obj["v"] ?: continue
                val decoded: Any? = when (type) {
                    "s" -> v.jsonPrimitive.contentOrNull
                    "i" -> v.jsonPrimitive.contentOrNull?.toIntOrNull()
                    "l" -> v.jsonPrimitive.contentOrNull?.toLongOrNull()
                    "f" -> v.jsonPrimitive.contentOrNull?.toFloatOrNull()
                    "b" -> v.jsonPrimitive.contentOrNull?.toBooleanStrictOrNull()
                    "S" -> (v as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet()
                    else -> null
                }
                if (decoded != null) result[key] = decoded
            }
            result
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    fun persist() {
        val json = buildJsonObject {
            for ((key, value) in values) {
                put(key, buildJsonObject {
                    when (value) {
                        is String -> { put("t", "s"); put("v", value) }
                        is Int -> { put("t", "i"); put("v", value.toString()) }
                        is Long -> { put("t", "l"); put("v", value.toString()) }
                        is Float -> { put("t", "f"); put("v", value.toString()) }
                        is Boolean -> { put("t", "b"); put("v", value.toString()) }
                        is Set<*> -> {
                            put("t", "S")
                            put("v", buildJsonArray { value.forEach { add(JsonPrimitive(it.toString())) } })
                        }
                        else -> { put("t", "s"); put("v", value.toString()) }
                    }
                })
            }
        }
        NSUserDefaults.standardUserDefaults.setObject(json.toString(), forKey = storageKey)
    }

    fun applyChanges(changes: Map<String, Any?>, clearFirst: Boolean) {
        if (clearFirst) values.clear()
        for ((key, value) in changes) {
            if (value == null) values.remove(key) else values[key] = value
        }
        persist()
    }

    override val all: Map<String, Any?> get() = values.toMap()

    override fun getString(key: String, defValue: String?): String? = (values[key] as? String) ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        (values[key] as? Set<String>)?.toSet() ?: defValues

    override fun getInt(key: String, defValue: Int): Int = when (val v = values[key]) {
        is Int -> v
        is Long -> v.toInt()
        else -> defValue
    }

    override fun getLong(key: String, defValue: Long): Long = when (val v = values[key]) {
        is Long -> v
        is Int -> v.toLong()
        else -> defValue
    }

    override fun getFloat(key: String, defValue: Float): Float = (values[key] as? Float) ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean = (values[key] as? Boolean) ?: defValue

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = IosEditor(this)
}

private class IosEditor(private val prefs: IosSharedPreferences) : SharedPreferences.Editor {
    private val changes = linkedMapOf<String, Any?>()
    private var clearFirst = false

    override fun putString(key: String, value: String?): SharedPreferences.Editor { changes[key] = value; return this }
    override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor { changes[key] = values?.toSet(); return this }
    override fun putInt(key: String, value: Int): SharedPreferences.Editor { changes[key] = value; return this }
    override fun putLong(key: String, value: Long): SharedPreferences.Editor { changes[key] = value; return this }
    override fun putFloat(key: String, value: Float): SharedPreferences.Editor { changes[key] = value; return this }
    override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { changes[key] = value; return this }
    override fun remove(key: String): SharedPreferences.Editor { changes[key] = null; return this }
    override fun clear(): SharedPreferences.Editor { clearFirst = true; return this }

    override fun commit(): Boolean {
        prefs.applyChanges(changes, clearFirst)
        changes.clear()
        clearFirst = false
        return true
    }

    override fun apply() {
        commit()
    }
}
