package com.example.pumppaperbot

import android.content.Context

/**
 * V6.6 compatibility bridge for dormant legacy screens/reports.
 * The old V6.5 VWAP account no longer runs. Any read from the old symbol is redirected to
 * the new AUTO CORE account so stale UI/report code can compile without reactivating V6.5 logic.
 */
object Vwap3265AutoStore {
    fun state(context: Context): T32V660AutoState = V660CoreStore.state(context)
}
