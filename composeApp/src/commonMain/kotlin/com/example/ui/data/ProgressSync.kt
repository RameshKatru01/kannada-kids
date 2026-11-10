package com.example.ui.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.platform.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

/**
 * Keeps a signed-in family's progress and settings online at progress/{uid} (Cloud Firestore),
 * so it survives a new phone or reinstall and is shared between Android and iPhone.
 *
 * What is synced: everything this app saves for the account (letters learned, answers, time in
 * the app, game stars and best scores, worksheets printed, stickers, Learning path settings) plus
 * the points total. NOT synced: subscription / Gold settings (those come only from the
 * server-checked subscription), the daily free-game runs, the child's name, and device settings
 * (language, theme). Guests are not synced.
 *
 * Merging (three-way, against what was last synced from this phone):
 *  - changed only here -> ours; changed only online -> theirs;
 *  - changed in both places -> numbers keep the higher value, sets are combined, yes/no keeps
 *    "yes", text keeps ours.
 * So playing on two devices never loses learned letters, stars or best scores.
 */
object ProgressSync {
    /** Stores whose keys start with the account's e-mail ("<email>_…") or end with it ("…_<email>"). */
    private val accountFiles = listOf("AppProgress", "ActivityStats", "GameProgress", "ReferenceProgress", "GamePrefs")

    /** Points: kept per device (not per account) by ScoreManager; synced as a whole. */
    private const val SCORES_FILE = "GameScores"

    /** What was last synced, per account (for the three-way merge). */
    private const val BASE_FILE = "ProgressSyncBase"

    /**
     * Never synced: Gold / plan switches (server-checked only), daily free runs, and the child's
     * name (so nothing that identifies a child goes online).
     */
    private val excluded = listOf("subscriptionPlan", "store_premium_granted", "_free_run_", "_learner_name")

    /** Firestore's limit is 1 MiB per document; stay well below it. */
    private const val MAX_JSON_CHARS = 900_000

    /** How often to sync while the app is open. */
    internal const val PERIOD_MS = 5 * 60_000L

    private val lock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ---- Encoding: every value keeps its type ----

    private fun encode(v: Any?): String? = when (v) {
        is Int -> "i:$v"
        is Long -> "l:$v"
        is Float -> "f:$v"
        is Boolean -> "b:$v"
        is String -> "s:$v"
        is Set<*> -> "S:" + JsonArray(v.map { JsonPrimitive(it.toString()) }.sortedBy { it.content }).toString()
        else -> null
    }

    private fun decodeSet(encoded: String): Set<String> = try {
        Json.parseToJsonElement(encoded.substring(2)).jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }.toSet()
    } catch (e: Exception) {
        emptySet()
    }

    private fun write(editor: SharedPreferences.Editor, key: String, encoded: String?) {
        if (encoded == null || encoded.length < 2) {
            editor.remove(key)
            return
        }
        val raw = encoded.substring(2)
        when (encoded[0]) {
            'i' -> raw.toIntOrNull()?.let { editor.putInt(key, it) }
            'l' -> raw.toLongOrNull()?.let { editor.putLong(key, it) }
            'f' -> raw.toFloatOrNull()?.let { editor.putFloat(key, it) }
            'b' -> editor.putBoolean(key, raw == "true")
            's' -> editor.putString(key, raw)
            'S' -> editor.putStringSet(key, decodeSet(encoded))
        }
    }

    private fun mapToJson(map: Map<String, String>): String =
        JsonObject(map.mapValues { JsonPrimitive(it.value) }).toString()

    private fun jsonToMap(text: String?): Map<String, String> = try {
        if (text.isNullOrBlank()) emptyMap()
        else Json.parseToJsonElement(text).jsonObject.mapNotNull { (k, v) ->
            v.jsonPrimitive.contentOrNull?.let { k to it }
        }.toMap()
    } catch (e: Exception) {
        emptyMap()
    }

    // ---- Which keys belong to the account ----

    private fun isExcluded(key: String) = key == "last_active_user" || excluded.any { key.contains(it) }

    /**
     * Sync key for a stored key, without the e-mail (so it is the same on every device):
     *  "<file>|p|<rest>" for "<email>_<rest>", "<file>|s|<rest>" for "<rest><email>",
     *  "GameScores|g|<key>" for points.
     */
    private fun syncKey(file: String, key: String, email: String): String? {
        if (isExcluded(key)) return null
        if (key.startsWith(email + "_")) return "$file|p|" + key.removePrefix(email + "_")
        if (key.length > email.length && key.endsWith(email) && key[key.length - email.length - 1] == '_') {
            return "$file|s|" + key.removeSuffix(email)
        }
        return null
    }

    private fun storedKey(syncKey: String, email: String): Pair<String, String>? {
        val parts = syncKey.split('|', limit = 3)
        if (parts.size != 3) return null
        val (file, kind, rest) = parts
        val key = when (kind) {
            "p" -> if (file in accountFiles) "${email}_$rest" else return null
            "s" -> if (file in accountFiles) rest + email else return null
            "g" -> if (file == SCORES_FILE) rest else return null
            else -> return null
        }
        return if (isExcluded(key)) null else file to key
    }

    private fun localSnapshot(email: String): Map<String, String> {
        val out = mutableMapOf<String, String>()
        accountFiles.forEach { file ->
            getSharedPreferences(file).all.forEach { (k, v) ->
                val sk = syncKey(file, k, email) ?: return@forEach
                encode(v)?.let { out[sk] = it }
            }
        }
        getSharedPreferences(SCORES_FILE).all.forEach { (k, v) ->
            if (k == "total_coins" || k.startsWith("score_")) encode(v)?.let { out["$SCORES_FILE|g|$k"] = it }
        }
        return out
    }

    // ---- Merge ----

    private fun bothChanged(ours: String?, theirs: String?): String? {
        if (ours == null) return theirs
        if (theirs == null) return ours
        if (ours[0] != theirs[0]) return ours
        val a = ours.substring(2)
        val b = theirs.substring(2)
        return when (ours[0]) {
            'i', 'l' -> if ((b.toLongOrNull() ?: Long.MIN_VALUE) > (a.toLongOrNull() ?: Long.MIN_VALUE)) theirs else ours
            'f' -> if ((b.toFloatOrNull() ?: Float.NEGATIVE_INFINITY) > (a.toFloatOrNull() ?: Float.NEGATIVE_INFINITY)) theirs else ours
            'b' -> if (a == "true" || b == "true") "b:true" else "b:false"
            'S' -> encode(decodeSet(ours) + decodeSet(theirs))
            else -> ours
        }
    }

    private fun merge(local: Map<String, String>, cloud: Map<String, String>, base: Map<String, String>): Map<String, String> {
        val out = mutableMapOf<String, String>()
        (local.keys + cloud.keys).forEach { k ->
            val l = local[k]
            val c = cloud[k]
            val b = base[k]
            val v = when {
                l == c -> l
                l == b -> c        // changed only online
                c == b -> l        // changed only on this phone
                else -> bothChanged(l, c)
            }
            if (v != null) out[k] = v
        }
        return out
    }

    // ---- Sync ----

    /** The account whose progress is on screen, if it is a real (non-guest) signed-in account. */
    private fun account(): Pair<String, String>? {
        val user = PlatformAuth.currentUser() ?: return null
        val email = getSharedPreferences("AppProgress").getString("last_active_user", null)?.takeIf { it.isNotBlank() }
            ?: return null
        val matches = email.equals(user.email ?: "", ignoreCase = true) ||
            email == "apple_${user.uid}@privaterelay.appleid.com"
        return if (matches) user.uid to email else null
    }

    /**
     * Merges this phone's progress with the online copy, both ways. Returns true when it worked.
     * Safe to call often; does nothing for guests or when offline.
     */
    suspend fun sync(): Boolean = lock.withLock {
        val (uid, email) = account() ?: return@withLock false
        val doc = try {
            CloudStore.get("progress/$uid")
        } catch (e: Exception) {
            Log.w("ProgressSync", "Couldn't read online progress", e)
            return@withLock false
        }
        val basePrefs = getSharedPreferences(BASE_FILE)
        val baseKey = "base_$uid"
        val local = localSnapshot(email)
        val cloud = jsonToMap(doc?.str("json"))
        // No online copy (first time, or it was removed): just combine everything we have.
        val base = if (doc == null) emptyMap() else jsonToMap(basePrefs.getString(baseKey, null))
        val merged = merge(local, cloud, base)

        // 1. Bring this phone up to date
        if (merged != local) {
            val editors = mutableMapOf<String, SharedPreferences.Editor>()
            (merged.keys + local.keys).forEach { k ->
                if (merged[k] == local[k]) return@forEach
                val (file, key) = storedKey(k, email) ?: return@forEach
                write(editors.getOrPut(file) { getSharedPreferences(file).edit() }, key, merged[k])
            }
            editors.values.forEach { it.apply() }
            notifyScreens()
        }

        // 2. Bring the online copy up to date
        val json = mapToJson(merged)
        if (merged != cloud) {
            if (json.length > MAX_JSON_CHARS) {
                Log.w("ProgressSync", "Progress too large to sync (${json.length} characters)")
                return@withLock false
            }
            try {
                CloudStore.update(
                    "progress/$uid",
                    mapOf("json" to json, "updatedAt" to currentTimeMillis(), "platform" to PlatformBilling.storeName())
                )
            } catch (e: Exception) {
                Log.w("ProgressSync", "Couldn't save progress online", e)
                return@withLock false
            }
        }
        basePrefs.edit().putString(baseKey, json).apply()
        true
    }

    /** Starts a sync in the background (e.g. when the app goes to the background). */
    fun syncInBackground() {
        scope.launch { runCatching { sync() } }
    }

    /** Forgets what was synced for [uid] (when the account is deleted). */
    fun forget(uid: String) {
        getSharedPreferences(BASE_FILE).edit().remove("base_$uid").apply()
    }

    private fun notifyScreens() {
        ScoreManager.reload()
        ActivityTracker.notifyChanged()
        Access.notifyChanged()
        com.example.ui.progress.GameProgress.notifyChanged()
        com.example.ui.reference.ReferenceProgress.notifyChanged()
    }
}

/**
 * Syncs progress while the app is open: when it opens or comes back, every few minutes, and once
 * more when it goes to the background.
 */
@Composable
fun SyncProgress() {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                while (true) {
                    runCatching { ProgressSync.sync() }
                    delay(ProgressSync.PERIOD_MS)
                }
            } finally {
                ProgressSync.syncInBackground()
            }
        }
    }
}
