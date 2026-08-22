package com.example.pumppaperbot

/**
 * DeepSig and FusionSim are independent paper participants.
 * Fusion may be visible as market/risk context, but its open position must never make
 * DeepSig behave as if its own paper account were already invested.
 */
data class DeepSigPositionScope(
    val deepSigPositionOpen: Boolean,
    val fusionPositionOpen: Boolean
) {
    val actionPositionOpen: Boolean get() = deepSigPositionOpen
    val fusionContextOnly: Boolean get() = fusionPositionOpen

    val allowedActions: Set<String>
        get() = if (actionPositionOpen) {
            setOf("HOLD", "WATCH", "EXIT")
        } else {
            setOf("BUY", "HOLD", "WATCH")
        }
}
