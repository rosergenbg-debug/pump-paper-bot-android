package com.example.pumppaperbot

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object FusionTradingCosts {
    /** V5/V6 control-group simulation cost. V6 observed account fees are stored separately. */
    const val FEE_RATE = 0.0025
    const val FEE_TIER = "симуляция 0,25% за сторону"
}

/** Keystore-backed storage. The plaintext key is never written to SharedPreferences or logs. */
object BitpandaFusionSecureKeyStore {
    private const val PREFS = "secure_bitpanda_fusion_credentials_v51"
    private const val CIPHER = "ciphertext"
    private const val IV = "iv"
    private const val ALIAS = "pump_signal_bitpanda_fusion_read_only_v51"
    private const val STORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    @Volatile private var cached: String? = null

    fun read(context: Context): String {
        cached?.let { return it }
        val result = runCatching {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val encrypted = p.getString(CIPHER, "").orEmpty()
            val iv = p.getString(IV, "").orEmpty()
            if (encrypted.isBlank() || iv.isBlank()) return@runCatching ""
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            )
            String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8).trim()
        }.getOrDefault("")
        cached = result
        return result
    }

    fun save(context: Context, value: String): Boolean = runCatching {
        val clean = value.trim()
        if (clean.isBlank()) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
            cached = ""
            return@runCatching true
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(CIPHER, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .commit().also { if (it) cached = clean }
    }.getOrDefault(false)

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }
    }
}

data class FusionBookLevel(
    val price: Double,
    val quantity: Double
) {
    val notionalEur: Double get() = price * quantity

    fun toJson() = JSONObject().put("price", price).put("quantity", quantity)

    companion object {
        fun fromJson(value: JSONObject?): FusionBookLevel? {
            if (value == null) return null
            fun number(key: String): Double? = when (val raw = value.opt(key)) {
                is Number -> raw.toDouble()
                is String -> raw.toDoubleOrNull()
                else -> null
            }?.takeIf(Double::isFinite)
            val price = number("price") ?: return null
            val quantity = number("quantity") ?: number("size") ?: return null
            return FusionBookLevel(price, quantity).takeIf { price > 0.0 && quantity > 0.0 }
        }
    }
}

data class FusionMarketSnapshot(
    val configured: Boolean = false,
    val connected: Boolean = false,
    val pair: String = "PUMP-EUR",
    val bid: Double = 0.0,
    val ask: Double = 0.0,
    val mid: Double = 0.0,
    val spreadPercent: Double = 0.0,
    val bidDepthEur: Double = 0.0,
    val askDepthEur: Double = 0.0,
    val bidLevels: List<FusionBookLevel> = emptyList(),
    val askLevels: List<FusionBookLevel> = emptyList(),
    /** Control-group cost used by all pre-V6 paper engines. Must remain 0.25% in V6.0. */
    val feeRate: Double = FusionTradingCosts.FEE_RATE,
    val feeTier: String = FusionTradingCosts.FEE_TIER,
    /** Authenticated account fee is evidence for V6 shadow only in V6.0. */
    val observedAccountFeeRate: Double? = null,
    val observedAccountFeeTier: String? = null,
    val tradedVolume30dEur: Double? = null,
    val feeUpdatedAt: Long = 0L,
    val lastAttempt: Long = 0L,
    val lastSuccess: Long = 0L,
    val error: String = ""
) {
    fun fresh(now: Long = System.currentTimeMillis()): Boolean =
        connected && lastSuccess > 0L && now - lastSuccess in 0..MAX_AGE

    fun v6ObservedFeeRate(): Double = observedAccountFeeRate ?: feeRate
    fun v6ObservedFeeTier(): String = observedAccountFeeTier ?: feeTier

    fun toJson(): JSONObject = JSONObject()
        .put("configured", configured).put("connected", connected).put("pair", pair)
        .put("bid", bid).put("ask", ask).put("mid", mid)
        .put("spreadPercent", spreadPercent)
        .put("bidDepthEur", bidDepthEur).put("askDepthEur", askDepthEur)
        .put("bidLevels", JSONArray(bidLevels.map { it.toJson() }))
        .put("askLevels", JSONArray(askLevels.map { it.toJson() }))
        .put("feeRate", FusionTradingCosts.FEE_RATE).put("feeTier", FusionTradingCosts.FEE_TIER)
        .put("observedAccountFeeRate", observedAccountFeeRate ?: JSONObject.NULL)
        .put("observedAccountFeeTier", observedAccountFeeTier ?: JSONObject.NULL)
        .put("tradedVolume30dEur", tradedVolume30dEur ?: JSONObject.NULL)
        .put("feeUpdatedAt", feeUpdatedAt)
        .put("lastAttempt", lastAttempt).put("lastSuccess", lastSuccess).put("error", error)

    companion object { const val MAX_AGE = 5L * 60L * 1000L }
}

object BitpandaFusionStore {
    private const val PREFS = "bitpanda_fusion_read_only_v51"
    private const val SNAPSHOT = "snapshot"
    private const val LAST_SYNC_STARTED = "last_sync_started"
    private const val LAST_ACCOUNT_SYNC_STARTED = "last_account_sync_started_v600"
    private const val MIN_SYNC_INTERVAL = 60_000L
    private const val ACCOUNT_SYNC_INTERVAL = 6L * 60L * 60L * 1000L

    fun state(context: Context): FusionMarketSnapshot {
        val configured = BitpandaFusionSecureKeyStore.read(context).isNotBlank()
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(SNAPSHOT, null)
        return runCatching {
            val j = JSONObject(raw.orEmpty())
            FusionMarketSnapshot(
                configured = configured,
                connected = j.optBoolean("connected"), pair = j.optString("pair", "PUMP-EUR"),
                bid = j.optDouble("bid"), ask = j.optDouble("ask"), mid = j.optDouble("mid"),
                spreadPercent = j.optDouble("spreadPercent"),
                bidDepthEur = j.optDouble("bidDepthEur"), askDepthEur = j.optDouble("askDepthEur"),
                bidLevels = levels(j.optJSONArray("bidLevels")),
                askLevels = levels(j.optJSONArray("askLevels")),
                // A V6.0 install must restore the V5 control-group fee even if an early V6 build
                // briefly persisted an observed account fee into these legacy fields.
                feeRate = FusionTradingCosts.FEE_RATE,
                feeTier = FusionTradingCosts.FEE_TIER,
                observedAccountFeeRate = nullableDouble(j, "observedAccountFeeRate"),
                observedAccountFeeTier = nullableString(j, "observedAccountFeeTier"),
                tradedVolume30dEur = nullableDouble(j, "tradedVolume30dEur"),
                feeUpdatedAt = j.optLong("feeUpdatedAt"),
                lastAttempt = j.optLong("lastAttempt"), lastSuccess = j.optLong("lastSuccess"),
                error = j.optString("error")
            )
        }.getOrElse {
            FusionMarketSnapshot(configured = configured)
        }
    }

    fun canSync(context: Context, now: Long): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = p.getLong(LAST_SYNC_STARTED, 0L)
        if (now - last < MIN_SYNC_INTERVAL) return false
        p.edit().putLong(LAST_SYNC_STARTED, now).apply()
        return true
    }

    fun shouldSyncAccount(context: Context, now: Long): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return now - p.getLong(LAST_ACCOUNT_SYNC_STARTED, 0L) >= ACCOUNT_SYNC_INTERVAL
    }

    fun markAccountAttempt(context: Context, now: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(LAST_ACCOUNT_SYNC_STARTED, now).apply()
    }

    fun save(context: Context, value: FusionMarketSnapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(SNAPSHOT, value.copy(
                feeRate = FusionTradingCosts.FEE_RATE,
                feeTier = FusionTradingCosts.FEE_TIER
            ).toJson().toString()).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun levels(array: JSONArray?): List<FusionBookLevel> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { FusionBookLevel.fromJson(array.optJSONObject(it)) }
    }

    private fun nullableDouble(j: JSONObject, key: String): Double? =
        if (!j.has(key) || j.isNull(key)) null else j.optDouble(key).takeIf(Double::isFinite)

    private fun nullableString(j: JSONObject, key: String): String? =
        if (!j.has(key) || j.isNull(key)) null else j.optString(key).takeIf(String::isNotBlank)
}

/** Read-only Fusion REST client. Deliberately contains GET requests only. */
class BitpandaFusionClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()

    fun sync(context: Context, force: Boolean = false): FusionMarketSnapshot {
        val key = BitpandaFusionSecureKeyStore.read(context)
        if (key.isBlank()) return FusionMarketSnapshot(configured = false)
        val now = System.currentTimeMillis()
        if (!force && !BitpandaFusionStore.canSync(context, now)) return BitpandaFusionStore.state(context)
        val previous = BitpandaFusionStore.state(context)
        val next = runCatching {
            val book = getJson("$BASE/v1/orderbook/$PAIR?depth=20", key)
            var snapshot = parseOrderbook(book, now, previous)
            if (BitpandaFusionStore.shouldSyncAccount(context, now)) {
                BitpandaFusionStore.markAccountAttempt(context, now)
                snapshot = runCatching {
                    parseAccount(getJson("$BASE/v1/account", key), now, snapshot)
                }.getOrElse { snapshot }
            }
            snapshot
        }.getOrElse { error ->
            previous.copy(
                configured = true, connected = false,
                feeRate = FusionTradingCosts.FEE_RATE,
                feeTier = FusionTradingCosts.FEE_TIER,
                lastAttempt = now,
                error = safeError(error)
            )
        }
        BitpandaFusionStore.save(context, next)
        UnifiedResearchLog.record(
            context,
            "BITPANDA_FUSION",
            if (next.connected) "OK" else "ERROR",
            if (next.connected) {
                val observed = next.observedAccountFeeRate?.let {
                    "account=${next.v6ObservedFeeTier()} ${String.format(java.util.Locale.US, "%.3f", it * 100.0)}%"
                } ?: "account fee ещё не получена"
                "Получен read-only стакан ${next.pair}; V5 fee=${FusionTradingCosts.FEE_TIER}; $observed; торговые команды отключены"
            } else next.error
        )
        return next
    }

    private fun getJson(url: String, key: String): JSONObject {
        val request = Request.Builder().url(url).header("x-api-key", key).get().build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("HTTP ${response.code}: ${body.take(180)}")
            return JSONObject(body)
        }
    }

    companion object {
        const val BASE = "https://api.fusion.bitpanda.com"
        const val PAIR = "PUMP-EUR"

        internal fun parseOrderbook(
            json: JSONObject,
            now: Long,
            previous: FusionMarketSnapshot = FusionMarketSnapshot(configured = true)
        ): FusionMarketSnapshot {
            val bids = json.optJSONArray("bids") ?: JSONArray()
            val asks = json.optJSONArray("asks") ?: JSONArray()
            val bidLevels = parseLevels(bids)
            val askLevels = parseLevels(asks)
            val bid = bidLevels.firstOrNull()?.price ?: 0.0
            val ask = askLevels.firstOrNull()?.price ?: 0.0
            require(bid > 0.0 && ask >= bid) { "Некорректный стакан PUMP-EUR" }
            val mid = (bid + ask) / 2.0
            return previous.copy(
                configured = true,
                connected = true,
                pair = json.optString("pair", PAIR),
                bid = bid,
                ask = ask,
                mid = mid,
                spreadPercent = if (mid > 0.0) (ask - bid) / mid * 100.0 else 0.0,
                bidDepthEur = bidLevels.sumOf { it.notionalEur },
                askDepthEur = askLevels.sumOf { it.notionalEur },
                bidLevels = bidLevels,
                askLevels = askLevels,
                feeRate = FusionTradingCosts.FEE_RATE,
                feeTier = FusionTradingCosts.FEE_TIER,
                lastAttempt = now,
                lastSuccess = now,
                error = ""
            )
        }

        /** Fusion /v1/account returns fee as percentage points, e.g. 0.15 means 0.15%. */
        internal fun parseAccount(
            json: JSONObject,
            now: Long,
            previous: FusionMarketSnapshot
        ): FusionMarketSnapshot {
            val tier = json.optJSONObject("current_tier") ?: return previous
            val feePercent = tier.number("fee")
            val mode = tier.optString("fee_mode", "Percentage")
            val rate = if (mode.equals("Percentage", ignoreCase = true) && feePercent != null) {
                (feePercent / 100.0).takeIf { it.isFinite() && it in 0.0..0.05 }
            } else null
            val volume = json.number("traded_volume30d")
            return previous.copy(
                feeRate = FusionTradingCosts.FEE_RATE,
                feeTier = FusionTradingCosts.FEE_TIER,
                observedAccountFeeRate = rate ?: previous.observedAccountFeeRate,
                observedAccountFeeTier = tier.optString("name", previous.observedAccountFeeTier.orEmpty())
                    .takeIf(String::isNotBlank) ?: previous.observedAccountFeeTier,
                tradedVolume30dEur = volume ?: previous.tradedVolume30dEur,
                feeUpdatedAt = if (rate != null || volume != null) now else previous.feeUpdatedAt
            )
        }

        private fun parseLevels(array: JSONArray): List<FusionBookLevel> =
            (0 until array.length()).mapNotNull { index ->
                val item = array.opt(index)
                val price = when (item) {
                    is JSONObject -> item.number("price")
                    is JSONArray -> item.number(0)
                    else -> null
                }
                val quantity = when (item) {
                    is JSONObject -> item.number("quantity") ?: item.number("size")
                    is JSONArray -> item.number(1)
                    else -> null
                }
                if (price != null && quantity != null && price > 0.0 && quantity > 0.0) {
                    FusionBookLevel(price, quantity)
                } else null
            }

        private fun JSONObject.number(key: String): Double? = when (val raw = opt(key)) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull()
            else -> null
        }?.takeIf(Double::isFinite)

        private fun JSONArray.number(index: Int): Double? = when (val raw = opt(index)) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull()
            else -> null
        }?.takeIf(Double::isFinite)

        private fun safeError(error: Throwable): String = error.message.orEmpty()
            .replace(Regex("(?i)(x-api-key|api[_ -]?key)[^,;\\s]*"), "API_KEY=[СКРЫТО]")
            .take(260).ifBlank { "Ошибка read-only соединения" }
    }
}
