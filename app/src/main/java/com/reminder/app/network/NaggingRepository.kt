package com.reminder.app.network

import com.reminder.app.BuildConfig
import com.reminder.app.data.Reminder
import com.reminder.app.data.SpiceLevel

class NaggingRepository(private val apiService: ClaudeApiService = ClaudeClient.apiService) {

    /**
     * Claude API를 호출해 잔소리/동기부여 문구를 생성한다.
     * API 키가 없거나 호출이 실패하면 [FallbackNagging]의 문구를 반환한다.
     */
    suspend fun generateMessage(reminder: Reminder, spiceLevel: SpiceLevel, usageContext: String? = null): String {
        if (BuildConfig.CLAUDE_API_KEY.isBlank()) {
            return FallbackNagging.pick(reminder, spiceLevel)
        }

        return try {
            val request = ClaudeMessageRequest(
                model = ClaudeApiService.MODEL,
                maxTokens = 150,
                system = NaggingPromptBuilder.buildSystemPrompt(spiceLevel, reminder.postponeCount, usageContext),
                messages = listOf(
                    ClaudeMessage(
                        role = "user",
                        content = "할 일: ${reminder.title}" +
                            if (reminder.description.isNotBlank()) "\n메모: ${reminder.description}" else "",
                    ),
                ),
            )
            val response = apiService.createMessage(request)
            val text = response.content.firstOrNull { it.type == "text" }?.text?.trim()
            if (text.isNullOrBlank()) FallbackNagging.pick(reminder, spiceLevel) else text
        } catch (e: Exception) {
            FallbackNagging.pick(reminder, spiceLevel)
        }
    }
}
