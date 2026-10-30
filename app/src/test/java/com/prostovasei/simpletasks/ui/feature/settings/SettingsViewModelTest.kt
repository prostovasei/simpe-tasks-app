package com.prostovasei.simpletasks.ui.feature.settings

import app.cash.turbine.test
import com.prostovasei.simpletasks.domain.model.DataStats
import com.prostovasei.simpletasks.domain.model.ImportProgress
import com.prostovasei.simpletasks.domain.model.ImportReport
import com.prostovasei.simpletasks.domain.model.InvalidImportFileException
import com.prostovasei.simpletasks.domain.repository.BackupRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.prostovasei.simpletasks.ui.feature.task.SmartDateSetting
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.prostovasei.simpletasks.core.files.DocumentOpener
import com.prostovasei.simpletasks.core.update.AppUpdater
import com.prostovasei.simpletasks.core.update.AvailableUpdate
import com.prostovasei.simpletasks.core.update.InstallStart
import com.prostovasei.simpletasks.testing.FakeTimeProvider
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val report = ImportReport(10, 2, 1, 0, 3, 1, 0)

    private class FakeRepository : BackupRepository {
        val stats = MutableStateFlow(DataStats(0, 0, 1))
        var importResult: suspend (onProgress: (ImportProgress) -> Unit) -> ImportReport = { error("не задано") }
        var clearCalls = 0
        val clearGate = CompletableDeferred<Unit>().apply { complete(Unit) }

        var exportResult: suspend () -> Int = { 0 }

        override suspend fun importTickTick(input: InputStream, onProgress: (ImportProgress) -> Unit) = importResult(onProgress)
        override suspend fun exportTickTick(output: OutputStream): Int = exportResult()
        override suspend fun clearAllData() {
            clearGate.await()
            clearCalls++
        }
        override fun observeStats(): Flow<DataStats> = stats
    }

    private val repository = FakeRepository()
    private var openedUri: String? = null
    private val dispatcher = UnconfinedTestDispatcher()

    private fun viewModel(
        open: (String) -> InputStream = { openedUri = it; "".byteInputStream() },
        create: (String) -> OutputStream = { ByteArrayOutputStream() },
    ) = SettingsViewModel(
        repository,
        object : DocumentOpener {
            override fun openInput(uri: String) = open(uri)
            override fun openOutput(uri: String) = create(uri)
        },
        dispatcher,
        FakeTimeProvider(now = Instant.parse("2026-10-13T12:00:00Z")),
        updater,
        smartDates,
    )

    private val smartDates = SmartDateSetting(initial = true, save = {})

    private class FakeUpdater : AppUpdater {
        var available: AvailableUpdate? = null
        var failCheck = false
        var result: InstallStart = InstallStart.Started
        var permissionScreens = 0
        override suspend fun check(): AvailableUpdate? = if (failCheck) throw IOException("нет сети") else available
        override suspend fun install(update: AvailableUpdate, onProgress: (Int) -> Unit): InstallStart {
            onProgress(50)
            return result
        }
        override suspend fun download(update: AvailableUpdate, onProgress: (Int) -> Unit): java.io.File? = error("не используется в «Настройках»")
        override suspend fun commit(apk: java.io.File) = error("не используется в «Настройках»")
        override fun canInstall(): Boolean = true
        override fun openInstallPermissionSettings() {
            permissionScreens++
        }
    }

    private val updater = FakeUpdater()
    private val newVersion = AvailableUpdate(40, "0.10.1", "https://example.com/a.apk")

    @Test
    fun checkShowsUpToDateOrAvailable() = runTest {
        val vm = viewModel()
        vm.onIntent(SettingsIntent.CheckUpdate)
        assertEquals(UpdateState.UpToDate, vm.state.value.update)
        updater.available = newVersion
        vm.onIntent(SettingsIntent.CheckUpdate)
        assertEquals(UpdateState.Available(newVersion), vm.state.value.update)
    }

    @Test
    fun checkWithoutInternetShowsError() = runTest {
        updater.failCheck = true
        val vm = viewModel()
        vm.onIntent(SettingsIntent.CheckUpdate)
        assertEquals(UpdateState.CheckFailed, vm.state.value.update)
    }

    @Test
    fun installAsksPermissionOnceThenConfirms() = runTest {
        updater.available = newVersion
        updater.result = InstallStart.NeedsPermission
        val vm = viewModel()
        vm.onIntent(SettingsIntent.CheckUpdate)
        vm.onIntent(SettingsIntent.InstallUpdate)
        assertEquals(UpdateState.NeedsPermission(newVersion), vm.state.value.update)
        vm.onIntent(SettingsIntent.GrantInstallPermission)
        assertEquals(1, updater.permissionScreens)
        updater.result = InstallStart.Started
        vm.onIntent(SettingsIntent.InstallUpdate)
        assertEquals(UpdateState.Confirming(newVersion), vm.state.value.update)
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun showsDatabaseStats() = runTest {
        val vm = viewModel()
        assertEquals(DataStats(0, 0, 1), vm.state.value.stats)
        repository.stats.value = DataStats(5, 3, 2)
        assertEquals(DataStats(5, 3, 2), vm.state.value.stats)
    }

    @Test
    fun successfulImportShowsProgressThenReport() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.importResult = { onProgress ->
            onProgress(ImportProgress(1, 2))
            gate.await()
            report
        }
        val vm = viewModel()

        vm.onIntent(SettingsIntent.FileSelected("content://backup.csv"))
        assertEquals("content://backup.csv", openedUri)
        assertEquals(ImportState.InProgress(1, 2), vm.state.value.import)
        assertTrue(vm.state.value.isBusy)

        gate.complete(Unit)
        assertEquals(ImportState.Done(report), vm.state.value.import)
        assertFalse(vm.state.value.isBusy)
    }

    @Test
    fun wrongFileShowsFormatError() = runTest {
        repository.importResult = { throw InvalidImportFileException("нет заголовка") }
        val vm = viewModel()

        vm.onIntent(SettingsIntent.FileSelected("content://photo.jpg"))

        assertEquals(ImportState.Failed(ImportFailure.NOT_TICKTICK_FILE), vm.state.value.import)
    }

    @Test
    fun unreadableFileShowsReadError() = runTest {
        val vm = viewModel(open = { throw IOException("нет доступа") })

        vm.onIntent(SettingsIntent.FileSelected("content://gone.csv"))

        assertEquals(ImportState.Failed(ImportFailure.READ_ERROR), vm.state.value.import)
    }

    @Test
    fun clearRequiresConfirmation() = runTest {
        val vm = viewModel()

        vm.onIntent(SettingsIntent.ClearRequested)
        assertTrue(vm.state.value.showClearConfirmation)
        assertEquals(0, repository.clearCalls)

        vm.onIntent(SettingsIntent.ClearDismissed)
        assertFalse(vm.state.value.showClearConfirmation)
        assertEquals(0, repository.clearCalls)
    }

    @Test
    fun confirmedClearWipesDataAndNotifies() = runTest {
        repository.importResult = { report }
        val vm = viewModel()
        vm.onIntent(SettingsIntent.FileSelected("content://backup.csv"))

        vm.effects.test {
            vm.onIntent(SettingsIntent.ClearRequested)
            vm.onIntent(SettingsIntent.ClearConfirmed)
            assertEquals(SettingsEffect.DataCleared, awaitItem())
        }
        assertEquals(1, repository.clearCalls)
        assertEquals(ImportState.Idle, vm.state.value.import)
        assertFalse(vm.state.value.showClearConfirmation)
        assertFalse(vm.state.value.isClearing)
    }

    @Test
    fun suggestsBackupFileNameWithDate() = runTest {
        assertEquals("simple-tasks-backup-2026-10-13.csv", viewModel().state.value.backupFileName)
    }

    @Test
    fun exportWritesFileAndShowsCount() = runTest {
        repository.exportResult = { 3399 }
        var createdUri: String? = null
        val vm = viewModel(create = { createdUri = it; ByteArrayOutputStream() })

        vm.onIntent(SettingsIntent.ExportTo("content://backup-out.csv"))

        assertEquals("content://backup-out.csv", createdUri)
        assertEquals(ExportState.Done(3399), vm.state.value.export)
    }

    @Test
    fun exportWriteErrorIsShown() = runTest {
        val vm = viewModel(create = { throw IOException("диск полон") })

        vm.onIntent(SettingsIntent.ExportTo("content://backup-out.csv"))

        assertEquals(ExportState.Failed, vm.state.value.export)
    }

    @Test
    fun secondImportIsIgnoredWhileBusy() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        repository.importResult = {
            calls++
            gate.await()
            report
        }
        val vm = viewModel()

        vm.onIntent(SettingsIntent.FileSelected("content://a.csv"))
        vm.onIntent(SettingsIntent.FileSelected("content://b.csv"))
        vm.onIntent(SettingsIntent.ClearRequested)

        assertEquals(1, calls)
        assertFalse(vm.state.value.showClearConfirmation)
        gate.complete(Unit)
    }

    @Test
    fun smartDatesSwitchIsSaved() {
        val vm = viewModel()
        assertTrue(vm.state.value.smartDates)
        vm.onIntent(SettingsIntent.SetSmartDates(false))
        assertFalse(vm.state.value.smartDates)
        assertFalse(smartDates.enabled.value)
    }
}
