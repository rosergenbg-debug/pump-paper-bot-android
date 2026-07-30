package com.example.pumppaperbot

data class ManualPositionControls(
    val buyEnabled: Boolean,
    val sellEnabled: Boolean
)

object ManualPositionControlPolicy {
    fun forWaitMode(waitMode: String): ManualPositionControls {
        val positionOpen = waitMode == "SELL"
        return ManualPositionControls(
            buyEnabled = !positionOpen,
            sellEnabled = positionOpen
        )
    }
}
