package com.example.pumppaperbot

enum class DeepSeekResearchCardTone { SAFE, WATCH, READY, DANGER, STALE }

data class DeepSeekResearchCardPresentation(
    val text: String,
    val tone: DeepSeekResearchCardTone
)

/**
 * Turns the research engine's internal state into an action-first explanation.
 * A low score is explicitly a "do not enter" state, never an entry candidate.
 */
object DeepSeekResearchCardPolicy {
    fun render(
        state: DeepSeekPrimaryState,
        inPosition: Boolean,
        fresh: Boolean
    ): DeepSeekResearchCardPresentation {
        if (!fresh) {
            return DeepSeekResearchCardPresentation(
                text = "DEEPSIG • РЕЗУЛЬТАТ УСТАРЕЛ\n" +
                    "СЕЙЧАС: НЕ ПРИНИМАТЬ РЕШЕНИЕ ПО ЭТОМУ АНАЛИЗУ\n" +
                    "Ждём новую проверку свежего рынка. Виртуальная сделка не выполняется.\n" +
                    "Независимый paper-тест • реальные деньги не используются.",
                tone = DeepSeekResearchCardTone.STALE
            )
        }
        return if (inPosition) renderExit(state) else renderEntry(state)
    }

    private fun renderEntry(state: DeepSeekPrimaryState): DeepSeekResearchCardPresentation {
        val score = state.entryReadiness.coerceIn(1, 10)
        val approvedBuy = state.action.equals("BUY", true) &&
            state.executionStatus.contains("ОДОБРЕНО", true)
        val (instruction, explanation, tone) = when {
            approvedBuy -> Triple(
                "ВИРТУАЛЬНЫЙ ВХОД ПОДТВЕРЖДЁН",
                "BUY прошёл независимую проверку и может быть исполнен только на виртуальном счёте.",
                DeepSeekResearchCardTone.READY
            )
            score <= 3 -> Triple(
                "НЕ ВХОДИТЬ",
                "Условий для покупки почти нет. Наблюдать за рынком и ждать нового подтверждённого импульса.",
                DeepSeekResearchCardTone.SAFE
            )
            score <= 6 -> Triple(
                "ТОЛЬКО НАБЛЮДАТЬ",
                "Есть отдельные признаки роста, но их недостаточно для входа. Не покупать только из-за высокого процента покупок.",
                DeepSeekResearchCardTone.WATCH
            )
            score <= 8 -> Triple(
                "ПОДГОТОВКА К ВХОДУ",
                "Условия усиливаются, но BUY ещё не подтверждён. Ждать откат/ретест и повторную проверку.",
                DeepSeekResearchCardTone.WATCH
            )
            else -> Triple(
                "ВОЗМОЖЕН ВХОД — ЖДАТЬ BUY",
                "Готовность высокая, но виртуальная покупка разрешается только после сформированного и проверенного BUY.",
                DeepSeekResearchCardTone.READY
            )
        }
        val execution = when {
            approvedBuy -> "Виртуальная покупка подтверждена."
            state.proposedAction.equals("BUY", true) ->
                "BUY рассматривался, но проверка его не подтвердила; виртуальная покупка не выполнена."
            else -> "Сигнал BUY не сформирован, поэтому виртуальная покупка не выполнялась."
        }
        return DeepSeekResearchCardPresentation(
            text = "DEEPSIG • ОЖИДАНИЕ ВХОДА\n" +
                "СЕЙЧАС: $instruction\n" +
                "Готовность $score/10. $explanation\n" +
                "$execution\n" +
                "Независимый paper-тест • реальные деньги не используются.",
            tone = tone
        )
    }

    private fun renderExit(state: DeepSeekPrimaryState): DeepSeekResearchCardPresentation {
        val score = state.danger.coerceIn(0, 10)
        val approvedExit = state.action.equals("EXIT", true) &&
            state.executionStatus.contains("ОДОБРЕНО", true)
        val (instruction, explanation, tone) = when {
            approvedExit -> Triple(
                "ВИРТУАЛЬНЫЙ ВЫХОД ПОДТВЕРЖДЁН",
                "EXIT прошёл независимую проверку и может быть исполнен на виртуальном счёте.",
                DeepSeekResearchCardTone.DANGER
            )
            score <= 3 -> Triple(
                "УДЕРЖИВАТЬ И НАБЛЮДАТЬ",
                "Подтверждённой угрозы позиции нет; обычный рыночный шум сам по себе не является причиной выхода.",
                DeepSeekResearchCardTone.SAFE
            )
            score <= 6 -> Triple(
                "УСИЛИТЬ КОНТРОЛЬ",
                "Риск вырос, но EXIT ещё не подтверждён несколькими независимыми группами признаков.",
                DeepSeekResearchCardTone.WATCH
            )
            else -> Triple(
                "ПОДГОТОВИТЬСЯ К ВЫХОДУ",
                "Опасность высокая. Ждать подтверждённый EXIT либо аварийное условие защиты.",
                DeepSeekResearchCardTone.DANGER
            )
        }
        val execution = when {
            approvedExit -> "Виртуальный выход подтверждён."
            state.proposedAction.equals("EXIT", true) ->
                "EXIT рассматривался, но проверка его не подтвердила; позиция пока удерживается."
            else -> "Сигнал EXIT не сформирован, поэтому виртуальная продажа не выполнялась."
        }
        return DeepSeekResearchCardPresentation(
            text = "DEEPSIG • КОНТРОЛЬ ВИРТУАЛЬНОЙ ПОЗИЦИИ\n" +
                "СЕЙЧАС: $instruction\n" +
                "Опасность $score/10. $explanation\n" +
                "$execution\n" +
                "Независимый paper-тест • реальные деньги не используются.",
            tone = tone
        )
    }
}
