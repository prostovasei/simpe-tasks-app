package com.prostovasei.simpletasks.data.local.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Миграция 4 → 5 (итерация 56): база версии 4 строится по выгруженной схеме `schemas/…/4.json` (как у PM на телефоне),
 * потом открывается приложением — Room сам проводит автоматическую миграцию и сверяет структуру с версией 5.
 * Папка «🏠Дом» из TickTick становится «Дом» с эмодзи 🏠; папки без эмодзи, списки и задачи не меняются.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun folderEmojiSplitFromName() {
        val file = context.getDatabasePath(AppDatabase.NAME).apply { parentFile?.mkdirs(); delete() }
        createVersion4(file)

        val db = AppDatabase.build(context, FakeTimeProvider())
        try {
            val groups = runBlocking { db.projectGroupDao().observeActive().first() }.associateBy { it.id }
            assertEquals("Дом", groups.getValue("g1").name)
            assertEquals("🏠", groups.getValue("g1").emoji)
            assertEquals("Работа", groups.getValue("g2").name)
            assertNull(groups.getValue("g2").emoji)
            val list = runBlocking { db.projectDao().getById("p1") }!!
            assertEquals("g1", list.groupId)
            assertEquals("Ремонт", list.name)
        } finally {
            db.close()
        }
    }

    private fun createVersion4(file: File) {
        val schema = JSONObject(File(SCHEMA_DIR, "4.json").readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { sql ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                sql.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) sql.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) sql.execSQL(setup.getString(i))
            sql.version = 4
            sql.execSQL("INSERT INTO project_groups (id, name, sort_order, is_deleted, created_at, updated_at) VALUES ('g1', '🏠Дом', 0, 0, 1, 1)")
            sql.execSQL("INSERT INTO project_groups (id, name, sort_order, is_deleted, created_at, updated_at) VALUES ('g2', 'Работа', 1, 0, 1, 1)")
            sql.execSQL(
                "INSERT INTO projects (id, group_id, name, sort_order, is_inbox, is_deleted, created_at, updated_at) VALUES ('p1', 'g1', 'Ремонт', 0, 0, 0, 1, 1)",
            )
        }
    }

    private companion object {
        /** Unit-тесты запускаются из папки модуля app. */
        val SCHEMA_DIR = File("schemas/com.prostovasei.simpletasks.data.local.db.AppDatabase")
    }
}
