package com.prostovasei.simpletasks.ui.feature.habits

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostovasei.simpletasks.R
import com.prostovasei.simpletasks.designsystem.theme.AppTheme
import com.prostovasei.simpletasks.designsystem.theme.TaskCanvasTheme
import com.prostovasei.simpletasks.designsystem.tokens.SizeTokens
import com.prostovasei.simpletasks.domain.model.habits.Habit
import com.prostovasei.simpletasks.domain.model.habits.HabitMath
import com.prostovasei.simpletasks.domain.model.habits.HabitSchedule
import com.prostovasei.simpletasks.ui.feature.finance.FinanceField
import com.prostovasei.simpletasks.ui.feature.finance.FinanceWindow
import com.prostovasei.simpletasks.ui.feature.finance.PrimaryAction
import com.prostovasei.simpletasks.ui.feature.finance.TwoActions
import com.prostovasei.simpletasks.ui.feature.finance.WindowText
import com.prostovasei.simpletasks.ui.feature.project.EmojiPickerDialog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Раздел «Привычки» (#340, ТЗ design/approved/340): сверху «N из M сегодня», ниже привычки на сегодня, затем «Не сегодня».
 * Белый плоский экран, как «Здоровье»; список идёт под общую капсулу.
 */
@Composable
fun HabitsRoute(viewModel: HabitsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HabitsEffects(viewModel)
    TaskCanvasTheme { HabitsScreen(state, viewModel::onIntent) }
}

/** Подсказки о резервной копии — на экране раздела и на страницах настроек. */
@Composable
fun HabitsEffects(viewModel: HabitsViewModel) {
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            val (text, long) = when (effect) {
                HabitsEffect.BackupSaved -> R.string.habits_backup_saved to false
                HabitsEffect.BackupFailed -> R.string.health_backup_failed to true
                HabitsEffect.BackupInvalid -> R.string.habits_backup_invalid to true
                HabitsEffect.BackupRestored -> R.string.habits_backup_restored to false
            }
            Toast.makeText(context, text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun HabitsScreen(state: HabitsState, onIntent: (HabitsIntent) -> Unit) {
    val colors = AppTheme.colors
    val habits = state.habits
    Box(Modifier.fillMaxSize().background(colors.background).testTag(TAG_HABITS)) {
        when {
            habits == null -> Unit
            state.active.isEmpty() -> HabitsEmpty { onIntent(HabitsIntent.Add) }
            else -> HabitsList(state, onIntent)
        }
    }
    state.open?.let { HabitCard(it, state, onIntent) }
    state.form?.let { HabitFormDialog(it, onIntent) }
}

/** Окно привычки поверх другого экрана (хаб): «+» → «Привычка». */
@Composable
fun HabitFormHost(state: HabitsState, onIntent: (HabitsIntent) -> Unit) {
    state.form?.let { HabitFormDialog(it, onIntent) }
}

@Composable
private fun HabitsEmpty(onAdd: () -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(horizontal = spacing.xl).testTag(TAG_HABITS_EMPTY),
    ) {
        Icon(painterResource(R.drawable.ic_module_habits), contentDescription = null, tint = colors.inactive, modifier = Modifier.size(AppTheme.sizes.financeEmptyIcon))
        Text(stringResource(R.string.habits_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.padding(top = spacing.sm))
        Text(
            stringResource(R.string.habits_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xs, bottom = spacing.lg),
        )
        PrimaryAction(stringResource(R.string.habit_add), TAG_HABITS_ADD_FIRST, onAdd, plus = true)
    }
}

@Composable
private fun HabitsList(state: HabitsState, onIntent: (HabitsIntent) -> Unit) {
    val today = state.today
    val (due, rest) = state.active.partition { HabitMath.dueOn(it, today) }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + SizeTokens.NAV_BAR_SPACE.dp
    LazyColumn(Modifier.fillMaxSize().testTag(TAG_HABITS_LIST), contentPadding = PaddingValues(bottom = bottom)) {
        item(key = "summary") { HabitsSummary(due.count { it.isDone(today) }, due.size) }
        items(due, key = { it.id }) { HabitRow(it, today, muted = false, onIntent) }
        if (rest.isNotEmpty()) {
            item(key = "rest") {
                Text(
                    stringResource(R.string.habits_not_today), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = AppTheme.spacing.md, top = AppTheme.spacing.md, bottom = AppTheme.spacing.xxs).testTag(TAG_HABITS_REST),
                )
            }
            items(rest, key = { it.id }) { HabitRow(it, today, muted = true, onIntent) }
        }
    }
}

/** «2 из 5 сегодня» и тонкая полоса — как у «Сегодня» в задачах. */
@Composable
private fun HabitsSummary(done: Int, total: Int) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        Modifier.fillMaxWidth().padding(horizontal = spacing.md).padding(top = spacing.md, bottom = spacing.xs).testTag(TAG_HABITS_SUMMARY),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.habits_today, done, total), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Spacer(Modifier.width(spacing.sm))
        Box(Modifier.weight(1f).height(AppTheme.sizes.hairline * 2).background(colors.divider)) {
            if (total > 0) Box(Modifier.fillMaxWidth(done.toFloat() / total).height(AppTheme.sizes.hairline * 2).background(colors.textPrimary))
        }
    }
}

/** Строка привычки (ТЗ §4): эмодзи · название и точки недели · серия · круг отметки. Нажатие на строку — карточка. */
@Composable
internal fun HabitRow(habit: Habit, today: LocalDate, muted: Boolean, onIntent: (HabitsIntent) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Row(
        Modifier.fillMaxWidth().heightIn(min = SizeTokens.HABIT_ROW.dp)
            .clickable(role = Role.Button) { onIntent(HabitsIntent.Open(habit.id)) }
            .padding(horizontal = spacing.md, vertical = spacing.xs).testTag("$TAG_HABIT_ROW:${habit.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val alpha = if (muted) MUTED_ALPHA else 1f
        Text(habit.emojiShown, fontSize = SizeTokens.HABIT_EMOJI_SP.sp, modifier = Modifier.width(SizeTokens.HABIT_CHECK.dp), textAlign = TextAlign.Center, color = colors.textPrimary.copy(alpha = alpha))
        Column(Modifier.weight(1f).padding(horizontal = spacing.sm)) {
            Text(habit.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary.copy(alpha = alpha), maxLines = 1, overflow = TextOverflow.Ellipsis)
            WeekDots(habit, today, Modifier.padding(top = spacing.xxs))
        }
        val streak = HabitMath.streakLabel(habit, today)
        if (streak.isNotEmpty()) {
            Text(streak, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (muted) colors.textSecondary else colors.warning,
                modifier = Modifier.padding(end = spacing.sm).testTag("$TAG_HABIT_STREAK:${habit.id}"))
        }
        HabitCheck(habit.isDone(today), habit.name) { onIntent(HabitsIntent.Toggle(habit.id, today)) }
    }
}

/** Точки недели пн–вс: сделано — заполненная, день расписания — кольцо, вне расписания — бледная; сегодня — обведена фиолетовым. */
@Composable
internal fun WeekDots(habit: Habit, today: LocalDate, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val accent = colors.priority.high.content
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(SizeTokens.HABIT_DOT_GAP.dp), verticalAlignment = Alignment.CenterVertically) {
        HabitMath.week(today).forEach { day ->
            val done = habit.isDone(day)
            val scheduled = HabitMath.scheduledOn(habit.schedule, day)
            val base = Modifier.size(SizeTokens.HABIT_DOT.dp).clip(CircleShape)
            val dot = when {
                done -> base.background(colors.textPrimary)
                scheduled -> base.border(AppTheme.sizes.hairline * 1.5f, colors.inactive, CircleShape)
                else -> base.background(colors.divider)
            }
            Box(if (day == today) Modifier.border(AppTheme.sizes.hairline * 1.5f, accent, CircleShape).padding(AppTheme.sizes.hairline * 1.5f) else Modifier) { Box(dot) }
        }
    }
}

/** Круг отметки 28: пустой — серое кольцо, сделано — фиолетовый с галочкой. */
@Composable
internal fun HabitCheck(done: Boolean, name: String, onToggle: () -> Unit) {
    val colors = AppTheme.colors
    val description = stringResource(if (done) R.string.habit_uncheck else R.string.habit_check, name)
    val state = stringResource(if (done) R.string.habit_done else R.string.habit_not_done)
    Box(
        Modifier.size(SizeTokens.HABIT_CHECK.dp).clip(CircleShape)
            .then(if (done) Modifier.background(colors.priority.high.content) else Modifier.border(SizeTokens.HABIT_RING.dp, colors.inactive, CircleShape))
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics { contentDescription = description; stateDescription = state }
            .testTag(TAG_HABIT_CHECK),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Default.Check, null, tint = colors.onAccent, modifier = Modifier.size(AppTheme.sizes.moduleIcon))
    }
}

/** Карточка привычки (ТЗ §5): серия, лучшая, за 30 дней, календарь месяца, действия. */
@Composable
internal fun HabitCard(habit: Habit, state: HabitsState, onIntent: (HabitsIntent) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val today = state.today
    val close = { onIntent(HabitsIntent.CloseCard) }
    if (state.confirmDelete) {
        FinanceWindow(
            title = stringResource(R.string.habit_delete_title),
            tag = TAG_HABIT_DELETE_WINDOW,
            onDismiss = { onIntent(HabitsIntent.CancelDelete) },
            actions = { TwoActions(stringResource(R.string.action_cancel), stringResource(R.string.habit_delete), TAG_HABIT_DELETE_WINDOW, { onIntent(HabitsIntent.CancelDelete) }, { onIntent(HabitsIntent.ConfirmDelete) }, danger = true) },
        ) { WindowText(stringResource(R.string.habit_delete_text, habit.name)) }
        return
    }
    FinanceWindow(
        title = "${habit.emojiShown} ${habit.name}",
        tag = TAG_HABIT_CARD,
        onDismiss = close,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                if (habit.archived) {
                    CardAction(stringResource(R.string.habit_unarchive), "$TAG_HABIT_CARD:unarchive", Modifier.weight(1f)) { onIntent(HabitsIntent.Archive(habit.id, false)) }
                    CardAction(stringResource(R.string.habit_delete), "$TAG_HABIT_CARD:delete", Modifier.weight(1f), danger = true) { onIntent(HabitsIntent.AskDelete) }
                } else {
                    CardAction(stringResource(R.string.habit_up), "$TAG_HABIT_CARD:up", Modifier.weight(1f)) { onIntent(HabitsIntent.Move(habit.id, true)) }
                    CardAction(stringResource(R.string.habit_down), "$TAG_HABIT_CARD:down", Modifier.weight(1f)) { onIntent(HabitsIntent.Move(habit.id, false)) }
                    CardAction(stringResource(R.string.habit_archive), "$TAG_HABIT_CARD:archive", Modifier.weight(1f)) { onIntent(HabitsIntent.Archive(habit.id, true)) }
                }
            }
            PrimaryAction(stringResource(R.string.habit_edit), "$TAG_HABIT_CARD:edit", { onIntent(HabitsIntent.CloseCard); onIntent(HabitsIntent.Edit(habit.id)) })
        },
    ) {
        Text(scheduleText(habit.schedule), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Row(Modifier.fillMaxWidth().padding(top = spacing.sm), horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            val weeks = habit.schedule is HabitSchedule.PerWeek
            Stat(stringResource(R.string.habit_streak), streakValue(HabitMath.streak(habit, today), weeks), "$TAG_HABIT_CARD:streak", Modifier.weight(1f))
            Stat(stringResource(R.string.habit_best), streakValue(HabitMath.bestStreak(habit, today), weeks), "$TAG_HABIT_CARD:best", Modifier.weight(1f))
            Stat(stringResource(R.string.habit_rate), "${HabitMath.rate(habit, today)}%", "$TAG_HABIT_CARD:rate", Modifier.weight(1f))
        }
        MonthCalendar(habit, state.month, today, { onIntent(HabitsIntent.ShiftMonth(it)) }) { onIntent(HabitsIntent.Toggle(habit.id, it)) }
    }
}

@Composable
private fun streakValue(n: Int, weeks: Boolean): String = if (weeks) stringResource(R.string.habit_weeks, n) else stringResource(R.string.habit_days, n)

@Composable
private fun Stat(label: String, value: String, tag: String, modifier: Modifier) {
    val colors = AppTheme.colors
    Column(modifier.clip(AppTheme.shapes.large).background(colors.selected).padding(AppTheme.spacing.sm).testTag(tag)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1)
    }
}

@Composable
private fun CardAction(label: String, tag: String, modifier: Modifier, danger: Boolean = false, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Text(
        label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (danger) colors.danger else colors.textPrimary,
        textAlign = TextAlign.Center, maxLines = 1,
        modifier = modifier.clip(CircleShape).background(colors.selected).clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = AppTheme.spacing.sm).testTag(tag),
    )
}

/** Календарь месяца (ТЗ §5): сделано — фиолетовый круг, день расписания — кольцо, будущее — бледное; прошедшие дни нажимаются. */
@Composable
private fun MonthCalendar(habit: Habit, month: YearMonth, today: LocalDate, onShift: (Int) -> Unit, onToggle: (LocalDate) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    Column(Modifier.fillMaxWidth().padding(top = spacing.md).testTag(TAG_HABIT_CALENDAR)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonthArrow("‹", stringResource(R.string.habit_month_prev), "$TAG_HABIT_CALENDAR:prev") { onShift(-1) }
            Text(monthTitle(month), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.textPrimary, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).testTag("$TAG_HABIT_CALENDAR:title"))
            MonthArrow("›", stringResource(R.string.habit_month_next), "$TAG_HABIT_CALENDAR:next") { onShift(1) }
        }
        Row(Modifier.fillMaxWidth().padding(top = spacing.xs)) {
            WEEKDAYS.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        val first = month.atDay(1)
        val offset = first.dayOfWeek.value - 1
        val cells = offset + month.lengthOfMonth()
        val accent = colors.priority.high.content
        (0 until (cells + 6) / 7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(top = spacing.xxs)) {
                (0 until 7).forEach { col ->
                    val index = week * 7 + col - offset
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (index in 0 until month.lengthOfMonth()) {
                            val day = first.plusDays(index.toLong())
                            val done = habit.isDone(day)
                            val future = day.isAfter(today)
                            val scheduled = HabitMath.scheduledOn(habit.schedule, day)
                            val label = stringResource(if (done) R.string.habit_uncheck_day else R.string.habit_check_day, day.dayOfMonth)
                            Box(
                                Modifier.size(SizeTokens.HABIT_CAL_DAY.dp).clip(CircleShape)
                                    .then(
                                        when {
                                            done -> Modifier.background(accent)
                                            day == today -> Modifier.border(SizeTokens.HABIT_RING.dp, accent, CircleShape)
                                            scheduled && !future -> Modifier.border(AppTheme.sizes.hairline * 1.5f, colors.inactive, CircleShape)
                                            else -> Modifier
                                        },
                                    )
                                    .then(if (future) Modifier else Modifier.clickable(role = Role.Checkbox) { onToggle(day) }.semantics { contentDescription = label })
                                    .testTag("$TAG_HABIT_DAY:$day"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    day.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        done -> colors.onAccent
                                        future -> colors.inactive
                                        else -> colors.textPrimary
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthArrow(symbol: String, description: String, tag: String, onClick: () -> Unit) {
    Box(
        Modifier.size(SizeTokens.HABIT_CAL_DAY.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }.testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Text(symbol, style = MaterialTheme.typography.titleLarge, color = AppTheme.colors.textPrimary) }
}

/** Окно создания и правки (ТЗ §6): название, эмодзи, расписание. */
@Composable
private fun HabitFormDialog(form: HabitForm, onIntent: (HabitsIntent) -> Unit) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val edit: (HabitForm.() -> HabitForm) -> Unit = { onIntent(HabitsIntent.EditForm(it)) }
    var picker by rememberSaveable { mutableStateOf(false) }
    FinanceWindow(
        title = stringResource(if (form.id == null) R.string.habit_new_title else R.string.habit_edit_title),
        tag = TAG_HABIT_FORM,
        onDismiss = { onIntent(HabitsIntent.CloseForm) },
        actions = { PrimaryAction(stringResource(R.string.action_save), TAG_HABIT_SAVE, { onIntent(HabitsIntent.SaveForm) }, enabled = form.canSave) },
    ) {
        FinanceField(stringResource(R.string.habit_name)) {
            val style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary, textAlign = TextAlign.End)
            BasicTextField(
                value = form.name,
                onValueChange = { value -> edit { copy(name = value.take(Habit.MAX_NAME)) } },
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().testTag(TAG_HABIT_NAME),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (form.name.isEmpty()) Text(stringResource(R.string.habit_name_hint), style = style, color = colors.inactive)
                        inner()
                    }
                },
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = spacing.sm), horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            QUICK_EMOJI.forEach { emoji ->
                EmojiChip(emoji, selected = (form.emoji ?: Habit.DEFAULT_EMOJI) == emoji, Modifier.weight(1f)) { edit { copy(emoji = emoji) } }
            }
            val other = form.emoji?.takeIf { it !in QUICK_EMOJI }
            EmojiChip(other ?: "…", selected = other != null, Modifier.weight(1f), tag = "$TAG_HABIT_EMOJI:more") { picker = true }
        }
        Row(Modifier.fillMaxWidth().padding(top = spacing.md).clip(AppTheme.shapes.large).background(colors.selected).padding(AppTheme.sizes.hairline * 3)) {
            listOf(
                ScheduleKind.DAILY to R.string.habit_daily,
                ScheduleKind.WEEKDAYS to R.string.habit_weekdays,
                ScheduleKind.PER_WEEK to R.string.habit_per_week,
            ).forEach { (kind, label) ->
                val on = form.kind == kind
                Text(
                    stringResource(label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1,
                    color = if (on) colors.textPrimary else colors.textSecondary,
                    modifier = Modifier.weight(1f).clip(AppTheme.shapes.medium).background(if (on) colors.surface else Color.Transparent)
                        .clickable(role = Role.Tab) { edit { copy(kind = kind) } }.padding(vertical = spacing.xs).testTag("$TAG_HABIT_KIND:${kind.name}"),
                )
            }
        }
        when (form.kind) {
            ScheduleKind.WEEKDAYS -> Row(Modifier.fillMaxWidth().padding(top = spacing.sm), horizontalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                DayOfWeek.entries.forEach { day ->
                    val bit = 1 shl (day.value - 1)
                    val on = form.mask and bit != 0
                    Text(
                        WEEKDAYS[day.value - 1], style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        color = if (on) colors.priority.high.content else colors.textPrimary,
                        modifier = Modifier.weight(1f).clip(AppTheme.shapes.medium).background(if (on) colors.priority.high.container else colors.selected)
                            .clickable(role = Role.Checkbox) { edit { copy(mask = mask xor bit) } }.padding(vertical = spacing.xs).testTag("$TAG_HABIT_DAY_CHIP:${day.name}"),
                    )
                }
            }
            ScheduleKind.PER_WEEK -> Row(Modifier.fillMaxWidth().padding(top = spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.habit_times_week, form.perWeek), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary,
                    modifier = Modifier.weight(1f).testTag(TAG_HABIT_PER_WEEK))
                Stepper("−", stringResource(R.string.habit_less), "$TAG_HABIT_PER_WEEK:minus", form.perWeek > 1) { edit { copy(perWeek = perWeek - 1) } }
                Spacer(Modifier.width(spacing.xs))
                Stepper("+", stringResource(R.string.habit_more), "$TAG_HABIT_PER_WEEK:plus", form.perWeek < HabitSchedule.MAX_PER_WEEK) { edit { copy(perWeek = perWeek + 1) } }
            }
            ScheduleKind.DAILY -> Unit
        }
        if (form.kind == ScheduleKind.WEEKDAYS && form.mask == 0) WindowText(stringResource(R.string.habit_pick_day))
        Spacer(Modifier.height(spacing.sm))
    }
    if (picker) {
        EmojiPickerDialog(form.emoji ?: Habit.DEFAULT_EMOJI, onConfirm = { chosen -> edit { copy(emoji = chosen) }; picker = false }, onDismiss = { picker = false })
    }
}

@Composable
private fun EmojiChip(emoji: String, selected: Boolean, modifier: Modifier, tag: String = "$TAG_HABIT_EMOJI:$emoji", onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        modifier.height(SizeTokens.HABIT_CAL_DAY.dp + AppTheme.spacing.xs).clip(AppTheme.shapes.medium)
            .background(if (selected) colors.priority.high.container else colors.selected)
            .then(if (selected) Modifier.border(SizeTokens.HABIT_RING.dp, colors.priority.high.content, AppTheme.shapes.medium) else Modifier)
            .clickable(role = Role.Button, onClick = onClick).testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = SizeTokens.HABIT_EMOJI_SP.sp, color = colors.textPrimary) }
}

@Composable
private fun Stepper(symbol: String, description: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        Modifier.size(SizeTokens.HABIT_CAL_DAY.dp).clip(CircleShape).background(colors.selected)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = description }.testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Text(symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (enabled) colors.textPrimary else colors.inactive) }
}

@Composable
internal fun scheduleText(schedule: HabitSchedule): String = when (schedule) {
    HabitSchedule.Daily -> stringResource(R.string.habit_daily)
    is HabitSchedule.Weekdays -> DayOfWeek.entries.filter { schedule.has(it) }.joinToString(", ") { WEEKDAYS[it.value - 1] }
    is HabitSchedule.PerWeek -> stringResource(R.string.habit_times_week, schedule.times)
}

private fun monthTitle(month: YearMonth): String = "${MONTHS[month.monthValue - 1]} ${month.year}"

internal val WEEKDAYS = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
private val MONTHS = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
private val QUICK_EMOJI = listOf("✅", "🏃", "📚", "💧", "🧘")
private const val MUTED_ALPHA = 0.5f

const val TAG_HABITS = "habits"
const val TAG_HABITS_EMPTY = "habits_empty"
const val TAG_HABITS_ADD_FIRST = "habits_add_first"
const val TAG_HABITS_LIST = "habits_list"
const val TAG_HABITS_SUMMARY = "habits_summary"
const val TAG_HABITS_REST = "habits_rest"
const val TAG_HABIT_ROW = "habit_row"
const val TAG_HABIT_STREAK = "habit_streak"
const val TAG_HABIT_CHECK = "habit_check"
const val TAG_HABIT_CARD = "habit_card"
const val TAG_HABIT_CALENDAR = "habit_calendar"
const val TAG_HABIT_DAY = "habit_day"
const val TAG_HABIT_FORM = "habit_form"
const val TAG_HABIT_NAME = "habit_name"
const val TAG_HABIT_SAVE = "habit_save"
const val TAG_HABIT_EMOJI = "habit_emoji"
const val TAG_HABIT_KIND = "habit_kind"
const val TAG_HABIT_DAY_CHIP = "habit_day_chip"
const val TAG_HABIT_PER_WEEK = "habit_per_week"
const val TAG_HABIT_DELETE_WINDOW = "habit_delete_window"
