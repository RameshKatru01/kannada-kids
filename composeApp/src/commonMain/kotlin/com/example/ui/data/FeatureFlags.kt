package com.example.ui.data

/** App-wide switches. */
object FeatureFlags {
    /**
     * TEMPORARY: when true, every module, letter, level, video and game is unlocked for everyone
     * (guests and free users included) and no "🔒 Locked" messages appear.
     * Set back to false to restore the normal guest / Free / Gold locking.
     */
    const val UNLOCK_ALL = true
}
