package `in`.deepak.remindme.data.templates

import `in`.deepak.remindme.domain.model.Category
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.model.TimeOfDay
import `in`.deepak.remindme.domain.model.TimeWindow
import java.time.DayOfWeek

/**
 * Hard-coded set of starter templates surfaced on the Templates tab.
 *
 * Kept in code (not in a remote config or JSON) because the curation IS the
 * product — these are the "Drink water → every hour" hooks that show new
 * users what the app is for. They change roughly never.
 *
 * Entries are grouped by [TemplateTag] (the filter chips). Each template names
 * an icon by [ReminderTemplate.iconKey]; the built [Reminder] carries the same
 * key so the tile and the saved reminder always show the same glyph. The
 * `daily`/`interval`/`weekly` helpers below keep each entry to a single line of
 * intent — adding a template is one call, not a hand-rolled [Reminder].
 */
object TemplateCatalog {

    val all: List<ReminderTemplate> = listOf(
        // --- Health ---
        interval("drink-water", "Drink water", "Every hour · 8 am–10 pm", TemplateTag.Health, "water", TemplateTone.Peach, everyMinutes = 60, from = 8, to = 22, category = Category.Health),
        daily("take-medication", "Take medication", "Daily at 9 am", TemplateTag.Health, "medication", TemplateTone.Peach, hour = 9, minute = 0, category = Category.Health),
        daily("take-vitamins", "Take vitamins", "Daily at 9 am", TemplateTag.Health, "health", TemplateTone.Peach, hour = 9, minute = 0, category = Category.Health),
        interval("stretch", "Stretch / posture check", "Every hour · 9 am–6 pm", TemplateTag.Health, "spa", TemplateTone.Peach, everyMinutes = 60, from = 9, to = 18, category = Category.Health),
        interval("rest-eyes", "Rest eyes (20-20-20)", "Every 20 min · 9 am–6 pm", TemplateTag.Health, "eye", TemplateTone.Peach, everyMinutes = 20, from = 9, to = 18, category = Category.Health),
        interval("stand-up", "Stand up from desk", "Every 30 min · 9 am–6 pm", TemplateTag.Health, "walk", TemplateTone.Peach, everyMinutes = 30, from = 9, to = 18, category = Category.Health),
        daily("apply-sunscreen", "Apply sunscreen", "Daily at 8 am", TemplateTag.Health, "sun", TemplateTone.Peach, hour = 8, minute = 0, category = Category.Health),

        // --- Mind & mood ---
        daily("meditate", "Meditate", "Daily at 7 am", TemplateTag.MindMood, "meditate", TemplateTone.Lavender, hour = 7, minute = 0, category = Category.Personal),
        interval("deep-breathing", "Deep breathing break", "Every 2 hours · 10 am–6 pm", TemplateTag.MindMood, "spa", TemplateTone.Lavender, everyMinutes = 120, from = 10, to = 18, category = Category.Personal),
        daily("journal", "Journal / gratitude", "Daily at 9 pm", TemplateTag.MindMood, "book", TemplateTone.Lavender, hour = 21, minute = 0, category = Category.Personal),

        // --- Fitness ---
        weekly("workout", "Workout time", "Mon · Wed · Fri at 6 pm", TemplateTag.Fitness, "workout", TemplateTone.Mint, days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), hour = 18, minute = 0, category = Category.Personal),
        daily("take-a-walk", "Take a walk", "Daily at 6 pm", TemplateTag.Fitness, "walk", TemplateTone.Mint, hour = 18, minute = 0, category = Category.Personal),

        // --- Sleep & routine ---
        daily("sleep-time", "Sleep time / bedtime", "Daily at 10 pm", TemplateTag.SleepRoutine, "sleep", TemplateTone.Sky, hour = 22, minute = 0, category = Category.Health),
        daily("wind-down", "Wind-down (no screens)", "Daily at 9:30 pm", TemplateTag.SleepRoutine, "spa", TemplateTone.Sky, hour = 21, minute = 30, category = Category.Personal),
        daily("wake-up", "Wake up", "Daily at 6:30 am", TemplateTag.SleepRoutine, "alarm", TemplateTone.Sky, hour = 6, minute = 30, category = Category.Personal),

        // --- Work & productivity ---
        interval("pomodoro", "Pomodoro break", "Every 25 min · 9 am–6 pm", TemplateTag.Work, "clock", TemplateTone.Pink, everyMinutes = 25, from = 9, to = 18, category = Category.Work),
        daily("end-workday", "End of workday", "Daily at 6 pm", TemplateTag.Work, "work", TemplateTone.Pink, hour = 18, minute = 0, category = Category.Work),
        daily("plan-tomorrow", "Plan tomorrow", "Daily at 9 pm", TemplateTag.Work, "checklist", TemplateTone.Pink, hour = 21, minute = 0, category = Category.Work),

        // --- Home & chores ---
        weekly("water-plants", "Water plants", "Sunday at 8 am", TemplateTag.Home, "plants", TemplateTone.Mint, days = setOf(DayOfWeek.SUNDAY), hour = 8, minute = 0, category = Category.Personal),
        weekly("take-out-trash", "Take out trash", "Monday at 7 am", TemplateTag.Home, "cleaning", TemplateTone.Mint, days = setOf(DayOfWeek.MONDAY), hour = 7, minute = 0, category = Category.Personal),
        weekly("change-bedsheets", "Change bedsheets", "Sunday at 10 am", TemplateTag.Home, "laundry", TemplateTone.Mint, days = setOf(DayOfWeek.SUNDAY), hour = 10, minute = 0, category = Category.Personal),

        // --- Personal finance ---
        weekly("pay-bills", "Pay bills", "Monday at 10 am", TemplateTag.Finance, "payment", TemplateTone.Peach, days = setOf(DayOfWeek.MONDAY), hour = 10, minute = 0, category = Category.Personal),

        // --- Family & social ---
        weekly("call-parents", "Call parents", "Sunday at 11 am", TemplateTag.Social, "call", TemplateTone.Lavender, days = setOf(DayOfWeek.SUNDAY), hour = 11, minute = 0, category = Category.Personal),
        weekly("text-a-friend", "Text a friend", "Saturday at 6 pm", TemplateTag.Social, "email", TemplateTone.Lavender, days = setOf(DayOfWeek.SATURDAY), hour = 18, minute = 0, category = Category.Personal),

        // --- Maintenance ---
        weekly("backup-phone", "Backup phone", "Sunday at 9 pm", TemplateTag.Maintenance, "cloud", TemplateTone.Sky, days = setOf(DayOfWeek.SUNDAY), hour = 21, minute = 0, category = Category.Personal),
    )

    private fun daily(
        id: String,
        title: String,
        summary: String,
        tag: TemplateTag,
        iconKey: String,
        tone: TemplateTone,
        hour: Int,
        minute: Int,
        category: Category,
    ) = ReminderTemplate(id, title, summary, setOf(tag), iconKey, tone) {
        Reminder.Daily(
            id = Reminder.UNSAVED_ID,
            title = title,
            enabled = true,
            category = category,
            timeOfDay = TimeOfDay.of(hour, minute),
            iconKey = iconKey,
        )
    }

    private fun interval(
        id: String,
        title: String,
        summary: String,
        tag: TemplateTag,
        iconKey: String,
        tone: TemplateTone,
        everyMinutes: Int,
        from: Int,
        to: Int,
        category: Category,
    ) = ReminderTemplate(id, title, summary, setOf(tag), iconKey, tone) {
        Reminder.Interval(
            id = Reminder.UNSAVED_ID,
            title = title,
            enabled = true,
            category = category,
            intervalMinutes = everyMinutes,
            activeWindow = TimeWindow(TimeOfDay.of(from, 0), TimeOfDay.of(to, 0)),
            iconKey = iconKey,
        )
    }

    private fun weekly(
        id: String,
        title: String,
        summary: String,
        tag: TemplateTag,
        iconKey: String,
        tone: TemplateTone,
        days: Set<DayOfWeek>,
        hour: Int,
        minute: Int,
        category: Category,
    ) = ReminderTemplate(id, title, summary, setOf(tag), iconKey, tone) {
        Reminder.Weekly(
            id = Reminder.UNSAVED_ID,
            title = title,
            enabled = true,
            category = category,
            timeOfDay = TimeOfDay.of(hour, minute),
            daysOfWeek = days,
            iconKey = iconKey,
        )
    }
}
