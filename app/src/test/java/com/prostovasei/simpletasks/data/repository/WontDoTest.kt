package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.domain.model.EarlierRepeats
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * «Не буду делать» (итерация 49): обычная задача, ближайший и будущий раз повтора (как в TickTick: незакрытые повторы перед будущим
 * пропускаются или выполняются), последний раз серии, «Отмена», список и возврат в работу. Сегодня — 13.10.2026 (вт).
 */
class WontDoTest : DatabaseTest() {
    private val today = LocalDate.of(2026, 10, 13)

    private suspend fun create(due: TaskDue?, rrule: String? = null, checklist: List<String> = emptyList()) =
        tasks.createTask(NewTask(projects.getInbox().id, "Стирка", due = due, rrule = rrule, checklist = checklist))

    private fun day(offset: Long) = TaskDue.AllDay(today.plusDays(offset))

    @Test
    fun plainTaskIsClosedAndListed() = runTest {
        val id = create(day(0))

        val result = tasks.wontDo(id)!!

        assertTrue(result.closedWhole)
        val task = tasks.getTask(id)!!
        assertEquals(TaskStatus.ABANDONED, task.status)
        assertTrue(task.isWontDo)
        assertEquals(listOf(id), tasks.observeWontDo().first().map { it.id })
        assertEquals(1, tasks.observeWontDoCount().first())
        assertTrue("Не в «Выполненных»", tasks.observeCompleted().first().isEmpty())
        assertTrue("Не на экране «Все»", tasks.observeAllScreen(today).first().let { s -> (s.overdue + s.todayAllDay + s.todayTimed + s.upcoming).none { it.id == id } })

        tasks.undoWontDo(result)
        assertEquals(TaskStatus.ACTIVE, tasks.getTask(id)!!.status)
        assertEquals(0, tasks.observeWontDoCount().first())
    }

    @Test
    fun nearestRepeatIsClosedAndSeriesMoves() = runTest {
        val id = create(day(0), rrule = "FREQ=WEEKLY", checklist = listOf("Постельное"))
        val item = tasks.observeTaskDetails(id).first()!!.checklist.single()
        checklist.setDone(item.id, true)

        val result = tasks.wontDo(id)!!

        assertFalse(result.closedWhole)
        val series = tasks.observeTaskDetails(id).first()!!
        assertEquals(day(7), series.task.due)
        assertEquals(TaskStatus.ACTIVE, series.task.status)
        assertTrue("Галочки серии сняты", series.checklist.none { it.isDone })
        val copy = tasks.getTask(result.copyIds.single())!!
        assertEquals(TaskStatus.ABANDONED, copy.status)
        assertEquals(day(0), copy.due)
        assertNull(copy.rrule)
        assertTrue("Раз серии — с пометкой повтора", copy.isRepeatDone)
        assertEquals(day(0), result.occurrence)

        tasks.undoWontDo(result)
        val back = tasks.observeTaskDetails(id).first()!!
        assertEquals(day(0), back.task.due)
        assertTrue("Галочки вернулись", back.checklist.all { it.isDone })
        assertNull("Раз стёрт", tasks.getTask(result.copyIds.single()))
    }

    @Test
    fun futureRepeatSkipsEarlierOnes() = runTest {
        val id = create(day(0), rrule = "FREQ=WEEKLY")

        val result = tasks.wontDo(id, occurrence = day(14), earlier = EarlierRepeats.SKIP)!!

        assertEquals(2, result.skipped)
        assertEquals(0, result.completed)
        assertEquals("Серия — со следующего после будущего раза", day(21), tasks.getTask(id)!!.due)
        assertEquals(listOf(day(14)), result.copyIds.map { tasks.getTask(it)!!.due })
        assertTrue("Пропущенные не выполнены", tasks.observeCompleted().first().isEmpty())

        tasks.undoWontDo(result)
        assertEquals(day(0), tasks.getTask(id)!!.due)
        assertTrue(tasks.observeWontDo().first().isEmpty())
    }

    /** #192: «Только этот» — отменён один будущий раз, ближайший и разы до него на месте, серия его пропускает и дальше. */
    @Test
    fun onlyThisFutureRepeatIsCancelled() = runTest {
        val id = create(day(0), rrule = "FREQ=WEEKLY", checklist = listOf("Постельное"))
        val item = tasks.observeTaskDetails(id).first()!!.checklist.single()
        checklist.setDone(item.id, true)

        val result = tasks.wontDo(id, occurrence = day(14), earlier = EarlierRepeats.ONLY)!!

        assertFalse(result.closedWhole)
        assertEquals(0, result.skipped)
        assertEquals(0, result.completed)
        assertEquals(day(14), result.occurrence)
        val series = tasks.observeTaskDetails(id).first()!!
        assertEquals("Ближайший раз не сдвинулся", day(0), series.task.due)
        assertEquals(setOf(today.plusDays(14)), series.task.skipped)
        assertTrue("Чек-лист серии не сброшен", series.checklist.all { it.isDone })
        assertEquals(listOf(day(14)), tasks.observeWontDo().first().map { it.due })
        assertTrue(tasks.observeCompleted().first().isEmpty())

        // Серия идёт дальше: 0 → 7 → (14 отменён) → 21.
        tasks.complete(id)
        assertEquals(day(7), tasks.getTask(id)!!.due)
        tasks.complete(id)
        assertEquals("Отменённый раз пропущен", day(21), tasks.getTask(id)!!.due)
        assertTrue("Прошедшая отменённая дата забыта", tasks.getTask(id)!!.skipped.isEmpty())
    }

    @Test
    fun onlyThisUndoRestoresDate() = runTest {
        val id = create(day(0), rrule = "FREQ=WEEKLY")

        val result = tasks.wontDo(id, occurrence = day(14), earlier = EarlierRepeats.ONLY)!!
        tasks.undoWontDo(result)

        assertTrue(tasks.getTask(id)!!.skipped.isEmpty())
        assertTrue(tasks.observeWontDo().first().isEmpty())
        assertEquals(day(0), tasks.getTask(id)!!.due)
    }

    @Test
    fun futureRepeatCompletesEarlierOnes() = runTest {
        val id = create(day(0), rrule = "FREQ=WEEKLY")

        val result = tasks.wontDo(id, occurrence = day(14), earlier = EarlierRepeats.COMPLETE)!!

        assertEquals(2, result.completed)
        assertEquals(day(21), tasks.getTask(id)!!.due)
        val done = tasks.observeCompleted().first()
        assertEquals(setOf(day(0), day(7)), done.map { it.due }.toSet())
        assertTrue(done.all { it.isRepeatDone && it.rrule == null })
        assertEquals(listOf(day(14)), tasks.observeWontDo().first().map { it.due })

        tasks.undoWontDo(result)
        assertTrue("Выполненные разы стёрты", tasks.observeCompleted().first().isEmpty())
    }

    @Test
    fun lastRepeatClosesTheSeries() = runTest {
        val id = create(day(0), rrule = "FREQ=DAILY;COUNT=1")

        val result = tasks.wontDo(id)!!

        assertTrue(result.closedWhole)
        assertEquals(TaskStatus.ABANDONED, tasks.getTask(id)!!.status)
        tasks.undoWontDo(result)
        assertEquals(TaskStatus.ACTIVE, tasks.getTask(id)!!.status)
    }

    @Test
    fun countIsKeptForRemainingRepeats() = runTest {
        val id = create(day(0), rrule = "FREQ=DAILY;COUNT=5")

        tasks.wontDo(id, occurrence = day(2))

        val series = tasks.getTask(id)!!
        assertEquals(day(3), series.due)
        assertEquals("Осталось два раза: 3-й и 4-й день", "FREQ=DAILY;COUNT=2", series.rrule)
    }

    @Test
    fun restoreReturnsWontDoToWork() = runTest {
        val id = create(day(0), rrule = "FREQ=WEEKLY")
        val copyId = tasks.wontDo(id)!!.copyIds.single()

        val previous = tasks.restore(copyId)!!

        val back = tasks.getTask(copyId)!!
        assertEquals(TaskStatus.ACTIVE, back.status)
        assertNull("Раз вернулся обычной задачей", back.rrule)
        assertFalse(back.isRepeatDone)
        assertEquals(day(7), tasks.getTask(id)!!.due)

        tasks.undoRestore(previous)
        assertEquals(TaskStatus.ABANDONED, tasks.getTask(copyId)!!.status)
    }

    /** 0.50.1 (проверка PM): «Не буду делать» не занимает время на сетке окна даты — ни обычная задача, ни закрытый раз серии. */
    @Test
    fun wontDoIsNotBusyOnTheGrid() = runTest {
        val zone = time.zone
        fun at(hour: Int) = TaskDue.AtTime(today.atTime(hour, 0).atZone(zone).toInstant())
        val plain = create(at(14))
        val series = create(at(20), rrule = "FREQ=WEEKLY")
        val kept = create(at(10))
        tasks.wontDo(plain)
        val copyId = tasks.wontDo(series)!!.copyIds.single()

        val from = today.atStartOfDay(zone).toInstant()
        val busy = tasks.observeTimedInRange(from, from.plus(java.time.Duration.ofDays(1))).first().map { it.id }

        assertTrue("Обычная задача — на сетке", kept in busy)
        assertFalse("«Не буду делать» — не на сетке", plain in busy)
        assertFalse("Закрытый раз серии — не на сетке", copyId in busy)
        assertTrue("Серия (следующий раз) — на сетке", series in busy)
    }

    @Test
    fun closedTaskIsNotClosedAgain() = runTest {
        val id = create(day(0))
        tasks.setCompleted(id, true)
        assertNull(tasks.wontDo(id))
    }
}
