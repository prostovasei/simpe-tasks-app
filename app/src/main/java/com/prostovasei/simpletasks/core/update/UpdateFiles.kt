package com.prostovasei.simpletasks.core.update

import java.io.File

/**
 * Скачанные обновления в `cacheDir` (#312). Файл нужен только до передачи установщику Android — тот копирует его в свою
 * сессию. Раньше файлы оставались навсегда: ~19 МБ на каждое обновление, у PM набралось 378 МБ кэша.
 */
internal object UpdateFiles {
    private const val PREFIX = "update-"
    private const val SUFFIX = ".apk"

    fun create(dir: File): File = File.createTempFile(PREFIX, SUFFIX, dir)

    /** Удаляет оставшиеся скачанные обновления; возвращает, сколько файлов удалено. Другие файлы кэша не трогает. */
    fun clear(dir: File): Int =
        dir.listFiles { file -> file.isFile && file.name.startsWith(PREFIX) && file.name.endsWith(SUFFIX) }?.count { it.delete() } ?: 0
}
