package com.prostovasei.simpletasks.ui.feature.snooze

import android.content.Context
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.size
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.core.di.ApplicationScope
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.domain.model.Moved
import com.prostovasei.simpletasks.domain.model.Snooze
import com.prostovasei.simpletasks.domain.model.SnoozeKind
import com.prostovasei.simpletasks.domain.model.SnoozeOption
import com.prostovasei.simpletasks.domain.model.Task
import com.prostovasei.simpletasks.domain.model.TaskDue
import com.prostovasei.simpletasks.domain.repository.TaskRepository
import com.prostovasei.simpletasks.notifications.ReminderNotifications
import com.prostovasei.simpletasks.ui.feature.task.DialogOption
import com.prostovasei.simpletasks.ui.feature.task.SheetDialog
import com.prostovasei.simpletasks.widget.WidgetDates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Вариант меню «Отложить» с подписями (итерация 53, макет iteration53.py): [label] — «Через 1 час», [whenText] — новый срок справа
 * («18:00» сегодня, «чт, 12 ноя, 17:00» в другой день, «чт, 12 ноя» у задачи на весь день). [option] = null — «Выбрать дату…».
 */
@Immutable
data class SnoozeChoice(val label: String, val whenText: String, val option: SnoozeOption?, val tag: String)

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Новый срок для подписи: время сегодня, иначе день с временем (или без него у задачи на весь день). */
fun snoozeWhen(due: TaskDue, today: LocalDate, zone: ZoneId): String = when (due) {
    is TaskDue.AllDay -> WidgetDates.withWeekday(due.date)
    is TaskDue.AtTime -> {
        val at = due.instant.atZone(zone)
        if (at.toLocalDate() == today) TIME.format(at) else "${WidgetDates.withWeekday(at.toLocalDate())}, ${TIME.format(at)}"
    }
}

/** Варианты «Отложить» задачи [task] сейчас — с подписями; последний — «Выбрать дату и время…» («Выбрать дату…» на весь день). */
fun snoozeChoices(context: Context, task: Task, time: TimeProvider): List<SnoozeChoice> {
    val zone = time.zone()
    val today = time.today()
    val options = Snooze.options(task.due, task.start, time.now(), zone).map { option ->
        SnoozeChoice(context.getString(option.kind.label), snoozeWhen(option.start ?: option.due, today, zone), option, "$TAG_SNOOZE_OPTION:${option.kind.name}")
    }
    val pick = if (task.due is TaskDue.AtTime) R.string.snooze_pick_time else R.string.snooze_pick_day
    return options + SnoozeChoice(context.getString(pick), "", null, TAG_SNOOZE_PICK)
}

private val SnoozeKind.label: Int
    get() = when (this) {
        SnoozeKind.HOUR -> R.string.snooze_hour
        SnoozeKind.THREE_HOURS -> R.string.snooze_three_hours
        SnoozeKind.EVENING -> R.string.snooze_evening
        SnoozeKind.TOMORROW -> R.string.snooze_tomorrow
        SnoozeKind.DAY_AFTER -> R.string.snooze_day_after
        SnoozeKind.SATURDAY -> R.string.snooze_saturday
        SnoozeKind.WEEK -> R.string.snooze_week
    }

/** Окно «Отложить» из уведомления: задача, варианты и подпись задачи («Созвон с командой · сегодня, 17:00»). */
data class SnoozeState(val title: String = "", val subtitle: String = "", val choices: List<SnoozeChoice> = emptyList(), val loaded: Boolean = false)

/**
 * «Отложить» из уведомления (итерация 53): Android не показывает меню в самом уведомлении, поэтому кнопка открывает окно поверх экрана.
 * Перенос и «Отмена» идут в области приложения — окно закрывается раньше, чем истечёт подсказка.
 */
@HiltViewModel
class SnoozeViewModel @Inject constructor(
    private val tasks: TaskRepository,
    private val time: TimeProvider,
    private val notifications: ReminderNotifications,
    @param:ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {
    private val _state = MutableStateFlow(SnoozeState())
    val state: StateFlow<SnoozeState> = _state

    fun load(context: Context, taskId: String, subtitle: (Task) -> String) {
        if (_state.value.loaded) return
        viewModelScope.launch {
            val task = tasks.getTask(taskId)
            _state.value = if (task == null) SnoozeState(loaded = true) else SnoozeState(task.title, subtitle(task), snoozeChoices(context, task, time), loaded = true)
        }
    }

    /** Перенос на [option]; [done] — с текстом подсказки и «Отменой». Уведомление задачи убирается. */
    fun snooze(taskId: String, option: SnoozeOption, done: (String, () -> Unit) -> Unit) {
        notifications.cancel(taskId)
        val text = snoozeWhen(option.start ?: option.due, time.today(), time.zone())
        appScope.launch {
            val moved: Moved? = tasks.snooze(taskId, option.due, option.start)
            withContext(Dispatchers.Main) { done(text) { if (moved != null) appScope.launch { tasks.undoMove(moved) } } }
        }
    }

    /** «Выбрать дату…»: уведомление убирается, дальше — окно даты задачи. */
    fun pickDate(taskId: String) = notifications.cancel(taskId)
}

/**
 * Окно «Отложить» поверх экрана (макет: доска «Уведомление»): заголовок, задача, варианты с новым сроком справа, «Отмена».
 * Вариант срабатывает сразу. Задачи нет — окно закрывается.
 */
@Composable
fun SnoozeRoute(
    taskId: String,
    onDismiss: () -> Unit,
    onSnoozed: (text: String, undo: () -> Unit) -> Unit,
    onPickDate: () -> Unit,
    subtitle: (Task) -> String,
    viewModel: SnoozeViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(taskId) { viewModel.load(context, taskId, subtitle) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.loaded) return
    if (state.choices.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val doneText = stringResource(R.string.snooze_done)
    SnoozeDialog(state, onDismiss) { choice ->
        val option = choice.option
        if (option == null) {
            viewModel.pickDate(taskId)
            onPickDate()
        } else {
            viewModel.snooze(taskId, option) { text, undo -> onSnoozed(doneText.format(text), undo) }
        }
    }
}

@Composable
fun SnoozeDialog(state: SnoozeState, onDismiss: () -> Unit, onPick: (SnoozeChoice) -> Unit) {
    SheetDialog(stringResource(R.string.snooze_action), TAG_SNOOZE_DIALOG, onDismiss, onDone = null, subtitle = state.subtitle.ifEmpty { null }) {
        state.choices.forEach { choice ->
            DialogOption(
                title = choice.label,
                selected = false,
                tag = choice.tag,
                onClick = { onPick(choice) },
                hint = choice.whenText.ifEmpty { null },
                leading = if (choice.option == null) {
                    { Icon(painterResource(R.drawable.ic_widget_all), null, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(AppTheme.sizes.metaIcon)) }
                } else {
                    null
                },
            )
        }
    }
}

const val TAG_SNOOZE_DIALOG = "snooze_dialog"
const val TAG_SNOOZE_OPTION = "snooze_option"
const val TAG_SNOOZE_PICK = "snooze_pick"
