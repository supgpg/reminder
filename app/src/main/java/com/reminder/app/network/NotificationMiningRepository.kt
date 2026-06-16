package com.reminder.app.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale

@Serializable
private data class ExtractedTask(
    val task: Boolean = false,
    val title: String = "",
    val due: String? = null,
    val desc: String = "",
)

data class MiningResult(val title: String, val dueAt: Long?, val description: String)

class NotificationMiningRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

    suspend fun extract(title: String, body: String): MiningResult? {
        val system = "알림에서 마감이 있는 할 일만 추출해. " +
            "있으면: {\"task\":true,\"title\":\"제목(20자이내)\",\"due\":\"yyyy-MM-dd HH:mm 없으면 null\",\"desc\":\"설명(30자이내)\"} " +
            "없으면: {\"task\":false}"

        val user = buildString {
            append("제목: $title")
            if (body.isNotBlank()) append("\n내용: ${body.take(200)}")
        }

        val raw = ClaudeTextClient.ask(system = system, userMessage = user, maxTokens = 80) ?: return null
        val jsonStr = JsonExtractor.extractJsonObject(raw) ?: return null

        return try {
            val parsed = json.decodeFromString<ExtractedTask>(jsonStr)
            if (!parsed.task || parsed.title.isBlank()) return null

            val dueAt = parsed.due?.let { dueStr ->
                runCatching { dateTimeFmt.parse(dueStr)?.time }.getOrNull()
                    ?: runCatching { dateFmt.parse(dueStr)?.time }.getOrNull()
            }

            MiningResult(title = parsed.title, dueAt = dueAt, description = parsed.desc)
        } catch (e: Exception) {
            null
        }
    }
}
