package com.example.pumppaperbot

import android.content.Context

/**
 * V5 remains a paper-only research period: the three autonomous systems never place
 * real exchange orders. User alerts are controlled separately so research and paper
 * journals keep running when Serge temporarily turns every call off.
 */
object ResearchModePolicy {
    const val ENABLED = true
    const val AUTONOMOUS_PARTICIPANTS = true
    const val USE_RESEARCH_APP_BASELINE = true
    const val DEFAULT_ALERTS_ENABLED = false

    private const val PREFS = "research_alert_master_v5"
    private const val KEY_ENABLED = "enabled"

    fun alertsEnabled(context: Context): Boolean = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_ENABLED, DEFAULT_ALERTS_ENABLED)

    fun setAlertsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .commit()
    }

    fun userAlertsAllowed(context: Context): Boolean = userAlertsAllowed(alertsEnabled(context))
    fun soundAllowed(context: Context): Boolean = soundAllowed(alertsEnabled(context))

    internal fun userAlertsAllowed(masterEnabled: Boolean): Boolean = masterEnabled
    internal fun soundAllowed(masterEnabled: Boolean): Boolean = masterEnabled
}
