package com.example.pumppaperbot

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BitpandaFusionV600Test {
    @Test fun `orderbook parser retains individual depth levels for execution intelligence`() {
        val snapshot = BitpandaFusionClient.parseOrderbook(
            JSONObject("""{
                "pair":"PUMP-EUR",
                "bids":[
                    {"price":"0.0020","quantity":"100000"},
                    {"price":"0.0019","quantity":"50000"}
                ],
                "asks":[
                    {"price":"0.0022","quantity":"80000"},
                    {"price":"0.0023","quantity":"40000"}
                ]
            }"""),
            now = 1234L
        )
        assertEquals(2, snapshot.bidLevels.size)
        assertEquals(2, snapshot.askLevels.size)
        assertEquals(200.0, snapshot.bidLevels.first().notionalEur, 0.0001)
        assertEquals(176.0, snapshot.askLevels.first().notionalEur, 0.0001)
        assertEquals(FusionTradingCosts.FEE_RATE, snapshot.feeRate, 0.0)
        assertNull(snapshot.observedAccountFeeRate)
    }

    @Test fun `account parser stores observed fee without changing V5 control fee`() {
        val prior = FusionMarketSnapshot(
            configured = true,
            connected = true,
            feeRate = FusionTradingCosts.FEE_RATE,
            feeTier = FusionTradingCosts.FEE_TIER
        )
        val parsed = BitpandaFusionClient.parseAccount(
            JSONObject("""{
                "traded_volume30d":"12500.00",
                "current_tier":{
                    "name":"Tier 2",
                    "fee":"0.15",
                    "fee_mode":"Percentage",
                    "required_volume30d":"10000.00"
                }
            }"""),
            now = 9876L,
            previous = prior
        )
        assertEquals(FusionTradingCosts.FEE_RATE, parsed.feeRate, 0.0)
        assertEquals(FusionTradingCosts.FEE_TIER, parsed.feeTier)
        assertEquals(0.0015, parsed.observedAccountFeeRate!!, 0.00000001)
        assertEquals("Tier 2", parsed.observedAccountFeeTier)
        assertEquals(12_500.0, parsed.tradedVolume30dEur!!, 0.0001)
        assertEquals(9876L, parsed.feeUpdatedAt)
    }

    @Test fun `unknown fee mode cannot silently replace the conservative fallback`() {
        val prior = FusionMarketSnapshot(feeRate = FusionTradingCosts.FEE_RATE, feeTier = FusionTradingCosts.FEE_TIER)
        val parsed = BitpandaFusionClient.parseAccount(
            JSONObject("""{
                "traded_volume30d":"100",
                "current_tier":{"name":"odd","fee":"3.0","fee_mode":"Absolute"}
            }"""),
            now = 10L,
            previous = prior
        )
        assertEquals(FusionTradingCosts.FEE_RATE, parsed.feeRate, 0.0)
        assertNull(parsed.observedAccountFeeRate)
        assertTrue(parsed.tradedVolume30dEur == 100.0)
    }
}
