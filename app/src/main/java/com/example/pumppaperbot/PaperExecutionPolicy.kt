package com.example.pumppaperbot

/**
 * One execution/freshness contract shared by all paper accounts and Serge's
 * manual position marker. Strategy decisions may use closed candles, but a
 * recorded fill must use a fresh quote and include the same conservative
 * slippage assumption as the historical backtest.
 */
object PaperExecutionPolicy {
    const val MAX_DECISION_AGE_MILLIS = 12L * 60L * 1000L
    const val APP_MAX_DECISION_AGE_MILLIS = 20L * 60L * 1000L
    const val MAX_LIVE_PRICE_AGE_MILLIS = 5L * 60L * 1000L
    const val APP_MAX_BUY_CHASE_PERCENT = 1.5

    fun isFreshDecision(decidedAt: Long, now: Long): Boolean =
        decidedAt > 0L && now >= decidedAt && now - decidedAt <= MAX_DECISION_AGE_MILLIS

    fun freshLivePrice(snapshot: LiveSnapshot, now: Long = System.currentTimeMillis()): Double? =
        snapshot.livePrice?.takeIf {
            it > 0.0 && snapshot.livePriceAt > 0L && now >= snapshot.livePriceAt &&
                now - snapshot.livePriceAt <= MAX_LIVE_PRICE_AGE_MILLIS
        }

    fun displayPrice(snapshot: LiveSnapshot, now: Long = System.currentTimeMillis()): Double =
        freshLivePrice(snapshot, now) ?: snapshot.lastPrice

    fun executionPrice(rawQuote: Double, action: String): Double {
        require(rawQuote > 0.0) { "Некорректная цена исполнения" }
        val normalized = action.uppercase()
        return if (normalized == "BUY") {
            rawQuote * (1.0 + PumpBotEngine.slippage)
        } else if (normalized == StrategyV2.ACTION_SELL ||
            normalized == StrategyV2.ACTION_SELL_HALF
        ) {
            rawQuote * (1.0 - PumpBotEngine.slippage)
        } else {
            rawQuote
        }
    }

    fun isTradeAction(action: String): Boolean {
        val normalized = action.uppercase()
        return normalized == "BUY" || normalized == StrategyV2.ACTION_SELL ||
            normalized == StrategyV2.ACTION_SELL_HALF
    }

    fun prepareAppEvaluation(
        evaluation: AppPaperEvaluation,
        rawQuote: Double,
        quoteAt: Long
    ): AppPaperEvaluation {
        if (!isTradeAction(evaluation.action)) return evaluation.copy(price = rawQuote)
        val appDecisionFresh = evaluation.candleTime > 0L && quoteAt >= evaluation.candleTime &&
            quoteAt - evaluation.candleTime <= APP_MAX_DECISION_AGE_MILLIS
        if (!appDecisionFresh) {
            return evaluation.copy(
                price = rawQuote,
                action = "WAIT",
                reason = "Сигнал APP пропущен: свежая цена исполнения получена позже 20-минутного срока. " +
                    "Ждём следующую закрытую свечу."
            )
        }
        val candidateFill = executionPrice(rawQuote, evaluation.action)
        if (evaluation.action.equals("BUY", ignoreCase = true) &&
            evaluation.entryZoneLow > 0.0 && evaluation.entryZoneHigh >= evaluation.entryZoneLow &&
            candidateFill !in evaluation.entryZoneLow..evaluation.entryZoneHigh
        ) {
            return evaluation.copy(
                price = rawQuote,
                action = "WAIT",
                reason = String.format(
                    java.util.Locale.GERMANY,
                    "V5 APP не исполнил виртуальный вход: цена €%.8f уже вне зоны €%.8f–€%.8f.",
                    candidateFill,
                    evaluation.entryZoneLow,
                    evaluation.entryZoneHigh
                )
            )
        }
        val chasedPercent = if (evaluation.price > 0.0) {
            (rawQuote / evaluation.price - 1.0) * 100.0
        } else 0.0
        if (evaluation.action.equals("BUY", ignoreCase = true) &&
            chasedPercent > APP_MAX_BUY_CHASE_PERCENT
        ) {
            return evaluation.copy(
                price = rawQuote,
                action = "WAIT",
                reason = String.format(
                    java.util.Locale.GERMANY,
                    "Сигнал APP не догоняем: живая цена уже на %+.2f%% выше цены решения (защитный предел +%.1f%%).",
                    chasedPercent,
                    APP_MAX_BUY_CHASE_PERCENT
                )
            )
        }
        return evaluation.copy(price = candidateFill)
    }
}
