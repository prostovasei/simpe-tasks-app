package com.prostovasei.simpletasks.ui.feature.health

import com.prostovasei.simpletasks.domain.model.health.HealthBackupInfo
import com.prostovasei.simpletasks.domain.model.health.HealthSettings
import com.prostovasei.simpletasks.domain.model.health.Weight
import com.prostovasei.simpletasks.domain.model.health.WeightEntry
import com.prostovasei.simpletasks.domain.model.health.WeightPeriod
import java.time.LocalDate

/** Окно ввода веса (#257): барабаны «кг» и «десятые», дата, комментарий. [editingDate] — запись, которую правят (null — новая). */
data class WeightForm(
    val whole: Int,
    val tenth: Int,
    val date: LocalDate,
    val comment: String = "",
    val editingDate: LocalDate? = null,
) {
    val tenths: Int get() = Weight.clamp(whole * 10 + tenth)

    fun withTenths(value: Int): WeightForm = Weight.clamp(value).let { copy(whole = it / 10, tenth = it % 10) }

    companion object {
        fun of(tenths: Int, date: LocalDate, comment: String = "", editingDate: LocalDate? = null) =
            WeightForm(0, 0, date, comment, editingDate).withTenths(tenths)
    }
}

/** Окно цели и роста (#257): оба значения необязательны. */
data class GoalForm(val goalTenths: Int?, val heightCm: Int?)

/** Файл копии выбран и проверен — ждём подтверждения замены. */
data class RestoreConfirm(val json: String, val info: HealthBackupInfo)

data class HealthState(
    val today: LocalDate,
    /** null — ещё читаются из базы. */
    val entries: List<WeightEntry>? = null,
    val settings: HealthSettings = HealthSettings(),
    val period: WeightPeriod = WeightPeriod.MONTH,
    /** Точка графика с подсказкой. */
    val tip: LocalDate? = null,
    val form: WeightForm? = null,
    val goalForm: GoalForm? = null,
    val restore: RestoreConfirm? = null,
    val lastBackupAt: Long? = null,
)

sealed interface HealthIntent {
    data class SelectPeriod(val period: WeightPeriod) : HealthIntent

    /** Нажатие на точку графика: первое — подсказка, повторное на той же — правка записи. */
    data class TapPoint(val date: LocalDate) : HealthIntent
    data object ClearTip : HealthIntent
    data object Add : HealthIntent
    data class OpenEntry(val date: LocalDate) : HealthIntent
    data class EditForm(val edit: WeightForm.() -> WeightForm) : HealthIntent
    data object CloseForm : HealthIntent
    data object SaveForm : HealthIntent
    data object DeleteForm : HealthIntent
    data class UndoDelete(val entry: WeightEntry) : HealthIntent
    data object OpenGoal : HealthIntent
    data class EditGoal(val edit: GoalForm.() -> GoalForm) : HealthIntent
    data object CloseGoal : HealthIntent
    data object SaveGoal : HealthIntent
    data class SetReminder(val on: Boolean) : HealthIntent
    data class SetReminderMinute(val minute: Int) : HealthIntent
    data class ExportBackup(val uri: String) : HealthIntent
    data class PickBackup(val uri: String) : HealthIntent
    data object ConfirmRestore : HealthIntent
    data object CloseRestore : HealthIntent
}

sealed interface HealthEffect {
    /** Запись удалена — на экране «Отменить». */
    data class EntryDeleted(val entry: WeightEntry) : HealthEffect
    data object BackupSaved : HealthEffect
    data object BackupFailed : HealthEffect
    data object BackupInvalid : HealthEffect
    data object BackupRestored : HealthEffect
}
