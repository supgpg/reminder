package com.reminder.app.network

import com.reminder.app.BuildConfig

/**
 * Claude에게 system/user 프롬프트를 보내고 첫 텍스트 블록을 반환하는 공용 헬퍼.
 * API 키가 없거나 호출이 실패하면 null을 반환한다 (호출부에서 폴백 처리).
 */
object ClaudeTextClient {

    suspend fun ask(
        system: String,
        userMessage: String,
        maxTokens: Int = 300,
        apiService: ClaudeApiService = ClaudeClient.apiService,
    ): String? {
        if (BuildConfig.CLAUDE_API_KEY.isBlank()) return null

        return try {
            val request = ClaudeMessageRequest(
                model = ClaudeApiService.MODEL,
                maxTokens = maxTokens,
                system = system,
                messages = listOf(ClaudeMessage(role = "user", content = userMessage)),
            )
            val response = apiService.createMessage(request)
            response.content.firstOrNull { it.type == "text" }?.text?.trim()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
