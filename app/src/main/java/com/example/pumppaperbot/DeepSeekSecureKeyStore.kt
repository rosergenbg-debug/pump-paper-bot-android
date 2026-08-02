package com.example.pumppaperbot

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.concurrent.TimeUnit

/** Android-Keystore-backed storage for the user supplied DeepSeek API key. */
object DeepSeekSecureKeyStore {
    private const val PREFS = "secure_deepseek_credentials_v4"
    private const val KEY_CIPHERTEXT = "api_key_ciphertext"
    private const val KEY_IV = "api_key_iv"
    private const val KEY_ALIAS = "pump_signal_deepseek_api_key_v4"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    @Volatile private var cachedValue: String? = null

    fun read(context: Context): String {
        cachedValue?.let { return it }
        val loaded = runCatching {
            val encrypted = prefs(context).getString(KEY_CIPHERTEXT, "").orEmpty()
            val iv = prefs(context).getString(KEY_IV, "").orEmpty()
            if (encrypted.isBlank() || iv.isBlank()) return@runCatching ""
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            )
            String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8).trim()
        }.getOrDefault("")
        cachedValue = loaded
        return loaded
    }

    fun save(context: Context, value: String): Boolean = runCatching {
        val clean = value.trim()
        if (clean.isBlank()) {
            prefs(context).edit().clear().commit()
            cachedValue = ""
            return@runCatching true
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
        prefs(context).edit()
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .commit()
            .also { saved -> if (saved) cachedValue = clean }
    }.getOrDefault(false)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

data class DeepSeekConnectionStatus(
    val lastSuccess: Long = 0L,
    val models: String = "",
    val error: String = ""
)

object DeepSeekConnectionStore {
    private const val PREFS = "deepseek_connection_v4"

    fun state(context: Context): DeepSeekConnectionStatus {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return DeepSeekConnectionStatus(
            lastSuccess = p.getLong("last_success", 0L),
            models = p.getString("models", "").orEmpty(),
            error = p.getString("error", "").orEmpty()
        )
    }

    fun success(context: Context, models: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("last_success", System.currentTimeMillis())
            .putString("models", models.take(300))
            .putString("error", "")
            .apply()
    }

    fun failure(context: Context, error: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("error", error.take(300)).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

/** Free /models verification; it does not send market or account data. */
class DeepSeekKeyVerifier {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun verify(context: Context, apiKey: String) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.deepseek.com/models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching {
                        JSONObject(body).optJSONObject("error")?.optString("message")
                    }.getOrNull().orEmpty().ifBlank { "DeepSeek HTTP ${response.code}" }
                    error(message)
                }
                val array = JSONObject(body).optJSONArray("data")
                val models = if (array == null) "модели доступны" else {
                    (0 until array.length()).mapNotNull { index ->
                        array.optJSONObject(index)?.optString("id")?.takeIf(String::isNotBlank)
                    }.joinToString(", ").ifBlank { "модели доступны" }
                }
                DeepSeekConnectionStore.success(context, models)
            }
        }.onFailure { DeepSeekConnectionStore.failure(context, it.message ?: "ошибка проверки") }
    }
}
