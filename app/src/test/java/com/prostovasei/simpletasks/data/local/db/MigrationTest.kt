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

    /**
     * Миграция 5 → 6 (финансы, этап 1, #221): появляется пустая таблица счетов, а папки, списки и задачи PM остаются как были.
     */
    @Test
    fun financeAccountsTableIsAddedAndTasksAreKept() {
        val file = context.getDatabasePath(AppDatabase.NAME).apply { parentFile?.mkdirs(); delete() }
        createVersion(file, 5) { sql ->
            sql.execSQL("INSERT INTO project_groups (id, name, sort_order, is_deleted, created_at, updated_at, emoji) VALUES ('g1', 'Дом', 0, 0, 1, 1, '🏠')")
            sql.execSQL("INSERT INTO projects (id, group_id, name, sort_order, is_inbox, is_deleted, created_at, updated_at) VALUES ('p1', 'g1', 'Ремонт', 0, 0, 0, 1, 1)")
            sql.execSQL("INSERT INTO tasks (id, project_id, title, status, priority, is_all_day, sort_order, is_deleted, created_at, updated_at) VALUES ('t1', 'p1', 'Купить краску', 0, 0, 1, 0, 0, 1, 1)")
        }

        val db = AppDatabase.build(context, FakeTimeProvider())
        try {
            assertEquals(emptyList<Any>(), runBlocking { db.financeAccountDao().observeAll().first() })
            assertEquals("🏠", runBlocking { db.projectGroupDao().getById("g1") }!!.emoji)
            assertEquals("Ремонт", runBlocking { db.projectDao().getById("p1") }!!.name)
            val title = db.openHelper.readableDatabase.query("SELECT title FROM tasks WHERE id = 't1'").use { it.moveToFirst(); it.getString(0) }
            assertEquals("Купить краску", title)
        } finally {
            db.close()
        }
    }

    /** Миграция 6 → 7 (финансы, этап 2, #225): появляются пустые таблицы операций и категорий; счета и задачи остаются. */
    @Test
    fun operationsAndCategoriesTablesAreAddedAndAccountsAreKept() {
        val file = context.getDatabasePath(AppDatabase.NAME).apply { parentFile?.mkdirs(); delete() }
        createVersion(file, 6) { sql ->
            sql.execSQL("INSERT INTO projects (id, name, sort_order, is_inbox, is_deleted, created_at, updated_at) VALUES ('p1', 'Ремонт', 0, 0, 0, 1, 1)")
            sql.execSQL("INSERT INTO finance_accounts (id, name, type, emoji, initial_balance, sort_order, created_at, updated_at) VALUES ('a1', 'Яндекс', 'card', NULL, 4230000, 0, 1, 1)")
        }

        val db = AppDatabase.build(context, FakeTimeProvider())
        try {
            val account = runBlocking { db.financeAccountDao().observeAll().first() }.single()
            assertEquals("Яндекс", account.account.name)
            assertEquals(4_230_000L, account.balance)
            assertEquals(emptyList<Any>(), runBlocking { db.financeOperationDao().observeActive().first() })
            assertEquals(emptyList<Any>(), runBlocking { db.financeOperationDao().observeCategories().first() })
            assertEquals("Ремонт", runBlocking { db.projectDao().getById("p1") }!!.name)
        } finally {
            db.close()
        }
    }

    /** Миграция 7 → 8 (уведомления банка, #233): появляется пустая таблица покупок; счета и операции остаются. */
    @Test
    fun bankPurchasesTableIsAddedAndOperationsAreKept() {
        val file = context.getDatabasePath(AppDatabase.NAME).apply { parentFile?.mkdirs(); delete() }
        createVersion(file, 7) { sql ->
            sql.execSQL("INSERT INTO finance_accounts (id, name, type, emoji, initial_balance, sort_order, created_at, updated_at) VALUES ('a1', 'Карта', 'card', NULL, 1000000, 0, 1, 1)")
            sql.execSQL("INSERT INTO finance_operations (id, kind, amount, account_id, date, is_deleted, created_at, updated_at) VALUES ('o1', 'expense', 68000, 'a1', 20739, 0, 1, 1)")
        }

        val db = AppDatabase.build(context, FakeTimeProvider())
        try {
            assertEquals(932_000L, runBlocking { db.financeAccountDao().observeAll().first() }.single().balance)
            assertEquals("o1", runBlocking { db.financeOperationDao().observeActive().first() }.single().operation.id)
            assertEquals(emptyList<Any>(), runBlocking { db.bankPurchaseDao().observePending().first() })
        } finally {
            db.close()
        }
    }

    /** Миграция 8 → 9 (эмодзи категорий, #237): колонка emoji у категорий пустая, категории, операции и счета остаются. */
    @Test
    fun categoryEmojiColumnIsAddedAndCategoriesAreKept() {
        val file = context.getDatabasePath(AppDatabase.NAME).apply { parentFile?.mkdirs(); delete() }
        createVersion(file, 8) { sql ->
            sql.execSQL("INSERT INTO finance_accounts (id, name, type, emoji, initial_balance, sort_order, created_at, updated_at) VALUES ('a1', 'Карта', 'card', NULL, 1000000, 0, 1, 1)")
            sql.execSQL("INSERT INTO finance_categories (id, name, kind, is_archived, sort_order, created_at, updated_at) VALUES ('c1', 'Продукты', 'expense', 0, 0, 1, 1)")
            sql.execSQL("INSERT INTO finance_operations (id, kind, amount, account_id, category_id, date, is_deleted, created_at, updated_at) VALUES ('o1', 'expense', 68000, 'a1', 'c1', 20739, 0, 1, 1)")
        }

        val db = AppDatabase.build(context, FakeTimeProvider())
        try {
            val category = runBlocking { db.financeOperationDao().observeCategories().first() }.single()
            assertEquals("Продукты", category.category.name)
            assertEquals(null, category.category.emoji)
            assertEquals("Продукты", runBlocking { db.financeOperationDao().observeActive().first() }.single().categoryName)
            assertEquals(932_000L, runBlocking { db.financeAccountDao().observeAll().first() }.single().balance)
        } finally {
            db.close()
        }
    }

    /** Миграция 9 → 10 (справочник мест, #239): места из операций попадают в справочник по одному на название; операции не меняются. */
    @Test
    fun placesDirectoryIsFilledFromOperations() {
        val file = context.getDatabasePath(AppDatabase.NAME).apply { parentFile?.mkdirs(); delete() }
        createVersion(file, 9) { sql ->
            sql.execSQL("INSERT INTO finance_accounts (id, name, type, emoji, initial_balance, sort_order, created_at, updated_at) VALUES ('a1', 'Карта', 'card', NULL, 1000000, 0, 1, 1)")
            sql.execSQL("INSERT INTO finance_operations (id, kind, amount, account_id, place, date, is_deleted, created_at, updated_at) VALUES ('o1', 'expense', 100, 'a1', 'Пятёрочка', 20739, 0, 1, 1)")
            sql.execSQL("INSERT INTO finance_operations (id, kind, amount, account_id, place, date, is_deleted, created_at, updated_at) VALUES ('o2', 'expense', 200, 'a1', 'Пятёрочка', 20739, 0, 2, 2)")
            sql.execSQL("INSERT INTO finance_operations (id, kind, amount, account_id, place, date, is_deleted, created_at, updated_at) VALUES ('o3', 'expense', 300, 'a1', 'Озон', 20739, 1, 3, 3)")
            sql.execSQL("INSERT INTO finance_operations (id, kind, amount, account_id, place, date, is_deleted, created_at, updated_at) VALUES ('o4', 'expense', 400, 'a1', NULL, 20739, 0, 4, 4)")
        }

        val db = AppDatabase.build(context, FakeTimeProvider())
        try {
            val places = runBlocking { db.financePlaceDao().observeAll().first() }
            assertEquals(setOf("Пятёрочка" to 2, "Озон" to 1), places.map { it.place.name to it.uses }.toSet())
            assertEquals(4, runBlocking { db.financeOperationDao().getAll() }.size)
            assertEquals(1_000_000L - 100 - 200 - 400, runBlocking { db.financeAccountDao().observeAll().first() }.single().balance)
        } finally {
            db.close()
        }
    }

    /** Миграция 10 → 11 (раздел «Здоровье», #257): появляется пустая таблица веса, а задачи и финансы PM остаются как были. */
    @Test
    fun weightTableIsAddedAndExistingDataIsKept() {
        val file = context.getDatabasePath(AppDatabase.NAME).apply { parentFile?.mkdirs(); delete() }
        createVersion(file, 10) { sql ->
            sql.execSQL("INSERT INTO projects (id, name, sort_order, is_inbox, is_deleted, created_at, updated_at) VALUES ('p1', 'Ремонт', 0, 0, 0, 1, 1)")
            sql.execSQL("INSERT INTO tasks (id, project_id, title, status, priority, is_all_day, sort_order, is_deleted, created_at, updated_at) VALUES ('t1', 'p1', 'Купить краску', 0, 0, 1, 0, 0, 1, 1)")
            sql.execSQL("INSERT INTO finance_accounts (id, name, type, emoji, initial_balance, sort_order, created_at, updated_at) VALUES ('a1', 'Карта', 'card', NULL, 4230000, 0, 1, 1)")
            sql.execSQL("INSERT INTO finance_places (id, name, is_archived, created_at, updated_at) VALUES ('pl1', 'Пятёрочка', 0, 1, 1)")
        }

        val db = AppDatabase.build(context, FakeTimeProvider())
        try {
            assertEquals(emptyList<Any>(), runBlocking { db.weightEntryDao().getAll() })
            // Новая таблица работает: запись, и вторая на ту же дату заменяет первую.
            runBlocking {
                db.weightEntryDao().upsert(com.prostovasei.simpletasks.data.local.entity.WeightEntryEntity("w1", 20_000, 824, null, 1, 1))
                db.weightEntryDao().upsert(com.prostovasei.simpletasks.data.local.entity.WeightEntryEntity("w2", 20_000, 826, "утро", 2, 2))
            }
            assertEquals(listOf(826), runBlocking { db.weightEntryDao().getAll() }.map { it.tenths })
            val title = db.openHelper.readableDatabase.query("SELECT title FROM tasks WHERE id = 't1'").use { it.moveToFirst(); it.getString(0) }
            assertEquals("Купить краску", title)
            assertEquals("Карта", runBlocking { db.financeAccountDao().observeAll().first() }.single().account.name)
            assertEquals("Пятёрочка", runBlocking { db.financePlaceDao().getById("pl1") }!!.name)
        } finally {
            db.close()
        }
    }

    private fun createVersion4(file: File) = createVersion(file, 4) { sql ->
        sql.execSQL("INSERT INTO project_groups (id, name, sort_order, is_deleted, created_at, updated_at) VALUES ('g1', '🏠Дом', 0, 0, 1, 1)")
        sql.execSQL("INSERT INTO project_groups (id, name, sort_order, is_deleted, created_at, updated_at) VALUES ('g2', 'Работа', 1, 0, 1, 1)")
        sql.execSQL(
            "INSERT INTO projects (id, group_id, name, sort_order, is_inbox, is_deleted, created_at, updated_at) VALUES ('p1', 'g1', 'Ремонт', 0, 0, 0, 1, 1)",
        )
    }

    /** База версии [version] по выгруженной схеме, затем строки [fill]. */
    private fun createVersion(file: File, version: Int, fill: (SQLiteDatabase) -> Unit) {
        val schema = JSONObject(File(SCHEMA_DIR, "$version.json").readText()).getJSONObject("database")
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
            sql.version = version
            fill(sql)
        }
    }

    private companion object {
        /** Unit-тесты запускаются из папки модуля app. */
        val SCHEMA_DIR = File("schemas/com.prostovasei.simpletasks.data.local.db.AppDatabase")
    }
}
