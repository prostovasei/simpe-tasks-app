package com.prostovasei.simpletasks.ui.common

import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeepPendingTest {
    private fun keep(previous: List<String>, current: List<String>, pending: Set<String>) =
        keepPending(previous, current, pending) { it }

    @Test
    fun noPendingReturnsCurrent() {
        assertEquals(listOf("b"), keep(listOf("a", "b"), listOf("b"), emptySet()))
    }

    @Test
    fun pendingItemStaysAtItsPosition() {
        assertEquals(listOf("a", "b", "c"), keep(listOf("a", "b", "c"), listOf("a", "c"), setOf("b")))
        assertEquals(listOf("a", "b"), keep(listOf("a", "b"), listOf("b"), setOf("a")))
        assertEquals(listOf("a", "b"), keep(listOf("a", "b"), listOf("a"), setOf("b")))
    }

    @Test
    fun severalPendingItemsKeepOrder() {
        assertEquals(listOf("a", "b", "c", "d"), keep(listOf("a", "b", "c", "d"), listOf("d"), setOf("a", "b", "c")))
    }

    @Test
    fun newItemsFromCurrentAreKept() {
        assertEquals(listOf("a", "b", "x"), keep(listOf("a", "b"), listOf("a", "x"), setOf("b")))
    }

    @Test
    fun notPendingRemovedItemDisappears() {
        assertEquals(listOf("a"), keep(listOf("a", "b"), listOf("a"), setOf("z")))
    }

    @Test
    fun listColorParsing() {
        assertEquals(Color(0xFF3D8BFD), parseListColor("#3D8BFD"))
        assertEquals(Color(0x803D8BFD), parseListColor("#803D8BFD"))
        assertNull(parseListColor(null))
        assertNull(parseListColor("blue"))
        assertNull(parseListColor("#12"))
    }
}
