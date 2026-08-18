package com.example.pumppaperbot

import android.content.Context

/** Prevents one optional participant from cancelling every later participant in a cycle. */
object CycleStageGuard {
    fun <T> run(
        context: Context,
        stage: String,
        fallback: () -> T,
        block: () -> T
    ): T = try {
        block()
    } catch (error: Exception) {
        val detail = "$stage продолжит работу в следующем цикле; остальные модули не остановлены: " +
            (error.message ?: error.javaClass.simpleName).take(360)
        runCatching { UnifiedResearchLog.record(context, stage, "ERROR_ISOLATED", detail) }
        runCatching {
            GeminiPaperStore.recordActivity(context, "ИЗОЛЯЦИЯ МОДУЛЯ", "ERROR", detail)
        }
        fallback()
    }
}
