package com.keval.fieldpintask.data.seed

import com.keval.fieldpintask.domain.gis.GeoMath
import com.keval.fieldpintask.domain.gis.LatLng
import com.keval.fieldpintask.domain.model.Site
import com.keval.fieldpintask.domain.model.SiteStatus
import com.keval.fieldpintask.domain.model.SyncState
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

/** Sample sites around landmarks in Ahmedabad. Outlines are approximated, not surveyed. */
object SeedSites {
    private const val METERS_PER_DEGREE = 111_195.0
    private const val DAY_MS = 86_400_000L
    private const val VERTEX_COUNT = 7

    private data class Spec(
        val name: String,
        val category: String,
        val lat: Double,
        val lng: Double,
        val widthM: Double,
        val heightM: Double,
        val status: SiteStatus,
        val inspectedDaysAgo: Int?,
    )

    private val specs = listOf(
        Spec("Kankaria Lake", "Lake", 23.0063, 72.6014, 700.0, 500.0, SiteStatus.ACTIVE, 3),
        Spec("Law Garden", "Park", 23.0270, 72.5560, 180.0, 160.0, SiteStatus.ACTIVE, 12),
        Spec("Riverfront Flower Park", "Park", 23.0508, 72.5700, 260.0, 150.0, SiteStatus.ACTIVE, 7),
        Spec("Vastrapur Lake", "Lake", 23.0378, 72.5272, 300.0, 220.0, SiteStatus.NEEDS_REPAIR, 20),
        Spec("Chandola Lake", "Lake", 22.9967, 72.6228, 550.0, 350.0, SiteStatus.ACTIVE, null),
        Spec("Narendra Modi Stadium", "Stadium", 23.0917, 72.5974, 330.0, 300.0, SiteStatus.ACTIVE, 30),
        Spec("Science City Solar Park", "Solar installation", 23.0733, 72.4975, 260.0, 200.0, SiteStatus.ACTIVE, 5),
        Spec("Gujarat University Ground", "Ground", 23.0348, 72.5460, 240.0, 180.0, SiteStatus.ACTIVE, 45),
        Spec("Sabarmati Ashram Lawn", "Heritage", 23.0607, 72.5800, 200.0, 130.0, SiteStatus.ACTIVE, 9),
        Spec("Hutheesing Temple Grounds", "Heritage", 23.0436, 72.5841, 120.0, 90.0, SiteStatus.NEEDS_REPAIR, 16),
        Spec("Bhadra Fort Plaza", "Heritage", 23.0240, 72.5811, 220.0, 160.0, SiteStatus.CLOSED, 60),
        Spec("Sarkhej Roza Complex", "Heritage", 22.9840, 72.5090, 300.0, 250.0, SiteStatus.ACTIVE, 2),
        Spec("Thaltej Water Tank", "Water tank", 23.0560, 72.5050, 70.0, 70.0, SiteStatus.ACTIVE, 14),
        Spec("Naroda Water Works", "Water tank", 23.0690, 72.6670, 140.0, 110.0, SiteStatus.NEEDS_REPAIR, 8),
        Spec("Vatva GIDC Solar Plant", "Solar installation", 22.9700, 72.6500, 400.0, 300.0, SiteStatus.ACTIVE, null),
        Spec("Sola Reservoir", "Water tank", 23.0750, 72.5300, 110.0, 90.0, SiteStatus.CLOSED, 90),
        Spec("Bopal Lake Park", "Park", 23.0300, 72.4700, 260.0, 190.0, SiteStatus.ACTIVE, 11),
        Spec("Memnagar Garden", "Park", 23.0480, 72.5400, 150.0, 120.0, SiteStatus.ACTIVE, 25),
        Spec("Gota Solar Field", "Solar installation", 23.1000, 72.5400, 300.0, 220.0, SiteStatus.NEEDS_REPAIR, 4),
        Spec("Maninagar Water Tank", "Water tank", 22.9950, 72.6050, 80.0, 80.0, SiteStatus.ACTIVE, 33),
        Spec("Nehru Bridge Riverwalk", "Park", 23.0270, 72.5780, 300.0, 70.0, SiteStatus.ACTIVE, 6),
        Spec("Naranpura Sports Complex", "Ground", 23.0560, 72.5600, 210.0, 170.0, SiteStatus.ACTIVE, 18),
    )

    fun create(now: Long): List<Site> = specs.mapIndexed { index, spec ->
        val boundary = outline(spec)
        Site(
            id = "site-%02d".format(index + 1),
            name = spec.name,
            category = spec.category,
            boundary = boundary,
            areaSqM = GeoMath.areaSquareMeters(boundary),
            perimeterM = GeoMath.perimeterMeters(boundary),
            status = spec.status,
            lastInspectionAt = spec.inspectedDaysAgo?.let { now - it * DAY_MS },
            version = 1,
            updatedAt = now,
            syncState = SyncState.SYNCED,
        )
    }

    // Seeded by name so every install (and the mock server) gets identical outlines.
    private fun outline(spec: Spec): List<LatLng> {
        val random = Random(spec.name.hashCode().toLong())
        val cosLat = cos(Math.toRadians(spec.lat))
        return (0 until VERTEX_COUNT).map { i ->
            val angle = Math.toRadians(i * 360.0 / VERTEX_COUNT + random.nextDouble() * 20 - 10)
            val scale = 0.8 + random.nextDouble() * 0.2
            val dx = cos(angle) * spec.widthM / 2 * scale
            val dy = sin(angle) * spec.heightM / 2 * scale
            LatLng(spec.lat + dy / METERS_PER_DEGREE, spec.lng + dx / (METERS_PER_DEGREE * cosLat))
        }
    }
}
