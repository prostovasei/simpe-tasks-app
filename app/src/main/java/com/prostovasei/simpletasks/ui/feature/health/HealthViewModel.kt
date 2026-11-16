package com.prostovasei.simpletasks.ui.feature.health

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.di.IoDispatcher
import com.prostovasei.simpletasks.core.files.DocumentOpener
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.health.Weight
import com.prostovasei.simpletasks.domain.model.health.WeightEntry
import com.prostovasei.simpletasks.domain.repository.HealthRepository
import com.prostovasei.simpletasks.domain.repository.HealthSettingsStore
import com.prostovasei.simpletasks.notifications.WeighInReminder
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import java.time.LocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Раздел «Здоровье» (#257, решения PM 16.11.2026). Живёт вместе с окном приложения, как «Финансы»: общая нижняя панель рисуется каркасом,
 * а окно ввода и выбранный период сохраняются при переходе между модулями.
 */
@HiltViewModel
class HealthViewModel @Inject constructor(
    private val health: HealthRepository,
    private val store: HealthSettingsStore,
    private val reminder: WeighInReminder,
    private val time: TimeProvider,
    private val documents: DocumentOpener,
    @IoDispatcher private val io: CoroutineDispatcher,
) : MviViewModel<HealthState, HealthIntent, HealthEffect>(
    HealthState(today = time.today(), settings = store.settings.value, lastBackupAt = store.lastBackupAt),
) {
    init {
        viewModelScope.launch { health.observeEntries().collect { entries -> setState { copy(entries = entries) } } }
        viewModelScope.launch { store.settings.collect { settings -> setState { copy(settings = settings) } } }
    }

    override fun onIntent(intent: HealthIntent) {
        when (intent) {
            is HealthIntent.SelectPeriod -> setState { copy(period = intent.period, tip = null) }
            is HealthIntent.TapPoint -> if (currentState.tip == intent.date) openEntry(intent.date) else setState { copy(tip = intent.date) }
            HealthIntent.ClearTip -> setState { copy(tip = null) }
            HealthIntent.Add -> add()
            is HealthIntent.OpenEntry -> openEntry(intent.date)
            is HealthIntent.EditForm -> setState { copy(form = form?.let(intent.edit)) }
            HealthIntent.CloseForm -> setState { copy(form = null) }
            HealthIntent.SaveForm -> saveForm()
            HealthIntent.DeleteForm -> deleteForm()
            is HealthIntent.UndoDelete -> viewModelScope.launch { health.save(intent.entry) }
            HealthIntent.OpenGoal -> setState { copy(goalForm = GoalForm(settings.goalTenths, settings.heightCm)) }
            is HealthIntent.EditGoal -> setState { copy(goalForm = goalForm?.let(intent.edit)) }
            HealthIntent.CloseGoal -> setState { copy(goalForm = null) }
            HealthIntent.SaveGoal -> saveGoal()
            is HealthIntent.SetReminder -> updateSettings { copy(reminderOn = intent.on) }
            is HealthIntent.SetReminderMinute -> updateSettings { copy(reminderMinute = intent.minute) }
            is HealthIntent.ExportBackup -> exportBackup(intent.uri)
            is HealthIntent.PickBackup -> pickBackup(intent.uri)
            HealthIntent.ConfirmRestore -> restoreBackup()
            HealthIntent.CloseRestore -> setState { copy(restore = null) }
        }
    }

    /** Новое взвешивание: заранее стоит последний вес (первый раз — 70,0), дата — сегодня. */
    private fun add() {
        val today = time.today()
        val last = currentState.entries?.lastOrNull()?.tenths ?: Weight.DEFAULT_TENTHS
        setState { copy(form = WeightForm.of(last, today), today = today, tip = null) }
    }

    private fun openEntry(date: LocalDate) {
        val entry = currentState.entries?.firstOrNull { it.date == date } ?: return
        setState { copy(form = WeightForm.of(entry.tenths, entry.date, entry.comment.orEmpty(), editingDate = entry.date), today = time.today(), tip = null) }
    }

    private fun saveForm() {
        val form = currentState.form ?: return
        // Будущие даты запрещены: вес ещё не измерен.
        if (form.date.isAfter(time.today())) return
        setState { copy(form = null, tip = null) }
        viewModelScope.launch { health.save(WeightEntry(form.date, form.tenths, form.comment), replacing = form.editingDate) }
    }

    private fun deleteForm() {
        val editing = currentState.form?.editingDate ?: return
        val entry = currentState.entries?.firstOrNull { it.date == editing }
        setState { copy(form = null, tip = null) }
        viewModelScope.launch {
            health.delete(editing)
            entry?.let { sendEffect(HealthEffect.EntryDeleted(it)) }
        }
    }

    private fun saveGoal() {
        val form = currentState.goalForm ?: return
        setState { copy(goalForm = null) }
        updateSettings {
            copy(goalTenths = form.goalTenths?.let(Weight::clamp), heightCm = form.heightCm?.coerceIn(Weight.MIN_HEIGHT, Weight.MAX_HEIGHT))
        }
    }

    private fun updateSettings(transform: com.prostovasei.simpletasks.domain.model.health.HealthSettings.() -> com.prostovasei.simpletasks.domain.model.health.HealthSettings) {
        store.update { it.transform() }
        reminder.schedule()
    }

    private fun exportBackup(uri: String) {
        viewModelScope.launch {
            val saved = runCatching {
                val json = health.exportBackup(store.settings.value)
                withContext(io) { documents.openOutput(uri).use { it.write(json.toByteArray()) } }
            }.isSuccess
            if (saved) {
                val now = time.now().toEpochMilli()
                store.lastBackupAt = now
                setState { copy(lastBackupAt = now) }
            }
            sendEffect(if (saved) HealthEffect.BackupSaved else HealthEffect.BackupFailed)
        }
    }

    /** Файл выбран: читаем и проверяем; годный — окно подтверждения, негодный — подсказка, данные не трогаем. */
    private fun pickBackup(uri: String) {
        viewModelScope.launch {
            val preview = runCatching {
                val json = withContext(io) { documents.openInput(uri).use { it.readBytes().decodeToString() } }
                RestoreConfirm(json, health.inspectBackup(json))
            }.getOrNull()
            if (preview == null) sendEffect(HealthEffect.BackupInvalid) else setState { copy(restore = preview) }
        }
    }

    private fun restoreBackup() {
        val confirm = currentState.restore ?: return
        setState { copy(restore = null, form = null, goalForm = null, tip = null) }
        viewModelScope.launch {
            val restored = runCatching { health.restoreBackup(confirm.json) }
            restored.getOrNull()?.let { settings ->
                store.update { settings }
                reminder.schedule()
            }
            sendEffect(if (restored.isSuccess) HealthEffect.BackupRestored else HealthEffect.BackupInvalid)
        }
    }
}
