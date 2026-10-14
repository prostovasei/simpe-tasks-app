package com.prostovasei.simpletasks.data.importer.ticktick

import java.time.format.DateTimeFormatter

/** Формат резервной копии TickTick: общий для импорта и выгрузки. */
internal object TickTickFormat {
    /** Колонки в порядке TickTick (версия резервной копии 7.2). */
    val COLUMNS = listOf(
        "Folder Name", "List Name", "Title", "Kind", "Tags", "Content", "Is Check list", "Start Date", "Due Date",
        "Reminder", "Repeat", "Priority", "Status", "Created Time", "Completed Time", "Order", "Timezone",
        "Is All Day", "Is Floating", "Column Name", "Column Order", "View Mode", "taskId", "parentId", "projectKind",
    )

    const val VERSION = "7.2"
    const val STATUS_LEGEND = "Status: \n0 Normal\n-1 Abandoned \n2 Completed"
    const val INBOX = "Inbox"

    /** Пункты чек-листа в поле Content. */
    const val CHECKED = '▪'
    const val UNCHECKED = '▫'

    /** Разделитель строк внутри Content в файлах TickTick. */
    const val CONTENT_LINE_BREAK = "\r"

    /** Даты TickTick: 2026-10-13T03:00:00+0000. */
    val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxx")
}
