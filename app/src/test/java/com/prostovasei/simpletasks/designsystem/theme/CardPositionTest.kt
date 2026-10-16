package com.prostovasei.simpletasks.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/** Плашки в блоке (итерация 12): скруглены только края блока. */
class CardPositionTest {

    @Test
    fun positionsInBlock() {
        assertEquals(listOf(CardPosition.Single), (0 until 1).map { CardPosition.of(it, 1) })
        assertEquals(
            listOf(CardPosition.First, CardPosition.Middle, CardPosition.Last),
            (0 until 3).map { CardPosition.of(it, 3) },
        )
    }

    @Test
    fun blockEdges() {
        assertEquals(listOf(true, true, false, false), CardPosition.entries.map { it.isBlockStart })
        assertEquals(listOf(true, false, false, true), CardPosition.entries.map { it.isBlockEnd })
    }
}
