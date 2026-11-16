package com.prostovasei.simpletasks.ui.feature.health

import app.cash.turbine.test
import com.prostovasei.simpletasks.domain.model.health.HealthSettings
import com.prostovasei.simpletasks.domain.model.health.Weight
import com.prostovasei.simpletasks.domain.model.health.WeightEntry
import com.prostovasei.simpletasks.domain.model.health.WeightPeriod
import com.prostovasei.simpletasks.testing.FakeHealthRepository
import com.prostovasei.simpletasks.testing.FakeHealthSettingsStore
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import com.prostovasei.simpletasks.testing.FakeWeighInReminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/** Раздел «Здоровье» (#257): окно ввода веса, одна запись на дату, удаление с «Отменить», цель и рост, напоминание, копия. */
@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val health = FakeHealthRepository()
    private val store = FakeHealthSettingsStore()
    private val reminder = FakeWeighInReminder()

    /** 13.10.2026, Москва. */
    private val time = FakeTimeProvider()
    private val today = LocalDate.parse("2026-10-13")

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val files = mutableMapOf<String, String>()
    private val documents = object : com.prostovasei.simpletasks.core.files.DocumentOpener {
        override fun openInput(uri: String) = (files[uri] ?: throw java.io.FileNotFoundException(uri)).byteInputStream()
        override fun openOutput(uri: String) = object : java.io.ByteArrayOutputStream() {
            override fun close() {
                if (uri == "readonly") throw java.io.IOException("нельзя записать")
                files[uri] = toString(Charsets.UTF_8)
            }
        }
    }

    private fun vm() = HealthViewModel(health, store, reminder, time, documents, dispatcher)

    private fun entry(daysAgo: Int, tenths: Int, comment: String? = null) = WeightEntry(today.minusDays(daysAgo.toLong()), tenths, comment)

    private fun HealthViewModel.edit(change: WeightForm.() -> WeightForm) = onIntent(HealthIntent.EditForm(change))

    @Test
    fun emptyStateAndFirstEntryStartFromDefaultWeight() = runTest(dispatcher) {
        val vm = vm()
        assertNull(vm.state.value.entries)
        runCurrent()
        assertEquals(emptyList<WeightEntry>(), vm.state.value.entries)

        vm.onIntent(HealthIntent.Add)
        val form = vm.state.value.form!!
        // Первый раз барабан стоит на 70,0, дата — сегодня.
        assertEquals(700, form.tenths)
        assertEquals(today, form.date)
        assertNull(form.editingDate)
    }

    @Test
    fun newEntryStartsFromTheLastWeightAndSavesWithComment() = runTest(dispatcher) {
        health.entries.value = listOf(entry(2, 830), entry(1, 826))
        val vm = vm()
        runCurrent()

        vm.onIntent(HealthIntent.Add)
        assertEquals(826, vm.state.value.form!!.tenths)
        vm.edit { withTenths(824).copy(comment = "после тренировки") }
        vm.onIntent(HealthIntent.SaveForm)
        runCurrent()

        assertNull(vm.state.value.form)
        assertEquals("save:$today:824:после тренировки:null", health.calls.last())
        assertEquals(824, vm.state.value.entries!!.last().tenths)
    }

    @Test
    fun savingOnAnOccupiedDateReplacesTheEntry() = runTest(dispatcher) {
        health.entries.value = listOf(entry(1, 826))
        val vm = vm()
        runCurrent()

        vm.onIntent(HealthIntent.Add)
        vm.edit { copy(date = today.minusDays(1)).withTenths(820) }
        vm.onIntent(HealthIntent.SaveForm)
        runCurrent()

        assertEquals(listOf(today.minusDays(1) to 820), vm.state.value.entries!!.map { it.date to it.tenths })
    }

    @Test
    fun futureDateIsNotSaved() = runTest(dispatcher) {
        val vm = vm()
        runCurrent()
        vm.onIntent(HealthIntent.Add)
        vm.edit { copy(date = today.plusDays(1)) }

        vm.onIntent(HealthIntent.SaveForm)
        runCurrent()

        assertTrue(health.calls.isEmpty())
        assertEquals(today.plusDays(1), vm.state.value.form!!.date)
    }

    @Test
    fun weightIsKeptInsideLimits() = runTest(dispatcher) {
        val vm = vm()
        runCurrent()
        vm.onIntent(HealthIntent.Add)

        vm.edit { withTenths(99_999) }
        assertEquals(Weight.MAX_TENTHS, vm.state.value.form!!.tenths)
        vm.edit { withTenths(1) }
        assertEquals(Weight.MIN_TENTHS, vm.state.value.form!!.tenths)
    }

    @Test
    fun openingAnEntryEditsItAndMovingTheDateReplacesTheOldOne() = runTest(dispatcher) {
        health.entries.value = listOf(entry(3, 830, "утро"), entry(0, 824))
        val vm = vm()
        runCurrent()

        vm.onIntent(HealthIntent.OpenEntry(today.minusDays(3)))
        val form = vm.state.value.form!!
        assertEquals(830, form.tenths)
        assertEquals("утро", form.comment)
        assertEquals(today.minusDays(3), form.editingDate)

        vm.edit { copy(date = today.minusDays(2)) }
        vm.onIntent(HealthIntent.SaveForm)
        runCurrent()

        assertEquals("save:${today.minusDays(2)}:830:утро:${today.minusDays(3)}", health.calls.last())
        assertEquals(listOf(today.minusDays(2), today), vm.state.value.entries!!.map { it.date })
    }

    @Test
    fun deleteSendsUndoAndUndoRestoresTheEntry() = runTest(dispatcher) {
        health.entries.value = listOf(entry(1, 826), entry(0, 824))
        val vm = vm()
        runCurrent()

        vm.effects.test {
            vm.onIntent(HealthIntent.OpenEntry(today))
            vm.onIntent(HealthIntent.DeleteForm)
            runCurrent()
            val deleted = awaitItem() as HealthEffect.EntryDeleted
            assertEquals(entry(0, 824), deleted.entry)
            assertEquals(listOf(826), vm.state.value.entries!!.map { it.tenths })
            assertNull(vm.state.value.form)

            vm.onIntent(HealthIntent.UndoDelete(deleted.entry))
            runCurrent()
            assertEquals(listOf(826, 824), vm.state.value.entries!!.map { it.tenths })
        }
    }

    @Test
    fun deletingIsOnlyPossibleForAnExistingEntry() = runTest(dispatcher) {
        val vm = vm()
        runCurrent()
        vm.onIntent(HealthIntent.Add)
        vm.onIntent(HealthIntent.DeleteForm)
        runCurrent()
        assertTrue(health.calls.isEmpty())
    }

    @Test
    fun tapOnPointShowsTipAndSecondTapOpensTheEntry() = runTest(dispatcher) {
        health.entries.value = listOf(entry(1, 826), entry(0, 824))
        val vm = vm()
        runCurrent()
        val day = today.minusDays(1)

        vm.onIntent(HealthIntent.TapPoint(day))
        assertEquals(day, vm.state.value.tip)
        assertNull(vm.state.value.form)

        vm.onIntent(HealthIntent.TapPoint(today))
        assertEquals(today, vm.state.value.tip)

        vm.onIntent(HealthIntent.TapPoint(today))
        assertEquals(today, vm.state.value.form!!.editingDate)
        assertNull(vm.state.value.tip)
    }

    @Test
    fun periodSwitchClearsTip() = runTest(dispatcher) {
        health.entries.value = listOf(entry(0, 824))
        val vm = vm()
        runCurrent()
        assertEquals(WeightPeriod.MONTH, vm.state.value.period)
        vm.onIntent(HealthIntent.TapPoint(today))

        vm.onIntent(HealthIntent.SelectPeriod(WeightPeriod.YEAR))

        assertEquals(WeightPeriod.YEAR, vm.state.value.period)
        assertNull(vm.state.value.tip)
    }

    @Test
    fun goalAndHeightAreSavedAndCanBeRemoved() = runTest(dispatcher) {
        val vm = vm()
        runCurrent()

        vm.onIntent(HealthIntent.OpenGoal)
        assertEquals(GoalForm(null, null), vm.state.value.goalForm)
        vm.onIntent(HealthIntent.EditGoal { copy(goalTenths = 780, heightCm = 180) })
        vm.onIntent(HealthIntent.SaveGoal)
        runCurrent()

        assertEquals(HealthSettings(goalTenths = 780, heightCm = 180), vm.state.value.settings)
        assertNull(vm.state.value.goalForm)

        // «Убрать цель»: рост остаётся.
        vm.onIntent(HealthIntent.OpenGoal)
        assertEquals(GoalForm(780, 180), vm.state.value.goalForm)
        vm.onIntent(HealthIntent.EditGoal { copy(goalTenths = null) })
        vm.onIntent(HealthIntent.SaveGoal)
        runCurrent()
        assertEquals(HealthSettings(goalTenths = null, heightCm = 180), vm.state.value.settings)
    }

    @Test
    fun cancelledGoalChangesAreDropped() = runTest(dispatcher) {
        store.update { it.copy(goalTenths = 780) }
        val vm = vm()
        runCurrent()
        vm.onIntent(HealthIntent.OpenGoal)
        vm.onIntent(HealthIntent.EditGoal { copy(goalTenths = 700) })

        vm.onIntent(HealthIntent.CloseGoal)

        assertEquals(780, vm.state.value.settings.goalTenths)
    }

    @Test
    fun reminderSettingsAreSavedAndRescheduled() = runTest(dispatcher) {
        val vm = vm()
        runCurrent()
        assertFalse(vm.state.value.settings.reminderOn)
        assertEquals(8 * 60, vm.state.value.settings.reminderMinute)

        vm.onIntent(HealthIntent.SetReminder(true))
        vm.onIntent(HealthIntent.SetReminderMinute(7 * 60 + 30))
        runCurrent()

        assertTrue(vm.state.value.settings.reminderOn)
        assertEquals(450, vm.state.value.settings.reminderMinute)
        assertEquals(2, reminder.scheduled)
    }

    @Test
    fun backupIsSavedToFileAndRemembersTime() = runTest(dispatcher) {
        health.backupJson = "{\"copy\":1}"
        val vm = vm()
        runCurrent()

        vm.effects.test {
            vm.onIntent(HealthIntent.ExportBackup("file:1"))
            runCurrent()
            assertEquals(HealthEffect.BackupSaved, awaitItem())
        }
        assertEquals("{\"copy\":1}", files["file:1"])
        assertEquals(time.now().toEpochMilli(), vm.state.value.lastBackupAt)
        assertEquals(time.now().toEpochMilli(), store.lastBackupAt)

        vm.effects.test {
            vm.onIntent(HealthIntent.ExportBackup("readonly"))
            runCurrent()
            assertEquals(HealthEffect.BackupFailed, awaitItem())
        }
    }

    @Test
    fun restoreAsksForConfirmationThenReplacesDataAndSettings() = runTest(dispatcher) {
        health.restoredSettings = HealthSettings(goalTenths = 770, heightCm = 175, reminderOn = true, reminderMinute = 420)
        files["file:2"] = "{\"ok\":1}"
        val vm = vm()
        runCurrent()

        vm.onIntent(HealthIntent.PickBackup("file:2"))
        runCurrent()
        val confirm = vm.state.value.restore!!
        assertEquals(3, confirm.info.entries)
        // Пока не подтвердили, данные не меняются.
        assertTrue(health.calls.none { it == "restore" })

        vm.effects.test {
            vm.onIntent(HealthIntent.ConfirmRestore)
            runCurrent()
            assertEquals(HealthEffect.BackupRestored, awaitItem())
        }
        assertNull(vm.state.value.restore)
        assertEquals(health.restoredSettings, vm.state.value.settings)
        assertEquals(1, reminder.scheduled)
    }

    @Test
    fun invalidFileIsRejectedWithoutChanges() = runTest(dispatcher) {
        files["bad-file"] = "bad"
        val vm = vm()
        runCurrent()

        vm.effects.test {
            vm.onIntent(HealthIntent.PickBackup("bad-file"))
            runCurrent()
            assertEquals(HealthEffect.BackupInvalid, awaitItem())
        }
        vm.effects.test {
            vm.onIntent(HealthIntent.PickBackup("missing"))
            runCurrent()
            assertEquals(HealthEffect.BackupInvalid, awaitItem())
        }
        assertNull(vm.state.value.restore)
        assertTrue(health.calls.isEmpty())
    }

    @Test
    fun cancellingRestoreKeepsEverything() = runTest(dispatcher) {
        files["file:2"] = "{}"
        val vm = vm()
        runCurrent()
        vm.onIntent(HealthIntent.PickBackup("file:2"))
        runCurrent()

        vm.onIntent(HealthIntent.CloseRestore)

        assertNull(vm.state.value.restore)
        assertTrue(health.calls.none { it == "restore" })
    }
}
