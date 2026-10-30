package com.keval.fieldpintask.data.remote

/**
 * Server-side merge for site edits, kept pure so it is easy to test.
 * A matching base version applies cleanly; a stale one is a conflict won by the newest updatedAt.
 */
object SyncMerger {

    fun merge(store: MutableMap<String, SiteDto>, incoming: List<SiteDto>): List<String> {
        val conflicts = mutableListOf<String>()
        for (change in incoming) {
            val current = store[change.id]
            when {
                current == null -> store[change.id] = change.copy(version = 1)
                change.version == current.version -> store[change.id] = change.copy(version = current.version + 1)
                else -> {
                    conflicts += change.id
                    if (change.updatedAt > current.updatedAt) {
                        store[change.id] = change.copy(version = current.version + 1)
                    }
                }
            }
        }
        return conflicts
    }
}
