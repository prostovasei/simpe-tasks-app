package com.prostovasei.simpletasks.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Ответ GitHub «последний релиз» → новая версия или ничего. */
@RunWith(RobolectricTestRunner::class)
class ParseLatestReleaseTest {
    private val json = """
        {"tag_name": "build-40", "name": "0.10.1 (сборка 40)",
         "assets": [{"name": "notes.txt", "browser_download_url": "https://x/notes.txt"},
                    {"name": "simple-tasks-0.10.1-build40.apk", "browser_download_url": "https://x/simple-tasks-0.10.1-build40.apk"}]}
    """.trimIndent()

    @Test
    fun newerBuildIsAnUpdate() {
        assertEquals(AvailableUpdate(40, "0.10.1", "https://x/simple-tasks-0.10.1-build40.apk"), parseLatestRelease(json, currentBuild = 34))
    }

    @Test
    fun sameOrOlderBuildIsNotAnUpdate() {
        assertNull(parseLatestRelease(json, currentBuild = 40))
        assertNull(parseLatestRelease(json, currentBuild = 41))
    }

    @Test
    fun releaseWithoutApkOrWithStrangeTagIsIgnored() {
        assertNull(parseLatestRelease("""{"tag_name": "build-50", "name": "x", "assets": []}""", 1))
        assertNull(parseLatestRelease("""{"tag_name": "v1.0", "name": "x", "assets": []}""", 1))
    }
}
