package com.reminder.app.ui

/**
 * 스마트 예약(자연어 분석) 결과를 새 리마인더 추가 화면에 전달하기 위한 임시 홀더.
 * Compose Navigation 인자로 한글 텍스트를 전달할 때 발생하는 인코딩 문제를 피하기 위해
 * 단순한 싱글톤으로 값을 전달하고, 소비 즉시 비운다.
 */
object AddEditPrefill {

    private var title: String? = null
    private var description: String? = null
    private var dueAt: Long? = null

    fun set(title: String, description: String = "", dueAt: Long) {
        this.title = title
        this.description = description
        this.dueAt = dueAt
    }

    fun consume(): Triple<String, String, Long>? {
        val title = this.title ?: return null
        val description = this.description ?: ""
        val dueAt = this.dueAt ?: return null
        clear()
        return Triple(title, description, dueAt)
    }

    private fun clear() {
        title = null
        description = null
        dueAt = null
    }
}
