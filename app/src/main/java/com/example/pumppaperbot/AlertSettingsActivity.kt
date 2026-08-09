package com.example.pumppaperbot

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class AlertSettingsActivity : AppCompatActivity() {
    private lateinit var workButton: Button
    private lateinit var dailyButton: Button
    private lateinit var alwaysButton: Button
    private lateinit var startButton: Button
    private lateinit var endButton: Button
    private lateinit var soundButton: Button
    private lateinit var status: TextView
    private val ringtoneRequest = 417

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        root.addView(button("← НАЗАД", "#30363D").apply { setOnClickListener { finish() } }, LinearLayout.LayoutParams(-1, dp(48)))
        root.addView(label("ВРЕМЯ ЗВОНКА", 25, "#F0F6FC", true))
        root.addView(label("По умолчанию подготовительные сигналы звонят в понедельник, вторник, четверг и пятницу с 06:15 до 23:00. Фактические сделки APP, DeepSeek и эксперимента звонят ежедневно в это время. Опасный выход Сержа остаётся круглосуточной тревогой.", 15, "#C9D1D9", true))

        workButton = button("РАБОЧИЕ ДНИ: ПН • ВТ • ЧТ • ПТ", "#238636").apply {
            setOnClickListener { AlertSchedule.setMode(this@AlertSettingsActivity, AlertSchedule.MODE_WORK); updateUi() }
        }
        root.addView(workButton, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(14) })

        dailyButton = button("ЕЖЕДНЕВНО: 06:15–23:00", "#30363D").apply {
            setOnClickListener { AlertSchedule.setMode(this@AlertSettingsActivity, AlertSchedule.MODE_DAILY); updateUi() }
        }
        root.addView(dailyButton, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(8) })

        alwaysButton = button("КРУГЛОСУТОЧНО: 24 ЧАСА", "#30363D").apply {
            setOnClickListener { AlertSchedule.setMode(this@AlertSettingsActivity, AlertSchedule.MODE_ALWAYS); updateUi() }
        }
        root.addView(alwaysButton, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(8) })

        root.addView(label("Режим меняет подготовительные звонки. В режиме рабочих дней фактические сделки всё равно звонят ежедневно 06:15–23:00; ночью они показываются без звука. Ранние сообщения «подготовиться» отключены: первый жёлтый сигнал приходит только при готовности 7/10.", 14, "#8B949E", false))
        root.addView(label("Разрешённое время в рабочие дни", 17, "#F0F6FC", true))
        val hours = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        startButton = button("С 06:15", "#238636").apply { isEnabled = false }
        endButton = button("ДО 23:00", "#B62324").apply { isEnabled = false }
        hours.addView(startButton, LinearLayout.LayoutParams(0, dp(58), 1f))
        hours.addView(endButton, LinearLayout.LayoutParams(0, dp(58), 1f).apply { leftMargin = dp(8) })
        root.addView(hours, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(8) })

        soundButton = button("МЕЛОДИЯ ЗВОНКА", "#1F6FEB").apply { setOnClickListener { chooseSound() } }
        root.addView(soundButton, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(12) })
        root.addView(label("Выбранная мелодия используется для входов в разрешённое время и для обязательной круглосуточной тревоги выхода из открытой позиции.", 14, "#F0B72F", true))

        root.addView(label("ПРОВЕРКА ЧЕТЫРЁХ КАНАЛОВ", 17, "#F0F6FC", true))
        val testRowOne = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        testRowOne.addView(testButton("ТЕСТ APP", PumpAlert.SoundTestTarget.APP), LinearLayout.LayoutParams(0, dp(54), 1f))
        testRowOne.addView(testButton("ТЕСТ DEEPSEEK", PumpAlert.SoundTestTarget.DEEPSEEK), LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(8) })
        root.addView(testRowOne, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(8) })
        val testRowTwo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        testRowTwo.addView(testButton("ТЕСТ ЭКСПЕРИМЕНТ", PumpAlert.SoundTestTarget.EXPERIMENT), LinearLayout.LayoutParams(0, dp(54), 1f))
        testRowTwo.addView(testButton("ТЕСТ СЕРЖ", PumpAlert.SoundTestTarget.SERGE), LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(8) })
        root.addView(testRowTwo, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(8) })

        root.addView(button("НАСТРОЙКИ УВЕДОМЛЕНИЙ ANDROID", "#30363D").apply {
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                })
            }
        }, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(10) })
        status = label("", 15, "#58A6FF", true)
        root.addView(status)
        root.addView(label("Если сигнал APP возник вне звукового окна, приложение сохранит цену и время. В следующий разрешённый период оно сообщит: вход ещё возможен или уже пропущен.", 14, "#C9D1D9", false))
        setContentView(ScrollView(this).apply { addView(root) })
        updateUi()
    }

    private fun updateUi() {
        AlertSchedule.enforceAgreedSchedule(this)
        val mode = AlertSchedule.mode(this)
        workButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (mode == AlertSchedule.MODE_WORK) "#238636" else "#30363D"))
        dailyButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (mode == AlertSchedule.MODE_DAILY) "#238636" else "#30363D"))
        alwaysButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (mode == AlertSchedule.MODE_ALWAYS) "#238636" else "#30363D"))
        startButton.text = "С ${format(AlertSchedule.startMinutes(this))}"
        endButton.text = "ДО ${format(AlertSchedule.endMinutes(this))}"
        soundButton.text = "МЕЛОДИЯ: ${AlertSoundPreferences.title(this)}"
        status.text = AlertSchedule.statusText(this)
    }

    private fun testButton(text: String, target: PumpAlert.SoundTestTarget) = button(text, "#1F6FEB").apply {
        setOnClickListener {
            status.text = runCatching {
                PumpAlert.showSoundTest(this@AlertSettingsActivity, target)
                "Тест ${target.name}: уведомление отправлено. Если звук не слышен, откройте настройки Android ниже и проверьте громкость будильника и канал."
            }.getOrElse { "Тест не отправлен: ${it.message ?: "проверьте разрешение уведомлений Android"}" }
        }
    }

    private fun chooseSound() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Выберите мелодию PumpSignal")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, AlertSoundPreferences.uri(this@AlertSettingsActivity))
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        }
        startActivityForResult(intent, ringtoneRequest)
    }

    @Deprecated("Android compatibility callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != ringtoneRequest || resultCode != RESULT_OK) return
        val selected = data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) ?: return
        AlertSoundPreferences.save(this, selected)
        updateUi()
    }

    private fun format(value: Int): String = String.format(Locale.GERMANY, "%02d:%02d", value / 60, value % 60)

    private fun button(text: String, color: String) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(color))
        textSize = 13f
        isAllCaps = false
        setPadding(dp(4), 0, dp(4), 0)
    }

    private fun label(text: String, size: Int, color: String, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(7), 0, dp(7))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
