package com.example.pumppaperbot

internal data class RangeGuideLevels(
    val reference: Double,
    val upper: Double,
    val lower: Double
)

/** Presentation-only ±1.5% market range used by V6.8 charts. */
internal object RangeGuidePolicy {
    const val HALF_RANGE_FRACTION = 0.015

    fun levels(reference: Double): RangeGuideLevels? {
        if (!reference.isFinite() || reference <= 0.0) return null
        return RangeGuideLevels(
            reference = reference,
            upper = reference * (1.0 + HALF_RANGE_FRACTION),
            lower = reference * (1.0 - HALF_RANGE_FRACTION)
        )
    }
}
