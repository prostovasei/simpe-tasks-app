package com.prostovasei.simpletasks.ui.common

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.components.TaskRow
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.parseListColor
import com.prostovasei.simpletasks.widget.WidgetDates
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Плашка задачи в списке. Отмеченная «рассыпается в пыль», затем список плавно смыкается (итерация 15).
 * [showDay] — справа день и время (у «Входящих» и списков); в ленте «Все» день уже в подписи — только время.
 */
@Composable
fun LazyItemScope.TaskItemRow(
    item: TaskItem,
    onToggle: () -> Unit,
    showDay: Boolean = true,
    showList: Boolean = true,
    isOverdue: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    TaskRow(
        modifier = Modifier.animateItem(),
        title = item.title,
        priority = item.priority,
        listColor = if (showList) listSquareColor(item) else null,
        listIcon = ListIcons.drawable(item.listIcon),
        isCompleting = item.isCompleting,
        isRecurring = item.isRecurring,
        reminder = item.reminder,
        isGhost = item.isGhost,
        isOverdue = isOverdue,
        trailing = if (showDay) dueLines(item) else item.timeLines(),
        onToggle = onToggle,
        testTag = "task:${item.id}",
        onClick = onClick,
    )
}

/** Цвет квадратика списка: у «Входящих» свой, у списка без цвета — нейтральный. */
@Composable
@ReadOnlyComposable
fun listSquareColor(item: TaskItem): Color =
    if (item.isInboxList) AppTheme.colors.inboxList else parseListColor(item.listColor) ?: AppTheme.colors.inactive

/** Время справа: «16:00» или интервал столбиком — «16:00», «17:00». */
fun TaskItem.timeLines(): List<String> = due?.time?.split("–").orEmpty()

/** День и время справа: «Сегодня» / «16 окт» и «10:00». */
@Composable
@ReadOnlyComposable
fun dueLines(item: TaskItem): List<String> {
    val due = item.due ?: return emptyList()
    val day = when (val label = due.day) {
        is DayLabel.Date -> WidgetDates.short(label.date)
        else -> dayText(label)
    }
    return listOfNotNull(day, due.time)
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
