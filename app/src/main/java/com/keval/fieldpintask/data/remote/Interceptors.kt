package com.keval.fieldpintask.data.remote

import com.keval.fieldpintask.data.session.SessionStore
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor @Inject constructor(private val sessionStore: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { sessionStore.token() }
        val request = chain.request().newBuilder()
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
        return chain.proceed(request)
    }
}

/** Answers every request locally; swap this out of the OkHttp client to talk to a real server. */
class FakeServerInterceptor @Inject constructor(private val server: FakeServer) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        runBlocking { delay(LATENCY_MS) }
        return server.handle(chain.request())
    }

    private companion object {
        const val LATENCY_MS = 500L
    }
}
