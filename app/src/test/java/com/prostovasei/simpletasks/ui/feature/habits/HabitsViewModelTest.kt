package com.prostovasei.simpletasks.ui.feature.habits

import com.prostovasei.simpletasks.domain.model.habits.Habit
import com.prostovasei.simpletasks.domain.model.habits.HabitBackupInfo
import com.prostovasei.simpletasks.domain.model.habits.HabitSchedule
import com.prostovasei.simpletasks.domain.model.habits.HabitSettings
import com.prostovasei.simpletasks.domain.repository.HabitRepository
import com.prostovasei.simpletasks.domain.repository.HabitSettingsStore
import com.prostovasei.simpletasks.notifications.HabitReminder
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Раздел «Привычки» (#340): окно правки, отметки (будущее — нельзя), карточка и месяц, архив, удаление, напоминание. */
@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val time = FakeTimeProvider()
    private val today = LocalDate.parse("2026-10-13")
    private val repo = FakeHabits()
    private val store = object : HabitSettingsStore {
        val state = MutableStateFlow(HabitSettings())
        override val settings: StateFlow<HabitSettings> = state
        override var lastBackupAt: Long? = null
        override fun update(transform: (HabitSettings) -> HabitSettings) { state.value = transform(state.value) }
    }
    private var scheduled = 0
    private val reminder = object : HabitReminder { override fun schedule() { scheduled++ } }
    private val documents = object : com.prostovasei.simpletasks.core.files.DocumentOpener {
        override fun openInput(uri: String) = throw java.io.FileNotFoundException(uri)
        override fun openOutput(uri: String) = java.io.ByteArrayOutputStream()
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = HabitsViewModel(repo, store, reminder, time, documents, dispatcher)

    @Test
    fun formSavesNewHabitAndBlocksEmptyNameOrNoDays() = runTest(dispatcher) {
        val vm = vm()
        runCurrent()
        assertEquals(emptyList<Habit>(), vm.state.value.habits)
        vm.onIntent(HabitsIntent.Add)
        assertFalse(vm.state.value.form!!.canSave)
        vm.onIntent(HabitsIntent.EditForm { copy(name = "Зарядка", kind = ScheduleKind.WEEKDAYS, mask = 0) })
        assertFalse(vm.state.value.form!!.canSave)
        vm.onIntent(HabitsIntent.SaveForm)
        runCurrent()
        assertEquals(0, repo.state.value.size)
        vm.onIntent(HabitsIntent.EditForm { copy(mask = 0b0000101) })
        vm.onIntent(HabitsIntent.SaveForm)
        runCurrent()
        assertNull(vm.state.value.form)
        assertEquals(HabitSchedule.Weekdays(0b0000101), repo.state.value.single().schedule)
        assertTrue(scheduled > 0)
    }

    @Test
    fun toggleIgnoresFutureAndCardOpensOnCurrentMonth() = runTest(dispatcher) {
        repo.state.value = listOf(Habit("h1", "Читать", "📚", HabitSchedule.Daily))
        val vm = vm()
        runCurrent()
        vm.onIntent(HabitsIntent.Toggle("h1", today.plusDays(1)))
        vm.onIntent(HabitsIntent.Toggle("h1", today))
        runCurrent()
        assertEquals(setOf(today), repo.state.value.single().checks)

        vm.onIntent(HabitsIntent.Open("h1"))
        vm.onIntent(HabitsIntent.ShiftMonth(-1))
        assertEquals(YearMonth.from(today).minusMonths(1), vm.state.value.month)
        assertEquals("Читать", vm.state.value.open!!.name)
        vm.onIntent(HabitsIntent.Edit("h1"))
        assertEquals("h1", vm.state.value.form!!.id)
    }

    @Test
    fun archiveAndDeleteCloseTheCard() = runTest(dispatcher) {
        repo.state.value = listOf(Habit("h1", "А", null, HabitSchedule.Daily), Habit("h2", "Б", null, HabitSchedule.PerWeek(2)))
        val vm = vm()
        runCurrent()
        vm.onIntent(HabitsIntent.Open("h1"))
        vm.onIntent(HabitsIntent.Archive("h1", true))
        runCurrent()
        assertNull(vm.state.value.openId)
        assertEquals(listOf("Б"), vm.state.value.active.map { it.name })
        assertEquals(listOf("А"), vm.state.value.archived.map { it.name })

        vm.onIntent(HabitsIntent.Open("h1"))
        vm.onIntent(HabitsIntent.AskDelete)
        assertTrue(vm.state.value.confirmDelete)
        vm.onIntent(HabitsIntent.ConfirmDelete)
        runCurrent()
        assertEquals(listOf("Б"), repo.state.value.map { it.name })
        vm.onIntent(HabitsIntent.SetReminder(true))
        assertTrue(store.state.value.reminderOn)
    }

    private class FakeHabits : HabitRepository {
        val state = MutableStateFlow<List<Habit>>(emptyList())
        private var next = 0
        override fun observeHabits(): Flow<List<Habit>> = state
        override suspend fun save(id: String?, name: String, emoji: String?, schedule: HabitSchedule): String {
            val key = id ?: "n${next++}"
            val old = state.value.firstOrNull { it.id == key }
            val habit = Habit(key, name.trim(), emoji, schedule, old?.sortOrder ?: state.value.size, old?.archived ?: false, old?.checks.orEmpty())
            state.value = if (old == null) state.value + habit else state.value.map { if (it.id == key) habit else it }
            return key
        }
        override suspend fun toggle(id: String, date: LocalDate): Boolean {
            var now = false
            state.value = state.value.map {
                if (it.id != id) it else { now = date !in it.checks; it.copy(checks = if (now) it.checks + date else it.checks - date) }
            }
            return now
        }
        override suspend fun setArchived(id: String, archived: Boolean) { state.value = state.value.map { if (it.id == id) it.copy(archived = archived) else it } }
        override suspend fun move(id: String, up: Boolean) = Unit
        override suspend fun delete(id: String) { state.value = state.value.filter { it.id != id } }
        override suspend fun exportBackup(settings: HabitSettings) = "{}"
        override fun inspectBackup(json: String) = HabitBackupInfo(0, 0, HabitSettings())
        override suspend fun restoreBackup(json: String) = HabitSettings()
    }
}
