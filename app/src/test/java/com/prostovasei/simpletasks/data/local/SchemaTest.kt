package com.prostovasei.simpletasks.data.local

import android.database.sqlite.SQLiteConstraintException
import com.prostovasei.simpletasks.data.local.db.InboxSeed
import com.prostovasei.simpletasks.data.local.entity.ChecklistItemEntity
import com.prostovasei.simpletasks.data.local.entity.ProjectEntity
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Структура БД: индексы, связи FOREIGN KEY и системный список «Входящие» (SPEC.md §2, §7 п.2). */
class SchemaTest : DatabaseTest() {

    @Test
    fun allIndicesExist() {
        val indices = mutableSetOf<String>()
        db.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type = 'index'").use { cursor ->
            while (cursor.moveToNext()) indices += cursor.getString(0)
        }
        val expected = setOf(
            "idx_tasks_due_status",
            "idx_tasks_project",
            "idx_checklist_task",
            "idx_projects_group",
            "idx_tasks_external_id",
        )
        assertTrue("Нет индексов: ${expected - indices}", indices.containsAll(expected))
    }

    @Test
    fun foreignKeysAreEnforced() = runTest {
        val taskError = runCatching { tasks.createTask(NewTask(projectId = "missing", title = "Задача")) }
        assertTrue(taskError.exceptionOrNull() is SQLiteConstraintException)

        val itemError = runCatching {
            db.checklistDao().insert(ChecklistItemEntity(id = "i", taskId = "missing", title = "x", createdAt = 0, updatedAt = 0))
        }
        assertTrue(itemError.exceptionOrNull() is SQLiteConstraintException)

        val projectError = runCatching {
            db.projectDao().insert(ProjectEntity(id = "p", groupId = "missing", name = "x", createdAt = 0, updatedAt = 0))
        }
        assertTrue(projectError.exceptionOrNull() is SQLiteConstraintException)
    }

    @Test
    fun inboxIsCreatedOnOpen() = runTest {
        val inbox = projects.getInbox()
        assertEquals(InboxSeed.INBOX_ID, inbox.id)
        assertTrue(inbox.isInbox)
    }

    @Test
    fun inboxIsNotDuplicatedOnReopen() = runTest {
        repeat(3) { InboxSeed(time).onOpen(db.openHelper.writableDatabase) }
        val count = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM projects WHERE is_inbox = 1").use {
            it.moveToFirst()
            it.getInt(0)
        }
        assertEquals(1, count)
    }
}
