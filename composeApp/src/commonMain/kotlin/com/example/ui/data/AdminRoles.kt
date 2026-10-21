package com.example.ui.data

import com.example.platform.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Admin rights, kept online in Firestore at admins/{email}:
 *  - "owner": UserManager.PRIMARY_ADMIN_EMAIL. Can give and take away admin rights.
 *  - "admin": can see and manage subscriptions.
 * The owner's own record is created automatically the first time the owner signs in.
 */
object AdminRoles {
    data class AdminEntry(val email: String, val role: String, val addedBy: String, val addedAt: Long)

    private val _cloudAdmins = MutableStateFlow<Set<String>>(emptySet())
    /** E-mails known to be admins online (used by UserManager.isAdmin). */
    val cloudAdmins: StateFlow<Set<String>> = _cloudAdmins.asStateFlow()

    private val _myRole = MutableStateFlow<String?>(null)
    val myRole: StateFlow<String?> = _myRole.asStateFlow()

    private fun clean(email: String) = email.trim().lowercase()
    private fun myEmail(): String? = PlatformAuth.currentUser()?.email?.let { clean(it) }

    fun isOwner(): Boolean = _myRole.value == "owner"

    /** Looks up this account's role (and creates the owner record when needed). */
    suspend fun load(): String? {
        val email = myEmail() ?: run { _myRole.value = null; return null }
        var role = try {
            CloudStore.get("admins/$email")?.str("role")
        } catch (e: Exception) {
            null
        }
        if (role == null && email == clean(UserManager.PRIMARY_ADMIN_EMAIL)) {
            try {
                CloudStore.update(
                    "admins/$email",
                    mapOf("role" to "owner", "addedBy" to email, "addedAt" to currentTimeMillis())
                )
                role = "owner"
            } catch (e: Exception) {
                Log.w("AdminRoles", "Couldn't create the owner record", e)
            }
        }
        _myRole.value = role
        if (role != null) _cloudAdmins.value = _cloudAdmins.value + email
        if (role != null) runCatching { list() }
        return role
    }

    /** Everyone with admin rights (admins only). */
    suspend fun list(): List<AdminEntry> {
        val entries = CloudStore.list("admins").map { (id, m) ->
            AdminEntry(id, m.str("role") ?: "admin", m.str("addedBy") ?: "", m.long("addedAt"))
        }.sortedWith(compareBy<AdminEntry> { if (it.role == "owner") 0 else 1 }.thenBy { it.email })
        _cloudAdmins.value = entries.map { it.email }.toSet()
        return entries
    }

    /** Owner only: give admin rights to [email]. */
    suspend fun grant(email: String) {
        val e = clean(email)
        require(e.contains("@") && e.contains(".")) { "Please enter a full e-mail address." }
        if (!isOwner()) throw IllegalStateException("Only the owner can give admin rights.")
        CloudStore.update("admins/$e", mapOf("role" to "admin", "addedBy" to (myEmail() ?: ""), "addedAt" to currentTimeMillis()))
        _cloudAdmins.value = _cloudAdmins.value + e
    }

    /** Owner only: take admin rights away (the owner can't remove themselves). */
    suspend fun revoke(email: String) {
        val e = clean(email)
        if (!isOwner()) throw IllegalStateException("Only the owner can remove admin rights.")
        if (e == clean(UserManager.PRIMARY_ADMIN_EMAIL)) throw IllegalStateException("The owner always keeps admin rights.")
        CloudStore.delete("admins/$e")
        _cloudAdmins.value = _cloudAdmins.value - e
    }
}
