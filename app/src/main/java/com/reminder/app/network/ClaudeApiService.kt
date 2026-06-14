package com.reminder.app.network

import retrofit2.http.Body
import retrofit2.http.POST

interface ClaudeApiService {

    @POST("v1/messages")
    suspend fun createMessage(@Body request: ClaudeMessageRequest): ClaudeMessageResponse

    companion object {
        const val MODEL = "claude-haiku-4-5"
        const val BASE_URL = "https://api.anthropic.com/"
        const val ANTHROPIC_VERSION = "2023-06-01"
    }
}
