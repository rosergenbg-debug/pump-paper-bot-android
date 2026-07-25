package com.example.pumppaperbot

import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class StrategyV2HistoricalResearchTest {
    @Test
    fun reproduceCurrentSixMonthStrategyFromExternalFixture() {
        val fixturePath = System.getenv("PUMP_V32_FIXTURE").orEmpty()
        assumeTrue("PUMP_V32_FIXTURE is optional outside research runs", fixturePath.isNotBlank())
        val root = JSONObject(File(fixturePath).readText())
        fun candles(name: String) = PumpBotEngine.parseCandles(root.getJSONArray(name).toString())
        val pumpEur = StrategyV2.synthesizeEur(candles("pump_spot"), candles("eur_spot"))
        val funding = PumpBotEngine.parseFunding(root.getJSONArray("funding").toString())
        listOf(false, true).forEach { aggressive ->
            val result = StrategyV2.backtest(
                pumpEur = pumpEur,
                btcUsdt = candles("btc_spot"),
                funding = funding,
                startTime = root.getLong("start_time"),
                aggressive = aggressive,
                ethUsdt = candles("eth_spot"),
                solUsdt = candles("sol_spot"),
                pumpFutures = candles("pump_futures"),
                premium = candles("premium")
            )
            println(
                "HISTORICAL_V32 aggressive=$aggressive return=${result.profitPercent} " +
                    "trades=${result.roundTrips} wins=${result.winRatePercent} " +
                    "drawdown=${result.maxDrawdownPercent} blocked=${result.blockedOverheatCount}"
            )
        }
    }
}
