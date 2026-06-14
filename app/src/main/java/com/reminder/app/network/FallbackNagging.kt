package com.reminder.app.network

import com.reminder.app.data.Reminder
import com.reminder.app.data.SpiceLevel

/**
 * Claude API 키가 없거나 호출이 실패했을 때 사용하는 기본 잔소리 문구.
 * 미룬 횟수(postponeCount)에 따라 톤이 점점 강해진다.
 */
object FallbackNagging {

    private val mildMessages = listOf(
        "조금만 힘내세요! 지금 시작하면 여유롭게 끝낼 수 있어요. 🙂",
        "한 번 미뤘다고 늦은 건 아니에요. 지금 5분만 시작해볼까요?",
        "벌써 두 번째네요. 작은 한 걸음만 떼면 훨씨 가벼워질 거예요.",
        "여기까지 미뤄왔지만, 지금부터라도 천천히 시작해봐요. 응원할게요!",
    )

    private val spicyMessages = listOf(
        "지금 시작 안 하면 나중에 본인이 후회합니다. 얼른 움직이세요.",
        "벌써 한 번 미뤘죠? 그 10분이 모여서 시험 점수가 됩니다.",
        "두 번째 미루기네요. 이러다 진짜 '내일의 나'가 다 떠안습니다.",
        "기말고사 성적표가 당신을 외면하기 전에, 지금 당장 책을 펴세요!",
    )

    fun pick(reminder: Reminder, spiceLevel: SpiceLevel): String {
        val messages = when (spiceLevel) {
            SpiceLevel.MILD -> mildMessages
            SpiceLevel.SPICY -> spicyMessages
        }
        val index = reminder.postponeCount.coerceIn(0, messages.size - 1)
        return "${reminder.title} - ${messages[index]}"
    }
}
