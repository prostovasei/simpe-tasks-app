package com.prostovasei.simpletasks.ui.feature.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Черновик новой задачи (итерация 38): хранится один, пустое название стирает его. */
class TaskDraftsTest {
    @Test
    fun keepsOneDraftAndBlankTitleClearsIt() {
        val saved = mutableListOf<TaskDraft?>()
        val drafts = TaskDrafts(initial = TaskDraft("Старый", ""), persist = { saved += it })
        assertEquals("Старый", drafts.get()?.title)

        drafts.save("Новый", "Описание")
        assertEquals(TaskDraft("Новый", "Описание"), drafts.get())
        assertEquals(drafts.get(), saved.last())

        drafts.save("   ", "Описание без названия")
        assertNull(drafts.get())
        assertNull(saved.last())
    }
}
