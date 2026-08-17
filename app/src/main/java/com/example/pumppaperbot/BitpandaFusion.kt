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
import kotlin.math.abs

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
    val feeRate: Double = GeminiPaperTrader.FEE_RATE,
    val feeTier: String = "резерв 0,15%",
    val lastAttempt: Long = 0L,
    val lastSuccess: Long = 0L,
    val error: String = ""
) {
    fun fresh(now: Long = System.currentTimeMillis()): Boolean =
        connected && lastSuccess > 0L && now - lastSuccess in 0..MAX_AGE

    fun toJson(): JSONObject = JSONObject()
        .put("configured", configured).put("connected", connected).put("pair", pair)
        .put("bid", bid).put("ask", ask).put("mid", mid)
        .put("spreadPercent", spreadPercent)
        .put("bidDepthEur", bidDepthEur).put("askDepthEur", askDepthEur)
        .put("feeRate", feeRate).put("feeTier", feeTier)
        .put("lastAttempt", lastAttempt).put("lastSuccess", lastSuccess).put("error", error)

    companion object { const val MAX_AGE = 5L * 60L * 1000L }
}

object BitpandaFusionStore {
    private const val PREFS = "bitpanda_fusion_read_only_v51"
    private const val SNAPSHOT = "snapshot"
    private const val LAST_SYNC_STARTED = "last_sync_started"
    private const val MIN_SYNC_INTERVAL = 60_000L

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
                feeRate = j.optDouble("feeRate", GeminiPaperTrader.FEE_RATE),
                feeTier = j.optString("feeTier", "резерв 0,15%"),
                lastAttempt = j.optLong("lastAttempt"), lastSuccess = j.optLong("lastSuccess"),
                error = j.optString("error")
            )
        }.getOrElse { FusionMarketSnapshot(configured = configured) }
    }

    fun canSync(context: Context, now: Long): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = p.getLong(LAST_SYNC_STARTED, 0L)
        if (now - last < MIN_SYNC_INTERVAL) return false
        p.edit().putLong(LAST_SYNC_STARTED, now).apply()
        return true
    }

    fun save(context: Context, value: FusionMarketSnapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(SNAPSHOT, value.toJson().toString()).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
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
            parseOrderbook(book, now, previous)
        }.getOrElse { error ->
            previous.copy(
                configured = true, connected = false, lastAttempt = now,
                error = safeError(error)
            )
        }
        BitpandaFusionStore.save(context, next)
        UnifiedResearchLog.record(context, "BITPANDA_FUSION", if (next.connected) "OK" else "ERROR",
            if (next.connected) "Получен read-only стакан ${next.pair}; торговые команды отключены"
            else next.error)
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
            fun price(item: Any?): Double = when (item) {
                is JSONObject -> item.optDouble("price")
                is JSONArray -> item.optDouble(0)
                else -> 0.0
            }
            fun quantity(item: Any?): Double = when (item) {
                is JSONObject -> item.optDouble("quantity", item.optDouble("size"))
                is JSONArray -> item.optDouble(1)
                else -> 0.0
            }
            val bid = price(bids.opt(0))
            val ask = price(asks.opt(0))
            require(bid > 0.0 && ask >= bid) { "Некорректный стакан PUMP-EUR" }
            fun depth(side: JSONArray): Double = (0 until side.length()).sumOf { i ->
                val item = side.opt(i)
                price(item) * quantity(item)
            }
            val mid = (bid + ask) / 2.0
            return previous.copy(
                configured = true, connected = true,
                pair = json.optString("pair", PAIR), bid = bid, ask = ask, mid = mid,
                spreadPercent = if (mid > 0.0) (ask - bid) / mid * 100.0 else 0.0,
                bidDepthEur = depth(bids), askDepthEur = depth(asks),
                lastAttempt = now, lastSuccess = now, error = ""
            )
        }

        private fun safeError(error: Throwable): String = error.message.orEmpty()
            .replace(Regex("(?i)(x-api-key|api[_ -]?key)[^,;\\s]*"), "API_KEY=[СКРЫТО]")
            .take(260).ifBlank { "Ошибка read-only соединения" }
    }
}
