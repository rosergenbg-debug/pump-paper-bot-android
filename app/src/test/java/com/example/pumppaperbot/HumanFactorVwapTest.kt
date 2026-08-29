package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HumanFactorVwapTest {
    @Test fun exactVwapSetupReachesOneHundred() {
        val candles=(0 until 61).map { i ->
            val base=100.0
            PumpCandle(i*60_000L,if(i==59)99.0 else base,base*1.001,if(i==59)98.9 else base*.999,
                if(i==59)99.2 else base,100.0,i*60_000L+59_999,10_000.0,100,
                if(i==58)40.0 else if(i==59)60.0 else 50.0)
        }
        val result=HumanFactorVwapPolicy.evaluate(candles)
        assertEquals(100,result.first)
        assertTrue(result.second>0.0)
    }

    @Test fun ordinaryMarketNeverCreatesConfirmation() {
        val candles=(0 until 61).map { i ->
            PumpCandle(i*60_000L,100.0,100.1,99.9,100.0,100.0,i*60_000L+59_999,10_000.0,100,45.0)
        }
        assertTrue(HumanFactorVwapPolicy.evaluate(candles).first<HumanFactorVwapPolicy.READY)
    }
}
