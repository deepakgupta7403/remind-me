package `in`.deepak.remindme.data.mapper

import `in`.deepak.remindme.data.db.TemplateEntity
import `in`.deepak.remindme.data.dto.ReminderDto
import `in`.deepak.remindme.data.templates.ReminderTemplate
import `in`.deepak.remindme.data.templates.TemplateTag
import `in`.deepak.remindme.data.templates.TemplateTone
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.model.TimeOfDay
import `in`.deepak.remindme.domain.model.TimeWindow

/**
 * [ReminderTemplate] ↔ [TemplateEntity].
 *
 * Seeding ([toEntity]) derives the stored schedule ingredients by running the
 * template's own `build()` once and reading the resulting [Reminder]'s fields —
 * so there's no second copy of "what schedule does this template produce".
 *
 * Reading ([toTemplate]) reconstructs the `build: () -> Reminder` lambda from
 * those ingredients and resolves the icon from [iconKey] via the icon pack
 * (handled inside [ReminderTemplate]). [tone]/[tags] are parsed by name with
 * safe fallbacks so an unknown value never crashes the grid.
 */
internal fun ReminderTemplate.toEntity(displayOrder: Int): TemplateEntity {
    val sample = build()
    val base = TemplateEntity(
        id = id,
        title = title,
        scheduleSummary = scheduleSummary,
        tags = tags.joinToString(",") { it.name },
        iconKey = iconKey,
        tone = tone.name,
        displayOrder = displayOrder,
        reminderType = sample.typeName(),
        category = sample.category.name,
    )
    return when (sample) {
        is Reminder.Interval -> base.copy(
            intervalMinutes = sample.intervalMinutes,
            activeStartMinute = sample.activeWindow.start.minuteOfDay,
            activeEndMinute = sample.activeWindow.end.minuteOfDay,
        )
        is Reminder.OneTime -> base
        is Reminder.Daily -> base.copy(
            timeOfDayMinute = sample.timeOfDay.minuteOfDay,
        )
        is Reminder.Weekly -> base.copy(
            timeOfDayMinute = sample.timeOfDay.minuteOfDay,
            daysOfWeekBitmask = encodeDays(sample.daysOfWeek),
        )
    }
}

internal fun TemplateEntity.toTemplate(): ReminderTemplate = ReminderTemplate(
    id = id,
    title = title,
    scheduleSummary = scheduleSummary,
    tags = tags.split(",")
        .mapNotNull { name -> runCatching { TemplateTag.valueOf(name) }.getOrNull() }
        .toSet(),
    iconKey = iconKey,
    tone = runCatching { TemplateTone.valueOf(tone) }.getOrNull() ?: TemplateTone.Lavender,
    build = { buildReminder() },
)

/** Rebuilds an unsaved [Reminder] from the stored ingredients. */
private fun TemplateEntity.buildReminder(): Reminder {
    val category = decodeCategory(category)
    return when (reminderType) {
        ReminderDto.TYPE_INTERVAL -> Reminder.Interval(
            id = Reminder.UNSAVED_ID,
            title = title,
            enabled = true,
            category = category,
            intervalMinutes = requireNotNull(intervalMinutes),
            activeWindow = TimeWindow(
                start = TimeOfDay.fromMinuteOfDay(requireNotNull(activeStartMinute)),
                end = TimeOfDay.fromMinuteOfDay(requireNotNull(activeEndMinute)),
            ),
            iconKey = iconKey,
        )

        ReminderDto.TYPE_DAILY -> Reminder.Daily(
            id = Reminder.UNSAVED_ID,
            title = title,
            enabled = true,
            category = category,
            timeOfDay = TimeOfDay.fromMinuteOfDay(requireNotNull(timeOfDayMinute)),
            iconKey = iconKey,
        )

        ReminderDto.TYPE_WEEKLY -> Reminder.Weekly(
            id = Reminder.UNSAVED_ID,
            title = title,
            enabled = true,
            category = category,
            timeOfDay = TimeOfDay.fromMinuteOfDay(requireNotNull(timeOfDayMinute)),
            daysOfWeek = decodeDays(requireNotNull(daysOfWeekBitmask)),
            iconKey = iconKey,
        )

        ReminderDto.TYPE_ONE_TIME -> Reminder.OneTime(
            id = Reminder.UNSAVED_ID,
            title = title,
            enabled = true,
            category = category,
            triggerAtEpochMillis = 0L,
            iconKey = iconKey,
        )

        else -> error("Unknown template reminderType: $reminderType")
    }
}

private fun Reminder.typeName(): String = when (this) {
    is Reminder.Interval -> ReminderDto.TYPE_INTERVAL
    is Reminder.OneTime -> ReminderDto.TYPE_ONE_TIME
    is Reminder.Daily -> ReminderDto.TYPE_DAILY
    is Reminder.Weekly -> ReminderDto.TYPE_WEEKLY
}
