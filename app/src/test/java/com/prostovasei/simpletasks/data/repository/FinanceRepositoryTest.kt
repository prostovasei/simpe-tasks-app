package com.prostovasei.simpletasks.data.repository

import com.prostovasei.simpletasks.core.id.UuidIdGenerator
import com.prostovasei.simpletasks.data.importer.ticktick.TickTickExporter
import com.prostovasei.simpletasks.data.importer.ticktick.TickTickImporter
import com.prostovasei.simpletasks.domain.model.NewTask
import com.prostovasei.simpletasks.domain.model.finance.AccountDraft
import com.prostovasei.simpletasks.domain.model.finance.AccountType
import com.prostovasei.simpletasks.testing.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

/** Счета финансов (этап 1, #221): создание, порядок, правка; очистка данных задач счета не трогает. */
class FinanceRepositoryTest : DatabaseTest() {
    private val finance by lazy { RoomFinanceRepository(db, time, UuidIdGenerator()) }

    @Test
    fun accountsAreCreatedInOrderWithBalance() = runTest {
        assertTrue(finance.observeAccounts().first().isEmpty())
        finance.createAccount(AccountDraft(" Карта ", AccountType.CARD, null, 4_230_000))
        time.advance(Duration.ofMinutes(1))
        finance.createAccount(AccountDraft("Заначка", AccountType.CASH, "🧦", 615_050))

        val accounts = finance.observeAccounts().first()
        assertEquals(listOf("Карта", "Заначка"), accounts.map { it.name })
        assertEquals(listOf(AccountType.CARD, AccountType.CASH), accounts.map { it.type })
        assertEquals(listOf("💳", "🧦"), accounts.map { it.emojiShown })
        assertNull(accounts[0].emoji)
        assertEquals(listOf(4_230_000L, 615_050L), accounts.map { it.balance })
        assertEquals(accounts.map { it.balance }, accounts.map { it.initialBalance })
    }

    @Test
    fun accountIsEdited() = runTest {
        val id = finance.createAccount(AccountDraft("Карта", AccountType.CARD, null, 100))
        finance.updateAccount(id, AccountDraft("Вклад", AccountType.SAVINGS, "💰", 8_000_000))

        val account = finance.observeAccounts().first().single()
        assertEquals(id, account.id)
        assertEquals("Вклад", account.name)
        assertEquals(AccountType.SAVINGS, account.type)
        assertEquals("💰", account.emoji)
        assertEquals(8_000_000L, account.balance)
    }

    @Test
    fun blankNameIsRejected() = runTest {
        assertTrue(runCatching { finance.createAccount(AccountDraft("  ", AccountType.CARD, null, 0)) }.exceptionOrNull() is IllegalArgumentException)
    }

    /** «Очистить все данные» в настройках задач удаляет задачи и списки, но не финансы. */
    @Test
    fun clearingTaskDataKeepsAccounts() = runTest {
        finance.createAccount(AccountDraft("Карта", AccountType.CARD, null, 4_230_000))
        tasks.createTask(NewTask(projectId = projects.getInbox().id, title = "Задача"))
        val backup = RoomBackupRepository(db, time, TickTickImporter(db, time, UuidIdGenerator()), TickTickExporter(db, time))

        backup.clearAllData()

        assertEquals(0, backup.observeStats().first().totalTasks)
        assertTrue(projects.getInbox().isInbox)
        assertEquals(listOf("Карта"), finance.observeAccounts().first().map { it.name })
    }
}
