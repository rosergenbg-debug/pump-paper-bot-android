package com.example.pumppaperbot

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class ImpulseSnapshot(
    val candleTime: Long = 0L,
    val readiness: Int = 0,
    val candidate: Boolean = false,
    val volumeRatio: Double? = null,
    val spotTakerRatio: Double? = null,
    val futuresTakerRatio: Double? = null,
    val return15m: Double? = null,
    val return60m: Double? = null,
    val compressionRatio: Double? = null,
    val relativeStrength15m: Double? = null,
    val openInterestChange10m: Double? = null,
    val breakout60m: Boolean = false,
    val status: String = "ЖДЁМ 5-МИН ДАННЫЕ",
    val error: String = ""
) {
    fun toJson(): JSONObject = JSONObject()
        .put("candleTime", candleTime)
        .put("readiness", readiness)
        .put("candidate", candidate)
        .putNullable("volumeRatio", volumeRatio)
        .putNullable("spotTakerRatio", spotTakerRatio)
        .putNullable("futuresTakerRatio", futuresTakerRatio)
        .putNullable("return15m", return15m)
        .putNullable("return60m", return60m)
        .putNullable("compressionRatio", compressionRatio)
        .putNullable("relativeStrength15m", relativeStrength15m)
        .putNullable("openInterestChange10m", openInterestChange10m)
        .put("breakout60m", breakout60m)
        .put("status", status)
        .put("error", error)

    companion object {
        fun fromJson(json: JSONObject) = ImpulseSnapshot(
            candleTime = json.optLong("candleTime"),
            readiness = json.optInt("readiness"),
            candidate = json.optBoolean("candidate"),
            volumeRatio = json.nullableDouble("volumeRatio"),
            spotTakerRatio = json.nullableDouble("spotTakerRatio"),
            futuresTakerRatio = json.nullableDouble("futuresTakerRatio"),
            return15m = json.nullableDouble("return15m"),
            return60m = json.nullableDouble("return60m"),
            compressionRatio = json.nullableDouble("compressionRatio"),
            relativeStrength15m = json.nullableDouble("relativeStrength15m"),
            openInterestChange10m = json.nullableDouble("openInterestChange10m"),
            breakout60m = json.optBoolean("breakout60m"),
            status = json.optString("status", "ЖДЁМ 5-МИН ДАННЫЕ"),
            error = json.optString("error")
        )
    }
}

object ImpulseRadarAnalyzer {
    const val VOLUME_RATIO_MIN = 3.5
    const val SPOT_TAKER_RATIO_MIN = 0.70
    const val FUTURES_TAKER_RATIO_MIN = 0.64
    const val RETURN_15M_MIN = 0.005
    const val RETURN_15M_MAX = 0.05
    const val RETURN_60M_MAX = 0.08
    const val COMPRESSION_RATIO_MAX = 0.65
    const val RELATIVE_STRENGTH_15M_MIN = 0.003

    /**
     * Descriptive shadow calculation only. The locked historical test of this
     * rule was negative, so [candidate] must never be treated as a BUY signal.
     */
    fun analyze(
        pump: List<PumpCandle>,
        btc: List<PumpCandle>,
        sol: List<PumpCandle>,
        futures: List<PumpCandle>,
        openInterestJson: String
    ): ImpulseSnapshot {
        if (pump.size < 100 || btc.size < 20 || sol.size < 20 || futures.size < 20) {
            return ImpulseSnapshot(status = "ЖДЁМ НЕ МЕНЕЕ 100 ЗАКРЫТЫХ 5-МИН СВЕЧЕЙ")
        }
        val index = pump.lastIndex
        val candle = pump[index]
        val btcByTime = btc.associateBy { it.closeTime }
        val solByTime = sol.associateBy { it.closeTime }
        val futuresByTime = futures.associateBy { it.closeTime }
        val alignedFutures = listOfNotNull(
            futuresByTime[candle.closeTime],
            futuresByTime[pump[index - 1].closeTime]
        )
        if (alignedFutures.size < 2) {
            return ImpulseSnapshot(candleTime = candle.closeTime, status = "ЖДЁМ СИНХРОННЫЙ FUTURES-ПОТОК")
        }

        val previousVolumes = pump.subList(index - 36, index).map { it.volume }.sorted()
        val volumeMedian = median(previousVolumes)
        val volumeRatio = if (volumeMedian > 0.0) candle.volume / volumeMedian else 0.0
        val spotTakerRatio = takerRatio(pump.subList(index - 1, index + 1))
        val futuresTakerRatio = takerRatio(alignedFutures)
        val return5m = returnBetween(pump[index - 1], candle)
        val return15m = returnBetween(pump[index - 3], candle)
        val return60m = returnBetween(pump[index - 12], candle)

        val priorHour = pump.subList(index - 12, index)
        val priorHigh = priorHour.maxOf { it.high }
        val priorLow = priorHour.minOf { it.low }
        val priorRange = if (priorLow > 0.0) priorHigh / priorLow - 1.0 else 0.0
        val historicalRanges = ArrayList<Double>()
        for (endExclusive in max(24, index - 1_000)..index) {
            val window = pump.subList(endExclusive - 12, endExclusive)
            val low = window.minOf { it.low }
            if (low > 0.0) historicalRanges += window.maxOf { it.high } / low - 1.0
        }
        val typicalRange = median(historicalRanges.sorted())
        val compressionRatio = if (typicalRange > 0.0) priorRange / typicalRange else Double.POSITIVE_INFINITY

        val btcNow = btcByTime[candle.closeTime]
        val btcOld = btcByTime[pump[index - 3].closeTime]
        val solNow = solByTime[candle.closeTime]
        val solOld = solByTime[pump[index - 3].closeTime]
        val btcReturn = pairedReturn(btcOld, btcNow)
        val solReturn = pairedReturn(solOld, solNow)
        val marketReturn = listOfNotNull(btcReturn, solReturn).averageOrNull()
        val relativeStrength = if (marketReturn != null) return15m - marketReturn else Double.NaN
        val oiChange = openInterestChange10m(openInterestJson, candle.closeTime)
        val breakout = candle.close > priorHigh

        val checks = listOf(
            volumeRatio >= VOLUME_RATIO_MIN to 15,
            spotTakerRatio >= SPOT_TAKER_RATIO_MIN to 15,
            futuresTakerRatio >= FUTURES_TAKER_RATIO_MIN to 15,
            (return15m in RETURN_15M_MIN..RETURN_15M_MAX) to 10,
            (return60m < RETURN_60M_MAX && return5m in 0.0..0.04) to 10,
            breakout to 10,
            (compressionRatio <= COMPRESSION_RATIO_MAX) to 15,
            (relativeStrength.isFinite() && relativeStrength >= RELATIVE_STRENGTH_15M_MIN) to 10
        )
        val readiness = checks.sumOf { (passed, weight) -> if (passed) weight else 0 }.coerceIn(0, 100)
        val candidate = checks.all { it.first } && (marketReturn == null || marketReturn > -0.025)
        val status = if (candidate) {
            "СОВПАДЕНИЕ 5-МИН ЕСТЬ, НО BUY ЗАПРЕЩЁН: BACKTEST УБЫТОЧЕН"
        } else {
            "5-МИН SHADOW: СОБИРАЕМ НАБЛЮДЕНИЯ, ТОРГОВЫЙ СИГНАЛ НЕ МЕНЯЕТСЯ"
        }
        return ImpulseSnapshot(
            candleTime = candle.closeTime,
            readiness = readiness,
            candidate = candidate,
            volumeRatio = volumeRatio,
            spotTakerRatio = spotTakerRatio,
            futuresTakerRatio = futuresTakerRatio,
            return15m = return15m,
            return60m = return60m,
            compressionRatio = compressionRatio,
            relativeStrength15m = relativeStrength.takeIf { it.isFinite() },
            openInterestChange10m = oiChange,
            breakout60m = breakout,
            status = status
        )
    }

    private fun takerRatio(candles: List<PumpCandle>): Double {
        val volume = candles.sumOf { it.volume }
        return if (volume > 0.0) candles.sumOf { it.takerBuyVolume } / volume else 0.0
    }

    private fun returnBetween(old: PumpCandle, current: PumpCandle): Double =
        if (old.close > 0.0) current.close / old.close - 1.0 else 0.0

    private fun pairedReturn(old: PumpCandle?, current: PumpCandle?): Double? =
        if (old != null && current != null && old.close > 0.0) current.close / old.close - 1.0 else null

    private fun median(sorted: List<Double>): Double {
        if (sorted.isEmpty()) return 0.0
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2.0 else sorted[middle]
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

    private fun openInterestChange10m(json: String, candleTime: Long): Double? = runCatching {
        val root = org.json.JSONArray(json)
        val values = (0 until root.length()).mapNotNull { index ->
            root.optJSONObject(index)?.let { item ->
                val time = item.optLong("timestamp")
                val value = item.optString("sumOpenInterest").toDoubleOrNull()
                if (time > 0L && time <= candleTime && value != null && value > 0.0) time to value else null
            }
        }.sortedBy { it.first }
        if (values.size < 3) return@runCatching null
        val current = values.last().second
        val old = values[values.lastIndex - 2].second
        if (old > 0.0) current / old - 1.0 else null
    }.getOrNull()
}

data class ImpulseRadarPayloads(
    val pumpJson: String,
    val btcJson: String,
    val solJson: String,
    val futuresJson: String,
    val openInterestJson: String
)

object ImpulseRadarStore {
    private const val prefsName = "PumpImpulseShadowV33"
    private const val keyPump = "pump_5m"
    private const val keyBtc = "btc_5m"
    private const val keySol = "sol_5m"
    private const val keyFutures = "futures_5m"
    private const val keyOpenInterest = "oi_5m"
    private const val keySnapshot = "snapshot"
    private const val keyLastAttempt = "last_attempt"
    private const val keyLastSuccess = "last_success"
    private const val keyLastLoggedCandle = "last_logged_candle"

    private fun prefs(context: Context) =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun shouldSync(context: Context, now: Long = System.currentTimeMillis()): Boolean =
        now - prefs(context).getLong(keyLastAttempt, 0L) >= 4L * 60L * 1000L

    fun markAttempt(context: Context, now: Long = System.currentTimeMillis()) {
        prefs(context).edit().putLong(keyLastAttempt, now).apply()
    }

    fun payloads(context: Context): ImpulseRadarPayloads {
        val p = prefs(context)
        return ImpulseRadarPayloads(
            p.getString(keyPump, "").orEmpty(),
            p.getString(keyBtc, "").orEmpty(),
            p.getString(keySol, "").orEmpty(),
            p.getString(keyFutures, "").orEmpty(),
            p.getString(keyOpenInterest, "").orEmpty()
        )
    }

    fun save(context: Context, payloads: ImpulseRadarPayloads, snapshot: ImpulseSnapshot) {
        prefs(context).edit()
            .putString(keyPump, payloads.pumpJson)
            .putString(keyBtc, payloads.btcJson)
            .putString(keySol, payloads.solJson)
            .putString(keyFutures, payloads.futuresJson)
            .putString(keyOpenInterest, payloads.openInterestJson)
            .putString(keySnapshot, snapshot.toJson().toString())
            .putLong(keyLastSuccess, System.currentTimeMillis())
            .apply()
        ImpulseObservationLog.appendIfNew(context, snapshot)
    }

    fun saveFailure(context: Context, message: String) {
        val current = state(context)
        prefs(context).edit()
            .putString(keySnapshot, current.copy(error = message.take(300)).toJson().toString())
            .apply()
    }

    fun state(context: Context): ImpulseSnapshot = runCatching {
        ImpulseSnapshot.fromJson(
            JSONObject(prefs(context).getString(keySnapshot, "{}").orEmpty())
        )
    }.getOrDefault(ImpulseSnapshot())

    internal fun shouldLog(context: Context, candleTime: Long): Boolean =
        candleTime > prefs(context).getLong(keyLastLoggedCandle, 0L)

    internal fun markLogged(context: Context, candleTime: Long) {
        prefs(context).edit().putLong(keyLastLoggedCandle, candleTime).apply()
    }
}

object ImpulseObservationLog {
    private const val fileName = "pump_impulse_shadow_v33.csv"
    private const val maxBytes = 8L * 1024L * 1024L

    fun appendIfNew(context: Context, snapshot: ImpulseSnapshot) {
        if (snapshot.candleTime <= 0L || !ImpulseRadarStore.shouldLog(context, snapshot.candleTime)) return
        runCatching {
            val file = File(context.filesDir, fileName)
            if (file.exists() && file.length() > maxBytes) {
                val old = File(file.parentFile, "$fileName.old")
                if (old.exists()) old.delete()
                file.renameTo(old)
            }
            val newFile = !file.exists() || file.length() == 0L
            file.appendText(
                buildString {
                    if (newFile) {
                        append("observed_at_ms,candle_close_ms,readiness,candidate,volume_ratio,spot_taker_ratio,futures_taker_ratio,return_15m,return_60m,compression_ratio,relative_strength_15m,oi_change_10m,breakout_60m,status\n")
                    }
                    append(System.currentTimeMillis()).append(',')
                    append(snapshot.candleTime).append(',')
                    append(snapshot.readiness).append(',')
                    append(snapshot.candidate).append(',')
                    appendNumber(snapshot.volumeRatio).append(',')
                    appendNumber(snapshot.spotTakerRatio).append(',')
                    appendNumber(snapshot.futuresTakerRatio).append(',')
                    appendNumber(snapshot.return15m).append(',')
                    appendNumber(snapshot.return60m).append(',')
                    appendNumber(snapshot.compressionRatio).append(',')
                    appendNumber(snapshot.relativeStrength15m).append(',')
                    appendNumber(snapshot.openInterestChange10m).append(',')
                    append(snapshot.breakout60m).append(',')
                    append('"').append(snapshot.status.replace("\"", "\"\"")).append('"').append('\n')
                },
                Charsets.UTF_8
            )
            ImpulseRadarStore.markLogged(context, snapshot.candleTime)
        }
    }

    private fun StringBuilder.appendNumber(value: Double?): StringBuilder =
        append(value?.let { String.format(Locale.US, "%.10f", it) }.orEmpty())
}

private fun JSONObject.putNullable(name: String, value: Double?): JSONObject =
    if (value == null || !value.isFinite()) put(name, JSONObject.NULL) else put(name, value)

private fun JSONObject.nullableDouble(name: String): Double? =
    if (isNull(name)) null else optDouble(name).takeIf { it.isFinite() }
