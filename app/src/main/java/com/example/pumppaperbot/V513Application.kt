package com.example.pumppaperbot

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import java.util.WeakHashMap

/**
 * V5.13.1 is strictly additive over V5.12/V5.13:
 * - keeps the V5.12 stable-scroll guard;
 * - keeps the V5.12 live main-chart four-flow updater;
 * - adds only the V5.13 big-overview entry point.
 */
class V513Application : Application() {
    private val mainChartUpdaters = WeakHashMap<Activity, Runnable>()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                activity.window?.decorView?.post {
                    StableScrollGuard.attach(activity.window.decorView)
                    if (activity is MainActivity) {
                        startMainChartPresentation(activity)
                        V513MainUiInjector.install(activity)
                    }
                }
            }

            override fun onActivityPaused(activity: Activity) {
                stopMainChartPresentation(activity)
            }

            override fun onActivityDestroyed(activity: Activity) {
                stopMainChartPresentation(activity)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        })
    }

    /** Exact V5.12 presentation behavior: MГН / 5м / 15м / 30м stay live on the main chart. */
    private fun startMainChartPresentation(activity: MainActivity) {
        stopMainChartPresentation(activity)
        val chart = activity.findViewById<StrategyChartView>(R.id.chart) ?: return
        chart.setMainViewportMode(true)
        val updater = object : Runnable {
            override fun run() {
                if (activity.isFinishing || activity.isDestroyed || !chart.isAttachedToWindow) return
                chart.setFlowScores(
                    MainChartFlowPresentation.from(
                        LiveMarketBreathingStore.snapshot(activity, System.currentTimeMillis())
                    )
                )
                chart.postDelayed(this, 2_000L)
            }
        }
        mainChartUpdaters[activity] = updater
        chart.post(updater)
    }

    private fun stopMainChartPresentation(activity: Activity) {
        val updater = mainChartUpdaters.remove(activity) ?: return
        activity.findViewById<StrategyChartView>(R.id.chart)?.removeCallbacks(updater)
    }
}

internal object V513MainUiInjector {
    private const val ROW_TAG = "v513_big_overview_row"

    fun install(activity: Activity) {
        if (activity !is MainActivity) return

        activity.findViewById<StrategyChartView>(R.id.chart)?.setOnClickListener {
            open(activity, false)
        }

        val speed = activity.findViewById<Button>(R.id.btnChartSpeed) ?: return
        val currentParent = speed.parent as? LinearLayout ?: return
        if (currentParent.tag == ROW_TAG) return

        val original = speed.layoutParams as? LinearLayout.LayoutParams
        val parent = currentParent
        val index = parent.indexOfChild(speed)
        if (index < 0) return
        parent.removeView(speed)

        val row = LinearLayout(activity).apply {
            tag = ROW_TAG
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val rowHeight = original?.height?.takeIf { it > 0 } ?: dp(activity, 64)
        val rowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            rowHeight
        ).apply {
            leftMargin = original?.leftMargin ?: 0
            topMargin = original?.topMargin ?: dp(activity, 7)
            rightMargin = original?.rightMargin ?: 0
            bottomMargin = original?.bottomMargin ?: 0
        }

        speed.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 3.2f)
        row.addView(speed)

        val big = Button(activity).apply {
            text = "×2\nОБЗОР"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#8250DF"))
            textSize = 13f
            isAllCaps = false
            gravity = Gravity.CENTER
            maxLines = 2
            setPadding(dp(activity, 2), 0, dp(activity, 2), 0)
            setOnClickListener { open(activity, true) }
        }
        row.addView(
            big,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                leftMargin = dp(activity, 6)
            }
        )
        parent.addView(row, index, rowParams)
    }

    private fun open(activity: Activity, zoomed: Boolean) {
        activity.startActivity(
            Intent(activity, BigOverviewActivity::class.java)
                .putExtra(BigOverviewActivity.EXTRA_OPEN_ZOOMED, zoomed)
        )
    }

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}

// Compatibility alias for the compact V5.13 presentation helper.
typealias MicroImpulseState = MicroImpulseSnapshot
