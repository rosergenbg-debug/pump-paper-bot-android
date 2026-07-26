package com.example.pumppaperbot

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small Keystore wrapper for the user-supplied Gemini key.
 * It avoids the deprecated EncryptedSharedPreferences API.
 */
object GeminiSecureKeyStore {
    private const val PREFS = "secure_gemini_credentials"
    private const val KEY_CIPHERTEXT = "gemini_api_key_ciphertext"
    private const val KEY_IV = "gemini_api_key_iv"
    private const val KEY_ALIAS = "pump_signal_gemini_api_key_v37"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    @Volatile private var cachedValue: String? = null

    fun read(context: Context): String {
        cachedValue?.let { return it }
        val loaded = runCatching {
            val prefs = prefs(context)
            val encrypted = prefs.getString(KEY_CIPHERTEXT, "").orEmpty()
            val iv = prefs.getString(KEY_IV, "").orEmpty()
            if (encrypted.isBlank() || iv.isBlank()) return@runCatching ""
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            )
            String(
                cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)),
                Charsets.UTF_8
            ).trim()
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
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
