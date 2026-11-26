package com.prostovasei.simpletasks.ui.feature.update

import app.cash.turbine.test
import com.prostovasei.simpletasks.core.update.AppUpdater
import com.prostovasei.simpletasks.core.update.AvailableUpdate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

/** Окно обновления с виджета: проверка → найдено → загрузка → «Обновить» (итерация 32). */
@OptIn(ExperimentalCoroutinesApi::class)
class UpdateDialogViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val newVersion = AvailableUpdate(130, "0.32.0", "https://example.com/a.apk", 16_000_000)
    private val apk = File("update.apk")

    private class FakeUpdater : AppUpdater {
        var available: AvailableUpdate? = null
        var checkGate: CompletableDeferred<Unit>? = null
        var failCheck = false
        var failDownload = false
        var allowed = true
        var downloadGate: CompletableDeferred<Unit>? = null
        var downloads = 0
        var committed: File? = null
        var failCommit = false
        var permissionScreens = 0

        override suspend fun check(): AvailableUpdate? {
            checkGate?.await()
            if (failCheck) throw IOException("нет сети")
            return available
        }

        override suspend fun download(update: AvailableUpdate, onProgress: (Int) -> Unit): File? {
            if (!allowed) return null
            downloads++
            onProgress(40)
            downloadGate?.await()
            if (failDownload) throw IOException("обрыв")
            return File("update.apk")
        }

        override suspend fun commit(apk: File) {
            if (failCommit) throw IOException("установщик")
            committed = apk
        }

        override fun canInstall(): Boolean = allowed
        override fun openInstallPermissionSettings() {
            permissionScreens++
        }
    }

    private val updater = FakeUpdater()

    private fun viewModel(auto: Boolean = false) = UpdateDialogViewModel(
        updater, androidx.lifecycle.SavedStateHandle(mapOf(com.prostovasei.simpletasks.UpdateActivity.EXTRA_AUTO to auto)),
    )

    /** #354: кнопка «Обновить приложение» в «Главной» — найденная версия сразу скачивается и передаётся установщику. */
    @Test
    fun autoModeDownloadsAndInstallsAtOnce() = runTest {
        updater.available = newVersion
        val vm = viewModel(auto = true)
        assertEquals(UpdateDialogState.Installing(newVersion), vm.state.value)
        assertEquals(apk, updater.committed)
    }

    @Test
    fun autoModeWithoutUpdateShowsUpToDate() = runTest {
        val vm = viewModel(auto = true)
        assertEquals(UpdateDialogState.UpToDate, vm.state.value)
        assertEquals(0, updater.downloads)
    }

    @Test
    fun startsByCheckingAndShowsUpToDate() = runTest {
        val vm = viewModel()
        assertEquals(UpdateDialogState.UpToDate, vm.state.value)
    }

    @Test
    fun checkingIsShownWhileWaiting() = runTest {
        updater.checkGate = CompletableDeferred()
        val vm = viewModel()
        assertEquals(UpdateDialogState.Checking, vm.state.value)
        updater.available = newVersion
        updater.checkGate!!.complete(Unit)
        assertEquals(UpdateDialogState.Available(newVersion), vm.state.value)
    }

    @Test
    fun checkWithoutInternetCanBeRetried() = runTest {
        updater.failCheck = true
        val vm = viewModel()
        assertEquals(UpdateDialogState.CheckFailed, vm.state.value)
        updater.failCheck = false
        updater.available = newVersion
        vm.onIntent(UpdateDialogIntent.Retry)
        assertEquals(UpdateDialogState.Available(newVersion), vm.state.value)
    }

    @Test
    fun downloadThenInstallSendsFileToInstaller() = runTest {
        updater.available = newVersion
        val vm = viewModel()
        vm.onIntent(UpdateDialogIntent.Download)
        assertEquals(UpdateDialogState.Ready(newVersion, apk), vm.state.value)
        // Пока не нажали «Обновить», установщику ничего не отдано.
        assertEquals(null, updater.committed)
        vm.onIntent(UpdateDialogIntent.Install)
        assertEquals(apk, updater.committed)
        assertEquals(UpdateDialogState.Installing(newVersion), vm.state.value)
    }

    @Test
    fun progressIsShownDuringDownload() = runTest {
        updater.available = newVersion
        updater.downloadGate = CompletableDeferred()
        val vm = viewModel()
        vm.onIntent(UpdateDialogIntent.Download)
        assertEquals(UpdateDialogState.Downloading(newVersion, 40), vm.state.value)
        updater.downloadGate!!.complete(Unit)
        assertTrue(vm.state.value is UpdateDialogState.Ready)
    }

    @Test
    fun downloadFailureCanBeRetried() = runTest {
        updater.available = newVersion
        updater.failDownload = true
        val vm = viewModel()
        vm.onIntent(UpdateDialogIntent.Download)
        assertEquals(UpdateDialogState.DownloadFailed(newVersion), vm.state.value)
        updater.failDownload = false
        vm.onIntent(UpdateDialogIntent.Retry)
        assertEquals(UpdateDialogState.Ready(newVersion, apk), vm.state.value)
        assertEquals(2, updater.downloads)
    }

    @Test
    fun missingPermissionAsksOnceAndContinuesAfterReturn() = runTest {
        updater.available = newVersion
        updater.allowed = false
        val vm = viewModel()
        vm.onIntent(UpdateDialogIntent.Download)
        assertEquals(UpdateDialogState.NeedsPermission(newVersion), vm.state.value)
        vm.onIntent(UpdateDialogIntent.GrantPermission)
        assertEquals(1, updater.permissionScreens)
        // Вернулись, не разрешив, — всё как было; разрешили — снова можно «Загрузить».
        vm.onIntent(UpdateDialogIntent.Resume)
        assertEquals(UpdateDialogState.NeedsPermission(newVersion), vm.state.value)
        updater.allowed = true
        vm.onIntent(UpdateDialogIntent.Resume)
        assertEquals(UpdateDialogState.Available(newVersion), vm.state.value)
    }

    @Test
    fun closeCancelsDownloadAndClosesWindow() = runTest {
        updater.available = newVersion
        updater.downloadGate = CompletableDeferred()
        val vm = viewModel()
        vm.onIntent(UpdateDialogIntent.Download)
        vm.effects.test {
            vm.onIntent(UpdateDialogIntent.Close)
            assertEquals(UpdateDialogEffect.Close, awaitItem())
        }
        // Загрузка остановлена: после «Закрыть» состояние не доходит до «Загружено».
        updater.downloadGate!!.complete(Unit)
        assertFalse(vm.state.value is UpdateDialogState.Ready)
    }

    @Test
    fun installerFailureIsShownAsFailedDownload() = runTest {
        updater.available = newVersion
        updater.failCommit = true
        val vm = viewModel()
        vm.onIntent(UpdateDialogIntent.Download)
        vm.onIntent(UpdateDialogIntent.Install)
        assertEquals(UpdateDialogState.DownloadFailed(newVersion), vm.state.value)
    }

    @Test
    fun intentsOutOfOrderAreIgnored() = runTest {
        val vm = viewModel()
        vm.onIntent(UpdateDialogIntent.Download)
        vm.onIntent(UpdateDialogIntent.Install)
        vm.onIntent(UpdateDialogIntent.Retry)
        assertEquals(UpdateDialogState.UpToDate, vm.state.value)
        assertEquals(0, updater.downloads)
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()
}
