package com.prostovasei.simpletasks.ui.feature.finance

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prostovasei.simpletasks.R
import java.text.Collator
import java.util.Locale

/**
 * Общее для справочников финансов — категорий и мест (#239): порядок по алфавиту (по-русски, без учёта регистра)
 * и окно архивной записи «Вернуть из архива / Удалить навсегда».
 */
private val RU: Collator = Collator.getInstance(Locale.forLanguageTag("ru")).apply { strength = Collator.SECONDARY }

fun <T> List<T>.sortedByName(name: (T) -> String): List<T> = sortedWith { a, b -> RU.compare(name(a), name(b)) }

/** Нажатие на архивную категорию или место (макет 2.3): вернуть или удалить навсегда (только из архива). */
@Composable
fun ArchivedDialog(title: String, uses: Int, tag: String, onRestore: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    FinanceWindow(
        title = title,
        tag = tag,
        onDismiss = onDismiss,
        actions = {
            SecondaryAction(stringResource(R.string.category_restore), "$tag:$TAG_ARCHIVED_RESTORE", onRestore)
            SecondaryAction(stringResource(R.string.delete_forever), "$tag:$TAG_ARCHIVED_DELETE", onDelete, danger = true)
        },
    ) { WindowText(stringResource(R.string.archived_uses, uses)) }
}

const val TAG_ARCHIVED_RESTORE = "restore"
const val TAG_ARCHIVED_DELETE = "delete"
const val TAG_ARCHIVE_CONFIRM = "archive_confirm"
const val TAG_DELETE_CONFIRM = "delete_confirm"
const val TAG_ARCHIVED_WINDOW = "archived_window"
