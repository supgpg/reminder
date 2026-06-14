package com.reminder.app.network

import com.reminder.app.data.ReportStats

/**
 * 완료/미룬 패턴 통계를 바탕으로 Claude에게 회고 피드백 문구를 생성한다.
 */
class ReportRepository {

    suspend fun generateFeedback(stats: ReportStats): String {
        val system = "너는 학생의 할 일 습관을 분석해주는 다정한 코치야. " +
            "아래 통계를 보고 잘한 점과 개선할 점을 짧고 친근한 한국어로 2~3문장으로 피드백해줘. " +
            "구체적인 수치를 1개 이상 언급하고, 마지막은 격려하는 말투로 마무리해. " +
            "일반 텍스트로만 답하고 다른 설명은 추가하지마."

        val user = buildString {
            append("기간: ${stats.periodLabel}\n")
            append("전체 할 일: ${stats.totalCount}개, 완료: ${stats.completedCount}개 ")
            append("(완료율 ${(stats.completionRate * 100).toInt()}%)\n")
            append("오전 마감 할 일: ${stats.morningTotal}개 중 ${stats.morningCompleted}개 완료\n")
            append("오후 마감 할 일: ${stats.afternoonTotal}개 중 ${stats.afternoonCompleted}개 완료\n")
            append("미룬 적 있는 할 일: ${stats.postponedCount}개, 평균 미룬 횟수: ")
            append("${"%.1f".format(stats.avgPostponeCount)}회\n")
            append("지난 할 일 중 이번에 처리한 일: ${stats.caughtUpCount}개")
        }

        return ClaudeTextClient.ask(system = system, userMessage = user, maxTokens = 200)
            ?: fallback(stats)
    }

    private fun fallback(stats: ReportStats): String {
        val percent = (stats.completionRate * 100).toInt()
        return if (stats.totalCount == 0) {
            "${stats.periodLabel}엔 등록된 할 일이 없었어요. 새로운 할 일을 추가해보세요!"
        } else if (stats.morningTotal > 0 && stats.morningCompleted >= stats.afternoonCompleted) {
            "${stats.periodLabel} 완료율은 ${percent}%예요. 오전 작업 완료율이 특히 좋았어요. 이 흐름 계속 이어가봐요!"
        } else {
            "${stats.periodLabel} 완료율은 ${percent}%예요. 다음에는 조금 더 힘내봐요!"
        }
    }
}
