package com.example.pumppaperbot

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import java.util.WeakHashMap
import kotlin.math.roundToInt

/**
 * V6.9.3 bootstrap for the BTC→PUMP hold/release research panel.
 *
 * This is intentionally independent of the app's Application subclass so the panel cannot be
 * silently skipped by wiring the helper to an Application class that is not declared in the
 * manifest. Presentation only: no entry/exit/account/alert logic is touched.
 */
class BtcPumpReleaseBootstrapProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val app = context?.applicationContext as? Application ?: return true
        app.registerActivityLifecycleCallbacks(BtcPumpReleaseLifecycle)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}

internal object BtcPumpReleaseLifecycle : Application.ActivityLifecycleCallbacks {
    private const val REFRESH_MILLIS = 10_000L
    private val updaters = WeakHashMap<Activity, Runnable>()

    override fun onActivityResumed(activity: Activity) {
        if (activity !is MainActivity) return
        val decor = activity.window?.decorView ?: return
        // The real V513Application also posts its legacy BTC/money-flow injectors on resume.
        // Double-post lets those existing views settle first, then places our panel between BTC and PUMP.
        decor.post {
            decor.post {
                start(activity)
            }
        }
    }

    override fun onActivityPaused(activity: Activity) = stop(activity)
    override fun onActivityDestroyed(activity: Activity) = stop(activity)

    private fun start(activity: MainActivity) {
        stop(activity)
        val chart = activity.findViewById<StrategyChartView>(R.id.chart) ?: return
        val gauge = installImmediatelyBeforeMainPumpChart(chart) ?: return
        var nextHeavyRefreshAt = 0L
        val updater = object : Runnable {
            override fun run() {
                if (activity.isFinishing || activity.isDestroyed || !gauge.isAttachedToWindow) return
                val now = System.currentTimeMillis()
                if (now >= nextHeavyRefreshAt) {
                    gauge.setData(
                        BtcPumpReleasePolicy.evaluate(
                            BtcPumpReleaseLiveSource.recentMinuteSamples(activity, now),
                            now
                        )
                    )
                    nextHeavyRefreshAt = now + REFRESH_MILLIS
                }
                gauge.postDelayed(this, 2_000L)
            }
        }
        updaters[activity] = updater
        gauge.post(updater)
    }

    /**
     * The existing V5.13 injector places the small 24h BTC chart immediately before the PUMP chart.
     * Inserting at the current PUMP-chart index therefore puts this new panel directly under BTC
     * and directly above PUMP, which is the intended visual relationship.
     */
    private fun installImmediatelyBeforeMainPumpChart(chart: StrategyChartView): BtcPumpReleaseGaugeView? {
        val parent = chart.parent as? ViewGroup ?: return null
        parent.findViewWithTag<BtcPumpReleaseGaugeView>(BtcPumpReleaseGaugeView.VIEW_TAG)?.let { existing ->
            // If an earlier runtime inserted it somewhere else, move it to the intended location.
            val chartIndex = parent.indexOfChild(chart)
            val existingIndex = parent.indexOfChild(existing)
            if (chartIndex >= 0 && existingIndex >= 0 && existingIndex != chartIndex - 1) {
                parent.removeView(existing)
                val newChartIndex = parent.indexOfChild(chart)
                parent.addView(existing, newChartIndex.coerceAtLeast(0))
            }
            return existing
        }

        val view = BtcPumpReleaseGaugeView(chart.context).apply {
            contentDescription = "BTC PUMP удержание, стабильность Bitcoin и возможное отпускание"
        }
        val height = (330f * chart.resources.displayMetrics.density).roundToInt()
        val margin = (8f * chart.resources.displayMetrics.density).roundToInt()
        val params = if (parent is LinearLayout) {
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply {
                topMargin = margin
                bottomMargin = margin
            }
        } else {
            ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply {
                topMargin = margin
                bottomMargin = margin
            }
        }
        val chartIndex = parent.indexOfChild(chart)
        if (chartIndex < 0) return null
        parent.addView(view, chartIndex, params)
        return view
    }

    private fun stop(activity: Activity) {
        val updater = updaters.remove(activity) ?: return
        activity.window?.decorView
            ?.findViewWithTag<BtcPumpReleaseGaugeView>(BtcPumpReleaseGaugeView.VIEW_TAG)
            ?.removeCallbacks(updater)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
