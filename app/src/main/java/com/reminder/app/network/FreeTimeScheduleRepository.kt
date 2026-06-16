package com.reminder.app.network

import com.reminder.app.data.Reminder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale

@Serializable
data class ParsedSuggestion(
    val id: Long,
    val time: String,
    val reason: String = "",
)

data class ScheduleSuggestion(
    val reminderId: Long,
    val reminderTitle: String,
    val currentDueAt: Long,
    val suggestedDueAt: Long,
    val reason: String,
)

class FreeTimeScheduleRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val dueAtFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)

    suspend fun suggest(
        pendingReminders: List<Reminder>,
        busyBlocksText: String,
        nowFormatted: String,
    ): List<ScheduleSuggestion> {
        if (pendingReminders.isEmpty()) return emptyList()

        val tasksText = pendingReminders.joinToString("\n") { r ->
            val due = dueAtFormat.format(java.util.Date(r.dueAt))
            "${r.id}. ${r.title} (마감: $due)"
        }

        val system = "너는 사용자의 할 일을 캘린더 빈 시간에 효율적으로 배치해주는 AI야. " +
            "바쁜 시간대를 피해 집중할 수 있는 시간대에 할 일을 배치해줘. " +
            "반드시 아래 JSON 배열 형식으로만 답하고 설명은 절대 추가하지마: " +
            "[{\"id\":할일ID,\"time\":\"yyyy-MM-dd HH:mm\",\"reason\":\"이유(15자이내)\"}]"

        val userMessage = "지금: $nowFormatted\n" +
            "바쁜 시간: $busyBlocksText\n" +
            "미완료 할 일:\n$tasksText"

        val raw = ClaudeTextClient.ask(system = system, userMessage = userMessage, maxTokens = 200)
            ?: return emptyList()

        val jsonText = JsonExtractor.extractJsonArray(raw) ?: return emptyList()

        return try {
            val parsed = json.decodeFromString<List<ParsedSuggestion>>(jsonText)
            parsed.mapNotNull { ps ->
                val reminder = pendingReminders.find { it.id == ps.id } ?: return@mapNotNull null
                val suggestedMillis = dueAtFormat.parse(ps.time)?.time ?: return@mapNotNull null
                if (suggestedMillis == reminder.dueAt) return@mapNotNull null
                ScheduleSuggestion(
                    reminderId = reminder.id,
                    reminderTitle = reminder.title,
                    currentDueAt = reminder.dueAt,
                    suggestedDueAt = suggestedMillis,
                    reason = ps.reason,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
