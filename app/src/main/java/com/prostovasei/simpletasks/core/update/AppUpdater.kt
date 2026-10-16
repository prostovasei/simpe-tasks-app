package com.prostovasei.simpletasks.core.update

import org.json.JSONObject

/** Новая версия на GitHub: [build] — номер сборки (versionCode), [apkUrl] — файл для скачивания. */
data class AvailableUpdate(val build: Int, val versionName: String, val apkUrl: String)

sealed interface InstallStart {
    /** Скачано, Android показывает окно «Обновить приложение?». */
    data object Started : InstallStart

    /** Сначала нужно разрешить приложению устанавливать обновления (один раз). */
    data object NeedsPermission : InstallStart
}

/**
 * Обновление приложения по кнопке (итерация 10, docs/ARCHITECTURE.md §4.14). APK выкладывает автосборка
 * в публичный репозиторий [RELEASES_REPO] — только файлы, без кода.
 */
interface AppUpdater {
    /** Есть ли сборка новее установленной; null — обновлений нет. Ошибка сети — исключение. */
    suspend fun check(): AvailableUpdate?

    /** Скачать и передать установщику Android. [onProgress] — проценты 0..100. */
    suspend fun install(update: AvailableUpdate, onProgress: (Int) -> Unit): InstallStart

    /** Открыть системный экран «Установка неизвестных приложений» для нашего приложения. */
    fun openInstallPermissionSettings()
}

const val RELEASES_REPO = "prostovasei/simple-tasks-releases"

private val BUILD_TAG = Regex("""build-(\d+)""")

/**
 * Ответ GitHub «последний релиз»: тег `build-34`, название `0.10.0 (сборка 34)`, файл `*.apk`.
 * Возвращает обновление, только если сборка новее [currentBuild].
 */
fun parseLatestRelease(json: String, currentBuild: Int): AvailableUpdate? {
    val release = JSONObject(json)
    val build = BUILD_TAG.matchEntire(release.optString("tag_name"))?.groupValues?.get(1)?.toIntOrNull() ?: return null
    if (build <= currentBuild) return null
    val assets = release.optJSONArray("assets") ?: return null
    val apk = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.optString("name").endsWith(".apk") } ?: return null
    val versionName = release.optString("name").substringBefore(" (").ifBlank { "сборка $build" }
    return AvailableUpdate(build, versionName, apk.getString("browser_download_url"))
}
