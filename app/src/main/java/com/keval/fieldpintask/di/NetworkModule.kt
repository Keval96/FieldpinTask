package com.keval.fieldpintask.di

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.keval.fieldpintask.BuildConfig
import com.keval.fieldpintask.data.remote.AuthInterceptor
import com.keval.fieldpintask.data.remote.FakeServerInterceptor
import com.keval.fieldpintask.data.remote.FieldPinApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // The fake interceptor goes last and short-circuits the chain, so no real request is made.
    @Provides
    @Singleton
    fun okHttpClient(auth: AuthInterceptor, fake: FakeServerInterceptor): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(auth).addInterceptor(fake).build()

    @Provides
    @Singleton
    fun api(client: OkHttpClient, json: Json): FieldPinApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(FieldPinApi::class.java)
}
