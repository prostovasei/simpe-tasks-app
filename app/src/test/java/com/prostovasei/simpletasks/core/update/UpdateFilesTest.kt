package com.prostovasei.simpletasks.core.update

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** #312: скачанные обновления не копятся в кэше. */
@RunWith(RobolectricTestRunner::class)
class UpdateFilesTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun clearRemovesOnlyDownloadedUpdates() {
        val dir = folder.root
        val first = UpdateFiles.create(dir).apply { writeBytes(ByteArray(10)) }
        val second = UpdateFiles.create(dir)
        val other = dir.resolve("image_cache.bin").apply { writeText("keep") }
        val apkWithoutPrefix = dir.resolve("app.apk").apply { writeText("keep") }
        assertEquals(2, UpdateFiles.clear(dir))
        assertFalse(first.exists())
        assertFalse(second.exists())
        assertTrue("Другие файлы кэша не трогаем", other.exists() && apkWithoutPrefix.exists())
        assertEquals("Повторная чистка — нечего удалять", 0, UpdateFiles.clear(dir))
    }

    /** После передачи установщику скачанный файл удаляется. */
    @Test
    fun commitDeletesDownloadedFile() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apk = UpdateFiles.create(context.cacheDir).apply { writeBytes(ByteArray(1024)) }
        val updater = GitHubAppUpdater(context, UnconfinedTestDispatcher(testScheduler))
        runCatching { updater.commit(apk) }
        assertFalse("Файл в кэше не остаётся", apk.exists())
    }
}
