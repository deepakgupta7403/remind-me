package `in`.deepak.remindme.ui.screens.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.repository.ReminderRepository
import `in`.deepak.remindme.scheduler.AlarmScheduler
import `in`.deepak.remindme.ui.state.ReminderForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State holder for the create/edit screen.
 *
 * The screen renders [form] and calls [update] for every field change. On
 * Save we convert the form to a domain [Reminder], persist, and ask the
 * scheduler to (re-)arm. Scheduling on save means a freshly-created reminder
 * starts firing immediately.
 */
class ReminderEditViewModel(
    private val repository: ReminderRepository,
    private val scheduler: AlarmScheduler,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val reminderId: Long =
        savedStateHandle[KEY_REMINDER_ID] ?: Reminder.UNSAVED_ID

    private val _form = MutableStateFlow(ReminderForm())
    val form: StateFlow<ReminderForm> = _form.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    init {
        if (reminderId != Reminder.UNSAVED_ID) {
            viewModelScope.launch {
                repository.get(reminderId)?.let { existing ->
                    _form.value = ReminderForm.fromReminder(existing)
                }
            }
        }
    }

    fun update(transform: (ReminderForm) -> ReminderForm) {
        _form.value = transform(_form.value)
    }

    fun save() {
        val reminder = _form.value.toReminderOrNull()
        if (reminder == null) {
            _saveState.value = SaveState.Invalid
            return
        }
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            val persisted = repository.upsert(reminder)
            scheduler.schedule(persisted)
            _saveState.value = SaveState.Saved(persisted.id)
        }
    }

    companion object {
        const val KEY_REMINDER_ID = "reminderId"
    }
}

sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data class Saved(val id: Long) : SaveState
    data object Invalid : SaveState
}
