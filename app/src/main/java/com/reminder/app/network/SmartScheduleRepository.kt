package com.reminder.app.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class ParsedSchedule(
    val title: String,
    val dueAt: String,
    val description: String = "",
)

data class SmartScheduleResult(
    val title: String,
    val dueAt: Long,
    val description: String,
)

/**
 * 자연어 문장을 Claude로 분석해 할 일 제목과 마감 일시(epoch millis)로 변환한다.
 * 파싱에 실패하면 null을 반환한다.
 */
class SmartScheduleRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val dueAtFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
    private val nowFormat = SimpleDateFormat("yyyy-MM-dd(E) HH:mm", Locale.KOREA)

    suspend fun parse(input: String, now: Long = System.currentTimeMillis()): SmartScheduleResult? {
        val nowText = nowFormat.format(Date(now))
        val system = "너는 사용자의 자연어 문장에서 할 일 제목과 마감 일시를 추출하는 비서야. " +
            "현재 날짜와 시간은 $nowText (한국 표준시)야. " +
            "문장 속 '내일', '다음주', '아침', '저녁' 같은 표현은 현재 시각을 기준으로 절대 날짜/시간으로 환산해. " +
            "시간이 명확하지 않으면 합리적으로 추정해 (예: '아침'은 08:00, '저녁'은 19:00, 시간 언급이 전혀 없으면 09:00). " +
            "반드시 아래 JSON 형식으로만 답하고 다른 설명은 절대 추가하지마: " +
            "{\"title\": \"할 일 제목\", \"dueAt\": \"yyyy-MM-dd HH:mm\", \"description\": \"\"}"

        val text = ClaudeTextClient.ask(system = system, userMessage = input) ?: return null
        val jsonText = JsonExtractor.extractJsonObject(text) ?: return null

        return try {
            val parsed = json.decodeFromString<ParsedSchedule>(jsonText)
            val dueAtMillis = dueAtFormat.parse(parsed.dueAt)?.time ?: return null
            if (parsed.title.isBlank()) return null
            SmartScheduleResult(title = parsed.title, dueAt = dueAtMillis, description = parsed.description)
        } catch (e: Exception) {
            null
        }
    }
}
