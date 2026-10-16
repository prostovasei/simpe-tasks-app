package com.prostovasei.simpletasks.ui.common

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.TaskRow
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.CardPosition
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Строка списка задач. Без анимаций (решение PM: всё переключается мгновенно). */
@Composable
fun LazyItemScope.TaskItemRow(
    item: TaskItem,
    onToggle: () -> Unit,
    showToday: Boolean = true,
    showList: Boolean = true,
    isOverdue: Boolean = false,
    onClick: (() -> Unit)? = null,
    position: CardPosition = CardPosition.Single,
) {
    TaskRow(
        title = item.title,
        priority = item.priority,
        meta = taskMeta(item, showToday, showList),
        metaColor = if (isOverdue) AppTheme.colors.overdue else null,
        listColor = if (showList) parseListColor(item.listColor) else null,
        isCompleting = item.isCompleting,
        isRecurring = item.isRecurring,
        isGhost = item.isGhost,
        onToggle = onToggle,
        testTag = "task:${item.id}",
        onClick = onClick,
        position = position,
    )
}

/** Подпись под задачей: «Вчера, 10:00 · Работа». */
@Composable
@ReadOnlyComposable
fun taskMeta(item: TaskItem, showToday: Boolean, showList: Boolean): String {
    val parts = mutableListOf<String>()
    item.due?.let { due ->
        val day = if (due.day == DayLabel.Today && !showToday) null else dayText(due.day)
        listOfNotNull(day, due.time).joinToString(", ").takeIf { it.isNotEmpty() }?.let(parts::add)
    }
    if (showList) {
        val list = if (item.isInboxList) stringResource(R.string.inbox_list_name) else item.listName
        list?.let(parts::add)
    }
    return parts.joinToString(stringResource(R.string.meta_separator))
}

@Composable
@ReadOnlyComposable
private fun dayText(day: DayLabel): String =
    DayNames(stringResource(R.string.day_today), stringResource(R.string.day_tomorrow), stringResource(R.string.day_yesterday)).day(day)

/** Подписи дней, заранее взятые из ресурсов, — чтобы форматировать срок вне Compose (например, в Snackbar). */
class DayNames(private val today: String, private val tomorrow: String, private val yesterday: String) {
    fun day(day: DayLabel): String = when (day) {
        DayLabel.Today -> today
        DayLabel.Tomorrow -> tomorrow
        DayLabel.Yesterday -> yesterday
        is DayLabel.Date -> (if (day.showYear) DATE_WITH_YEAR else DATE).format(day.date)
    }

    /** «Завтра, 10:00». */
    fun format(due: DueText): String = listOfNotNull(day(due.day), due.time).joinToString(", ")
}

private val RUSSIAN = Locale.forLanguageTag("ru")
private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", RUSSIAN)
private val DATE_WITH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", RUSSIAN)

/** Snackbar «Задача выполнена · Отмена». Новый заменяет предыдущий. */
suspend fun SnackbarHostState.showUndo(message: String, actionLabel: String, onUndo: () -> Unit) {
    currentSnackbarData?.dismiss()
    if (showSnackbar(message, actionLabel, withDismissAction = false, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
        onUndo()
    }
}
