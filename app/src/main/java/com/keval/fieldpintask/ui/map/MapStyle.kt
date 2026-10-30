package com.keval.fieldpintask.ui.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.keval.fieldpintask.domain.gis.LatLng
import com.keval.fieldpintask.domain.location.DeviceLocation
import com.keval.fieldpintask.domain.model.Site
import kotlin.math.cos
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.maps.Style
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

/** Builds the base style and owns the GeoJSON sources and layers drawn on top of it. */
internal object MapStyle {
    private const val SITES = "sites"
    private const val DRAFT = "draft"
    private const val ME = "me"
    private const val ME_ACCURACY_LAYER = "me-accuracy"
    private const val TILE_SIZE_PX = 512.0
    private const val EARTH_CIRCUMFERENCE_M = 40_075_016.686
    private const val SELECTED_OUTLINE = "#111111"
    private const val DRAFT_COLOR = "#1565C0"
    private const val USER_COLOR = "#1A73E8"

    fun json(tileUrlTemplate: String): String = """
        {"version":8,
         "sources":{"base":{"type":"raster","tiles":["$tileUrlTemplate"],"tileSize":256,"maxzoom":19,
                    "attribution":"© OpenStreetMap contributors"}},
         "layers":[{"id":"base","type":"raster","source":"base"}]}
    """.trimIndent()

    fun install(style: Style) {
        style.addSource(GeoJsonSource(SITES))
        style.addSource(GeoJsonSource(DRAFT))
        style.addSource(GeoJsonSource(ME))

        style.addLayer(
            FillLayer("sites-fill", SITES).withProperties(
                fillColor(Expression.toColor(Expression.get("color"))),
                fillOpacity(0.45f),
            ),
        )
        style.addLayer(
            LineLayer("sites-line", SITES).withProperties(
                lineColor(Expression.toColor(Expression.get("color"))),
                lineWidth(1.5f),
            ),
        )
        style.addLayer(
            LineLayer("sites-selected", SITES)
                .withFilter(Expression.eq(Expression.get("selected"), Expression.literal(true)))
                .withProperties(lineColor(SELECTED_OUTLINE), lineWidth(4f)),
        )

        style.addLayer(
            FillLayer("draft-fill", DRAFT)
                .withFilter(Expression.eq(Expression.geometryType(), Expression.literal("Polygon")))
                .withProperties(fillColor(Expression.toColor(Expression.get("color"))), fillOpacity(0.3f)),
        )
        style.addLayer(
            LineLayer("draft-line", DRAFT).withProperties(
                lineColor(Expression.toColor(Expression.get("color"))),
                lineWidth(3f),
            ),
        )
        style.addLayer(
            CircleLayer("draft-points", DRAFT)
                .withFilter(Expression.eq(Expression.geometryType(), Expression.literal("Point")))
                .withProperties(
                    circleRadius(6f),
                    circleColor(Expression.toColor(Expression.get("color"))),
                    circleStrokeColor("#FFFFFF"),
                    circleStrokeWidth(2f),
                ),
        )

        style.addLayer(
            CircleLayer(ME_ACCURACY_LAYER, ME).withProperties(
                circleColor(USER_COLOR),
                circleOpacity(0.15f),
                circleRadius(0f),
            ),
        )
        style.addLayer(
            CircleLayer("me-dot", ME).withProperties(
                circleRadius(7f),
                circleColor(USER_COLOR),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(2.5f),
            ),
        )
    }

    fun updateSites(style: Style, sites: List<Site>, selectedId: String?, statusColor: (Site) -> Color) {
        val features = sites.map { site ->
            Feature.fromGeometry(site.boundary.toPolygon()).apply {
                addStringProperty("id", site.id)
                addStringProperty("color", statusColor(site).toHex())
                addBooleanProperty("selected", site.id == selectedId)
            }
        }
        style.getSourceAs<GeoJsonSource>(SITES)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    fun updateDraft(style: Style, points: List<LatLng>, valid: Boolean) {
        val color = if (valid) DRAFT_COLOR else "#D64545"
        val features = buildList {
            if (points.size >= 3) add(Feature.fromGeometry(points.toPolygon()))
            else if (points.size == 2) add(Feature.fromGeometry(LineString.fromLngLats(points.map { it.toPoint() })))
            points.forEach { add(Feature.fromGeometry(it.toPoint())) }
        }.onEach { it.addStringProperty("color", color) }
        style.getSourceAs<GeoJsonSource>(DRAFT)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    fun updateUser(style: Style, location: DeviceLocation?) {
        val source = style.getSourceAs<GeoJsonSource>(ME) ?: return
        if (location == null) {
            source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            return
        }
        source.setGeoJson(Feature.fromGeometry(location.latLng.toPoint()))
        // Radius in pixels is accuracy / metres-per-pixel, which halves every zoom level.
        val metersPerPixelAtZoom0 = EARTH_CIRCUMFERENCE_M * cos(Math.toRadians(location.latLng.latitude)) / TILE_SIZE_PX
        val radiusAtZoom0 = (location.accuracyM / metersPerPixelAtZoom0).toFloat()
        style.getLayerAs<CircleLayer>(ME_ACCURACY_LAYER)?.setProperties(
            circleRadius(
                Expression.interpolate(
                    Expression.exponential(2),
                    Expression.zoom(),
                    Expression.stop(0, radiusAtZoom0),
                    Expression.stop(MAX_ZOOM, radiusAtZoom0 * (1L shl MAX_ZOOM)),
                ),
            ),
        )
    }

    private const val MAX_ZOOM = 22

    private fun LatLng.toPoint() = Point.fromLngLat(longitude, latitude)

    // GeoJSON rings must be closed, so the first vertex is repeated at the end.
    private fun List<LatLng>.toPolygon() = Polygon.fromLngLats(listOf((this + first()).map { it.toPoint() }))

    private fun Color.toHex() = String.format("#%06X", 0xFFFFFF and toArgb())
}
