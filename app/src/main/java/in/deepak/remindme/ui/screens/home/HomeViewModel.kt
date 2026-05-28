package `in`.deepak.remindme.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.deepak.remindme.data.preferences.GreetingStyle
import `in`.deepak.remindme.data.preferences.UserPreferences
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.repository.ReminderRepository
import `in`.deepak.remindme.scheduler.AlarmScheduler
import `in`.deepak.remindme.scheduler.nextFireTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * State holder for the redesigned Home screen.
 *
 * Maps the raw reminder list into a presentation-ready shape:
 *   - greeting + name from [UserPreferences]
 *   - the single "next reminder" surfaced as a banner
 *   - reminders split into Today vs. Upcoming buckets
 *
 * The split is computed once per emission rather than per recomposition because
 * it touches every reminder's [nextFireTimeMillis] and we don't want to do that
 * during layout passes.
 */
class HomeViewModel(
    private val repository: ReminderRepository,
    private val scheduler: AlarmScheduler,
    private val userPreferences: UserPreferences,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    // Tick every 30s so the "in X min" label and the Today/Upcoming split
    // refresh as the clock moves — otherwise the banner is frozen until a
    // reminder changes. WhileSubscribed in stateIn pauses this when the
    // screen isn't visible, so it doesn't waste battery.
    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(TICK_INTERVAL_MS)
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeAll(),
        ticker,
    ) { reminders, _ -> buildState(reminders) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeUiState.Loading,
        )

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(id, enabled)
            val updated = repository.get(id) ?: return@launch
            scheduler.schedule(updated)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            scheduler.cancel(id)
            repository.delete(id)
        }
    }

    // --- internals --------------------------------------------------------

    private fun buildState(reminders: List<Reminder>): HomeUiState {
        val now = clock.instant()
        val zone = ZoneId.systemDefault()
        val endOfToday = endOfTodayInstant(now, zone)

        // Pair each enabled reminder with its next fire instant. Skip reminders
        // that have no future fire (e.g., a OneTime in the past) — we don't
        // hide them, we just put them in Upcoming with a null fire time so
        // they still surface for the user to act on.
        val withNext = reminders.map { it to nextFireTimeMillis(now, it, zone) }

        val (today, upcoming) = withNext.partition { (_, fireAt) ->
            fireAt != null && fireAt <= endOfToday.toEpochMilli()
        }

        val sortedToday = today
            .sortedBy { it.second ?: Long.MAX_VALUE }
            .map { it.first }
        val sortedUpcoming = upcoming
            .sortedBy { it.second ?: Long.MAX_VALUE }
            .map { it.first }

        val nextPair = withNext
            .filter { it.second != null }
            .minByOrNull { it.second!! }

        // Profile prefs are read here (not cached in a val) so editing them in
        // Settings → Profile reflects on the next tick without recreating the VM.
        val greetingWord = when (userPreferences.greetingStyle) {
            GreetingStyle.TimeBased -> greetingFor(LocalTime.now(clock))
            GreetingStyle.AlwaysHello -> "Hello"
        }

        return HomeUiState.Loaded(
            greeting = greetingWord,
            userName = userPreferences.userName,
            showGreeting = userPreferences.showGreeting,
            totalToday = sortedToday.size,
            nextReminder = nextPair?.let { (r, fireAt) ->
                NextReminderInfo(
                    reminder = r,
                    fireAtEpochMillis = fireAt!!,
                    relativeLabel = relativeLabel(now, Instant.ofEpochMilli(fireAt)),
                )
            },
            today = sortedToday,
            upcoming = sortedUpcoming,
        )
    }

    private fun endOfTodayInstant(now: Instant, zone: ZoneId): Instant =
        LocalDate.now(clock).plusDays(1).atStartOfDay(zone).toInstant()

    private fun greetingFor(time: LocalTime): String = when (time.hour) {
        in 5..11  -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else      -> "Good night"
    }

    private companion object {
        // 30s keeps the worst-case staleness on the "in X min" label to half a
        // minute without burning the cache window on excessive recompositions.
        const val TICK_INTERVAL_MS = 30_000L
    }

    private fun relativeLabel(now: Instant, target: Instant): String {
        val minutes = Duration.between(now, target).toMinutes()
        return when {
            minutes <= 0L     -> "now"
            minutes < 60L     -> "in $minutes min"
            minutes < 24 * 60 -> {
                val hours = minutes / 60
                val rem   = minutes % 60
                if (rem == 0L) "in ${hours} hr" else "in ${hours} hr ${rem} min"
            }
            else              -> {
                val days = minutes / (24 * 60)
                if (days == 1L) "tomorrow" else "in $days days"
            }
        }
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Loaded(
        val greeting: String,
        val userName: String,
        val showGreeting: Boolean,
        val totalToday: Int,
        val nextReminder: NextReminderInfo?,
        val today: List<Reminder>,
        val upcoming: List<Reminder>,
    ) : HomeUiState
}

data class NextReminderInfo(
    val reminder: Reminder,
    val fireAtEpochMillis: Long,
    /** Pre-formatted "in 23 min" / "tomorrow" string ready for the banner. */
    val relativeLabel: String,
)
