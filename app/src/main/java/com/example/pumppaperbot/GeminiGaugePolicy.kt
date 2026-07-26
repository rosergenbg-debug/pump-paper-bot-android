package com.example.pumppaperbot

internal object GeminiGaugePolicy {
    const val DECISION_FRESH_MILLIS = 90L * 60L * 1000L

    fun currentDecision(
        state: GeminiExperimentState,
        now: Long = System.currentTimeMillis()
    ): GeminiHourlyDecision? = currentDecision(
        decision = state.portfolio.decisions.lastOrNull(),
        lastSuccess = state.lastSuccess,
        lastFailure = state.lastFailure,
        now = now
    )

    fun currentDecision(
        decision: GeminiHourlyDecision?,
        lastSuccess: Long,
        lastFailure: Long,
        now: Long
    ): GeminiHourlyDecision? = decision?.takeIf {
        lastSuccess >= lastFailure &&
            now - it.decidedAt <= DECISION_FRESH_MILLIS
    }

    fun unavailableReason(
        state: GeminiExperimentState,
        now: Long = System.currentTimeMillis()
    ): String {
        val decision = state.portfolio.decisions.lastOrNull()
            ?: return "Свежего самостоятельного решения Gemini ещё нет."
        if (state.lastFailure > state.lastSuccess) {
            return "Последняя проверка Gemini завершилась ошибкой, поэтому старое решение не выдаётся за текущее."
        }
        if (now - decision.decidedAt > DECISION_FRESH_MILLIS) {
            return "Последнее решение старше 90 минут, поэтому на живой шкале оно временно скрыто."
        }
        return ""
    }
}
