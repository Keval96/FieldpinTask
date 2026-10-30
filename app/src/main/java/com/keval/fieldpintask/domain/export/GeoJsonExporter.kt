package com.keval.fieldpintask.domain.export

import com.keval.fieldpintask.domain.gis.LatLng
import com.keval.fieldpintask.domain.model.Site
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Builds an RFC 7946 FeatureCollection, one Polygon feature per site. */
object GeoJsonExporter {
    private val pretty = Json { prettyPrint = true }

    fun export(sites: List<Site>): String {
        val collection = buildJsonObject {
            put("type", "FeatureCollection")
            put("features", buildJsonArray { sites.forEach { add(feature(it)) } })
        }
        return pretty.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), collection)
    }

    private fun feature(site: Site) = buildJsonObject {
        put("type", "Feature")
        put("id", site.id)
        put("geometry", buildJsonObject {
            put("type", "Polygon")
            put("coordinates", buildJsonArray { add(ring(site.boundary)) })
        })
        put("properties", buildJsonObject {
            put("name", site.name)
            put("category", site.category)
            put("status", site.status.name)
            put("areaSqM", site.areaSqM)
            put("areaAcres", site.areaAcres)
            put("perimeterM", site.perimeterM)
            put("lastInspectionAt", site.lastInspectionAt?.let { JsonPrimitive(Instant.ofEpochMilli(it).toString()) } ?: JsonNull)
            put("version", site.version)
        })
    }

    /** GeoJSON wants [lng, lat], a closed ring, and a counter-clockwise exterior ring. */
    private fun ring(boundary: List<LatLng>): JsonArray {
        val ordered = if (signedArea(boundary) < 0) boundary.reversed() else boundary
        val closed = if (ordered.first() == ordered.last()) ordered else ordered + ordered.first()
        return buildJsonArray {
            closed.forEach { add(buildJsonArray { add(JsonPrimitive(it.longitude)); add(JsonPrimitive(it.latitude)) }) }
        }
    }

    private fun signedArea(points: List<LatLng>): Double {
        var sum = 0.0
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            sum += a.longitude * b.latitude - b.longitude * a.latitude
        }
        return sum / 2
    }
}
