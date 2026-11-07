package com.prostovasei.simpletasks.ui.feature.task

import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Сетка недели (итерация 16): геометрия нажатий и занятое время. */
class WeekGridModelTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 14)
    private val hourPx = 100f

    /** Итерация 51: пересекающиеся задачи — рядом (дорожки), встык — не пересечение. */
    @Test
    fun overlappingBusySlotsShareTheColumn() {
        fun slot(id: String, from: Int, to: Int, day: LocalDate = today) = BusySlot(id, id, day, from, to, isDone = false)
        val hookah = slot("Кальян", 19 * 60 + 30, 21 * 60 + 30)
        val football = slot("Футбол", 21 * 60 + 30, 23 * 60 + 30)
        val flowers = slot("Цветы", 23 * 60, 23 * 60 + 30)
        val dinner = slot("Ужин", 18 * 60, 19 * 60 + 30, today.plusDays(1))
        val call = slot("Звонок", 18 * 60 + 30, 19 * 60 + 30, today.plusDays(1))
        val series = slot("Сериал", 19 * 60, 20 * 60, today.plusDays(1))
        val lanes = busyLanes(listOf(flowers, hookah, football, dinner, call, series))
        assertEquals("Встык — на всю ширину", BusyLane(0, 1), lanes[hookah])
        assertEquals(BusyLane(0, 2), lanes[football])
        assertEquals(BusyLane(1, 2), lanes[flowers])
        assertEquals(listOf(0, 1, 2), listOf(dinner, call, series).map { lanes.getValue(it).index })
        assertEquals(setOf(3), listOf(dinner, call, series).map { lanes.getValue(it).count }.toSet())
        assertEquals("19:30–\u200B21:30", busyTimeText(hookah))
    }

    @Test
    fun minuteSnapsToHalfHour() {
        assertEquals(0, WeekGridMath.minuteAt(0f, hourPx))
        assertEquals(9 * 60, WeekGridMath.minuteAt(9 * hourPx + 10f, hourPx))
        assertEquals(9 * 60 + 30, WeekGridMath.minuteAt(9 * hourPx + 55f, hourPx))
        assertEquals("Ниже сетки — последний шаг 23:30", 23 * 60 + 30, WeekGridMath.minuteAt(25 * hourPx, hourPx))
        assertEquals(0, WeekGridMath.minuteAt(-5f, hourPx))
    }

    @Test
    fun dayUnderFingerFollowsScroll() {
        assertEquals(0, WeekGridMath.dayAt(10f, 0f, 50f))
        assertEquals(2, WeekGridMath.dayAt(110f, 0f, 50f))
        assertEquals("Пролистано на полдня назад", -1, WeekGridMath.dayAt(10f, -0.5f, 50f))
    }

    @Test
    fun swipeSettlesOnDayBorder() {
        assertEquals(1f, WeekGridMath.settleTarget(0.6f, 0f, 50f))
        assertEquals(0f, WeekGridMath.settleTarget(0.4f, 0f, 50f))
        assertEquals("Быстрый свайп влево уводит дальше", 3f, WeekGridMath.settleTarget(0.4f, -1000f, 50f))
    }

    @Test
    fun timelineCentersTappedDayButNotBeforeToday() {
        // Нажали на «послезавтра» — видно завтра, послезавтра (в центре) и день после.
        assertEquals(1f, WeekGridMath.timelineScroll(today, today.plusDays(2)))
        assertEquals(5f, WeekGridMath.timelineScroll(today, today.plusDays(6)))
        // Нажали на сегодня — таймлайн всё равно с сегодняшнего дня.
        assertEquals(0f, WeekGridMath.timelineScroll(today, today))
    }

    @Test
    fun fieldTexts() {
        fun at(h: Int, m: Int) = TaskDue.AtTime(today.atTime(h, m).toInstant(zone))
        fun fields(due: TaskDue?, start: TaskDue? = null, today: LocalDate = this.today) =
            SheetFields(due, start, "p", com.prostovasei.simpletasks.domain.model.Priority.NONE, null, emptyList(), emptyList(), today, zone, 0, emptyList())
        assertEquals(null, sheetDayText(fields(null)))
        assertEquals(null, sheetTimeText(fields(null)))
        assertEquals("ср, 14 окт", sheetDayText(fields(TaskDue.AllDay(today))))
        assertEquals("Без времени — поля времени нет", null, sheetTimeText(fields(TaskDue.AllDay(today))))
        assertEquals("09:30", sheetTimeText(fields(at(9, 30))))
        // Период — «начало–конец».
        assertEquals("15:00–16:30", sheetTimeText(fields(at(16, 30), at(15, 0))))
        // Другой год — год в подписи дня.
        assertEquals("ср, 14 окт 2026", sheetDayText(fields(TaskDue.AllDay(today), today = LocalDate.of(2025, 12, 31))))
        assertEquals("чт, 01 янв 2026", createdText(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun weekRangeUsesTwoDigitDays() {
        assertEquals("26 окт – 08 ноя", WeekBlockMath.range(LocalDate.of(2026, 10, 26)))
        assertEquals("12 окт – 25 окт", WeekBlockMath.range(LocalDate.of(2026, 10, 12)))
    }

    @Test
    fun windowOpensAtTenOrAtSelectedHour() {
        assertEquals("Новая задача без времени — с 10:00", 10 * 60, WeekGridMath.openMinute(null))
        assertEquals("Со временем — с его часа", 15 * 60, WeekGridMath.openMinute(15 * 60 + 40))
        assertEquals("Раньше десяти — с этого часа, выше прокручивается", 6 * 60, WeekGridMath.openMinute(6 * 60 + 15))
    }

    @Test
    fun weekBlockStaysInsideTwoWeeksAndFollowsFarDays() {
        // 14.10.2026 — среда, неделя с понедельника 12.10.
        val base = LocalDate.of(2026, 10, 12)
        assertEquals(base, WeekBlockMath.monday(today))
        assertEquals(base, WeekBlockMath.first(today, null, null))
        assertEquals("Внутри двух недель блок не прыгает", base, WeekBlockMath.first(today, today.plusDays(10), null))
        assertEquals("Дальше — неделя выбранного дня", LocalDate.of(2026, 11, 16), WeekBlockMath.first(today, LocalDate.of(2026, 11, 19), base))
        assertEquals("Стрелкой уехали вперёд — остались", base.plusDays(14), WeekBlockMath.first(today, today.plusDays(16), base.plusDays(14)))
    }

    @Test
    fun pickDayMovesDateKeepingTime() {
        val at = Schedule(TaskDue.AtTime(today.atTime(10, 30).toInstant(zone)), null)
        val moved = at.after(GridAction.PickDay(today.plusDays(3)), zone)
        assertEquals(Schedule(TaskDue.AtTime(today.plusDays(3).atTime(10, 30).toInstant(zone)), null), moved)
        assertEquals("Тот же день — без изменений", at, at.after(GridAction.PickDay(today), zone))
        assertEquals("Без даты — просто день", Schedule(TaskDue.AllDay(today.plusDays(1)), null), Schedule(null, null).after(GridAction.PickDay(today.plusDays(1)), zone))
        val period = Schedule(TaskDue.AtTime(today.atTime(12, 0).toInstant(zone)), TaskDue.AtTime(today.atTime(10, 0).toInstant(zone)))
        assertEquals("«←» возвращает прежний срок", at, Schedule(null, null).after(GridAction.Restore(at), zone))
        assertEquals("Период переезжает целиком", Schedule(TaskDue.AtTime(today.plusDays(2).atTime(12, 0).toInstant(zone)), TaskDue.AtTime(today.plusDays(2).atTime(10, 0).toInstant(zone))), period.after(GridAction.PickDay(today.plusDays(2)), zone))
    }

    @Test
    fun dragSnapsToStepsAndStaysInDay() {
        // День 50 px, шаг 30 минут — 20 px.
        assertEquals(1 to 10 * 60 + 30, WeekGridMath.moveTarget(10 * 60, 60, 30f, 25f, 50f, 20f))
        assertEquals(-1 to 9 * 60, WeekGridMath.moveTarget(10 * 60, 60, -40f, -45f, 50f, 20f))
        assertEquals("Конец не дальше 24:00", 0 to 22 * 60, WeekGridMath.moveTarget(10 * 60, 120, 0f, 2000f, 50f, 20f))
        assertEquals(0 to 0, WeekGridMath.moveTarget(1 * 60, 30, 0f, -500f, 50f, 20f))
    }

    @Test
    fun resizeSnapsAndLimits() {
        assertEquals(90, WeekGridMath.resizeTarget(16 * 60, 30, 41f, 20f))
        assertEquals("Не короче 30 минут", 30, WeekGridMath.resizeTarget(16 * 60, 60, -300f, 20f))
        assertEquals("Не дальше 24:00", 60, WeekGridMath.resizeTarget(23 * 60, 30, 500f, 20f))
    }

    @Test
    fun busySlotsFromTimedTasks() {
        val tasks = listOf(
            task("a", due = at("2026-10-14T16:00:00Z")),
            task("b", due = at("2026-10-15T11:00:00Z"), start = at("2026-10-15T09:30:00Z"), status = TaskStatus.COMPLETED),
            task("c", due = at("2026-10-17T01:00:00Z"), start = at("2026-10-16T22:00:00Z")),
            task("d", due = TaskDue.AllDay(today)),
            task("e", due = at("2026-10-30T10:00:00Z")),
        )
        assertEquals(
            listOf(
                BusySlot("a", "a", today, 16 * 60, 16 * 60 + 30, isDone = false),
                BusySlot("b", "b", today.plusDays(1), 9 * 60 + 30, 11 * 60, isDone = true),
                BusySlot("c", "c", today.plusDays(2), 22 * 60, 24 * 60, isDone = false),
            ),
            busySlots(tasks, zone, today, today.plusDays(6)),
        )
    }

    @Test
    fun repeatingTaskShowsNextOccurrences() {
        val daily = task("r", due = at("2026-10-14T08:00:00Z")).copy(rrule = "FREQ=DAILY")
        val days = busySlots(listOf(daily), zone, today, today.plusDays(3)).map { it.date }
        assertEquals(listOf(today, today.plusDays(1), today.plusDays(2), today.plusDays(3)), days)
    }

    private fun at(text: String) = TaskDue.AtTime(Instant.parse(text))
}
