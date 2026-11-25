package com.prostovasei.simpletasks.ui.feature.habits

import androidx.lifecycle.viewModelScope
import com.prostovasei.simpletasks.core.di.IoDispatcher
import com.prostovasei.simpletasks.core.files.DocumentOpener
import com.prostovasei.simpletasks.core.time.TimeProvider
import com.prostovasei.simpletasks.domain.model.habits.HabitSettings
import com.prostovasei.simpletasks.domain.repository.HabitRepository
import com.prostovasei.simpletasks.domain.repository.HabitSettingsStore
import com.prostovasei.simpletasks.notifications.HabitReminder
import com.prostovasei.simpletasks.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import javax.inject.Inject

/**
 * Раздел «Привычки» (#340). Живёт вместе с окном приложения, как «Здоровье»: отметки из хаба и из раздела идут через одну модель,
 * а открытая карточка и окно правки сохраняются при переходе между разделами.
 */
@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habits: HabitRepository,
    private val store: HabitSettingsStore,
    private val reminder: HabitReminder,
    private val time: TimeProvider,
    private val documents: DocumentOpener,
    @IoDispatcher private val io: CoroutineDispatcher,
) : MviViewModel<HabitsState, HabitsIntent, HabitsEffect>(
    HabitsState(today = time.today(), settings = store.settings.value, lastBackupAt = store.lastBackupAt),
) {
    init {
        viewModelScope.launch { habits.observeHabits().collect { list -> setState { copy(habits = list, today = time.today()) } } }
        viewModelScope.launch { store.settings.collect { settings -> setState { copy(settings = settings) } } }
    }

    override fun onIntent(intent: HabitsIntent) {
        when (intent) {
            HabitsIntent.Add -> setState { copy(form = HabitForm(), today = time.today()) }
            is HabitsIntent.Edit -> currentState.habits?.firstOrNull { it.id == intent.id }?.let { setState { copy(form = HabitForm.of(it)) } }
            is HabitsIntent.EditForm -> setState { copy(form = form?.let(intent.edit)) }
            HabitsIntent.CloseForm -> setState { copy(form = null) }
            HabitsIntent.SaveForm -> saveForm()
            is HabitsIntent.Toggle -> toggle(intent)
            is HabitsIntent.Open -> setState { copy(openId = intent.id, month = YearMonth.from(time.today()), today = time.today(), confirmDelete = false) }
            HabitsIntent.CloseCard -> setState { copy(openId = null, confirmDelete = false) }
            is HabitsIntent.ShiftMonth -> setState { copy(month = month.plusMonths(intent.delta.toLong())) }
            is HabitsIntent.Move -> viewModelScope.launch { habits.move(intent.id, intent.up) }
            is HabitsIntent.Archive -> {
                setState { copy(openId = null) }
                viewModelScope.launch { habits.setArchived(intent.id, intent.archived); reminder.schedule() }
            }
            HabitsIntent.AskDelete -> setState { copy(confirmDelete = true) }
            HabitsIntent.CancelDelete -> setState { copy(confirmDelete = false) }
            HabitsIntent.ConfirmDelete -> {
                val id = currentState.openId ?: return
                setState { copy(openId = null, confirmDelete = false) }
                viewModelScope.launch { habits.delete(id); reminder.schedule() }
            }
            is HabitsIntent.SetReminder -> updateSettings { copy(reminderOn = intent.on) }
            is HabitsIntent.SetReminderMinute -> updateSettings { copy(reminderMinute = intent.minute) }
            is HabitsIntent.ExportBackup -> exportBackup(intent.uri)
            is HabitsIntent.PickBackup -> pickBackup(intent.uri)
            HabitsIntent.ConfirmRestore -> restoreBackup()
            HabitsIntent.CloseRestore -> setState { copy(restore = null) }
        }
    }

    private var saving = false

    private fun saveForm() {
        val form = currentState.form ?: return
        if (saving || !form.canSave) return
        saving = true
        viewModelScope.launch {
            try {
                habits.save(form.id, form.name, form.emoji, form.schedule)
                setState { copy(form = null) }
                reminder.schedule()
            } finally {
                saving = false
            }
        }
    }

    /** Будущие дни не отмечаются: привычка ещё не сделана. */
    private fun toggle(intent: HabitsIntent.Toggle) {
        if (intent.date.isAfter(time.today())) return
        viewModelScope.launch { habits.toggle(intent.id, intent.date) }
    }

    private fun updateSettings(transform: HabitSettings.() -> HabitSettings) {
        store.update { it.transform() }
        reminder.schedule()
    }

    private fun exportBackup(uri: String) {
        viewModelScope.launch {
            val saved = runCatching {
                val json = habits.exportBackup(store.settings.value)
                withContext(io) { documents.openOutput(uri).use { it.write(json.toByteArray()) } }
            }.isSuccess
            if (saved) {
                val now = time.now().toEpochMilli()
                store.lastBackupAt = now
                setState { copy(lastBackupAt = now) }
            }
            sendEffect(if (saved) HabitsEffect.BackupSaved else HabitsEffect.BackupFailed)
        }
    }

    private fun pickBackup(uri: String) {
        viewModelScope.launch {
            val preview = runCatching {
                val json = withContext(io) { documents.openInput(uri).use { it.readBytes().decodeToString() } }
                HabitRestore(json, habits.inspectBackup(json))
            }.getOrNull()
            if (preview == null) sendEffect(HabitsEffect.BackupInvalid) else setState { copy(restore = preview) }
        }
    }

    private fun restoreBackup() {
        val confirm = currentState.restore ?: return
        setState { copy(restore = null, form = null, openId = null) }
        viewModelScope.launch {
            val restored = runCatching { habits.restoreBackup(confirm.json) }
            restored.getOrNull()?.let { settings ->
                store.update { settings }
                reminder.schedule()
            }
            sendEffect(if (restored.isSuccess) HabitsEffect.BackupRestored else HabitsEffect.BackupInvalid)
        }
    }
}
