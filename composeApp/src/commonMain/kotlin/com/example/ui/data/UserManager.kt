package com.example.ui.data

import com.example.platform.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data model representing a registered user and their subscription plan details.
 */
data class AppUser(
    val email: String,
    val displayName: String,
    val plan: String, // e.g. "Gold (Annual)", "Silver (Annual)", "Gold (Lifetime Admin)", "Free Explorer"
    val expiryDate: String, // e.g. "17 Oct 2027", "Permanent (No Expiry)"
    val expiryTimestamp: Long, // timestamp in ms; 0 for lifetime free, Long.MAX_VALUE for permanent admin
    val role: String = "User", // "Admin" or "User"
    val joinedDate: String = "15 Jan 2026",
    val status: String = "Active" // "Active", "Expiring Soon", "Expired"
) {
    val isAdmin: Boolean
        get() = role.equals("Admin", ignoreCase = true) ||
                email.equals(UserManager.PRIMARY_ADMIN_EMAIL, ignoreCase = true)

    val isGold: Boolean
        get() = plan.contains("Gold", ignoreCase = true)

    val isSilver: Boolean
        get() = plan.contains("Silver", ignoreCase = true)

    val isExpired: Boolean
        get() = expiryTimestamp > 0 && expiryTimestamp != Long.MAX_VALUE && currentTimeMillis() > expiryTimestamp

    /**
     * Checks if the user has one of the allowed unlocked profiles: Admin, Gold, or Silver.
     * Expired users (unless Admin) are considered locked.
     */
    val hasAllowedProfile: Boolean
        get() = (isAdmin || isGold || isSilver) && (!isExpired || isAdmin)

    val daysRemaining: Long
        get() {
            if (expiryTimestamp == 0L || expiryTimestamp == Long.MAX_VALUE) return Long.MAX_VALUE
            val diff = expiryTimestamp - currentTimeMillis()
            return if (diff > 0) diff / (24 * 3600 * 1000) else 0L
        }
}

/**
 * Central repository managing app users, subscription plans, expiry dates, and administrative roles.
 */
object UserManager {
    const val PRIMARY_ADMIN_EMAIL = "ramesh.katru01@gmail.com"
    private const val PREFS_NAME = "UserManagerPrefs"
    private const val KEY_USERS_JSON = "key_users_json"

    private val _usersFlow = MutableStateFlow<List<AppUser>>(emptyList())
    val usersFlow: StateFlow<List<AppUser>> = _usersFlow.asStateFlow()

    /**
     * Checks if the given email corresponds to an assigned Administrator.
     * Assigns ramesh.katru01@gmail.com as the primary admin.
     */
    fun isAdmin(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val clean = email.trim().lowercase()
        if (clean == PRIMARY_ADMIN_EMAIL.lowercase()) return true
        if (clean in AdminRoles.cloudAdmins.value) return true
        val user = _usersFlow.value.find { it.email.equals(clean, ignoreCase = true) }
        return user?.role.equals("Admin", ignoreCase = true)
    }

    /**
     * Checks if the user is logged in as a guest.
     */
    fun isGuest(email: String?): Boolean {
        // Locks in the app are based on "is this a guest?"; while everything is unlocked nobody is locked.
        if (FeatureFlags.UNLOCK_ALL) return false
        if (email.isNullOrBlank()) return true
        val clean = email.trim().lowercase()
        return clean == "guest" ||
                clean.startsWith("guest.") ||
                clean.contains("guest") ||
                clean.startsWith("guest_") ||
                clean == "guest.explorer@funbets.com"
    }

    /**
     * Checks if a user has full unlocked access based on having an Admin, Gold, or Silver profile.
     * Users without these profiles (Free Explorer, Guests, Expired users) return false (locked).
     */
    fun hasUnlockedAccess(context: Context? = null, email: String?): Boolean {
        if (FeatureFlags.UNLOCK_ALL) return true
        if (email.isNullOrBlank() || isGuest(email)) return false
        val clean = email.trim().lowercase()
        if (clean == PRIMARY_ADMIN_EMAIL.lowercase()) return true

        // 1. Check in-memory state
        val inMemory = _usersFlow.value.find { it.email.equals(clean, ignoreCase = true) }
        if (inMemory != null) {
            if (inMemory.isAdmin) return true
            if (inMemory.isExpired) return false
            if (inMemory.isGold || inMemory.isSilver) return true
            return false
        }

        // 2. If context provided, try loading or reading AppProgress
        if (context != null) {
            try {
                if (_usersFlow.value.isEmpty()) {
                    init(context)
                }
                val rechecked = _usersFlow.value.find { it.email.equals(clean, ignoreCase = true) }
                if (rechecked != null) {
                    if (rechecked.isAdmin) return true
                    if (rechecked.isExpired) return false
                    if (rechecked.isGold || rechecked.isSilver) return true
                    return false
                }

                val appProgress = getSharedPreferences("AppProgress")
                val plan = appProgress.getString("kn_subscriptionPlan_$clean", null)
                    ?: appProgress.getString("en_subscriptionPlan_$clean", null)
                    ?: appProgress.getString("subscriptionPlan_$clean", null)
                if (plan != null) {
                    val p = plan.trim().lowercase()
                    if (p.contains("admin") || p.contains("gold") || p.contains("silver")) {
                        return true
                    }
                }
            } catch (e: Exception) {
                Log.e("UserManager", "Error checking unlocked access", e)
            }
        }
        return false
    }

    /**
     * Returns true if user is locked (i.e. does not have an Admin, Gold, or Silver profile).
     */
    fun isUserLocked(context: Context? = null, email: String?): Boolean {
        return !hasUnlockedAccess(context, email)
    }

    /**
     * Initializes the user database. If empty, seeds initial users and assigns ramesh.katru01@gmail.com as Admin.
     */
    fun init(context: Context) {
        val prefs = getSharedPreferences(PREFS_NAME)
        val jsonStr = prefs.getString(KEY_USERS_JSON, null)
        if (jsonStr.isNullOrBlank()) {
            val seeded = createDefaultUsers()
            saveUsers(prefs, seeded)
            _usersFlow.value = seeded
        } else {
            val list = parseUsersJson(jsonStr)
            // Ensure primary admin is always present and marked as Admin
            val hasPrimaryAdmin = list.any { it.email.equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true) }
            val finalList = if (!hasPrimaryAdmin) {
                listOf(createPrimaryAdmin()) + list
            } else {
                list.map {
                    if (it.email.equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true)) {
                        it.copy(role = "Admin", plan = "Gold (Lifetime Admin)", expiryDate = "Permanent (No Expiry)", expiryTimestamp = Long.MAX_VALUE)
                    } else it
                }
            }
            saveUsers(prefs, finalList)
            _usersFlow.value = finalList
        }
    }

    /**
     * Called whenever a user logs in. If they are a new user, register them; if existing, update active details.
     */
    fun onUserLoggedIn(context: Context, email: String, displayName: String?) {
        init(context)
        val cleanEmail = email.trim()
        val name = displayName?.takeIf { it.isNotBlank() } ?: cleanEmail.substringBefore("@")
            .replace(".", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

        val prefs = getSharedPreferences(PREFS_NAME)
        val currentList = _usersFlow.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.email.equals(cleanEmail, ignoreCase = true) }

        if (existingIndex >= 0) {
            val existing = currentList[existingIndex]
            val isPrimaryAdmin = cleanEmail.equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true)
            val updated = existing.copy(
                displayName = if (existing.displayName.isNotBlank()) existing.displayName else name,
                role = if (isPrimaryAdmin) "Admin" else existing.role
            )
            currentList[existingIndex] = updated
        } else {
            val isPrimaryAdmin = cleanEmail.equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true)
            val expiryCal = addToDate(currentTimeMillis(), years = 1)
            val expiryStr = if (isPrimaryAdmin) "Permanent (No Expiry)" else "Unlimited Free"
            val expiryTimestamp = if (isPrimaryAdmin) Long.MAX_VALUE else 0L
            val newUser = AppUser(
                email = cleanEmail,
                displayName = name,
                plan = if (isPrimaryAdmin) "Gold (Lifetime Admin)" else "Free Explorer",
                expiryDate = expiryStr,
                expiryTimestamp = expiryTimestamp,
                role = if (isPrimaryAdmin) "Admin" else "User",
                joinedDate = formatDate(currentTimeMillis()),
                status = "Active"
            )
            currentList.add(0, newUser)
        }

        saveUsers(prefs, currentList)
        _usersFlow.value = currentList
    }

    /**
     * Updates an existing user's subscription plan and expiry date.
     */
    fun updateUserPlan(context: Context, email: String, newPlan: String, durationDays: Int = 365) {
        val prefs = getSharedPreferences(PREFS_NAME)
        val currentList = _usersFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.email.equals(email, ignoreCase = true) }

        val expiryCal = addToDate(currentTimeMillis(), days = durationDays)
        val expiryStr = if (durationDays <= 0 || newPlan.contains("Free", ignoreCase = true)) {
            "Unlimited Free"
        } else if (newPlan.contains("Lifetime", ignoreCase = true)) {
            "Permanent (No Expiry)"
        } else {
            formatDate(expiryCal)
        }
        val expiryTimestamp = if (durationDays <= 0 || newPlan.contains("Free", ignoreCase = true)) {
            0L
        } else if (newPlan.contains("Lifetime", ignoreCase = true)) {
            Long.MAX_VALUE
        } else {
            expiryCal
        }

        if (index >= 0) {
            val existing = currentList[index]
            val updated = existing.copy(
                plan = newPlan,
                expiryDate = expiryStr,
                expiryTimestamp = expiryTimestamp,
                status = if (newPlan.contains("Free", ignoreCase = true)) "Active" else "Active"
            )
            currentList[index] = updated
        } else {
            val newUser = AppUser(
                email = email,
                displayName = email.substringBefore("@"),
                plan = newPlan,
                expiryDate = expiryStr,
                expiryTimestamp = expiryTimestamp,
                role = if (email.equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true)) "Admin" else "User",
                joinedDate = formatDate(currentTimeMillis()),
                status = "Active"
            )
            currentList.add(newUser)
        }

        saveUsers(prefs, currentList)
        _usersFlow.value = currentList

        // Synchronize with AppProgress SharedPreferences so the app features react immediately
        try {
            val appProgress = getSharedPreferences("AppProgress")
            val langCode = "kn"
            val shortPlan = when {
                newPlan.contains("Gold", ignoreCase = true) -> "Gold"
                newPlan.contains("Silver", ignoreCase = true) -> "Silver"
                newPlan.contains("Admin", ignoreCase = true) -> "Admin"
                else -> "Free"
            }
            appProgress.edit().putString("${langCode}_subscriptionPlan_$email", shortPlan).apply()
        } catch (e: Exception) {
            Log.e("UserManager", "Failed to sync with AppProgress", e)
        }
    }

    /**
     * Updates a user with explicit custom expiry date string and timestamp.
     */
    fun updateUserCustomExpiry(
        context: Context,
        email: String,
        newPlan: String,
        expiryDateStr: String,
        expiryTimestamp: Long,
        role: String = "User"
    ) {
        val prefs = getSharedPreferences(PREFS_NAME)
        val currentList = _usersFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.email.equals(email, ignoreCase = true) }

        val isPrimaryAdmin = email.equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true)
        val finalRole = if (isPrimaryAdmin) "Admin" else role

        if (index >= 0) {
            val existing = currentList[index]
            val updated = existing.copy(
                plan = newPlan,
                expiryDate = expiryDateStr,
                expiryTimestamp = expiryTimestamp,
                role = finalRole,
                status = if (expiryTimestamp > 0 && expiryTimestamp != Long.MAX_VALUE && currentTimeMillis() > expiryTimestamp) "Expired" else "Active"
            )
            currentList[index] = updated
        } else {
            val newUser = AppUser(
                email = email,
                displayName = email.substringBefore("@"),
                plan = newPlan,
                expiryDate = expiryDateStr,
                expiryTimestamp = expiryTimestamp,
                role = finalRole,
                joinedDate = formatDate(currentTimeMillis()),
                status = "Active"
            )
            currentList.add(newUser)
        }

        saveUsers(prefs, currentList)
        _usersFlow.value = currentList
    }

    /**
     * Adds a brand new user to the system.
     */
    fun addNewUser(
        context: Context,
        email: String,
        displayName: String,
        plan: String,
        durationMonths: Int
    ): Boolean {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty()) return false

        val prefs = getSharedPreferences(PREFS_NAME)
        val currentList = _usersFlow.value.toMutableList()

        if (currentList.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
            return false // User already exists
        }

        val expiryCal = addToDate(currentTimeMillis(), months = durationMonths)
        val expiryStr = if (plan.contains("Free", ignoreCase = true)) {
            "Unlimited Free"
        } else {
            formatDate(expiryCal)
        }
        val expiryTs = if (plan.contains("Free", ignoreCase = true)) 0L else expiryCal
        val isPrimaryAdmin = cleanEmail == PRIMARY_ADMIN_EMAIL.lowercase()

        val newUser = AppUser(
            email = cleanEmail,
            displayName = displayName.ifBlank { cleanEmail.substringBefore("@") },
            plan = plan,
            expiryDate = if (isPrimaryAdmin) "Permanent (No Expiry)" else expiryStr,
            expiryTimestamp = if (isPrimaryAdmin) Long.MAX_VALUE else expiryTs,
            role = if (isPrimaryAdmin) "Admin" else "User",
            joinedDate = formatDate(currentTimeMillis()),
            status = "Active"
        )
        currentList.add(0, newUser)
        saveUsers(prefs, currentList)
        _usersFlow.value = currentList
        return true
    }

    /**
     * Toggles administrative privileges for a user.
     * Note: Primary admin ramesh.katru01@gmail.com cannot be downgraded.
     */
    fun toggleAdminRole(context: Context, email: String) {
        if (email.equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true)) return

        val prefs = getSharedPreferences(PREFS_NAME)
        val currentList = _usersFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.email.equals(email, ignoreCase = true) }
        if (index >= 0) {
            val user = currentList[index]
            val newRole = if (user.role.equals("Admin", ignoreCase = true)) "User" else "Admin"
            currentList[index] = user.copy(role = newRole)
            saveUsers(prefs, currentList)
            _usersFlow.value = currentList
        }
    }

    /**
     * Removes a user from the directory.
     */
    fun deleteUser(context: Context, email: String) {
        if (email.equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true)) return
        val prefs = getSharedPreferences(PREFS_NAME)
        val currentList = _usersFlow.value.filterNot { it.email.equals(email, ignoreCase = true) }
        saveUsers(prefs, currentList)
        _usersFlow.value = currentList
    }

    private fun createPrimaryAdmin(): AppUser {
        return AppUser(
            email = PRIMARY_ADMIN_EMAIL,
            displayName = "Ramesh Kartru",
            plan = "Gold (Lifetime Admin)",
            expiryDate = "Permanent (No Expiry)",
            expiryTimestamp = Long.MAX_VALUE,
            role = "Admin",
            joinedDate = "01 Jan 2026",
            status = "Active"
        )
    }

    private fun createDefaultUsers(): List<AppUser> {
        val now = currentTimeMillis()
        val cal1 = addToDate(currentTimeMillis(), years = 1)
        val cal2 = addToDate(currentTimeMillis(), months = 2)
        val cal3 = addToDate(currentTimeMillis(), months = 6)
        val cal4 = addToDate(currentTimeMillis(), days = 7)
        val calExpired = addToDate(currentTimeMillis(), days = -15)

        return listOf(
            createPrimaryAdmin(),
            AppUser(
                email = "Neeraja.katru@gmail.com",
                displayName = "Neeraja Katru",
                plan = "Gold (Annual)",
                expiryDate = formatDate(cal1),
                expiryTimestamp = cal1,
                role = "User",
                joinedDate = "10 Jan 2026",
                status = "Active"
            ),
            AppUser(
                email = "ananya.sharma@gmail.com",
                displayName = "Ananya Sharma",
                plan = "Silver (Annual)",
                expiryDate = formatDate(cal1),
                expiryTimestamp = cal1,
                role = "User",
                joinedDate = "15 Aug 2026",
                status = "Active"
            ),
            AppUser(
                email = "rahul.kamat@gmail.com",
                displayName = "Rahul Kamat",
                plan = "Silver (Monthly)",
                expiryDate = formatDate(cal2),
                expiryTimestamp = cal2,
                role = "User",
                joinedDate = "20 Apr 2026",
                status = "Active"
            ),
            AppUser(
                email = "priya.kulkarni@gmail.com",
                displayName = "Priya Kulkarni",
                plan = "Free Explorer",
                expiryDate = "Unlimited Free",
                expiryTimestamp = 0L,
                role = "User",
                joinedDate = "05 Mar 2026",
                status = "Active"
            ),
            AppUser(
                email = "darshan.gowda@gmail.com",
                displayName = "Darshan Gowda",
                plan = "Gold (Monthly)",
                expiryDate = formatDate(cal4),
                expiryTimestamp = cal4,
                role = "User",
                joinedDate = "12 Sep 2026",
                status = "Expiring Soon"
            ),
            AppUser(
                email = "kavya.nair@gmail.com",
                displayName = "Kavya Nair",
                plan = "Gold (Monthly)",
                expiryDate = formatDate(calExpired),
                expiryTimestamp = calExpired,
                role = "User",
                joinedDate = "01 Sep 2025",
                status = "Expired"
            ),
            AppUser(
                email = "siddharth.m@gmail.com",
                displayName = "Siddharth M",
                plan = "Free Explorer",
                expiryDate = "Unlimited Free",
                expiryTimestamp = 0L,
                role = "User",
                joinedDate = "14 Feb 2026",
                status = "Active"
            )
        )
    }

    private fun saveUsers(prefs: SharedPreferences, users: List<AppUser>) {
        val array = buildJsonArray {
            users.forEach { user ->
                add(buildJsonObject {
                    put("email", user.email)
                    put("displayName", user.displayName)
                    put("plan", user.plan)
                    put("expiryDate", user.expiryDate)
                    put("expiryTimestamp", user.expiryTimestamp)
                    put("role", user.role)
                    put("joinedDate", user.joinedDate)
                    put("status", user.status)
                })
            }
        }
        prefs.edit().putString(KEY_USERS_JSON, array.toString()).apply()
    }

    private fun JsonObject.optString(key: String, fallback: String = ""): String =
        (this[key] as? JsonPrimitive)?.contentOrNull ?: fallback

    private fun JsonObject.optLong(key: String, fallback: Long = 0L): Long =
        (this[key] as? JsonPrimitive)?.let { it.longOrNull ?: it.contentOrNull?.toLongOrNull() } ?: fallback

    private fun parseUsersJson(jsonStr: String): List<AppUser> {
        val list = mutableListOf<AppUser>()
        try {
            val array = Json.parseToJsonElement(jsonStr).jsonArray
            for (element in array) {
                val obj = element as? JsonObject ?: continue
                list.add(
                    AppUser(
                        email = obj.optString("email"),
                        displayName = obj.optString("displayName"),
                        plan = obj.optString("plan", "Free Explorer"),
                        expiryDate = obj.optString("expiryDate", "Unlimited Free"),
                        expiryTimestamp = obj.optLong("expiryTimestamp", 0L),
                        role = obj.optString("role", "User"),
                        joinedDate = obj.optString("joinedDate", "15 Jan 2026"),
                        status = obj.optString("status", "Active")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("UserManager", "Error parsing users JSON", e)
        }
        return list
    }
}
