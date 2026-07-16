package com.example.pumppaperbot

import android.content.Context
import java.io.File
import java.util.Locale

/**
 * Local research journal. It stores only public market observations and never
 * account data or API keys. The file is capped so background monitoring cannot
 * grow storage without limit.
 */
object MarketObservationLog {
    private const val fileName = "pump_market_breathing.csv"
    private const val maxBytes = 8L * 1024L * 1024L

    fun append(context: Context, snapshot: LiveSnapshot) {
        if (snapshot.lastPrice <= 0.0 || snapshot.lastCandle <= 0L) return
        runCatching {
            val file = File(context.filesDir, fileName)
            if (file.exists() && file.length() > maxBytes) rotate(file)
            val newFile = !file.exists() || file.length() == 0L
            file.appendText(
                buildString {
                    if (newFile) {
                        append("observed_at_ms,candle_close_ms,price_eur,state,energy,compression,direction,confidence,late_risk,book_imbalance,spread_pct,open_interest,open_interest_change_pct,readiness,action\n")
                    }
                    append(System.currentTimeMillis()).append(',')
                    append(snapshot.lastCandle).append(',')
                    append(format(snapshot.lastPrice)).append(',')
                    append(csv(snapshot.breathingState)).append(',')
                    append(snapshot.energyScore).append(',')
                    append(snapshot.compressionScore).append(',')
                    append(snapshot.directionScore).append(',')
                    append(snapshot.breathingConfidence).append(',')
                    append(snapshot.lateEntryRisk).append(',')
                    append(snapshot.bookImbalance?.let(::format).orEmpty()).append(',')
                    append(snapshot.spreadPercent?.let(::format).orEmpty()).append(',')
                    append(snapshot.openInterest?.let(::format).orEmpty()).append(',')
                    append(snapshot.openInterestChangePercent?.let(::format).orEmpty()).append(',')
                    append(snapshot.readinessScore).append(',')
                    append(csv(snapshot.signalAction)).append('\n')
                },
                Charsets.UTF_8
            )
        }
    }

    private fun rotate(file: File) {
        val old = File(file.parentFile, "$fileName.old")
        if (old.exists()) old.delete()
        file.renameTo(old)
    }

    private fun format(value: Double): String = String.format(Locale.US, "%.10f", value)

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
