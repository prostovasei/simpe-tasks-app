package com.prostovasei.simpletasks.ui.feature.search

import com.prostovasei.simpletasks.domain.model.SearchableTask
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.model.TaskStatus
import com.prostovasei.simpletasks.testing.FakeProjectRepository
import com.prostovasei.simpletasks.testing.FakeTaskRepository
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import com.prostovasei.simpletasks.testing.task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Поиск (итерация 15): что находится, в каком порядке, выполнение и возврат из результатов. */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchTest {
    private val today = LocalDate.of(2026, 10, 17)
    private val doctor = task("doctor", "Записаться к Врачу", due = TaskDue.AllDay(today.plusDays(2)))
    private val book = task("book", "Вернуть книгу").copy(content = "библиотека\nдо 20-го, спросить врача")
    private val trip = task("trip", "Поездка", due = TaskDue.AllDay(today))
    private val called = task("called", "Позвонить врачу", status = TaskStatus.COMPLETED).copy(completedAt = Instant.parse("2026-10-10T10:00:00Z"))
    private val old = task("old", "Врач старый", status = TaskStatus.COMPLETED).copy(completedAt = Instant.parse("2026-01-10T10:00:00Z"))
    private val all = listOf(
        SearchableTask(doctor, emptyList()),
        SearchableTask(book, emptyList()),
        SearchableTask(trip, listOf("Паспорт", "Справка от врача")),
        SearchableTask(called, emptyList()),
        SearchableTask(old, emptyList()),
    )

    @Test
    fun findsByTitleDescriptionAndChecklistIgnoringCase() {
        val (active, done) = searchTasks(all, "  вра ")

        // Активные: по сроку, без срока — в конце.
        assertEquals(listOf("trip", "doctor", "book"), active.map { it.entry.task.id })
        assertEquals("Справка от врача", active[0].match)
        assertNull("Нашлось в названии — строка не нужна", active[1].match)
        assertEquals("до 20-го, спросить врача", active[2].match)
        // Выполненные: недавние сверху.
        assertEquals(listOf("called", "old"), done.map { it.entry.task.id })
    }

    @Test
    fun emptyQueryFindsNothing() {
        assertEquals(emptyList<SearchHit>() to emptyList<SearchHit>(), searchTasks(all, " "))
        assertTrue(searchTasks(all, "молоко").first.isEmpty())
    }

    private val dispatcher = StandardTestDispatcher()
    private val tasks = FakeTaskRepository(today)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        tasks.tasks.value = listOf(doctor, book, called)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val saved = mutableListOf<List<String>>()
    private val recents = RecentSearches(initial = emptyList(), persist = { saved += it })

    private fun viewModel() = SearchViewModel(tasks, FakeProjectRepository(), { flowOf(today) }, FakeTimeProvider(zone = ZoneOffset.UTC), recents)

    @Test
    fun resultsFollowQueryAndToggleCompletesOrRestores() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        assertTrue(vm.state.value.found == 0)

        vm.onIntent(SearchIntent.Query("врач"))
        runCurrent()
        assertEquals("врач", vm.state.value.query)
        assertEquals(listOf("doctor", "book"), vm.state.value.active.map { it.item.id })
        assertEquals(listOf("called"), vm.state.value.done.map { it.item.id })
        assertTrue(vm.state.value.done.single().item.isCompleting)

        vm.onIntent(SearchIntent.Toggle("doctor", isDone = false))
        runCurrent()
        assertEquals(listOf("doctor" to true), tasks.completedCalls)
        assertTrue(vm.state.value.done.any { it.item.id == "doctor" })

        vm.onIntent(SearchIntent.Toggle("called", isDone = true))
        runCurrent()
        assertEquals(listOf("called"), tasks.restored)
        assertTrue(vm.state.value.active.any { it.item.id == "called" })
    }

    /** Итерация 38: запрос запоминается по «поиску» на клавиатуре и по открытию найденной задачи; нажатие ищет; крестик и «Очистить». */
    @Test
    fun recentQueriesAreRememberedPickedAndRemoved() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onIntent(SearchIntent.Query("врач"))
        runCurrent()
        assertEquals("Пока не нажали «поиск», в недавние не попадает", emptyList<String>(), vm.state.value.recents)

        vm.onIntent(SearchIntent.Submit)
        runCurrent()
        assertEquals(listOf("врач"), vm.state.value.recents)

        vm.onIntent(SearchIntent.Query("книга"))
        vm.onIntent(SearchIntent.Submit)
        runCurrent()
        assertEquals(listOf("книга", "врач"), vm.state.value.recents)

        vm.onIntent(SearchIntent.Query(""))
        vm.onIntent(SearchIntent.PickRecent("врач"))
        runCurrent()
        assertEquals("врач", vm.state.value.query)
        assertEquals(listOf("doctor", "book"), vm.state.value.active.map { it.item.id })
        assertEquals("Выбранный встаёт наверх", listOf("врач", "книга"), vm.state.value.recents)

        vm.onIntent(SearchIntent.RemoveRecent("книга"))
        runCurrent()
        assertEquals(listOf("врач"), vm.state.value.recents)

        vm.onIntent(SearchIntent.ClearRecents)
        runCurrent()
        assertEquals(emptyList<String>(), vm.state.value.recents)
        assertEquals(emptyList<String>(), saved.last())
    }
}
