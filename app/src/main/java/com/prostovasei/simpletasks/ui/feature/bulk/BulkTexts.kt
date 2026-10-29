package com.prostovasei.simpletasks.ui.feature.bulk

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.UndoKind
import com.prostovasei.simpletasks.widget.WidgetDates

/** Тексты и значки подсказки «Отмена» после массового действия (итерация 40). Строки берутся один раз в композиции. */
class BulkTexts private constructor(private val templates: Map<BulkKind, String>, private val inbox: String) {

    fun text(result: BulkResult): String {
        val template = templates.getValue(result.kind)
        return when (result.kind) {
            BulkKind.OtherDay -> template.format(result.date?.let { WidgetDates.short(it) }.orEmpty(), result.count)
            BulkKind.Moved -> template.format(result.listName ?: inbox, result.count)
            else -> template.format(result.count)
        }
    }

    fun kind(result: BulkResult): UndoKind = when (result.kind) {
        BulkKind.Done -> UndoKind.Done
        BulkKind.Tomorrow -> UndoKind.Tomorrow
        BulkKind.OtherDay -> UndoKind.Calendar
        BulkKind.Moved -> UndoKind.List
        BulkKind.Priority -> UndoKind.Priority
        BulkKind.Reopened, BulkKind.Restored -> UndoKind.Restored
        BulkKind.Deleted -> UndoKind.Deleted
    }

    companion object {
        @Composable
        fun load() = BulkTexts(
            mapOf(
                BulkKind.Done to stringResource(R.string.bulk_done),
                BulkKind.Tomorrow to stringResource(R.string.bulk_moved_tomorrow),
                BulkKind.OtherDay to stringResource(R.string.bulk_moved_day),
                BulkKind.Moved to stringResource(R.string.bulk_moved_list),
                BulkKind.Priority to stringResource(R.string.bulk_priority_set),
                BulkKind.Reopened to stringResource(R.string.bulk_reopened),
                BulkKind.Deleted to stringResource(R.string.bulk_deleted),
                BulkKind.Restored to stringResource(R.string.trash_restored_many),
            ),
            stringResource(R.string.inbox_list_name),
        )
    }
}
