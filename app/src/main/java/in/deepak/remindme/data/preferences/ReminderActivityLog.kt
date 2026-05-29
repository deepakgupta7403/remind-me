package `in`.deepak.remindme.data.preferences

import `in`.deepak.remindme.data.db.ActivityDao
import `in`.deepak.remindme.data.db.ActivityStatEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/**
 * Per-day tally of reminder activity, powering the Stats screen. Now backed by
 * Room ([ActivityDao]) instead of a SharedPreferences blob — but the public API
 * is unchanged, so [in.deepak.remindme.ui.screens.stats.StatsViewModel],
 * [in.deepak.remindme.scheduler.ReminderFireHandler], and the alert screen are
 * untouched.
 *
 * Storage is one normalized row per (day, reminder); [observe] re-aggregates
 * those into the per-day [DayStat] the UI expects. History is pruned to the last
 * [RETENTION_DAYS] days — the window the 12-week heatmap needs.
 *
 * Vocabulary:
 *   fired = the alarm triggered and we showed the alert (recorded in
 *           [in.deepak.remindme.scheduler.ReminderFireHandler]).
 *   done  = the user tapped "Mark as done" on the alert
 *           ([in.deepak.remindme.ui.screens.alert.ReminderAlertActivity]).
 *   completion % = done / fired.
 *
 * [recordFired]/[recordDone] are intentionally non-suspend, fire-and-forget:
 * `recordFired` is called from a suspend context but `recordDone` fires from a
 * Compose `onClick` immediately before the alert activity finishes. Both launch
 * on [scope] — owned here at app scope, not by any caller — so the Room write
 * completes even after the activity is gone.
 */
class ReminderActivityLog(
    private val dao: ActivityDao,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) {
    /** Daily aggregates, oldest-first, capped to the last [RETENTION_DAYS] days. */
    fun observe(): Flow<List<DayStat>> =
        dao.observeAll().map { rows -> rows.toDayStats() }

    fun recordFired(reminderId: Long) {
        val today = LocalDate.now(clock).toEpochDay()
        scope.launch { dao.recordFired(today, reminderId, today - RETENTION_DAYS) }
    }

    fun recordDone(reminderId: Long) {
        val today = LocalDate.now(clock).toEpochDay()
        scope.launch { dao.recordDone(today, reminderId, today - RETENTION_DAYS) }
    }

    /** Collapse the normalized rows back into one [DayStat] per calendar day. */
    private fun List<ActivityStatEntity>.toDayStats(): List<DayStat> =
        groupBy { it.epochDay }
            .map { (day, rows) ->
                DayStat(
                    epochDay = day,
                    firedByReminder = rows.filter { it.fired > 0 }.associate { it.reminderId to it.fired },
                    doneByReminder = rows.filter { it.done > 0 }.associate { it.reminderId to it.done },
                )
            }
            .sortedBy { it.epochDay }

    private companion object {
        const val RETENTION_DAYS = 84L // 12 weeks
    }
}

/** Activity counts for one calendar day, keyed by reminder id. */
data class DayStat(
    val epochDay: Long,
    val firedByReminder: Map<Long, Int> = emptyMap(),
    val doneByReminder: Map<Long, Int> = emptyMap(),
) {
    val totalFired: Int get() = firedByReminder.values.sum()
    val totalDone: Int get() = doneByReminder.values.sum()
}
