package com.keval.fieldpintask.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface FieldPinApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("sync")
    suspend fun sync(@Body request: SyncRequest): SyncResponse
}
