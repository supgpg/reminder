package com.reminder.app.network

import com.reminder.app.data.SpiceLevel

/**
 * 페르소나(매운맛 단계)와 미룬 횟수(postponeCount)를 반영해
 * Claude에게 보낼 system prompt를 만든다.
 */
object NaggingPromptBuilder {

    fun buildSystemPrompt(spiceLevel: SpiceLevel, postponeCount: Int, usageContext: String? = null): String {
        val persona = when (spiceLevel) {
            SpiceLevel.MILD ->
                "너는 따뜻하고 다정한 친구야. 사용자가 할 일을 미루고 있을 때, " +
                    "부드럽고 다정한 말투로 짧게 응원하며 동기를 부여해줘."
            SpiceLevel.SPICY ->
                "너는 시험 기간인 고등학생의 뼈를 때리는 고수위 잔소리꾼이야. " +
                    "사용자가 미루고 있는 할 일에 대해 유머러스하면서도 정신이 번쩍 들게 팩트로 압박해줘."
        }

        val escalation = when {
            postponeCount <= 0 ->
                "아직 처음 알림이니 가볍게 시작해."
            postponeCount == 1 ->
                "이미 한 번 미뤘어. 살짝 더 단호하게 말해."
            postponeCount == 2 ->
                "벌써 두 번이나 미뤘어. 강도를 한층 올려서 따끔하게 말해."
            else ->
                "벌써 ${postponeCount}번이나 미뤘어! 더 이상 못 봐줘. 최고 강도로 팩트폭격하면서도 " +
                    "결국엔 응원하는 느낌으로 마무리해."
        }

        val usagePart = usageContext?.let {
            " 참고로 사용자가 최근 2시간 동안 $it 등을 사용하고 있었으니, 이 사실을 자연스럽게 한 번 언급하면서 잔소리를 더 뼈아프게 만들어."
        } ?: ""

        return buildString {
            append(persona)
            append(" ")
            append(escalation)
            append(usagePart)
            append(" 반드시 한국어로, 2문장 이내, 이모지 1개 이하로 답해.")
        }
    }
}
