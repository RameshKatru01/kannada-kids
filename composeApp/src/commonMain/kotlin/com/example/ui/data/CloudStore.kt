package com.example.ui.data

import com.example.platform.*
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.*

/**
 * Tiny client for the Firebase online database (Cloud Firestore, project kannadakids-fd92d),
 * through its REST API so the same code runs on Android and iOS.
 *
 * Documents are plain maps of String / Long / Boolean / Double / List / Map values.
 */
object CloudStore {
    const val PROJECT_ID = "kannadakids-fd92d"
    private const val BASE = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents"
    private val http by lazy { createHttpClient() }

    class CloudException(message: String, val code: Int = 0) : Exception(message)

    private suspend fun token(): String =
        PlatformAuth.idToken() ?: throw CloudException("Please sign in with Google to use online features.", 401)

    /** Reads one document, or null when it doesn't exist. */
    suspend fun get(path: String): Map<String, Any?>? {
        val response = http.get("$BASE/$path") { header("Authorization", "Bearer ${token()}") }
        if (response.status.value == 404) return null
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw CloudException(errorMessage(text, response.status.value), response.status.value)
        return fromFields(Json.parseToJsonElement(text).jsonObject["fields"]?.jsonObject)
    }

    /** Reads a document that the rules let anyone read (no sign-in needed), or null. */
    suspend fun getPublic(path: String): Map<String, Any?>? {
        val response = http.get("$BASE/$path")
        if (response.status.value == 404) return null
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw CloudException(errorMessage(text, response.status.value), response.status.value)
        return fromFields(Json.parseToJsonElement(text).jsonObject["fields"]?.jsonObject)
    }

    /** All documents of a collection (id -> fields). */
    suspend fun list(collection: String): Map<String, Map<String, Any?>> {
        val out = linkedMapOf<String, Map<String, Any?>>()
        var pageToken: String? = null
        do {
            val url = "$BASE/$collection?pageSize=300" + (pageToken?.let { "&pageToken=$it" } ?: "")
            val response = http.get(url) { header("Authorization", "Bearer ${token()}") }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) throw CloudException(errorMessage(text, response.status.value), response.status.value)
            val json = Json.parseToJsonElement(text).jsonObject
            json["documents"]?.jsonArray?.forEach { d ->
                val obj = d.jsonObject
                val id = obj["name"]?.jsonPrimitive?.content?.substringAfterLast('/') ?: return@forEach
                out[id] = fromFields(obj["fields"]?.jsonObject) ?: emptyMap()
            }
            pageToken = json["nextPageToken"]?.jsonPrimitive?.contentOrNull
        } while (pageToken != null)
        return out
    }

    /** Creates or updates only the given fields of a document. */
    suspend fun update(path: String, fields: Map<String, Any?>) {
        val mask = fields.keys.joinToString("&") { "updateMask.fieldPaths=${Uri.encode(it)}" }
        val body = buildJsonObject { put("fields", toFields(fields)) }
        val response = http.patch("$BASE/$path?$mask") {
            header("Authorization", "Bearer ${token()}")
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        if (!response.status.isSuccess()) {
            throw CloudException(errorMessage(response.bodyAsText(), response.status.value), response.status.value)
        }
    }

    /** Deletes a document. */
    suspend fun delete(path: String) {
        val response = http.delete("$BASE/$path") { header("Authorization", "Bearer ${token()}") }
        if (!response.status.isSuccess() && response.status.value != 404) {
            throw CloudException(errorMessage(response.bodyAsText(), response.status.value), response.status.value)
        }
    }

    private fun errorMessage(text: String, code: Int): String = try {
        Json.parseToJsonElement(text).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
    } catch (e: Exception) {
        null
    } ?: "Online database error ($code)"

    // ---- Firestore value encoding ----

    private fun toFields(map: Map<String, Any?>): JsonObject = buildJsonObject {
        map.forEach { (k, v) -> put(k, toValue(v)) }
    }

    private fun toValue(v: Any?): JsonObject = when (v) {
        null -> buildJsonObject { put("nullValue", JsonNull) }
        is String -> buildJsonObject { put("stringValue", v) }
        is Boolean -> buildJsonObject { put("booleanValue", v) }
        is Int -> buildJsonObject { put("integerValue", v.toString()) }
        is Long -> buildJsonObject { put("integerValue", v.toString()) }
        is Double -> buildJsonObject { put("doubleValue", v) }
        is Float -> buildJsonObject { put("doubleValue", v.toDouble()) }
        is List<*> -> buildJsonObject {
            put("arrayValue", buildJsonObject { put("values", JsonArray(v.map { toValue(it) })) })
        }
        is Map<*, *> -> buildJsonObject {
            @Suppress("UNCHECKED_CAST")
            put("mapValue", buildJsonObject { put("fields", toFields(v as Map<String, Any?>)) })
        }
        else -> buildJsonObject { put("stringValue", v.toString()) }
    }

    private fun fromFields(fields: JsonObject?): Map<String, Any?>? =
        fields?.mapValues { (_, v) -> fromValue(v.jsonObject) }

    private fun fromValue(v: JsonObject): Any? {
        v["stringValue"]?.let { return it.jsonPrimitive.content }
        v["integerValue"]?.let { return it.jsonPrimitive.content.toLongOrNull() }
        v["booleanValue"]?.let { return it.jsonPrimitive.booleanOrNull }
        v["doubleValue"]?.let { return it.jsonPrimitive.doubleOrNull }
        v["timestampValue"]?.let { return it.jsonPrimitive.content }
        v["arrayValue"]?.let { a -> return a.jsonObject["values"]?.jsonArray?.map { fromValue(it.jsonObject) } ?: emptyList<Any?>() }
        v["mapValue"]?.let { m -> return fromFields(m.jsonObject["fields"]?.jsonObject) ?: emptyMap<String, Any?>() }
        return null
    }
}

// Small helpers for reading document maps
fun Map<String, Any?>.str(key: String): String? = this[key] as? String
fun Map<String, Any?>.long(key: String): Long = (this[key] as? Long) ?: (this[key] as? Double)?.toLong() ?: 0L
fun Map<String, Any?>.bool(key: String): Boolean = this[key] as? Boolean ?: false
