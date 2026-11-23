package com.prostovasei.simpletasks.core.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.prostovasei.simpletasks.BuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Проверка на GitHub, скачивание и установка через системный установщик Android (PackageInstaller). */
class GitHubAppUpdater(private val context: Context, private val io: CoroutineDispatcher) : AppUpdater {

    override suspend fun check(): AvailableUpdate? = withContext(io) {
        val connection = open("https://api.github.com/repos/$RELEASES_REPO/releases/latest")
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> parseLatestRelease(connection.inputStream.bufferedReader().readText(), BuildConfig.VERSION_CODE)
                // Релизов ещё нет.
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw IOException("GitHub: ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun download(update: AvailableUpdate, onProgress: (Int) -> Unit): File? {
        if (!canInstall()) return null
        return withContext(io) { download(update.apkUrl, onProgress) }
    }

    override suspend fun commit(apk: File) = withContext(io) { commitSession(apk) }

    override fun canInstall(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    override fun openInstallPermissionSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Скачивание в `cacheDir`; отмена корутины ([ensureActive] на каждом куске) обрывает его и удаляет недокачанный файл. */
    private fun CoroutineScope.download(url: String, onProgress: (Int) -> Unit): File {
        val file = UpdateFiles.create(context.cacheDir)
        val connection = open(url)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("Скачивание: ${connection.responseCode}")
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER)
                    var done = 0L
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) onProgress((done * PERCENT / total).toInt())
                    }
                }
            }
        } catch (e: Throwable) {
            file.delete()
            throw e
        } finally {
            connection.disconnect()
        }
        return file
    }

    /**
     * Передать файл установщику; окно «Обновить?» покажет [UpdateInstallReceiver]. Установщик копирует файл к себе, поэтому
     * скачанный файл сразу удаляется — и при ошибке тоже (#312): повторная попытка скачает заново.
     */
    private fun commitSession(apk: File) {
        try {
            writeSession(apk)
        } finally {
            apk.delete()
        }
    }

    private fun writeSession(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            // Android 12+: когда приложение уже ставило себя само, обновление может пройти без вопроса.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("simple-tasks.apk", 0, apk.length()).use { output ->
                apk.inputStream().use { it.copyTo(output) }
                session.fsync(output)
            }
            val result = Intent(context, UpdateInstallReceiver::class.java).setAction(UpdateInstallReceiver.ACTION)
            // Установщик дописывает в интент статус — поэтому изменяемый.
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            session.commit(PendingIntent.getBroadcast(context, sessionId, result, flags).intentSender)
        }
    }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        instanceFollowRedirects = true
    }

    private companion object {
        const val TIMEOUT_MS = 15_000
        const val BUFFER = 64 * 1024
        const val PERCENT = 100
    }
}
