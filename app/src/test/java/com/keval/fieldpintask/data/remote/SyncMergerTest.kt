package com.keval.fieldpintask.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergerTest {

    private fun site(version: Int, updatedAt: Long, name: String = "Lake") = SiteDto(
        id = "s1", name = name, category = "Lake", boundary = emptyList(), areaSqM = 0.0,
        perimeterM = 0.0, status = "ACTIVE", lastInspectionAt = null, version = version, updatedAt = updatedAt,
    )

    @Test fun `new site is stored at version 1`() {
        val store = mutableMapOf<String, SiteDto>()
        val conflicts = SyncMerger.merge(store, listOf(site(version = 0, updatedAt = 10)))
        assertTrue(conflicts.isEmpty())
        assertEquals(1, store.getValue("s1").version)
    }

    @Test fun `matching base version applies and bumps the version`() {
        val store = mutableMapOf("s1" to site(version = 3, updatedAt = 10))
        val conflicts = SyncMerger.merge(store, listOf(site(version = 3, updatedAt = 20, name = "Edited")))
        assertTrue(conflicts.isEmpty())
        assertEquals(4, store.getValue("s1").version)
        assertEquals("Edited", store.getValue("s1").name)
    }

    @Test fun `stale edit with a newer timestamp wins but is flagged`() {
        val store = mutableMapOf("s1" to site(version = 5, updatedAt = 10, name = "Phone B"))
        val conflicts = SyncMerger.merge(store, listOf(site(version = 3, updatedAt = 20, name = "Phone A")))
        assertEquals(listOf("s1"), conflicts)
        assertEquals("Phone A", store.getValue("s1").name)
        assertEquals(6, store.getValue("s1").version)
    }

    @Test fun `stale edit with an older timestamp loses and is flagged`() {
        val store = mutableMapOf("s1" to site(version = 5, updatedAt = 30, name = "Phone B"))
        val conflicts = SyncMerger.merge(store, listOf(site(version = 3, updatedAt = 20, name = "Phone A")))
        assertEquals(listOf("s1"), conflicts)
        assertEquals("Phone B", store.getValue("s1").name)
        assertEquals(5, store.getValue("s1").version)
    }
}
