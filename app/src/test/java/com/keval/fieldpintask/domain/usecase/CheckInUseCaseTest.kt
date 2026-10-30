package com.keval.fieldpintask.domain.usecase

import com.keval.fieldpintask.data.seed.SeedSites
import com.keval.fieldpintask.domain.gis.GeoMath
import com.keval.fieldpintask.domain.gis.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInUseCaseTest {
    private val site = SeedSites.create(0L).first()
    private val centroid = LatLng(
        site.boundary.map { it.latitude }.average(),
        site.boundary.map { it.longitude }.average(),
    )

    @Test fun `inside the boundary check-in is allowed`() {
        assertEquals(CheckInResult.Allowed, CheckInUseCase()(site, centroid))
    }

    @Test fun `outside the boundary reports the distance`() {
        val away = LatLng(centroid.latitude + 0.05, centroid.longitude)
        val result = CheckInUseCase()(site, away) as CheckInResult.Outside
        assertTrue(result.distanceM > 4_000.0)
        assertTrue(result.distanceM < GeoMath.distanceMeters(away, centroid))
    }
}
