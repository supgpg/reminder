package com.reminder.app.network

import com.reminder.app.data.PostponeReason
import com.reminder.app.data.Reminder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ConsultantSuggestion(val newTitle: String, val advice: String)

/**
 * 자주 미루는 할 일에 대해 Claude가 이유를 묻고, 더 작은 대체 작업을 제안한다.
 */
class PostponeConsultantRepository {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getDiagnosisQuestion(reminder: Reminder): String {
        val system = "너는 학생이 할 일을 미루는 이유를 다정하게 물어보는 상담가야. " +
            "학생이 '${reminder.title}'이라는 할 일을 ${reminder.postponeCount}번 미뤘어. " +
            "짧고 다정한 말투로, 왜 미루고 있는지 묻는 질문을 1~2문장으로 만들어줘. " +
            "일반 텍스트로만 답하고 다른 설명은 추가하지마."

        return ClaudeTextClient.ask(system = system, userMessage = "할 일: ${reminder.title}")
            ?: FALLBACK_QUESTION
    }

    suspend fun getSuggestion(reminder: Reminder, reason: PostponeReason): ConsultantSuggestion {
        val system = "너는 학생을 돕는 학습 코치야. 학생이 '${reminder.title}'을 ${reason.label} " +
            "미루고 있어. 이 학생이 지금 당장 시작할 수 있을 만큼 훨씬 작은 대체 작업을 " +
            "한국어로 제안하고, 짧은 응원 메시지를 덧붙여줘. " +
            "반드시 아래 JSON 형식으로만 답하고 다른 설명은 추가하지마: " +
            "{\"newTitle\": \"대체 작업\", \"advice\": \"응원 메시지\"}"

        val text = ClaudeTextClient.ask(system = system, userMessage = "할 일: ${reminder.title}")
            ?: return fallback(reminder)

        val jsonText = JsonExtractor.extractJsonObject(text) ?: return fallback(reminder)
        return try {
            json.decodeFromString<ConsultantSuggestion>(jsonText)
        } catch (e: Exception) {
            fallback(reminder)
        }
    }

    private fun fallback(reminder: Reminder) = ConsultantSuggestion(
        newTitle = "${reminder.title} - 한 단계만",
        advice = "작은 한 걸음부터 시작해봐요!",
    )

    companion object {
        const val FALLBACK_QUESTION = "혹시 너무 어려워서 손이 안 가나요, 아니면 그냥 귀찮아서인가요?"
    }
}
