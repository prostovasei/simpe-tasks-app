package com.prostovasei.simpletasks.widget

import com.prostovasei.simpletasks.R

// Сгенерировано design/tools/gen_widget_week.py (#313): id элементов вида «Неделя» в виджете. Не править руками.

/** Ячейка дня: корень, строка даты (день недели и число) и список задач. */
class DayIds(val root: Int, val head: Int, val weekday: Int, val number: Int, val list: Int)

object WeekIds {
    /** Дни недели по порядку: пн … вс. */
    val DAYS = listOf(
        DayIds(R.id.w1_root, R.id.w1_head, R.id.w1_wd, R.id.w1_num, R.id.w1_list),
        DayIds(R.id.w2_root, R.id.w2_head, R.id.w2_wd, R.id.w2_num, R.id.w2_list),
        DayIds(R.id.w3_root, R.id.w3_head, R.id.w3_wd, R.id.w3_num, R.id.w3_list),
        DayIds(R.id.w4_root, R.id.w4_head, R.id.w4_wd, R.id.w4_num, R.id.w4_list),
        DayIds(R.id.w5_root, R.id.w5_head, R.id.w5_wd, R.id.w5_num, R.id.w5_list),
        DayIds(R.id.w6_root, R.id.w6_head, R.id.w6_wd, R.id.w6_num, R.id.w6_list),
        DayIds(R.id.w7_root, R.id.w7_head, R.id.w7_wd, R.id.w7_num, R.id.w7_list),
    )

    /** Лента недель мини-календаря. */
    val CALENDAR = R.id.wc_list

    /** Строка недели в ленте календаря: её семь чисел. */
    val CAL_DAYS = listOf(R.id.wcw_d1, R.id.wcw_d2, R.id.wcw_d3, R.id.wcw_d4, R.id.wcw_d5, R.id.wcw_d6, R.id.wcw_d7)
}
