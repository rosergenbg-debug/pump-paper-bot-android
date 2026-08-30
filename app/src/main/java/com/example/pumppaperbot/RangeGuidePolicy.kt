package com.example.pumppaperbot

internal data class RangeGuideLevels(
    val reference: Double,
    val outerUpper: Double,
    val innerUpper: Double,
    val innerLower: Double,
    val outerLower: Double
)

/** Presentation-only ±1.0% and ±1.5% market ranges used by V6.9 charts. */
internal object RangeGuidePolicy {
    const val INNER_FRACTION = 0.010
    const val OUTER_FRACTION = 0.015

    fun levels(reference: Double): RangeGuideLevels? {
        if (!reference.isFinite() || reference <= 0.0) return null
        return RangeGuideLevels(
            reference = reference,
            outerUpper = reference * (1.0 + OUTER_FRACTION),
            innerUpper = reference * (1.0 + INNER_FRACTION),
            innerLower = reference * (1.0 - INNER_FRACTION),
            outerLower = reference * (1.0 - OUTER_FRACTION)
        )
    }
}
