package com.prostovasei.simpletasks.ui.feature.task

import android.content.Context
import androidx.core.content.edit
import com.prostovasei.simpletasks.domain.model.Reminders

/**
 * «Созданные вручную» напоминания (итерация 31, ТЗ PM): общая история для всех задач, переживает перезапуск.
 * По [SIZE] последних отдельно для задач со временем («за N дней/часов/минут») и без времени («за N дней в 09:00»).
 * Новые — сверху, без повторов. Хранится в настройках приложения: схема базы не меняется.
 */
object ReminderHistory {
    const val SIZE = 3
    private const val PREFS = "reminder_history"
    private const val KEY_TIMED = "timed"
    private const val KEY_ALL_DAY = "all_day"
    private const val SEPARATOR = ","

    fun load(context: Context, allDay: Boolean): List<String> =
        prefs(context).getString(key(allDay), null)?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    /** Запоминает [created] (в порядке создания) — последнее окажется сверху. */
    fun add(context: Context, allDay: Boolean, created: List<String>) {
        if (created.isEmpty()) return
        val merged = merge(load(context, allDay), created)
        prefs(context).edit { putString(key(allDay), merged.joinToString(SEPARATOR)) }
    }

    /** Новые сверху, одинаковые моменты («-PT60M» и «-PT1H») — одной строкой, не больше [SIZE]. */
    fun merge(history: List<String>, created: List<String>): List<String> =
        created.fold(history) { list, trigger -> listOf(trigger) + list.filterNot { Reminders.sameMoment(it, trigger) } }.take(SIZE)

    private fun key(allDay: Boolean) = if (allDay) KEY_ALL_DAY else KEY_TIMED

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
