package com.prostovasei.simpletasks.ui.feature.trash

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.prostovasei.simpletasks.core.time.DayTicker
import com.prostovasei.simpletasks.domain.model.Deletion
import com.prostovasei.simpletasks.testing.FakeProjectRepository
import com.prostovasei.simpletasks.testing.FakeTaskRepository
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import com.prostovasei.simpletasks.testing.task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/** Корзина (итерация 39): раздел, задача в корзине, восстановление с «Отменой». */
@OptIn(ExperimentalCoroutinesApi::class)
class TrashViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 10, 29)
    private val tasks = FakeTaskRepository(today)
    private val projects = FakeProjectRepository()
    private val time = FakeTimeProvider(zone = ZoneOffset.UTC)
    private val restores = TrashRestores(tasks, TestScope(dispatcher))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        tasks.tasks.value = listOf(task("a", title = "Первая"), task("b", title = "Вторая"))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun trashVm() = TrashViewModel(tasks, projects, DayTicker { flowOf(today) }, time, restores)

    private fun taskVm(id: String) = TrashTaskViewModel(SavedStateHandle(mapOf("taskId" to id)), tasks, projects, DayTicker { flowOf(today) }, time, restores)

    @Test
    fun sectionListsDeletedTasksAndRestoreAllOffersUndo() = runTest(dispatcher) {
        tasks.deleteTask("a")
        tasks.deleteTask("b")
        val vm = trashVm()
        runCurrent()

        assertFalse(vm.state.value.isLoading)
        assertEquals(setOf("a", "b"), vm.state.value.items.map { it.id }.toSet())
        assertEquals(setOf("a", "b"), vm.state.value.deletedOn.keys)

        restores.events.test {
            vm.onIntent(TrashIntent.RestoreAll)
            runCurrent()
            val restoration = awaitItem()
            assertEquals(2, restoration.count)
            runCurrent()
            assertTrue(vm.state.value.items.isEmpty())

            restores.undo(restoration)
            runCurrent()
            assertEquals(2, vm.state.value.items.size)
        }
    }

    @Test
    fun deleteAllEmptiesTrash() = runTest(dispatcher) {
        tasks.deleteTask("a")
        val vm = trashVm()
        runCurrent()
        vm.onIntent(TrashIntent.DeleteAll)
        runCurrent()
        assertTrue(vm.state.value.items.isEmpty())
        assertEquals(0, tasks.trash.value.size)
    }

    @Test
    fun trashedTaskClosesWhenRestoredOrErasedAndRestoreOffersUndo() = runTest(dispatcher) {
        tasks.deleteTask("a")
        tasks.deleteTask("b")
        val restored = taskVm("a")
        val erased = taskVm("b")
        runCurrent()
        assertEquals("Первая", restored.state.value.trashed?.details?.task?.title)

        restores.events.test {
            restored.effects.test {
                restored.onIntent(TrashTaskIntent.Restore)
                runCurrent()
                assertEquals(TrashTaskEffect.Close, awaitItem())
            }
            assertEquals(1, awaitItem().count)
        }
        erased.effects.test {
            erased.onIntent(TrashTaskIntent.DeleteForever)
            runCurrent()
            assertEquals(TrashTaskEffect.Close, awaitItem())
        }
        assertEquals(emptyList<Deletion>(), tasks.trash.value.map { Deletion(it.id, 0) })
    }

    @Test
    fun missingTrashedTaskClosesAtOnce() = runTest(dispatcher) {
        val vm = taskVm("нет-такой")
        vm.effects.test {
            runCurrent()
            assertEquals(TrashTaskEffect.Close, awaitItem())
        }
    }
}
