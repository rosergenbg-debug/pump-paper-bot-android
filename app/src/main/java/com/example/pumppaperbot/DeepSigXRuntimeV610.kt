package com.example.pumppaperbot

import android.content.Context

/**
 * V6.1 explicit runtime tick for the independent DeepSigX paper account.
 *
 * The historical implementation lives in Gemini-named classes. Keeping this adapter separate
 * makes the ownership visible: DeepSigX is an autonomous research participant and is not a side
 * effect of the primary DeepSeek paper account. Failure is isolated by the caller's CycleStageGuard.
 */
object DeepSigXRuntimeV610 {
    fun sync(
        context: Context,
        deepSeek: DeepSeekPrimaryState = DeepSeekPrimaryStore.state(context),
        now: Long = System.currentTimeMillis()
    ): GeminiExitExperimentState? {
        val frame = GeminiMarketFrame.from(context) ?: return GeminiExitExperimentStore.state(context)
        val evaluationAt = frame.snapshot.lastSync.takeIf { it > 0L } ?: now
        return GeminiExitExperimentStore.evaluate(
            context = context,
            controlPortfolio = GeminiPaperStore.state(context).portfolio,
            deepSeekDecision = deepSeek,
            frame = frame,
            impulse = ImpulseRadarStore.state(context),
            appEvaluation = PumpBotEngine.evaluateAppPaper(context, AppPaperStore.state(context)),
            now = evaluationAt
        )
    }
}
