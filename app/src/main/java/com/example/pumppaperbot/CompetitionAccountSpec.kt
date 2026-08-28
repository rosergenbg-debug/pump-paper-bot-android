package com.example.pumppaperbot

/** Owner-facing V6.6 focused network: exactly three automatic profiles plus one human profile. */
internal object CompetitionAccountSpec {
    const val COUNT = 4
    const val SCREEN_TITLE = "←  V6.6 • 3 AUTO + HUMAN"

    val ORDER = listOf(
        "AUTO CORE",
        "AUTO BTC GUARD",
        "AUTO SOL/BTC SELECT",
        "HUMAN SELECT"
    )
}
