package com.prostovasei.simpletasks.ui.feature.habits

import com.prostovasei.simpletasks.domain.model.habits.Habit
import com.prostovasei.simpletasks.domain.model.habits.HabitBackupInfo
import com.prostovasei.simpletasks.domain.model.habits.HabitSchedule
import com.prostovasei.simpletasks.domain.model.habits.HabitSettings
import java.time.LocalDate
import java.time.YearMonth

/** Вид расписания в окне привычки (#340). */
enum class ScheduleKind { DAILY, WEEKDAYS, PER_WEEK }

/** Окно создания и правки привычки. [id] = null — новая. [mask] и [perWeek] хранятся, даже когда выбран другой вид. */
data class HabitForm(
    val id: String? = null,
    val name: String = "",
    val emoji: String? = null,
    val kind: ScheduleKind = ScheduleKind.DAILY,
    val mask: Int = HabitSchedule.ALL_DAYS,
    val perWeek: Int = DEFAULT_PER_WEEK,
) {
    val schedule: HabitSchedule
        get() = when (kind) {
            ScheduleKind.DAILY -> HabitSchedule.Daily
            ScheduleKind.WEEKDAYS -> HabitSchedule.Weekdays(mask)
            ScheduleKind.PER_WEEK -> HabitSchedule.PerWeek(perWeek)
        }

    val canSave: Boolean get() = name.isNotBlank() && (kind != ScheduleKind.WEEKDAYS || mask != 0)

    companion object {
        const val DEFAULT_PER_WEEK = 3

        fun of(habit: Habit) = when (val s = habit.schedule) {
            HabitSchedule.Daily -> HabitForm(habit.id, habit.name, habit.emoji)
            is HabitSchedule.Weekdays -> HabitForm(habit.id, habit.name, habit.emoji, ScheduleKind.WEEKDAYS, mask = s.mask)
            is HabitSchedule.PerWeek -> HabitForm(habit.id, habit.name, habit.emoji, ScheduleKind.PER_WEEK, perWeek = s.times)
        }
    }
}

/** Файл копии выбран и проверен — ждём подтверждения замены. */
data class HabitRestore(val json: String, val info: HabitBackupInfo)

data class HabitsState(
    val today: LocalDate,
    /** null — ещё читаются из базы. */
    val habits: List<Habit>? = null,
    val settings: HabitSettings = HabitSettings(),
    val form: HabitForm? = null,
    /** Открытая карточка привычки и месяц её календаря. */
    val openId: String? = null,
    val month: YearMonth = YearMonth.from(today),
    val confirmDelete: Boolean = false,
    val restore: HabitRestore? = null,
    val lastBackupAt: Long? = null,
) {
    val active: List<Habit> get() = habits.orEmpty().filter { !it.archived }
    val archived: List<Habit> get() = habits.orEmpty().filter { it.archived }
    val open: Habit? get() = openId?.let { id -> habits?.firstOrNull { it.id == id } }
}

sealed interface HabitsIntent {
    data object Add : HabitsIntent
    data class Edit(val id: String) : HabitsIntent
    data class EditForm(val edit: HabitForm.() -> HabitForm) : HabitsIntent
    data object CloseForm : HabitsIntent
    data object SaveForm : HabitsIntent
    data class Toggle(val id: String, val date: LocalDate) : HabitsIntent
    data class Open(val id: String) : HabitsIntent
    data object CloseCard : HabitsIntent
    data class ShiftMonth(val delta: Int) : HabitsIntent
    data class Move(val id: String, val up: Boolean) : HabitsIntent
    data class Archive(val id: String, val archived: Boolean) : HabitsIntent
    data object AskDelete : HabitsIntent
    data object CancelDelete : HabitsIntent
    data object ConfirmDelete : HabitsIntent
    data class SetReminder(val on: Boolean) : HabitsIntent
    data class SetReminderMinute(val minute: Int) : HabitsIntent
    data class ExportBackup(val uri: String) : HabitsIntent
    data class PickBackup(val uri: String) : HabitsIntent
    data object ConfirmRestore : HabitsIntent
    data object CloseRestore : HabitsIntent
}

sealed interface HabitsEffect {
    data object BackupSaved : HabitsEffect
    data object BackupFailed : HabitsEffect
    data object BackupInvalid : HabitsEffect
    data object BackupRestored : HabitsEffect
}
