package com.keval.fieldpintask.domain.export

import com.keval.fieldpintask.domain.gis.LatLng
import com.keval.fieldpintask.domain.model.Site
import com.keval.fieldpintask.domain.model.SiteStatus
import com.keval.fieldpintask.domain.model.SyncState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoJsonExporterTest {
    private fun site(boundary: List<LatLng>, lastInspectionAt: Long? = null) = Site(
        id = "s1", name = "Law Garden", category = "Park", boundary = boundary,
        areaSqM = 1000.0, perimeterM = 130.0, status = SiteStatus.ACTIVE,
        lastInspectionAt = lastInspectionAt, version = 2, updatedAt = 0, syncState = SyncState.SYNCED,
    )

    private val ccw = listOf(LatLng(23.0, 72.0), LatLng(23.0, 72.001), LatLng(23.001, 72.001), LatLng(23.001, 72.0))

    private fun firstRing(json: String): JsonArray {
        val feature = Json.parseToJsonElement(json).jsonObject["features"]!!.jsonArray[0].jsonObject
        return feature["geometry"]!!.jsonObject["coordinates"]!!.jsonArray[0].jsonArray
    }

    @Test fun `output is a feature collection with one polygon per site`() {
        val root = Json.parseToJsonElement(GeoJsonExporter.export(listOf(site(ccw), site(ccw)))).jsonObject
        assertEquals("FeatureCollection", root["type"]!!.jsonPrimitive.content)
        assertEquals(2, root["features"]!!.jsonArray.size)
    }

    @Test fun `coordinates are lng then lat`() {
        val first = firstRing(GeoJsonExporter.export(listOf(site(ccw))))[0].jsonArray
        assertEquals(72.0, first[0].jsonPrimitive.double, 0.0)
        assertEquals(23.0, first[1].jsonPrimitive.double, 0.0)
    }

    @Test fun `ring is closed`() {
        val ring = firstRing(GeoJsonExporter.export(listOf(site(ccw))))
        assertEquals(ccw.size + 1, ring.size)
        assertEquals(ring.first(), ring.last())
    }

    @Test fun `clockwise input is written counter-clockwise`() {
        val ring = firstRing(GeoJsonExporter.export(listOf(site(ccw.reversed()))))
        var sum = 0.0
        for (i in 0 until ring.size - 1) {
            val a = ring[i].jsonArray
            val b = ring[i + 1].jsonArray
            sum += a[0].jsonPrimitive.double * b[1].jsonPrimitive.double - b[0].jsonPrimitive.double * a[1].jsonPrimitive.double
        }
        assertTrue(sum > 0)
    }

    @Test fun `properties carry name status and a null last inspection`() {
        val props = Json.parseToJsonElement(GeoJsonExporter.export(listOf(site(ccw)))).jsonObject["features"]!!
            .jsonArray[0].jsonObject["properties"]!!.jsonObject
        assertEquals("Law Garden", props["name"]!!.jsonPrimitive.content)
        assertEquals("ACTIVE", props["status"]!!.jsonPrimitive.content)
        assertTrue(props["lastInspectionAt"]!!.toString() == "null")
    }
}
