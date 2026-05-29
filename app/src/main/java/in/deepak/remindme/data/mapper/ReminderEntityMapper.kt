package `in`.deepak.remindme.data.mapper

import `in`.deepak.remindme.data.db.ReminderEntity
import `in`.deepak.remindme.data.dto.ReminderDto
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.model.TimeOfDay
import `in`.deepak.remindme.domain.model.TimeWindow

/**
 * Domain ↔ Room entity mapping. Mirrors the DTO mapper in [ReminderMapper] one
 * branch per reminder type, but against [ReminderEntity]. Reuses the shared
 * [encodeDays]/[decodeDays]/[decodeCategory] codecs and the [ReminderDto] type
 * constants so the type-string vocabulary stays single-sourced.
 *
 * [toDomainOrNull] returns null for a malformed row or an unknown `type`, so a
 * column written by a newer build decodes "as much as it can" rather than
 * crashing — same forgiving contract the file repo had.
 */
internal fun Reminder.toEntity(): ReminderEntity = when (this) {
    is Reminder.Interval -> ReminderEntity(
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

    is Reminder.OneTime -> ReminderEntity(
        id = id,
        title = title,
        enabled = enabled,
        type = ReminderDto.TYPE_ONE_TIME,
        category = category.name,
        iconKey = iconKey,
        triggerAtEpochMillis = triggerAtEpochMillis,
    )

    is Reminder.Daily -> ReminderEntity(
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

    is Reminder.Weekly -> ReminderEntity(
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

internal fun ReminderEntity.toDomainOrNull(): Reminder? {
    val category = decodeCategory(category)

    return when (type) {
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
