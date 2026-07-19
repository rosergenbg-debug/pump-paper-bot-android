package com.example.pumppaperbot

import android.util.Base64

/** Personal-build default. A key entered in the app overrides this value. */
internal object EmbeddedGeminiKey {
    private const val encoded = "GtGLi40saxC15KKRcFAgtviTmHdGAe7ng59sCzPpga6tEiABuNiSiw5xNOjtjVNwDxel/6g="

    val value: String by lazy {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        bytes.indices.forEach { index ->
            bytes[index] = (bytes[index].toInt() xor ((index * 37 + 91) and 0xff)).toByte()
        }
        bytes.toString(Charsets.UTF_8)
    }
}
