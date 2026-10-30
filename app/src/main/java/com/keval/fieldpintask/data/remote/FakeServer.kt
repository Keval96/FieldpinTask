package com.keval.fieldpintask.data.remote

import com.keval.fieldpintask.BuildConfig
import com.keval.fieldpintask.data.seed.SeedSites
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

/** In-process stand-in for the backend, served through [FakeServerInterceptor]. State lives in memory. */
@Singleton
class FakeServer @Inject constructor(private val json: Json) {
    private val sites = LinkedHashMap<String, SiteDto>()
    private val inspections = LinkedHashMap<String, InspectionDto>()
    private val tokens = mutableSetOf<String>()

    init {
        SeedSites.create(System.currentTimeMillis()).forEach { sites[it.id] = it.toDto() }
    }

    @Synchronized
    fun handle(request: Request): Response {
        val path = request.url.encodedPath.trimStart('/')
        return when {
            request.method == "POST" && path.endsWith("auth/login") -> login(request)
            request.method == "POST" && path.endsWith("sync") -> sync(request)
            else -> respond(request, 404, """{"error":"not found"}""")
        }
    }

    private fun login(request: Request): Response {
        val body = json.decodeFromString<LoginRequest>(request.bodyText())
        if (body.email != BuildConfig.DEMO_EMAIL || body.password != BuildConfig.DEMO_PASSWORD) {
            return respond(request, 401, """{"error":"invalid credentials"}""")
        }
        val token = UUID.randomUUID().toString().also(tokens::add)
        val payload = LoginResponse(token, body.email, DISPLAY_NAME)
        return respond(request, 200, json.encodeToString(LoginResponse.serializer(), payload))
    }

    private fun sync(request: Request): Response {
        val token = request.header("Authorization")?.removePrefix("Bearer ")
        if (token !in tokens) return respond(request, 401, """{"error":"unauthorized"}""")
        val body = json.decodeFromString<SyncRequest>(request.bodyText())
        val conflicts = SyncMerger.merge(sites, body.sites)
        body.inspections.forEach { inspections[it.id] = it }
        val payload = SyncResponse(sites.values.toList(), conflicts, body.inspections.map { it.id })
        return respond(request, 200, json.encodeToString(SyncResponse.serializer(), payload))
    }

    private fun Request.bodyText(): String = Buffer().also { body?.writeTo(it) }.readUtf8()

    private fun respond(request: Request, code: Int, body: String): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(if (code == 200) "OK" else "Error")
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()

    private companion object {
        const val DISPLAY_NAME = "Field Officer"
    }
}
