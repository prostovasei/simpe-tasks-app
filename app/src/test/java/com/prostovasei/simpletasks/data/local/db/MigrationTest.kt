package com.prostovasei.simpletasks.data.local.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Миграции базы: данные PM переживают обновление приложения (итерация 14: 1 → 2, значок списка).
 * База версии 1 создаётся по сохранённой схеме `app/schemas/.../1.json`, затем её открывает текущая версия Room
 * (Room сам сверяет итоговую структуру со схемой и падает при расхождении).
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test
    fun migrate1To2KeepsListsAndAddsEmptyIcon() {
        val file = context.getDatabasePath(DB).apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            schemaSql(1).forEach(db::execSQL)
            db.execSQL(
                "INSERT INTO projects (id, group_id, name, color, sort_order, is_inbox, is_deleted, created_at, updated_at) " +
                    "VALUES ('work', NULL, 'Работа', '#3D8BFD', 0, 0, 0, 1, 1)",
            )
            db.version = 1
        }

        val room = Room.databaseBuilder(context, AppDatabase::class.java, DB).allowMainThreadQueries().build()
        try {
            room.openHelper.readableDatabase.query("SELECT name, color, icon FROM projects WHERE id = 'work'").use { c ->
                c.moveToFirst()
                assertEquals("Работа", c.getString(0))
                assertEquals("#3D8BFD", c.getString(1))
                assertNull(c.getString(2))
            }
            assertEquals(LATEST, room.openHelper.readableDatabase.version)
        } finally {
            room.close()
        }
    }

    /**
     * Итерация 47 (решения PM): 2 → 3 — эмодзи списков вместо значков и два приоритета. Значок переходит в эмодзи, «Входящие» — 📥,
     * эмодзи в начале названия списка из TickTick отделяется; средний приоритет (3) становится высоким (5), низкий (1) — обычным (0).
     */
    @Test
    fun migrate2To3GivesEmojiAndTwoPriorities() {
        val file = context.getDatabasePath(DB).apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            schemaSql(2).forEach(db::execSQL)
            fun list(id: String, name: String, icon: String?, inbox: Int = 0) = db.execSQL(
                "INSERT INTO projects (id, group_id, name, color, sort_order, is_inbox, is_deleted, created_at, updated_at, icon) " +
                    "VALUES (?, NULL, ?, NULL, 0, ?, 0, 1, 1, ?)",
                arrayOf<Any?>(id, name, inbox, icon),
            )
            list("inbox", "Inbox", null, inbox = 1)
            list("work", "Работа", "work")
            list("home", "🏠Дом", null)
            list("plain", "Идеи", null)
            listOf("p0" to 0, "p1" to 1, "p3" to 3, "p5" to 5).forEach { (id, priority) ->
                db.execSQL(
                    "INSERT INTO tasks (id, project_id, title, priority, created_at, updated_at) VALUES (?, 'work', ?, ?, 1, 1)",
                    arrayOf<Any>(id, id, priority),
                )
            }
            db.version = 2
        }

        val room = Room.databaseBuilder(context, AppDatabase::class.java, DB).allowMainThreadQueries().build()
        try {
            val lists = mutableMapOf<String, Pair<String, String?>>()
            room.openHelper.readableDatabase.query("SELECT id, name, emoji FROM projects").use { c ->
                while (c.moveToNext()) lists[c.getString(0)] = c.getString(1) to c.getString(2)
            }
            assertEquals("Inbox" to "📥", lists["inbox"])
            assertEquals("Работа" to "💼", lists["work"])
            assertEquals("Дом" to "🏠", lists["home"])
            assertEquals("Идеи" to null, lists["plain"])
            val priorities = mutableMapOf<String, Int>()
            room.openHelper.readableDatabase.query("SELECT id, priority FROM tasks").use { c ->
                while (c.moveToNext()) priorities[c.getString(0)] = c.getInt(1)
            }
            assertEquals(mapOf("p0" to 0, "p1" to 0, "p3" to 5, "p5" to 5), priorities)
            assertEquals(LATEST, room.openHelper.readableDatabase.version)
        } finally {
            room.close()
        }
    }

    /** SQL создания таблиц и индексов из экспортированной схемы Room. */
    private fun schemaSql(version: Int): List<String> {
        val json = JSONObject(File("schemas/${AppDatabase::class.java.name}/$version.json").readText()).getJSONObject("database")
        val sql = mutableListOf<String>()
        val entities = json.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            sql += entity.getString("createSql").replace("\${TABLE_NAME}", table)
            entity.optJSONArray("indices")?.let { indices ->
                for (j in 0 until indices.length()) sql += indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table)
            }
        }
        val setup = json.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) sql += setup.getString(i)
        return sql
    }

    private companion object {
        const val DB = "migration-test.db"
        const val LATEST = 3
    }
}
