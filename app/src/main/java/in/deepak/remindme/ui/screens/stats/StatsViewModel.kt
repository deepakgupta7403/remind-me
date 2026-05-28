package `in`.deepak.remindme.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.deepak.remindme.data.preferences.DayStat
import `in`.deepak.remindme.data.preferences.ReminderActivityLog
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

/**
 * Computes the Stats screen from the reminder list + [ReminderActivityLog].
 *
 * Everything here is derived from real recorded activity ("fired"/"done"); there
 * is no fabricated data. Until at least one reminder has fired the screen shows
 * [StatsUiState.Empty].
 *
 *   streak       = consecutive days (ending today, or yesterday if today is
 *                  still blank) on which ≥1 reminder was marked done.
 *   completion % = done / fired over the last 7 days.
 *   heatmap      = done-count intensity per day for the last 12 weeks.
 *   top          = reminders ranked by completion rate (done / fired).
 */
class StatsViewModel(
    repository: ReminderRepository,
    activityLog: ReminderActivityLog,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        repository.observeAll(),
        activityLog.observe(),
    ) { reminders, days -> buildState(reminders, days) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = StatsUiState.Loading,
        )

    private fun buildState(reminders: List<Reminder>, days: List<DayStat>): StatsUiState {
        val today = LocalDate.now(clock)
        val byDay = days.associateBy { it.epochDay }

        if (days.none { it.totalFired > 0 }) return StatsUiState.Empty

        return StatsUiState.Loaded(
            streakDays = computeStreak(byDay, today),
            completionPct = weeklyCompletion(days, today),
            weeks = buildHeatmap(byDay, today),
            topReminders = topReminders(reminders, days),
        )
    }

    /** Walk backwards from today (with a one-day grace) counting days with ≥1 done. */
    private fun computeStreak(byDay: Map<Long, DayStat>, today: LocalDate): Int {
        val doneToday = (byDay[today.toEpochDay()]?.totalDone ?: 0) > 0
        var cursor = if (doneToday) today else today.minusDays(1)
        var streak = 0
        while ((byDay[cursor.toEpochDay()]?.totalDone ?: 0) > 0) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** done / fired over today + the previous 6 days; null when nothing fired. */
    private fun weeklyCompletion(days: List<DayStat>, today: LocalDate): Int? {
        val from = today.minusDays(6).toEpochDay()
        val window = days.filter { it.epochDay in from..today.toEpochDay() }
        val fired = window.sumOf { it.totalFired }
        if (fired == 0) return null
        val done = window.sumOf { it.totalDone }
        return (done * 100 / fired).coerceIn(0, 100)
    }

    /**
     * 12 columns (weeks) × 7 rows (Mon→Sun) of intensity levels.
     * Level 0..4 maps done-count buckets; -1 marks future days in the current
     * (partial) week so they render blank.
     */
    private fun buildHeatmap(byDay: Map<Long, DayStat>, today: LocalDate): List<List<Int>> {
        val mondayThisWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val gridStart = mondayThisWeek.minusWeeks((WEEKS - 1).toLong())
        return (0 until WEEKS).map { col ->
            (0 until 7).map { row ->
                val date = gridStart.plusDays((col * 7 + row).toLong())
                when {
                    date.isAfter(today) -> -1
                    else -> bucket(byDay[date.toEpochDay()]?.totalDone ?: 0)
                }
            }
        }
    }

    private fun bucket(count: Int): Int = when {
        count <= 0 -> 0
        count >= 4 -> 4
        else -> count
    }

    private fun topReminders(reminders: List<Reminder>, days: List<DayStat>): List<TopReminderStat> {
        val firedById = mutableMapOf<Long, Int>()
        val doneById = mutableMapOf<Long, Int>()
        days.forEach { day ->
            day.firedByReminder.forEach { (id, n) -> firedById.merge(id, n, Int::plus) }
            day.doneByReminder.forEach { (id, n) -> doneById.merge(id, n, Int::plus) }
        }
        return reminders
            .mapNotNull { reminder ->
                val fired = firedById[reminder.id] ?: 0
                if (fired == 0) return@mapNotNull null
                val done = doneById[reminder.id] ?: 0
                TopReminderStat(
                    reminder = reminder,
                    completionPct = (done * 100 / fired).coerceIn(0, 100),
                    doneCount = done,
                )
            }
            .sortedWith(compareByDescending<TopReminderStat> { it.completionPct }.thenByDescending { it.doneCount })
            .take(MAX_TOP)
    }

    private companion object {
        const val WEEKS = 12
        const val MAX_TOP = 5
    }
}

sealed interface StatsUiState {
    data object Loading : StatsUiState

    /** No reminder has fired yet — nothing to chart. */
    data object Empty : StatsUiState

    data class Loaded(
        val streakDays: Int,
        /** This-week done/fired; null when nothing fired in the window. */
        val completionPct: Int?,
        /** 12 columns × 7 rows; level 0..4, or -1 for a blank future cell. */
        val weeks: List<List<Int>>,
        val topReminders: List<TopReminderStat>,
    ) : StatsUiState
}

data class TopReminderStat(
    val reminder: Reminder,
    val completionPct: Int,
    val doneCount: Int,
)
