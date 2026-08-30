package com.example.pumppaperbot

internal data class RangeGuideLevels(
    val reference: Double,
    val levels: List<RangeGuideLevel>
)

internal data class RangeGuideLevel(
    val percent: Int,
    val upper: Double,
    val lower: Double
)

/** Presentation-only integer percentage grid; it never changes TP/SL or execution. */
internal object RangeGuidePolicy {
    val PERCENTS = 1..4

    fun levels(reference: Double): RangeGuideLevels? {
        if (!reference.isFinite() || reference <= 0.0) return null
        return RangeGuideLevels(
            reference = reference,
            levels = PERCENTS.map { percent ->
                val fraction = percent / 100.0
                RangeGuideLevel(
                    percent = percent,
                    upper = reference * (1.0 + fraction),
                    lower = reference * (1.0 - fraction)
                )
            }
        )
    }
}
