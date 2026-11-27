package com.example.ui.data

/** App-wide switches. */
object FeatureFlags {
    /**
     * Keeps the OLD scattered locks (guest / "Gold" checks in older screens) switched off.
     * Locks are now decided in one place: ui/data/Access.kt.
     * (Previously: when true, every module, letter, level, video and game is unlocked for everyone)
     * (guests and free users included) and no "🔒 Locked" messages appear.
     * Set back to false to restore the normal guest / Free / Gold locking.
     */
    const val UNLOCK_ALL = true

    /**
     * Classroom homework (teachers make a class, parents join with a code, children do homework,
     * teachers see progress). All of it lives in ui/classroom/.
     * Set to false to switch it off completely (nothing shows and the code is left out of the app).
     * To delete it for good, see ui/classroom/README.md.
     */
    const val CLASSROOM = true
}
