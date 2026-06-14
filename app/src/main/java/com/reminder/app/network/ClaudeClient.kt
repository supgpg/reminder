package com.reminder.app.network

import com.reminder.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ClaudeClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-api-key", BuildConfig.CLAUDE_API_KEY)
                .addHeader("anthropic-version", ClaudeApiService.ANTHROPIC_VERSION)
                .addHeader("content-type", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    val apiService: ClaudeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ClaudeApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ClaudeApiService::class.java)
    }
}
