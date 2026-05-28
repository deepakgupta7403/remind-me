package `in`.deepak.remindme.data.mapper

import `in`.deepak.remindme.data.dto.ReminderDto
import `in`.deepak.remindme.domain.model.Category
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.model.TimeOfDay
import `in`.deepak.remindme.domain.model.TimeWindow
import java.time.DayOfWeek

/**
 * Domain ↔ DTO mapping lives here, not in the repository or the model.
 *
 * Keeping it as top-level functions (no class) means:
 *  - testable without instantiation
 *  - mapper grows linearly with new reminder types (one new branch in each direction)
 */

internal fun Reminder.toDto(): ReminderDto = when (this) {
    is Reminder.Interval -> ReminderDto(
        id = id,
        title = title,
        enabled = enabled,
        type = ReminderDto.TYPE_INTERVAL,
        category = category.name,
        iconKey = iconKey,
        intervalMinutes = intervalMinutes,
        activeStartMinute = activeWindow.start.minuteOfDay,
        activeEndMinute = activeWindow.end.minuteOfDay,
    )

    is Reminder.OneTime -> ReminderDto(
        id = id,
        title = title,
        enabled = enabled,
        type = ReminderDto.TYPE_ONE_TIME,
        category = category.name,
        iconKey = iconKey,
        triggerAtEpochMillis = triggerAtEpochMillis,
    )

    is Reminder.Daily -> ReminderDto(
        id = id,
        title = title,
        enabled = enabled,
        type = ReminderDto.TYPE_DAILY,
        category = category.name,
        iconKey = iconKey,
        timeOfDayMinute = timeOfDay.minuteOfDay,
        minutesBefore = minutesBefore,
        skipWeekends = skipWeekends,
    )

    is Reminder.Weekly -> ReminderDto(
        id = id,
        title = title,
        enabled = enabled,
        type = ReminderDto.TYPE_WEEKLY,
        category = category.name,
        iconKey = iconKey,
        timeOfDayMinute = timeOfDay.minuteOfDay,
        daysOfWeekBitmask = encodeDays(daysOfWeek),
    )
}

/**
 * Returns null if [ReminderDto] is malformed or carries an unknown `type` — that
 * way a future-versioned JSON file decodes "as much as it can" rather than
 * crashing the app at startup.
 */
internal fun ReminderDto.toDomainOrNull(): Reminder? {
    val id = this.id ?: return null
    val title = this.title ?: return null
    val enabled = this.enabled ?: return null
    val category = decodeCategory(this.category)

    return when (this.type) {
        ReminderDto.TYPE_INTERVAL -> {
            val interval = intervalMinutes ?: return null
            val startMin = activeStartMinute ?: return null
            val endMin = activeEndMinute ?: return null
            runCatching {
                Reminder.Interval(
                    id = id,
                    title = title,
                    enabled = enabled,
                    category = category,
                    intervalMinutes = interval,
                    activeWindow = TimeWindow(
                        start = TimeOfDay.fromMinuteOfDay(startMin),
                        end = TimeOfDay.fromMinuteOfDay(endMin),
                    ),
                    iconKey = iconKey,
                )
            }.getOrNull()
        }

        ReminderDto.TYPE_ONE_TIME -> {
            val ts = triggerAtEpochMillis ?: return null
            Reminder.OneTime(id, title, enabled, category, ts, iconKey)
        }

        ReminderDto.TYPE_DAILY -> {
            val tod = timeOfDayMinute ?: return null
            runCatching {
                Reminder.Daily(
                    id = id,
                    title = title,
                    enabled = enabled,
                    category = category,
                    timeOfDay = TimeOfDay.fromMinuteOfDay(tod),
                    minutesBefore = minutesBefore ?: 0,
                    skipWeekends = skipWeekends ?: false,
                    iconKey = iconKey,
                )
            }.getOrNull()
        }

        ReminderDto.TYPE_WEEKLY -> {
            val tod = timeOfDayMinute ?: return null
            val mask = daysOfWeekBitmask ?: return null
            runCatching {
                Reminder.Weekly(
                    id = id,
                    title = title,
                    enabled = enabled,
                    category = category,
                    timeOfDay = TimeOfDay.fromMinuteOfDay(tod),
                    daysOfWeek = decodeDays(mask),
                    iconKey = iconKey,
                )
            }.getOrNull()
        }

        else -> null
    }
}

private fun decodeCategory(raw: String?): Category =
    raw?.let { runCatching { Category.valueOf(it) }.getOrNull() } ?: Category.Other

// --- DayOfWeek bitmask: Monday = bit 0 (value 1), ..., Sunday = bit 6 (value 64) ---

private fun encodeDays(days: Set<DayOfWeek>): Int =
    days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }

private fun decodeDays(mask: Int): Set<DayOfWeek> =
    DayOfWeek.values()
        .filter { (mask shr (it.value - 1)) and 1 == 1 }
        .toSet()
