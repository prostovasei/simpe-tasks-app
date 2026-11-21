package com.prostovasei.simpletasks.widget

import com.prostovasei.simpletasks.R

// Сгенерировано design/tools/gen_widget_week.py (итерация 45): id элементов ячеек и мини-календаря вида «Неделя» в виджете. Не править руками.

/** Пассивная строка недели: контейнер, название, эмодзи списка и начало справа. */
class SlotIds(val plaque: Int, val title: Int, val emoji: Int, val time: Int)

/** Элементы ячейки дня: общий корень, строка даты (день недели, число, «+N») и плашки задач. */
class CellIds(val root: Int, val head: Int, val weekday: Int, val number: Int, val more: Int, val slots: List<SlotIds>)

/** Строка недели мини-календаря и её семь дней. */
class CalRow(val row: Int, val days: List<Int>)

object WeekIds {
    /** Плашек в ячейке не больше. */
    const val SLOTS = 6

    val LEFT = CellIds(
        root = R.id.wl_root, head = R.id.wl_head, weekday = R.id.wl_wd, number = R.id.wl_num, more = R.id.wl_more,
        slots = listOf(
            SlotIds(R.id.wl_s1, R.id.wl_s1_title, R.id.wl_s1_emoji, R.id.wl_s1_time),
            SlotIds(R.id.wl_s2, R.id.wl_s2_title, R.id.wl_s2_emoji, R.id.wl_s2_time),
            SlotIds(R.id.wl_s3, R.id.wl_s3_title, R.id.wl_s3_emoji, R.id.wl_s3_time),
            SlotIds(R.id.wl_s4, R.id.wl_s4_title, R.id.wl_s4_emoji, R.id.wl_s4_time),
            SlotIds(R.id.wl_s5, R.id.wl_s5_title, R.id.wl_s5_emoji, R.id.wl_s5_time),
            SlotIds(R.id.wl_s6, R.id.wl_s6_title, R.id.wl_s6_emoji, R.id.wl_s6_time),
        ),
    )

    val RIGHT = CellIds(
        root = R.id.wr_root, head = R.id.wr_head, weekday = R.id.wr_wd, number = R.id.wr_num, more = R.id.wr_more,
        slots = listOf(
            SlotIds(R.id.wr_s1, R.id.wr_s1_title, R.id.wr_s1_emoji, R.id.wr_s1_time),
            SlotIds(R.id.wr_s2, R.id.wr_s2_title, R.id.wr_s2_emoji, R.id.wr_s2_time),
            SlotIds(R.id.wr_s3, R.id.wr_s3_title, R.id.wr_s3_emoji, R.id.wr_s3_time),
            SlotIds(R.id.wr_s4, R.id.wr_s4_title, R.id.wr_s4_emoji, R.id.wr_s4_time),
            SlotIds(R.id.wr_s5, R.id.wr_s5_title, R.id.wr_s5_emoji, R.id.wr_s5_time),
            SlotIds(R.id.wr_s6, R.id.wr_s6_title, R.id.wr_s6_emoji, R.id.wr_s6_time),
        ),
    )

    val CAL_ROWS = listOf(
        CalRow(R.id.wc_r1, listOf(R.id.wc_r1_d1, R.id.wc_r1_d2, R.id.wc_r1_d3, R.id.wc_r1_d4, R.id.wc_r1_d5, R.id.wc_r1_d6, R.id.wc_r1_d7)),
        CalRow(R.id.wc_r2, listOf(R.id.wc_r2_d1, R.id.wc_r2_d2, R.id.wc_r2_d3, R.id.wc_r2_d4, R.id.wc_r2_d5, R.id.wc_r2_d6, R.id.wc_r2_d7)),
        CalRow(R.id.wc_r3, listOf(R.id.wc_r3_d1, R.id.wc_r3_d2, R.id.wc_r3_d3, R.id.wc_r3_d4, R.id.wc_r3_d5, R.id.wc_r3_d6, R.id.wc_r3_d7)),
        CalRow(R.id.wc_r4, listOf(R.id.wc_r4_d1, R.id.wc_r4_d2, R.id.wc_r4_d3, R.id.wc_r4_d4, R.id.wc_r4_d5, R.id.wc_r4_d6, R.id.wc_r4_d7)),
        CalRow(R.id.wc_r5, listOf(R.id.wc_r5_d1, R.id.wc_r5_d2, R.id.wc_r5_d3, R.id.wc_r5_d4, R.id.wc_r5_d5, R.id.wc_r5_d6, R.id.wc_r5_d7)),
        CalRow(R.id.wc_r6, listOf(R.id.wc_r6_d1, R.id.wc_r6_d2, R.id.wc_r6_d3, R.id.wc_r6_d4, R.id.wc_r6_d5, R.id.wc_r6_d6, R.id.wc_r6_d7)),
    )
}
