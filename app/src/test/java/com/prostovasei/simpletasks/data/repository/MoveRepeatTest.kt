package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.domain.model.upcomingRepeats
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Перенос одного повтора (итерация 50): «Текущий повтор» у ближайшего раза, «Все повторы с этого момента» у будущего (деление серии),
 * «На завтра» у повторяющейся — только этот раз. Сегодня — 13.10.2026 (вт).
 */
class MoveRepeatTest : DatabaseTest() {
    private val today = LocalDate.of(2026, 10, 13)

    private suspend fun create(due: TaskDue?, rrule: String? = "FREQ=WEEKLY", checklist: List<String> = emptyList(), reminders: List<String> = emptyList()) =
        tasks.createTask(NewTask(projects.getInbox().id, "Стирка", due = due, rrule = rrule, checklist = checklist, reminders = reminders))

    private fun day(offset: Long) = TaskDue.AllDay(today.plusDays(offset))

    private fun at(offset: Long, hour: Int) = TaskDue.AtTime(LocalDateTime.of(today.plusDays(offset), java.time.LocalTime.of(hour, 0)).atZone(time.zone).toInstant())

    @Test
    fun currentRepeatBecomesOneOffTaskAndSeriesMovesOn() = runTest {
        val id = create(at(0, 14), checklist = listOf("Постельное"), reminders = listOf("PT0S"))
        val item = tasks.observeTaskDetails(id).first()!!.checklist.single()
        checklist.setDone(item.id, true)

        val moved = tasks.moveRepeat(id, at(1, 18), null)!!

        val copy = tasks.observeTaskDetails(moved.createdId!!).first()!!
        assertEquals(at(1, 18), copy.task.due)
        assertNull("Раз — обычная задача без повтора", copy.task.rrule)
        assertFalse("Без пометки повтора", copy.task.isRepeatDone)
        assertEquals(TaskStatus.ACTIVE, copy.task.status)
        assertEquals("Напоминание уехало с разом", listOf("PT0S"), copy.task.reminders)
        assertTrue("Чек-лист раза — с галочками", copy.checklist.single().isDone)
        val series = tasks.observeTaskDetails(id).first()!!
        assertEquals("Серия — со следующего раза", at(7, 14), series.task.due)
        assertEquals("FREQ=WEEKLY", series.task.rrule)
        assertTrue("У серии чек-лист заново", series.checklist.none { it.isDone })
        assertEquals(at(1, 18), moved.day)

        tasks.undoMove(moved)
        assertNull("Раз стёрт", tasks.getTask(moved.createdId!!))
        val back = tasks.observeTaskDetails(id).first()!!
        assertEquals(at(0, 14), back.task.due)
        assertTrue(back.checklist.single().isDone)
    }

    @Test
    fun lastRepeatTurnsSeriesIntoOneOffTask() = runTest {
        val id = create(day(0), rrule = "FREQ=DAILY;COUNT=1")

        val moved = tasks.moveRepeat(id, day(2), null)!!

        assertNull(moved.createdId)
        val task = tasks.getTask(id)!!
        assertEquals(day(2), task.due)
        assertNull(task.rrule)
        tasks.undoMove(moved)
        assertEquals("FREQ=DAILY;COUNT=1", tasks.getTask(id)!!.rrule)
    }

    @Test
    fun plainTaskIsNotMovedAsRepeat() = runTest {
        assertNull(tasks.moveRepeat(create(day(0), rrule = null), day(1), null))
    }

    @Test
    fun futureRepeatSplitsSeries() = runTest {
        val id = create(day(0), checklist = listOf("Постельное"))

        // Раз через две недели (вт 27.10) — на среду 28.10: старая серия до 26.10, новая — по средам с 28.10.
        val moved = tasks.moveRepeatsFrom(id, day(14), day(15), null)!!

        val old = tasks.getTask(id)!!
        assertEquals(day(0), old.due)
        assertEquals("FREQ=WEEKLY;UNTIL=20261026", old.rrule)
        val oldRepeats = old.upcomingRepeats(time.zone, today, today.plusDays(60)).map { it.due }
        assertEquals("Старая серия — до раза", listOf(day(7)), oldRepeats)
        val split = tasks.observeTaskDetails(moved.createdId!!).first()!!
        assertEquals(day(15), split.task.due)
        assertEquals("FREQ=WEEKLY", split.task.rrule)
        assertEquals(TaskStatus.ACTIVE, split.task.status)
        assertEquals(listOf("Постельное"), split.checklist.map { it.title })
        assertEquals(listOf(day(22), day(29)), split.task.upcomingRepeats(time.zone, today, today.plusDays(29)).map { it.due })

        tasks.undoMove(moved)
        assertNull(tasks.getTask(moved.createdId!!))
        assertEquals("FREQ=WEEKLY", tasks.getTask(id)!!.rrule)
    }

    @Test
    fun splitKeepsRemainingCount() = runTest {
        val id = create(at(0, 9), rrule = "FREQ=DAILY;COUNT=5")

        val moved = tasks.moveRepeatsFrom(id, at(2, 9), at(2, 12), null)!!

        assertEquals("Старая часть — сегодня и завтра", listOf(at(1, 9)), tasks.getTask(id)!!.upcomingRepeats(time.zone, today, today.plusDays(10)).map { it.due })
        val split = tasks.getTask(moved.createdId!!)!!
        assertEquals(at(2, 12), split.due)
        assertEquals("Осталось три раза", "FREQ=DAILY;COUNT=3", split.rrule)
    }

    @Test
    fun nearestRepeatFromAllShiftsSeries() = runTest {
        val id = create(day(0))
        val moved = tasks.moveRepeatsFrom(id, day(0), day(1), null)!!
        assertNull(moved.createdId)
        assertEquals(day(1), tasks.getTask(id)!!.due)
        assertEquals("FREQ=WEEKLY", tasks.getTask(id)!!.rrule)
    }

    @Test
    fun tomorrowMovesOnlyThisRepeat() = runTest {
        val id = create(at(-1, 18))

        val moved = tasks.moveToTomorrow(id)!!

        val copy = tasks.getTask(moved.createdId!!)!!
        assertEquals("Раз — завтра в то же время", at(1, 18), copy.due)
        assertNull(copy.rrule)
        assertEquals("Серия — на своём расписании", at(6, 18), tasks.getTask(id)!!.due)
        tasks.undoMove(moved)
        assertEquals(at(-1, 18), tasks.getTask(id)!!.due)
        assertNull(tasks.getTask(moved.createdId!!))
    }

    @Test
    fun tomorrowMovesPlainTaskWithUndo() = runTest {
        val id = create(day(-2), rrule = null)
        val moved = tasks.moveToTomorrow(id)
        assertNotNull(moved)
        assertEquals(day(1), tasks.getTask(id)!!.due)
        tasks.undoMove(moved!!)
        assertEquals(day(-2), tasks.getTask(id)!!.due)
    }

    /** 0.51.1: «Все повторы» у ближайшего раза правила «10-го числа» — серия на выбранную дату, правило под неё. */
    @Test
    fun monthlyOnTenthMovesToChosenDay() = runTest {
        val id = create(day(28), rrule = "FREQ=MONTHLY;BYMONTHDAY=10")

        tasks.moveRepeatsFrom(id, day(28), day(22), null)

        val series = tasks.getTask(id)!!
        assertEquals(day(22), series.due)
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=4", series.rrule)
    }

    /** 0.51.1: режим выбора и «Перенести просроченные» у повторяющейся переносят только этот раз; «Отмена» возвращает всё. */
    @Test
    fun bulkAndOverdueMoveOnlyThisRepeat() = runTest {
        val repeat = create(day(-3))
        val plain = create(day(-2), rrule = null)

        val moved = tasks.moveTasks(listOf(repeat, plain), today.plusDays(1))

        assertEquals(2, moved.count)
        assertEquals("Обычная — на завтра", day(1), tasks.getTask(plain)!!.due)
        assertEquals("Серия — на своём расписании", day(4), tasks.getTask(repeat)!!.due)
        assertEquals(day(1), tasks.getTask(moved.moves.single().createdId!!)!!.due)
        tasks.undoReschedule(moved)
        assertEquals(day(-3), tasks.getTask(repeat)!!.due)
        assertEquals(day(-2), tasks.getTask(plain)!!.due)
        assertNull(tasks.getTask(moved.moves.single().createdId!!))

        val overdue = tasks.moveOverdueToToday()
        assertEquals(2, overdue.count)
        assertEquals(day(0), tasks.getTask(plain)!!.due)
        assertEquals(day(4), tasks.getTask(repeat)!!.due)
        assertEquals(day(0), tasks.getTask(overdue.moves.single().createdId!!)!!.due)
    }
}
