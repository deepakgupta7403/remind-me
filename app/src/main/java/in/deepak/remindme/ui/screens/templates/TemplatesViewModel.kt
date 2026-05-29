package `in`.deepak.remindme.ui.screens.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.deepak.remindme.data.repository.TemplateRepository
import `in`.deepak.remindme.data.templates.ReminderTemplate
import `in`.deepak.remindme.data.templates.TemplateTag
import `in`.deepak.remindme.domain.repository.ReminderRepository
import `in`.deepak.remindme.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Templates screen state.
 *
 * Templates are read from [TemplateRepository] (Room-backed); the catalog is
 * seeded into the DB once at app start. Filtering by query and tag happens
 * against the observed list in memory.
 *
 * "Apply template" upserts a reminder and arms its alarm — the user lands back
 * on Home and sees the new card, no editor step required. They can still tweak
 * from Home's tap-to-edit if they want.
 */
class TemplatesViewModel(
    private val repository: ReminderRepository,
    private val scheduler: AlarmScheduler,
    private val templateRepository: TemplateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        TemplatesUiState(
            query = "",
            selectedTag = null,
            allTemplates = emptyList(),
        )
    )
    val state: StateFlow<TemplatesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            templateRepository.observeAll().collect { templates ->
                _state.update { it.copy(allTemplates = templates) }
            }
        }
    }

    fun setQuery(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun selectTag(tag: TemplateTag?) {
        _state.update { it.copy(selectedTag = tag) }
    }

    fun apply(template: ReminderTemplate, onApplied: (String) -> Unit) {
        viewModelScope.launch {
            val saved = repository.upsert(template.build())
            scheduler.schedule(saved)
            onApplied(template.title)
        }
    }
}

data class TemplatesUiState(
    val query: String,
    val selectedTag: TemplateTag?,
    val allTemplates: List<ReminderTemplate>,
) {
    /** Cheap enough to compute on demand. */
    val filtered: List<ReminderTemplate> get() = allTemplates.filter { t ->
        (selectedTag == null || selectedTag in t.tags) &&
            (query.isBlank() || t.title.contains(query.trim(), ignoreCase = true))
    }
}
