package com.example.pumppaperbot

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlin.math.abs

/**
 * Keeps data-heavy screens visually stable while also wiring presentation-only live helpers.
 * Trading/decision logic is deliberately not touched here.
 */
class StableScrollApplication : Application() {
    private val mainChartUpdaters = WeakHashMap<Activity, Runnable>()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                activity.window?.decorView?.post {
                    StableScrollGuard.attach(activity.window.decorView)
                    if (activity is MainActivity) startMainChartPresentation(activity)
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

    private fun startMainChartPresentation(activity: MainActivity) {
        stopMainChartPresentation(activity)
        val chart = activity.findViewById<StrategyChartView>(R.id.chart) ?: return
        chart.setMainViewportMode(true)
        MainChartRangeGuideOverlay.install(chart)
        // V6.9.2: additive, presentation-only BTC→PUMP context. No existing view is moved or hidden.
        val releaseGauge = BtcPumpReleasePanelInstaller.install(chart)
        var lastReleaseRefreshAt = 0L
        val updater = object : Runnable {
            override fun run() {
                if (activity.isFinishing || activity.isDestroyed || !chart.isAttachedToWindow) return
                val now = System.currentTimeMillis()
                chart.setFlowScores(
                    MainChartFlowPresentation.from(
                        LiveMarketBreathingStore.snapshot(activity, now)
                    )
                )
                // Tail CSV parsing is intentionally throttled; the existing 2s chart animation stays light.
                if (releaseGauge != null && now - lastReleaseRefreshAt >= 10_000L) {
                    releaseGauge.setData(
                        BtcPumpReleasePolicy.evaluate(
                            BtcPumpReleaseLiveSource.recentMinuteSamples(activity, now),
                            now
                        )
                    )
                    lastReleaseRefreshAt = now
                }
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

internal object StableScrollGuard {
    private data class State(
        var anchor: WeakReference<View>? = null,
        var anchorOffset: Int = 0,
        var touching: Boolean = false,
        var settlingUntil: Long = 0L,
        var restoring: Boolean = false
    )

    private val states = WeakHashMap<ScrollView, State>()

    fun attach(root: View) {
        visit(root) { scroll ->
            if (states.containsKey(scroll)) return@visit
            val state = State()
            states[scroll] = state
            scroll.isFocusableInTouchMode = true
            capture(scroll, state)

            scroll.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> state.touching = true
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        state.touching = false
                        state.settlingUntil = SystemClock.uptimeMillis() + 900L
                        scroll.postDelayed({ capture(scroll, state) }, 950L)
                    }
                }
                false
            }
            scroll.setOnScrollChangeListener { _, _, _, _, _ ->
                if (!state.restoring && (state.touching || SystemClock.uptimeMillis() < state.settlingUntil)) {
                    capture(scroll, state)
                }
            }
            scroll.viewTreeObserver.addOnGlobalLayoutListener {
                val userMoving = state.touching || SystemClock.uptimeMillis() < state.settlingUntil
                if (userMoving) {
                    capture(scroll, state)
                    return@addOnGlobalLayoutListener
                }
                val anchor = state.anchor?.get()
                if (anchor == null || !anchor.isAttachedToWindow) {
                    capture(scroll, state)
                    return@addOnGlobalLayoutListener
                }
                val desired = (anchor.top - state.anchorOffset).coerceIn(0, scrollRange(scroll))
                if (abs(desired - scroll.scrollY) > 1) {
                    state.restoring = true
                    scroll.scrollTo(scroll.scrollX, desired)
                    state.restoring = false
                }
                scroll.post { capture(scroll, state) }
            }
        }
    }

    private fun visit(view: View, action: (ScrollView) -> Unit) {
        if (view is ScrollView) action(view)
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) visit(view.getChildAt(index), action)
        }
    }

    private fun capture(scroll: ScrollView, state: State) {
        val content = scroll.getChildAt(0) as? ViewGroup ?: return
        if (content.childCount == 0) return
        val top = scroll.scrollY
        var anchor = content.getChildAt(0)
        for (index in 0 until content.childCount) {
            val child = content.getChildAt(index)
            if (child.bottom >= top) {
                anchor = child
                break
            }
        }
        state.anchor = WeakReference(anchor)
        state.anchorOffset = anchor.top - top
    }

    private fun scrollRange(scroll: ScrollView): Int {
        val child = scroll.getChildAt(0) ?: return 0
        return (child.height - scroll.height + scroll.paddingTop + scroll.paddingBottom).coerceAtLeast(0)
    }
}
