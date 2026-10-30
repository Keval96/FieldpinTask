package com.keval.fieldpintask.data.seed

import com.keval.fieldpintask.domain.gis.GeoMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedSitesTest {
    private val sites = SeedSites.create(0L)

    @Test fun `has at least 20 sites with unique ids`() {
        assertTrue(sites.size >= 20)
        assertEquals(sites.size, sites.map { it.id }.toSet().size)
    }

    @Test fun `every site has a closed usable boundary`() {
        sites.forEach { site ->
            assertTrue(site.name, site.boundary.size >= 3)
            assertTrue(site.name, site.areaSqM > 0)
            assertEquals(GeoMath.areaSquareMeters(site.boundary), site.areaSqM, 1e-6)
        }
    }

    @Test fun `outlines are deterministic`() {
        assertEquals(sites.map { it.boundary }, SeedSites.create(1L).map { it.boundary })
    }
}
