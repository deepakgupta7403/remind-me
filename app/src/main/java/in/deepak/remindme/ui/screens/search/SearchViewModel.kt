package `in`.deepak.remindme.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.deepak.remindme.data.preferences.SearchPreferences
import `in`.deepak.remindme.data.templates.ReminderTemplate
import `in`.deepak.remindme.data.templates.TemplateCatalog
import `in`.deepak.remindme.domain.model.Category
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.repository.ReminderRepository
import `in`.deepak.remindme.scheduler.AlarmScheduler
import `in`.deepak.remindme.scheduler.nextFireTimeMillis
import `in`.deepak.remindme.ui.state.ReminderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Search screen state.
 *
 * Holds the query + the four active filters, derives the result list by
 * intersecting them against the reminder repository (a hot Flow). Recent
 * searches live in [SearchPreferences] so they survive process death and
 * are shared with no other screen (yet) — when Settings exposes a "Clear
 * recent searches" toggle, it'll write to the same prefs.
 *
 * "Mode" is computed: blank query + all filters at their defaults → Empty
 * (recent + suggestions); anything narrowing → Results.
 */
class SearchViewModel(
    private val repository: ReminderRepository,
    private val scheduler: AlarmScheduler,
    private val searchPreferences: SearchPreferences,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val filters = MutableStateFlow(SearchFilters())
    private val recent = MutableStateFlow(searchPreferences.recent())

    val uiState: StateFlow<SearchUiState> = combine(
        repository.observeAll(),
        filters,
        recent,
    ) { reminders, f, r -> build(reminders, f, r) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchUiState.empty(searchPreferences.recent()),
        )

    fun setQuery(value: String) {
        filters.update { it.copy(query = value) }
    }

    fun setType(type: ReminderType?) {
        filters.update { it.copy(type = type) }
    }

    fun setCategory(category: Category?) {
        filters.update { it.copy(category = category) }
    }

    fun setStatus(status: StatusFilter) {
        filters.update { it.copy(status = status) }
    }

    fun setTime(window: TimeFilter) {
        filters.update { it.copy(time = window) }
    }

    fun clearAllFilters() {
        filters.update { SearchFilters(query = it.query) }
    }

    /** Tap a recent search → re-runs that query. */
    fun useRecent(query: String) {
        filters.update { it.copy(query = query) }
    }

    /** Persist whatever the user just typed; called when results are first shown. */
    fun commitQueryToHistory() {
        val q = filters.value.query
        if (q.length >= 2) {
            searchPreferences.record(q)
            recent.value = searchPreferences.recent()
        }
    }

    fun clearRecent() {
        searchPreferences.clear()
        recent.value = emptyList()
    }

    /** Tap a suggested template card → instantiate it as a real reminder. */
    fun applyTemplate(template: ReminderTemplate, onApplied: (String) -> Unit) {
        viewModelScope.launch {
            val saved = repository.upsert(template.build())
            scheduler.schedule(saved)
            onApplied(template.title)
        }
    }

    /** Mirror of HomeViewModel.setEnabled — keeps the result-row Switch in sync. */
    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(id, enabled)
            val updated = repository.get(id) ?: return@launch
            scheduler.schedule(updated)
        }
    }

    // --- internals --------------------------------------------------------

    private fun build(
        reminders: List<Reminder>,
        f: SearchFilters,
        recent: List<String>,
    ): SearchUiState {
        val now = clock.instant()
        val results = reminders
            .asSequence()
            .filter { f.type == null || it.matchesType(f.type) }
            .filter { f.category == null || it.category == f.category }
            .filter { matchesStatus(it, f.status) }
            .filter { matchesTime(it, f.time, now) }
            .filter { f.query.isBlank() || it.title.contains(f.query.trim(), ignoreCase = true) }
            .toList()

        val templateMatches = if (f.query.isBlank()) emptyList() else
            TemplateCatalog.all.filter { it.title.contains(f.query.trim(), ignoreCase = true) }

        return SearchUiState(
            filters = f,
            recent = recent,
            results = results,
            templateMatches = templateMatches,
        )
    }

    private fun matchesStatus(r: Reminder, status: StatusFilter): Boolean = when (status) {
        StatusFilter.All    -> true
        StatusFilter.Active -> r.enabled
        StatusFilter.Paused -> !r.enabled
    }

    private fun matchesTime(r: Reminder, window: TimeFilter, now: Instant): Boolean {
        if (window == TimeFilter.AllTime) return true
        val fireAt = nextFireTimeMillis(now, r) ?: return false
        val delta = Duration.between(now, Instant.ofEpochMilli(fireAt))
        return when (window) {
            TimeFilter.AllTime    -> true
            TimeFilter.ActiveNow  -> delta.toMinutes() in 0..60
            TimeFilter.ThisWeek   -> delta.toDays() in 0..6
        }
    }
}

// --- State + filter types -------------------------------------------------

data class SearchUiState(
    val filters: SearchFilters,
    val recent: List<String>,
    val results: List<Reminder>,
    val templateMatches: List<ReminderTemplate>,
) {
    /** Empty-state shows recent/suggested; once anything narrows, switch to Results. */
    val mode: SearchMode get() =
        if (filters == SearchFilters()) SearchMode.Empty else SearchMode.Results

    companion object {
        fun empty(recent: List<String>) = SearchUiState(
            filters = SearchFilters(),
            recent = recent,
            results = emptyList(),
            templateMatches = emptyList(),
        )
    }
}

enum class SearchMode { Empty, Results }

data class SearchFilters(
    val query: String = "",
    val type: ReminderType? = null,
    val category: Category? = null,
    val status: StatusFilter = StatusFilter.All,
    val time: TimeFilter = TimeFilter.AllTime,
)

enum class StatusFilter { All, Active, Paused }
enum class TimeFilter { AllTime, ActiveNow, ThisWeek }

private fun Reminder.matchesType(t: ReminderType): Boolean = when (t) {
    ReminderType.INTERVAL -> this is Reminder.Interval
    ReminderType.ONE_TIME -> this is Reminder.OneTime
    ReminderType.DAILY    -> this is Reminder.Daily
    ReminderType.WEEKLY   -> this is Reminder.Weekly
}
