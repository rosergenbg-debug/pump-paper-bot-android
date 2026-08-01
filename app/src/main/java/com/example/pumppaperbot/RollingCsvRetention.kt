package com.example.pumppaperbot

import java.io.File

internal object RollingCsvRetention {
    const val RETENTION_MILLIS = 24L * 60L * 60L * 1000L

    fun prune(file: File, now: Long = System.currentTimeMillis()) {
        if (!file.exists() || file.length() == 0L) return
        val lines = file.readLines(Charsets.UTF_8)
        val retained = retain(lines, now - RETENTION_MILLIS)
        if (retained.size != lines.size) {
            file.writeText(retained.joinToString("\n", postfix = "\n"), Charsets.UTF_8)
        }
    }

    internal fun retain(lines: List<String>, cutoff: Long): List<String> {
        if (lines.isEmpty()) return emptyList()
        val header = lines.first()
        return listOf(header) + lines.drop(1).filter { line ->
            line.substringBefore(',').toLongOrNull()?.let { it >= cutoff } == true
        }
    }
}
