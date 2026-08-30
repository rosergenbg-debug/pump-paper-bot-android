package com.example.pumppaperbot

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File

internal object SupportExportValidator {
    const val MAX_PART_BYTES = 900_000

    fun validateJsonFiles(files: List<File>) {
        require(files.isNotEmpty()) { "JSON-отчёт не создан" }
        val expectedParts = files.size
        var totalRows = 0
        files.forEachIndexed { index, file ->
            require(file.isFile && file.length() in 1..MAX_PART_BYTES.toLong()) {
                "Некорректный размер JSON ${file.name}: ${file.length()}"
            }
            val root = JSONObject(file.readText(Charsets.UTF_8))
            require(root.optInt("windowHours") == 24) { "JSON не ограничен последними 24 часами" }
            val safety = root.optJSONObject("safety")
                ?: throw IllegalArgumentException("В JSON отсутствует раздел безопасности")
            require(!safety.optBoolean("containsApiKeys", true)) {
                "JSON не прошёл проверку безопасности"
            }
            val parts = root.getJSONObject("parts")
            require(parts.getInt("partCount") == expectedParts) { "Неверное число частей JSON" }
            require(parts.getInt("part") == index + 1) { "Нарушен порядок частей JSON" }
            totalRows += root.optJSONArray("journal")?.length() ?: 0
        }
        require(totalRows >= 0) { "JSON-журнал не читается" }
    }

    fun validateTextFiles(files: List<File>) {
        require(files.isNotEmpty()) { "TXT-отчёт не создан" }
        files.forEachIndexed { index, file ->
            require(file.isFile && file.length() in 1..MAX_PART_BYTES.toLong()) {
                "Некорректный размер TXT ${file.name}: ${file.length()}"
            }
            val text = file.readText(Charsets.UTF_8)
            require("windowHours=24" in text) { "TXT не ограничен последними 24 часами" }
            require("CONTAINS_API_KEYS=false" in text) { "TXT не прошёл проверку безопасности" }
            require("parts=${index + 1}/${files.size}" in text) { "Нарушен порядок частей TXT" }
        }
    }
}

internal object SupportReportDelivery {
    fun share(
        context: Context,
        files: List<File>,
        mimeType: String,
        subject: String,
        chooserTitle: String
    ) {
        require(files.isNotEmpty()) { "Нет файлов для отправки" }
        val uris = ArrayList(files.map {
            FileProvider.getUriForFile(context, "${context.packageName}.files", it)
        })
        val send = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.single())
            else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, files.first().name, uris.first()).also { clip ->
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            }
        }
        context.packageManager.queryIntentActivities(send, 0).forEach { target ->
            uris.forEach { uri ->
                context.grantUriPermission(
                    target.activityInfo.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        context.startActivity(Intent.createChooser(send, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }
}
