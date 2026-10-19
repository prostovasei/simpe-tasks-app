package com.prostovasei.simpletasks.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Строки дня в виджете: Glance роняет всё, что сверх 10 детей в колонке, — ни одна задача не должна пропасть. */
class WidgetRowsTest {
    /** Сколько строк отрисуется и сколько детей у самой полной колонки (в колонке дня к строкам добавляется подпись дня). */
    private fun render(count: Int): Pair<Int, Int> {
        var widest = 0
        fun walk(items: List<Int>, limit: Int, extra: Int): Int {
            val chunks = rowChunks(items, limit)
            val children = chunks?.size ?: items.size
            widest = maxOf(widest, children + extra)
            return chunks?.sumOf { walk(it, MAX_CHILDREN, 0) } ?: items.size
        }
        return walk((1..count).toList(), MAX_CHILDREN - 1, extra = 1) to widest
    }

    @Test
    fun tenTasksInADayAllFitNextToTheDayTitle() {
        // Было: 10 задач + подпись дня = 11 детей в одной колонке — последняя задача («Автобус») пропадала.
        val (rows, widest) = render(10)
        assertEquals(10, rows)
        assertTrue("Детей в колонке: $widest", widest <= MAX_CHILDREN)
    }

    @Test
    fun noRowIsLostInAnyDayLength() {
        for (count in 1..500) {
            val (rows, widest) = render(count)
            assertEquals("Задач в дне: $count", count, rows)
            assertTrue("Задач в дне: $count, детей в колонке: $widest", widest <= MAX_CHILDREN)
        }
    }

    @Test
    fun shortDayIsNotSplit() {
        assertNull(rowChunks((1..9).toList(), MAX_CHILDREN - 1))
        assertEquals(10, rowChunks((1..10).toList(), MAX_CHILDREN - 1)!!.sumOf { it.size })
    }
}
