package com.example.pumppaperbot

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import java.util.WeakHashMap

/**
 * Sequential presentation layer retained from V5.13.1+.
 * V5.18 adds only a visual money-flow strip; trading logic is not changed here.
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
                        V513MainUiInjector.install(activity)
                        startMainChartPresentation(activity)
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

    /** Main-chart flow and V5.18 money strip share one cheap two-second presentation refresh. */
    private fun startMainChartPresentation(activity: MainActivity) {
        stopMainChartPresentation(activity)
        val chart = activity.findViewById<StrategyChartView>(R.id.chart) ?: return
        chart.setMainViewportMode(true)
        val updater = object : Runnable {
            override fun run() {
                if (activity.isFinishing || activity.isDestroyed || !chart.isAttachedToWindow) return
                val now = System.currentTimeMillis()
                val breathing = LiveMarketBreathingStore.snapshot(activity, now)
                chart.setFlowScores(MainChartFlowPresentation.from(breathing))
                V513MainUiInjector.updateMoneyFlow(
                    activity,
                    MoneyFlowPresentation.from(MicroImpulseStore.state(activity), breathing, now)
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

        installMoneyFlowStrip(activity)
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

    fun updateMoneyFlow(activity: Activity, data: MoneyFlowPanelData) {
        activity.window?.decorView
            ?.findViewWithTag<MoneyFlowStripView>(MoneyFlowStripView.VIEW_TAG)
            ?.setData(data)
    }

    private fun installMoneyFlowStrip(activity: MainActivity) {
        val root = activity.findViewById<View>(R.id.tvLatestSignal)?.parent as? LinearLayout ?: return
        if (root.findViewWithTag<MoneyFlowStripView>(MoneyFlowStripView.VIEW_TAG) != null) return
        val anchor = activity.findViewById<View>(R.id.tvLatestSignal) ?: return
        val index = root.indexOfChild(anchor)
        if (index < 0) return
        val strip = MoneyFlowStripView(activity).apply {
            minimumHeight = dp(activity, 104)
            contentDescription = "Денежный поток за одну, пять и пятнадцать минут"
        }
        root.addView(
            strip,
            index,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(activity, 104)
            ).apply { topMargin = dp(activity, 7) }
        )
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
