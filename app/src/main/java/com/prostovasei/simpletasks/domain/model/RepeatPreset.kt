package com.prostovasei.simpletasks.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Готовые варианты окна повтора (итерация 31, ТЗ PM): нет, ежедневно, еженедельно (в день недели даты задачи),
 * ежемесячно (в число месяца), ежегодно (в день и месяц), каждый рабочий день (пн–пт); остальное — «Произвольно…».
 */
enum class RepeatPreset {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY, WORKDAYS, CUSTOM;

    /**
     * Правило варианта. Без дня недели и числа: повтор идёт от даты задачи, как у правил конструктора, —
     * и не разойдётся с подписью, если дату потом перенести. Для [NONE] — null, для [CUSTOM] правила нет.
     */
    fun rule(): String? = when (this) {
        NONE -> null
        DAILY -> "FREQ=DAILY"
        WEEKLY -> "FREQ=WEEKLY"
        MONTHLY -> "FREQ=MONTHLY"
        YEARLY -> "FREQ=YEARLY"
        WORKDAYS -> "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
        CUSTOM -> error("У «Произвольно» нет готового правила")
    }

    companion object {
        val WORKDAY_SET: Set<DayOfWeek> = DayOfWeek.entries.take(5).toSet()

        /**
         * Какой вариант отметить для правила [rrule] задачи с датой [due]. Записи TickTick тоже узнаются:
         * «каждую неделю по средам» у задачи в среду — «Еженедельно», «каждый месяц 28-го» у задачи 28-го — «Ежемесячно».
         */
        fun of(rrule: String?, due: LocalDate?): RepeatPreset {
            if (rrule == null) return NONE
            val rule = RepeatRule.parse(rrule) ?: return CUSTOM
            if (!rule.isSimple(due) || rule.interval != 1) return CUSTOM
            val days = rule.weekDays
            return when (rule.frequency) {
                RepeatFrequency.DAILY -> when {
                    days.isEmpty() -> DAILY
                    days == WORKDAY_SET -> WORKDAYS
                    else -> CUSTOM
                }
                RepeatFrequency.WEEKLY -> when {
                    days.isEmpty() || (due != null && days == setOf(due.dayOfWeek)) -> WEEKLY
                    days == WORKDAY_SET -> WORKDAYS
                    else -> CUSTOM
                }
                RepeatFrequency.MONTHLY -> if (days.isEmpty()) MONTHLY else CUSTOM
                RepeatFrequency.YEARLY -> if (days.isEmpty()) YEARLY else CUSTOM
            }
        }
    }
}
