package com.prostovasei.simpletasks.testing

import androidx.test.core.app.ApplicationProvider
import com.prostovasei.simpletasks.core.id.UuidIdGenerator
import com.prostovasei.simpletasks.data.local.db.AppDatabase
import com.prostovasei.simpletasks.data.repository.RoomChecklistRepository
import com.prostovasei.simpletasks.data.repository.RoomProjectRepository
import com.prostovasei.simpletasks.data.repository.RoomTaskRepository
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Основа тестов БД: настоящая Room-база в памяти, управляемое время. Запросы идут в потоке теста (прямой исполнитель):
 * у Room нет фоновых потоков, которые могли бы упасть после закрытия базы уже во время следующего теста (бэклог №54).
 */
@RunWith(RobolectricTestRunner::class)
abstract class DatabaseTest {
    protected val time = FakeTimeProvider()
    protected lateinit var db: AppDatabase
    protected lateinit var tasks: RoomTaskRepository
    protected lateinit var projects: RoomProjectRepository
    protected lateinit var checklist: RoomChecklistRepository

    @Before
    fun openDatabase() {
        db = AppDatabase.build(ApplicationProvider.getApplicationContext(), time, inMemory = true, executor = { it.run() })
        val ids = UuidIdGenerator()
        tasks = RoomTaskRepository(db, time, ids)
        projects = RoomProjectRepository(db, time, ids)
        checklist = RoomChecklistRepository(db, time, ids)
    }

    @After
    fun closeDatabase() {
        db.close()
    }
}
