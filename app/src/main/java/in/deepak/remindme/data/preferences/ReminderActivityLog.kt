package `in`.deepak.remindme.data.preferences

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.LocalDate

/**
 * Per-day tally of reminder activity, powering the Stats screen.
 *
 * Stores compact daily aggregates (one [DayStat] per calendar day, not one row
 * per fire) so a 15-minute interval reminder can't bloat storage. History is
 * pruned to the last [RETENTION_DAYS] days — exactly the window the 12-week
 * heatmap needs.
 *
 * Vocabulary:
 *   fired  = the alarm triggered and we showed the alert (recorded in
 *            [in.deepak.remindme.scheduler.ReminderFireHandler]).
 *   done   = the user tapped "Mark as done" on the alert
 *            ([in.deepak.remindme.ui.screens.alert.ReminderAlertActivity]).
 *   completion % = done / fired.
 *
 * SharedPreferences + Gson mirrors [SearchPreferences] — not worth a DataStore
 * for one small blob. A [StateFlow] is exposed so the Stats screen updates live
 * when a reminder fires or is completed while the app is open (single process).
 */
class ReminderActivityLog(
    context: Context,
    private val gson: Gson = Gson(),
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val prefs = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _days = MutableStateFlow(load())

    /** Daily aggregates, oldest-first, capped to the last [RETENTION_DAYS] days. */
    fun observe(): StateFlow<List<DayStat>> = _days.asStateFlow()

    fun recordFired(reminderId: Long) = mutateToday { day ->
        day.copy(firedByReminder = day.firedByReminder.plusOne(reminderId))
    }

    fun recordDone(reminderId: Long) = mutateToday { day ->
        day.copy(doneByReminder = day.doneByReminder.plusOne(reminderId))
    }

    // Synchronized because fired events arrive on a background receiver thread
    // while a "done" tap arrives on the main thread — both mutate today's row.
    @Synchronized
    private fun mutateToday(transform: (DayStat) -> DayStat) {
        val today = LocalDate.now(clock).toEpochDay()
        val current = _days.value
        val existing = current.firstOrNull { it.epochDay == today } ?: DayStat(today)
        val next = (current.filterNot { it.epochDay == today } + transform(existing))
            .filter { it.epochDay >= today - RETENTION_DAYS }
            .sortedBy { it.epochDay }
        save(next)
        _days.value = next
    }

    private fun load(): List<DayStat> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { gson.fromJson<List<DayStat>>(raw, TYPE) }
            .getOrNull()
            ?.sortedBy { it.epochDay }
            ?: emptyList()
    }

    private fun save(days: List<DayStat>) {
        prefs.edit().putString(KEY, gson.toJson(days)).apply()
    }

    private fun Map<Long, Int>.plusOne(id: Long): Map<Long, Int> =
        this + (id to (this[id] ?: 0) + 1)

    private companion object {
        const val FILE = "remindme.activity"
        const val KEY = "days"
        const val RETENTION_DAYS = 84L // 12 weeks
        val TYPE = object : TypeToken<List<DayStat>>() {}.type
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
