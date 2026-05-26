package `in`.deepak.remindme.ui.state

import `in`.deepak.remindme.domain.model.Category
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.model.TimeOfDay
import `in`.deepak.remindme.domain.model.TimeWindow
import java.time.DayOfWeek

/**
 * What the user is editing right now.
 *
 * Holds every per-type field as a separate property so the form can show
 * partially-invalid intermediate states (e.g., interval = 0 while the user
 * is typing) without forcing the domain model into a nullable-spaghetti shape.
 *
 * Convert to a domain [Reminder] with [toReminderOrNull] — returns null if
 * the current values aren't a valid reminder of the selected [type].
 *
 * Adding a new reminder type:
 *   1. Add a value to [ReminderType].
 *   2. Add the fields to this class (with sensible defaults).
 *   3. Handle the new branch in [toReminderOrNull] and [fromReminder].
 */
enum class ReminderType { INTERVAL, ONE_TIME, DAILY, WEEKLY }

data class ReminderForm(
    val id: Long = Reminder.UNSAVED_ID,
    val title: String = "",
    val enabled: Boolean = true,
    val type: ReminderType = ReminderType.INTERVAL,
    val category: Category = Category.Other,

    // Interval
    val intervalMinutes: Int = 60,
    val activeWindowStart: TimeOfDay = TimeOfDay.of(8, 0),
    val activeWindowEnd: TimeOfDay = TimeOfDay.of(22, 0),

    // OneTime
    val triggerAtEpochMillis: Long? = null,

    // Daily + Weekly
    val timeOfDay: TimeOfDay = TimeOfDay.of(9, 0),

    // Daily extras
    val minutesBefore: Int = 0,
    val skipWeekends: Boolean = false,

    // Weekly
    val daysOfWeek: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY),
) {

    /**
     * True if the current form is something we could save. The screen uses
     * this to enable/disable the Save button so we don't fight the user with
     * inline errors while they're still typing.
     */
    val isValid: Boolean get() = toReminderOrNull() != null

    fun toReminderOrNull(): Reminder? {
        if (title.isBlank()) return null
        return runCatching {
            when (type) {
                ReminderType.INTERVAL -> Reminder.Interval(
                    id = id,
                    title = title.trim(),
                    enabled = enabled,
                    category = category,
                    intervalMinutes = intervalMinutes,
                    activeWindow = TimeWindow(activeWindowStart, activeWindowEnd),
                )
                ReminderType.ONE_TIME -> {
                    val trigger = triggerAtEpochMillis ?: return null
                    Reminder.OneTime(id, title.trim(), enabled, category, trigger)
                }
                ReminderType.DAILY -> Reminder.Daily(
                    id = id,
                    title = title.trim(),
                    enabled = enabled,
                    category = category,
                    timeOfDay = timeOfDay,
                    minutesBefore = minutesBefore.coerceAtLeast(0),
                    skipWeekends = skipWeekends,
                )
                ReminderType.WEEKLY -> Reminder.Weekly(
                    id = id,
                    title = title.trim(),
                    enabled = enabled,
                    category = category,
                    timeOfDay = timeOfDay,
                    daysOfWeek = daysOfWeek,
                )
            }
        }.getOrNull()
    }

    companion object {
        fun fromReminder(reminder: Reminder): ReminderForm = when (reminder) {
            is Reminder.Interval -> ReminderForm(
                id = reminder.id,
                title = reminder.title,
                enabled = reminder.enabled,
                category = reminder.category,
                type = ReminderType.INTERVAL,
                intervalMinutes = reminder.intervalMinutes,
                activeWindowStart = reminder.activeWindow.start,
                activeWindowEnd = reminder.activeWindow.end,
            )
            is Reminder.OneTime -> ReminderForm(
                id = reminder.id,
                title = reminder.title,
                enabled = reminder.enabled,
                category = reminder.category,
                type = ReminderType.ONE_TIME,
                triggerAtEpochMillis = reminder.triggerAtEpochMillis,
            )
            is Reminder.Daily -> ReminderForm(
                id = reminder.id,
                title = reminder.title,
                enabled = reminder.enabled,
                category = reminder.category,
                type = ReminderType.DAILY,
                timeOfDay = reminder.timeOfDay,
                minutesBefore = reminder.minutesBefore,
                skipWeekends = reminder.skipWeekends,
            )
            is Reminder.Weekly -> ReminderForm(
                id = reminder.id,
                title = reminder.title,
                enabled = reminder.enabled,
                category = reminder.category,
                type = ReminderType.WEEKLY,
                timeOfDay = reminder.timeOfDay,
                daysOfWeek = reminder.daysOfWeek,
            )
        }
    }
}
