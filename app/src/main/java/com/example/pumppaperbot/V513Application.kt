package com.example.pumppaperbot

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.WeakHashMap

/**
 * Sequential presentation layer retained from V5.13.1+.
 * V5.18 adds visual money-flow surfaces only; trading logic is not changed here.
 */
class V513Application : Application() {
    private val mainChartUpdaters = WeakHashMap<Activity, Runnable>()
    private val bigMoneyUpdaters = WeakHashMap<Activity, Runnable>()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                activity.window?.decorView?.post {
                    StableScrollGuard.attach(activity.window.decorView)
                    when (activity) {
                        is MainActivity -> {
                            V513MainUiInjector.install(activity)
                            startMainChartPresentation(activity)
                        }
                        is BigOverviewActivity -> {
                            V518BigOverviewInjector.install(activity)
                            startBigMoneyPresentation(activity)
                        }
                    }
                }
            }

            override fun onActivityPaused(activity: Activity) {
                stopMainChartPresentation(activity)
                stopBigMoneyPresentation(activity)
            }

            override fun onActivityDestroyed(activity: Activity) {
                stopMainChartPresentation(activity)
                stopBigMoneyPresentation(activity)
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
        MainChartRangeGuideOverlay.install(chart)
        val updater = object : Runnable {
            override fun run() {
                if (activity.isFinishing || activity.isDestroyed || !chart.isAttachedToWindow) return
                val now = System.currentTimeMillis()
                val breathing = LiveMarketBreathingStore.snapshot(activity, now)
                val micro = MicroImpulseStore.state(activity)
                chart.setFlowScores(MainChartFlowPresentation.from(breathing))
                V513MainUiInjector.updateMoneyFlow(
                    activity,
                    MoneyFlowPresentation.from(micro, breathing, now)
                )
                V513MainUiInjector.updateBtc(
                    activity,
                    BtcMiniPresentation.from(
                        PumpBotEngine.btcCandles(activity),
                        micro.bitcoinPriceUsdt.takeIf { it > 0.0 },
                        micro.bitcoinUpdatedAt,
                        now
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

    private fun startBigMoneyPresentation(activity: BigOverviewActivity) {
        stopBigMoneyPresentation(activity)
        val dial = activity.window?.decorView
            ?.findViewWithTag<MoneyMassDialView>(MoneyMassDialView.VIEW_TAG) ?: return
        val updater = object : Runnable {
            override fun run() {
                if (activity.isFinishing || activity.isDestroyed || !dial.isAttachedToWindow) return
                val now = System.currentTimeMillis()
                val breathing = LiveMarketBreathingStore.snapshot(activity, now)
                V518BigOverviewInjector.update(
                    activity,
                    MoneyFlowPresentation.from(MicroImpulseStore.state(activity), breathing, now)
                )
                dial.postDelayed(this, 2_000L)
            }
        }
        bigMoneyUpdaters[activity] = updater
        dial.post(updater)
    }

    private fun stopBigMoneyPresentation(activity: Activity) {
        val updater = bigMoneyUpdaters.remove(activity) ?: return
        activity.window?.decorView
            ?.findViewWithTag<MoneyMassDialView>(MoneyMassDialView.VIEW_TAG)
            ?.removeCallbacks(updater)
    }
}

internal object V513MainUiInjector {
    private const val ROW_TAG = "v513_big_overview_row"

    fun install(activity: Activity) {
        if (activity !is MainActivity) return

        installMoneyFlowStrip(activity)
        installBtcMiniChart(activity)
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

    fun updateBtc(activity: Activity, data: BtcMiniChartData) {
        activity.window?.decorView
            ?.findViewWithTag<BtcMiniChartView>(BtcMiniChartView.VIEW_TAG)
            ?.setData(data)
    }

    private fun installBtcMiniChart(activity: MainActivity) {
        val chart = activity.findViewById<StrategyChartView>(R.id.chart) ?: return
        val root = chart.parent as? LinearLayout ?: return
        if (root.findViewWithTag<BtcMiniChartView>(BtcMiniChartView.VIEW_TAG) != null) return
        val index = root.indexOfChild(chart)
        if (index < 0) return
        root.addView(
            BtcMiniChartView(activity).apply {
                contentDescription = "График Bitcoin за 24 часа и изменения за 2, 6 и 24 часа"
            },
            index,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(activity, 132)
            ).apply {
                topMargin = dp(activity, 8)
                bottomMargin = dp(activity, 2)
            }
        )
    }

    private fun installMoneyFlowStrip(activity: MainActivity) {
        val chart = activity.findViewById<StrategyChartView>(R.id.chart) ?: return
        val root = chart.parent as? LinearLayout ?: return
        if (root.findViewWithTag<MoneyFlowStripView>(MoneyFlowStripView.VIEW_TAG) != null) return
        val index = root.indexOfChild(chart)
        if (index < 0) return
        val strip = MoneyFlowStripView(activity).apply {
            minimumHeight = dp(activity, 215)
            contentDescription = "Денежный поток сейчас, за пять, пятнадцать, тридцать и шестьдесят минут"
        }
        root.addView(
            strip,
            (index + 1).coerceAtMost(root.childCount),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(activity, 215)
            ).apply {
                topMargin = dp(activity, 8)
                bottomMargin = dp(activity, 2)
            }
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

internal object V518BigOverviewInjector {
    private const val SECTION_TAG = "v518_money_mass_section"
    private const val SUMMARY_TAG = "v518_money_mass_summary"

    fun install(activity: BigOverviewActivity) {
        val decor = activity.window?.decorView ?: return
        if (decor.findViewWithTag<View>(SECTION_TAG) != null) return
        val scroll = findScrollView(decor) ?: return
        val content = scroll.getChildAt(0) as? LinearLayout ?: return
        var chartIndex = -1
        for (index in 0 until content.childCount) {
            if (content.getChildAt(index) is StrategyChartView) {
                chartIndex = index
                break
            }
        }
        if (chartIndex < 0) return

        val section = LinearLayout(activity).apply {
            tag = SECTION_TAG
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 0), dp(activity, 4), dp(activity, 0), dp(activity, 4))
        }
        section.addView(TextView(activity).apply {
            text = "ДЕНЕЖНЫЙ ПОТОК • СЕЙЧАС / 5 / 15 / 30 / 60 МИН"
            setTextColor(Color.parseColor("#F0F6FC"))
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        section.addView(
            MoneyMassDialView(activity).apply {
                contentDescription = "Круг денежного потока: доли покупок и продаж за пять минут"
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 275)).apply {
                topMargin = dp(activity, 5)
            }
        )
        section.addView(TextView(activity).apply {
            tag = SUMMARY_TAG
            text = "НАКАПЛИВАЕМ ДЕНЕЖНЫЙ ПОТОК"
            setTextColor(Color.parseColor("#C9D1D9"))
            setBackgroundColor(Color.parseColor("#161B22"))
            textSize = 13f
            setPadding(dp(activity, 10), dp(activity, 9), dp(activity, 10), dp(activity, 9))
        })

        val insertIndex = (chartIndex + 2).coerceAtMost(content.childCount)
        content.addView(
            section,
            insertIndex,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(activity, 10) }
        )
    }

    fun update(activity: BigOverviewActivity, data: MoneyFlowPanelData) {
        val decor = activity.window?.decorView ?: return
        decor.findViewWithTag<MoneyMassDialView>(MoneyMassDialView.VIEW_TAG)?.setData(data)
        decor.findViewWithTag<TextView>(SUMMARY_TAG)?.text = MoneyFlowPresentation.summary(data)
    }

    private fun findScrollView(view: View): ScrollView? {
        if (view is ScrollView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findScrollView(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}

// Compatibility alias for the compact V5.13 presentation helper.
typealias MicroImpulseState = MicroImpulseSnapshot


/** V5.23 one-shot reset implementation retained but intentionally dormant in V5.24. */
internal object CleanFusionLabResetV523 {
    private const val MARKER_PREFS = "v523_clean_fusion_lab_reset"
    private const val DONE = "done"
    private const val PUMP_MACHINE_PREFS = "pump_machine_paper_v521"

    @Synchronized
    fun ensure(context: android.content.Context) {
        val marker = context.getSharedPreferences(MARKER_PREFS, android.content.Context.MODE_PRIVATE)
        if (marker.getBoolean(DONE, false)) return

        FusionSimStore.reset(context)
        context.getSharedPreferences(PUMP_MACHINE_PREFS, android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()

        marker.edit().putBoolean(DONE, true).commit()
        UnifiedResearchLog.record(
            context,
            "V523_LAB",
            "START",
            "Fusion и Pump Machine сброшены один раз: €1000 / 0 сделок / чистые entry-cooldown состояния"
        )
    }
}
