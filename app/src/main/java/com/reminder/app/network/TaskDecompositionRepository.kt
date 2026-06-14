package com.reminder.app.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SubtaskListResponse(val subtasks: List<String> = emptyList())

/**
 * 막연한 목표를 Claude로 분석해 실행 가능한 하위 작업 목록으로 쪼갠다.
 */
class TaskDecompositionRepository {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun decompose(goal: String): List<String> {
        val system = "너는 학생의 막연한 목표를 실행 가능한 하위 작업으로 쪼개주는 플래너야. " +
            "사용자가 입력한 목표를 분석해서, 실제로 바로 실행할 수 있는 구체적인 하위 작업을 " +
            "3~6개, 한국어로, 각 항목은 20자 이내 명령형으로 만들어줘. " +
            "반드시 아래 JSON 형식으로만 답하고 다른 설명은 절대 추가하지마: " +
            "{\"subtasks\": [\"작업1\", \"작업2\"]}"

        val text = ClaudeTextClient.ask(system = system, userMessage = "목표: $goal")
            ?: return fallback(goal)

        val jsonText = JsonExtractor.extractJsonObject(text) ?: return fallback(goal)
        return try {
            val response = json.decodeFromString<SubtaskListResponse>(jsonText)
            response.subtasks.filter { it.isNotBlank() }.ifEmpty { fallback(goal) }
        } catch (e: Exception) {
            fallback(goal)
        }
    }

    private fun fallback(goal: String): List<String> = listOf(goal)
}
