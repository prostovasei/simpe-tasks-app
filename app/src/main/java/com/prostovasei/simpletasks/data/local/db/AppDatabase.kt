package com.prostovasei.simpletasks.data.local.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.prostovasei.simpletasks.domain.model.ListEmoji
import com.prostovasei.simpletasks.domain.model.Priority
import androidx.sqlite.db.SupportSQLiteDatabase
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.data.local.dao.ChecklistDao
import com.prostovasei.simpletasks.data.local.dao.ProjectDao
import com.prostovasei.simpletasks.data.local.dao.ProjectGroupDao
import com.prostovasei.simpletasks.data.local.dao.TaskDao
import com.prostovasei.simpletasks.data.local.entity.ChecklistItemEntity
import com.prostovasei.simpletasks.data.local.entity.ProjectEntity
import com.prostovasei.simpletasks.data.local.entity.ProjectGroupEntity
import com.prostovasei.simpletasks.data.local.entity.TaskEntity
import java.util.concurrent.Executor

@Database(
    entities = [
        ProjectGroupEntity::class,
        ProjectEntity::class,
        TaskEntity::class,
        ChecklistItemEntity::class,
    ],
    // 1 → 2 (итерация 14): колонка projects.icon. 2 → 3 (итерация 47): колонка projects.emoji и два приоритета ([V2to3]).
    // Данные PM сохраняются автоматической миграцией Room.
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3, spec = V2to3::class)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectGroupDao(): ProjectGroupDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun checklistDao(): ChecklistDao

    companion object {
        const val NAME = "simple_tasks.db"

        /** Единая точка создания БД: в приложении и в тестах подключается один и тот же InboxSeed. */
        fun build(context: Context, time: TimeProvider, inMemory: Boolean = false, executor: Executor? = null): AppDatabase {
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            } else {
                Room.databaseBuilder(context, AppDatabase::class.java, NAME)
            }
            // [executor] — только для тестов: запросы и транзакции идут в том же потоке, без фоновых потоков Room,
            // которые могли пережить тест и уронить следующий (бэклог №54).
            if (executor != null) builder.allowMainThreadQueries().setQueryExecutor(executor).setTransactionExecutor(executor)
            return builder.addCallback(InboxSeed(time)).build()
        }
    }
}

/**
 * Миграция 2 → 3 (итерация 47, решения PM): эмодзи списков вместо значков — у списков со значком эмодзи по нему
 * ([ListEmoji.BY_ICON]), у «Входящих» — 📥; приоритетов два — средний (3) становится высоким (5), низкий (1) — обычным (0).
 */
class V2to3 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        ListEmoji.BY_ICON.forEach { (icon, emoji) ->
            db.execSQL("UPDATE projects SET emoji = ? WHERE icon = ? AND emoji IS NULL", arrayOf<Any>(emoji, icon))
        }
        db.execSQL("UPDATE projects SET emoji = ? WHERE is_inbox = 1", arrayOf<Any>(ListEmoji.INBOX))
        // Списки из TickTick с эмодзи в начале названия («🏠Дом»): эмодзи — отдельно, название — «Дом».
        val named = mutableListOf<Pair<String, String>>()
        db.query("SELECT id, name FROM projects WHERE emoji IS NULL AND is_inbox = 0").use { c ->
            while (c.moveToNext()) named += c.getString(0) to c.getString(1)
        }
        named.forEach { (id, name) ->
            val (emoji, rest) = ListEmoji.split(name)
            if (emoji != null) db.execSQL("UPDATE projects SET emoji = ?, name = ? WHERE id = ?", arrayOf<Any>(emoji, rest, id))
        }
        db.execSQL("UPDATE tasks SET priority = ? WHERE priority = ?", arrayOf<Any>(Priority.HIGH.value, Priority.MEDIUM_TICKTICK))
        db.execSQL("UPDATE tasks SET priority = ? WHERE priority = ?", arrayOf<Any>(Priority.NONE.value, LOW_TICKTICK))
    }

    private companion object {
        /** Низкий приоритет TickTick. */
        const val LOW_TICKTICK = 1
    }
}

/**
 * Гарантирует, что системный список «Входящие» существует ровно в одном экземпляре (аудит R4).
 * Выполняется при каждом открытии БД; повторный запуск ничего не меняет.
 */
class InboxSeed(private val time: TimeProvider) : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) = seed(db)

    fun seed(db: SupportSQLiteDatabase) {
        val now = time.now().toEpochMilli()
        db.execSQL(
            """
            INSERT OR IGNORE INTO projects (id, group_id, name, color, sort_order, is_inbox, is_deleted, created_at, updated_at)
            VALUES (?, NULL, ?, NULL, 0, 1, 0, ?, ?)
            """.trimIndent(),
            arrayOf<Any>(INBOX_ID, INBOX_NAME, now, now),
        )
    }

    companion object {
        /** Фиксированный UUIDv4 «Входящих»: одинаковый на всех устройствах (задел под синхронизацию). */
        const val INBOX_ID = "00000000-0000-4000-8000-000000000001"

        /** Служебное имя; на экране показывается локализованное название из ресурсов. */
        const val INBOX_NAME = "Inbox"
    }
}
