package com.prostovasei.simpletasks.ui.feature.voice

import com.prostovasei.simpletasks.domain.model.Reminders
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.testing.FakeProjectRepository
import com.prostovasei.simpletasks.testing.FakeTaskRepository
import com.prostovasei.simpletasks.ui.feature.task.SmartDateSetting
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** #310: список продиктованных задач и их сохранение галочкой с «Отменой». */
class VoiceTasksTest {
    private fun task(id: Long, title: String, due: TaskDue? = null) = VoiceTask(id, title, title, due)

    @Test
    fun listAddRemoveRestoreSaved() {
        val list = VoiceListModel()
        list.add { task(it, "Первая") }
        list.add { task(it, "Вторая") }
        list.add { task(it, "Третья") }
        val (index, removed) = list.remove(2)!!
        assertEquals(listOf("Первая", "Третья"), list.tasks.value.map { it.title })
        list.restore(index, removed)
        assertEquals("«Отмена» возвращает на место", listOf("Первая", "Вторая", "Третья"), list.tasks.value.map { it.title })
        list.saved(1)
        assertEquals(listOf("Вторая", "Третья"), list.tasks.value.map { it.title })
        assertNull(list.remove(99))
        list.clear()
        assertTrue(list.tasks.value.isEmpty())
    }

    @Test
    fun saveAllIntoInboxAndUndo() = runTest {
        val tasks = FakeTaskRepository()
        val saver = VoiceTasksSaver(tasks, FakeProjectRepository(), SmartDateSetting(true) {}, this)
        val due = TaskDue.AllDay(LocalDate.of(2026, 11, 24))
        val ids = saver.save(listOf(task(1, "Купить продукты", due), task(2, "Стоматолог")))
        assertEquals(listOf("Купить продукты", "Стоматолог"), tasks.created.map { it.title })
        assertTrue(tasks.created.all { it.projectId == "inbox" })
        assertEquals("Напоминание по умолчанию, как у новой задачи", Reminders.default(due), tasks.created[0].reminders)
        assertEquals(2, ids.size)
        saver.remove(ids)
        assertTrue("«Отмена» удаляет созданные задачи", tasks.tasks.value.none { it.id in ids })
        assertTrue("И в корзине их нет", tasks.trash.value.none { it.id in ids })
    }
}
