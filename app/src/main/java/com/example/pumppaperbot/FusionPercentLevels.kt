package com.example.pumppaperbot

import java.util.Locale

/**
 * Price of the dashed +/- N% line relative to the newest Bitpanda Fusion price.
 * The displayed line is informational: order execution and fees are separate.
 */
internal object FusionPercentLevels {
    fun price(current: Double, percent: Int): Double {
        require(current.isFinite() && current > 0.0)
        require(percent > -100)
        return current * (1.0 + percent / 100.0)
    }

    /** Extra digits are useful for entering stop-loss targets without guesswork. */
    fun format(price: Double, bitcoin: Boolean): String =
        if (bitcoin) String.format(Locale.GERMANY, "%,.2f", price)
        else String.format(Locale.GERMANY, "%.8f", price)
}
