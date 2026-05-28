package `in`.deepak.remindme.domain.model

import java.time.DayOfWeek

/**
 * A user-defined reminder.
 *
 * Sealed interface so:
 *   - new reminder types = new subclass + compiler-enforced exhaustive `when`
 *     across the scheduler/UI/notification layers
 *   - each variant carries exactly the fields it needs (no nullable spaghetti)
 *
 * To add a new type later (e.g., Reminder.Monthly):
 *   1. Add the data class here.
 *   2. Compile — every `when (reminder)` site lights up red until you handle it.
 *   3. Add the mapper rows in [in.deepak.remindme.data.dto.ReminderDto] +
 *      [in.deepak.remindme.data.mapper.ReminderMapper].
 *   4. Add a scheduling strategy in [in.deepak.remindme.scheduler] (Step 5).
 *   5. Add an edit form in [in.deepak.remindme.ui.screens.edit] (Step 8).
 * Steps 3–5 are the ≤3-file-touch rule from project guidance.
 */
sealed interface Reminder {

    val id: Long
    val title: String
    val enabled: Boolean
    val category: Category

    /**
     * Optional icon-pack key from [in.deepak.remindme.data.icons.ReminderIconPack].
     * Null means "use the per-type default icon" at the render site.
     */
    val iconKey: String?

    /** Repeating reminder that fires every [intervalMinutes] within [activeWindow]. */
    data class Interval(
        override val id: Long,
        override val title: String,
        override val enabled: Boolean,
        override val category: Category,
        val intervalMinutes: Int,
        val activeWindow: TimeWindow,
        override val iconKey: String? = null,
    ) : Reminder {
        init {
            require(intervalMinutes in 1..1440) {
                "intervalMinutes out of range: $intervalMinutes"
            }
        }
    }

    /** Single-shot reminder at a specific instant. */
    data class OneTime(
        override val id: Long,
        override val title: String,
        override val enabled: Boolean,
        override val category: Category,
        val triggerAtEpochMillis: Long,
        override val iconKey: String? = null,
    ) : Reminder

    /**
     * Same time every day.
     *
     * [minutesBefore] is a planned early-warning offset (UI-only for now —
     * persisted, but the scheduler still fires only the main alarm). When the
     * early-warning alarm is wired the second `setExact` will live in
     * [`in.deepak.remindme.scheduler.AndroidAlarmScheduler`].
     *
     * [skipWeekends] *is* honoured by [`nextFireTimeMillis`]; Sat/Sun are
     * skipped and the next Monday is picked instead.
     */
    data class Daily(
        override val id: Long,
        override val title: String,
        override val enabled: Boolean,
        override val category: Category,
        val timeOfDay: TimeOfDay,
        val minutesBefore: Int = 0,
        val skipWeekends: Boolean = false,
        override val iconKey: String? = null,
    ) : Reminder

    /** Same time on specified days of week (must have at least one day). */
    data class Weekly(
        override val id: Long,
        override val title: String,
        override val enabled: Boolean,
        override val category: Category,
        val timeOfDay: TimeOfDay,
        val daysOfWeek: Set<DayOfWeek>,
        override val iconKey: String? = null,
    ) : Reminder {
        init {
            require(daysOfWeek.isNotEmpty()) {
                "Weekly reminder needs at least one day of week"
            }
        }
    }

    companion object {
        /** Id assigned by the repository on first insert. Means "unsaved" before that. */
        const val UNSAVED_ID: Long = 0L
    }
}
