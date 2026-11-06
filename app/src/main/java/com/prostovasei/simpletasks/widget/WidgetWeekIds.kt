package com.prostovasei.simpletasks.widget

import com.prostovasei.simpletasks.R

// Сгенерировано design/tools/gen_widget_week.py (итерация 45): id элементов ячеек и мини-календаря вида «Неделя» в виджете. Не править руками.

/**
 * Элементы одной плашки задачи в ячейке: сама плашка, место кружка, кружок, галочка, название, эмодзи списка (итерация 48), время
 * и зона нажатия «выполнить» ~40 dp поверх кружка и начала названия (итерация 50).
 */
class SlotIds(val plaque: Int, val check: Int, val node: Int, val tick: Int, val title: Int, val emoji: Int, val time: Int, val hit: Int)

/** Элементы ячейки дня: строка даты (день недели, число, «+N») и плашки задач. */
class CellIds(val head: Int, val weekday: Int, val number: Int, val more: Int, val slots: List<SlotIds>)

/** Строка недели мини-календаря и её семь дней. */
class CalRow(val row: Int, val days: List<Int>)

object WeekIds {
    /** Плашек в ячейке не больше. */
    const val SLOTS = 6

    val LEFT = CellIds(
        head = R.id.wl_head, weekday = R.id.wl_wd, number = R.id.wl_num, more = R.id.wl_more,
        slots = listOf(
            SlotIds(R.id.wl_s1, R.id.wl_s1_check, R.id.wl_s1_node, R.id.wl_s1_tick, R.id.wl_s1_title, R.id.wl_s1_emoji, R.id.wl_s1_time, R.id.wl_s1_hit),
            SlotIds(R.id.wl_s2, R.id.wl_s2_check, R.id.wl_s2_node, R.id.wl_s2_tick, R.id.wl_s2_title, R.id.wl_s2_emoji, R.id.wl_s2_time, R.id.wl_s2_hit),
            SlotIds(R.id.wl_s3, R.id.wl_s3_check, R.id.wl_s3_node, R.id.wl_s3_tick, R.id.wl_s3_title, R.id.wl_s3_emoji, R.id.wl_s3_time, R.id.wl_s3_hit),
            SlotIds(R.id.wl_s4, R.id.wl_s4_check, R.id.wl_s4_node, R.id.wl_s4_tick, R.id.wl_s4_title, R.id.wl_s4_emoji, R.id.wl_s4_time, R.id.wl_s4_hit),
            SlotIds(R.id.wl_s5, R.id.wl_s5_check, R.id.wl_s5_node, R.id.wl_s5_tick, R.id.wl_s5_title, R.id.wl_s5_emoji, R.id.wl_s5_time, R.id.wl_s5_hit),
            SlotIds(R.id.wl_s6, R.id.wl_s6_check, R.id.wl_s6_node, R.id.wl_s6_tick, R.id.wl_s6_title, R.id.wl_s6_emoji, R.id.wl_s6_time, R.id.wl_s6_hit),
        ),
    )

    val RIGHT = CellIds(
        head = R.id.wr_head, weekday = R.id.wr_wd, number = R.id.wr_num, more = R.id.wr_more,
        slots = listOf(
            SlotIds(R.id.wr_s1, R.id.wr_s1_check, R.id.wr_s1_node, R.id.wr_s1_tick, R.id.wr_s1_title, R.id.wr_s1_emoji, R.id.wr_s1_time, R.id.wr_s1_hit),
            SlotIds(R.id.wr_s2, R.id.wr_s2_check, R.id.wr_s2_node, R.id.wr_s2_tick, R.id.wr_s2_title, R.id.wr_s2_emoji, R.id.wr_s2_time, R.id.wr_s2_hit),
            SlotIds(R.id.wr_s3, R.id.wr_s3_check, R.id.wr_s3_node, R.id.wr_s3_tick, R.id.wr_s3_title, R.id.wr_s3_emoji, R.id.wr_s3_time, R.id.wr_s3_hit),
            SlotIds(R.id.wr_s4, R.id.wr_s4_check, R.id.wr_s4_node, R.id.wr_s4_tick, R.id.wr_s4_title, R.id.wr_s4_emoji, R.id.wr_s4_time, R.id.wr_s4_hit),
            SlotIds(R.id.wr_s5, R.id.wr_s5_check, R.id.wr_s5_node, R.id.wr_s5_tick, R.id.wr_s5_title, R.id.wr_s5_emoji, R.id.wr_s5_time, R.id.wr_s5_hit),
            SlotIds(R.id.wr_s6, R.id.wr_s6_check, R.id.wr_s6_node, R.id.wr_s6_tick, R.id.wr_s6_title, R.id.wr_s6_emoji, R.id.wr_s6_time, R.id.wr_s6_hit),
        ),
    )

    val CAL_WEEKDAYS = listOf(R.id.wc_wd1, R.id.wc_wd2, R.id.wc_wd3, R.id.wc_wd4, R.id.wc_wd5, R.id.wc_wd6, R.id.wc_wd7)
    val CAL_ROWS = listOf(
        CalRow(R.id.wc_r1, listOf(R.id.wc_r1_d1, R.id.wc_r1_d2, R.id.wc_r1_d3, R.id.wc_r1_d4, R.id.wc_r1_d5, R.id.wc_r1_d6, R.id.wc_r1_d7)),
        CalRow(R.id.wc_r2, listOf(R.id.wc_r2_d1, R.id.wc_r2_d2, R.id.wc_r2_d3, R.id.wc_r2_d4, R.id.wc_r2_d5, R.id.wc_r2_d6, R.id.wc_r2_d7)),
        CalRow(R.id.wc_r3, listOf(R.id.wc_r3_d1, R.id.wc_r3_d2, R.id.wc_r3_d3, R.id.wc_r3_d4, R.id.wc_r3_d5, R.id.wc_r3_d6, R.id.wc_r3_d7)),
        CalRow(R.id.wc_r4, listOf(R.id.wc_r4_d1, R.id.wc_r4_d2, R.id.wc_r4_d3, R.id.wc_r4_d4, R.id.wc_r4_d5, R.id.wc_r4_d6, R.id.wc_r4_d7)),
        CalRow(R.id.wc_r5, listOf(R.id.wc_r5_d1, R.id.wc_r5_d2, R.id.wc_r5_d3, R.id.wc_r5_d4, R.id.wc_r5_d5, R.id.wc_r5_d6, R.id.wc_r5_d7)),
        CalRow(R.id.wc_r6, listOf(R.id.wc_r6_d1, R.id.wc_r6_d2, R.id.wc_r6_d3, R.id.wc_r6_d4, R.id.wc_r6_d5, R.id.wc_r6_d6, R.id.wc_r6_d7)),
    )
}
