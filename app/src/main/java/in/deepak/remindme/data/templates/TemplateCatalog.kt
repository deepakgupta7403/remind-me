package `in`.deepak.remindme.data.templates

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import `in`.deepak.remindme.domain.model.Category
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.model.TimeOfDay
import `in`.deepak.remindme.domain.model.TimeWindow

/**
 * Hard-coded set of starter templates surfaced on the Templates tab.
 *
 * Kept in code (not in a remote config or JSON) because the curation IS the
 * product — these are the "Drink water → every hour" hooks that show new
 * users what the app is for. They change roughly never.
 */
object TemplateCatalog {

    val all: List<ReminderTemplate> = listOf(
        ReminderTemplate(
            id = "drink-water",
            title = "Drink water",
            scheduleSummary = "Every hour",
            tags = setOf(TemplateTag.Health),
            icon = Icons.Filled.WaterDrop,
            tone = TemplateTone.Lavender,
            build = {
                Reminder.Interval(
                    id = Reminder.UNSAVED_ID,
                    title = "Drink water",
                    enabled = true,
                    category = Category.Health,
                    intervalMinutes = 60,
                    activeWindow = TimeWindow(TimeOfDay.of(8, 0), TimeOfDay.of(22, 0)),
                )
            },
        ),
        ReminderTemplate(
            id = "medication",
            title = "Medication",
            scheduleSummary = "Daily at 9 am",
            tags = setOf(TemplateTag.Health),
            icon = Icons.Filled.Medication,
            tone = TemplateTone.Peach,
            build = {
                Reminder.Daily(
                    id = Reminder.UNSAVED_ID,
                    title = "Medication",
                    enabled = true,
                    category = Category.Health,
                    timeOfDay = TimeOfDay.of(9, 0),
                )
            },
        ),
        ReminderTemplate(
            id = "take-a-walk",
            title = "Take a walk",
            scheduleSummary = "Every 2 hours",
            tags = setOf(TemplateTag.Fitness),
            icon = Icons.Filled.DirectionsWalk,
            tone = TemplateTone.Mint,
            build = {
                Reminder.Interval(
                    id = Reminder.UNSAVED_ID,
                    title = "Take a walk",
                    enabled = true,
                    category = Category.Personal,
                    intervalMinutes = 120,
                    activeWindow = TimeWindow(TimeOfDay.of(8, 0), TimeOfDay.of(22, 0)),
                )
            },
        ),
        ReminderTemplate(
            id = "rest-eyes",
            title = "Rest eyes",
            scheduleSummary = "Every 20 min",
            tags = setOf(TemplateTag.Health, TemplateTag.Work),
            icon = Icons.Filled.Visibility,
            tone = TemplateTone.Pink,
            build = {
                Reminder.Interval(
                    id = Reminder.UNSAVED_ID,
                    title = "Rest eyes",
                    enabled = true,
                    category = Category.Work,
                    intervalMinutes = 20,
                    activeWindow = TimeWindow(TimeOfDay.of(9, 0), TimeOfDay.of(18, 0)),
                )
            },
        ),
        ReminderTemplate(
            id = "meditate",
            title = "Meditate",
            scheduleSummary = "Daily at 7 am",
            tags = setOf(TemplateTag.Fitness, TemplateTag.Health),
            icon = Icons.Filled.SelfImprovement,
            tone = TemplateTone.Lavender,
            build = {
                Reminder.Daily(
                    id = Reminder.UNSAVED_ID,
                    title = "Meditate",
                    enabled = true,
                    category = Category.Personal,
                    timeOfDay = TimeOfDay.of(7, 0),
                )
            },
        ),
        ReminderTemplate(
            id = "sleep-time",
            title = "Sleep time",
            scheduleSummary = "Daily at 10 pm",
            tags = setOf(TemplateTag.Health),
            icon = Icons.Filled.Bedtime,
            tone = TemplateTone.Sky,
            build = {
                Reminder.Daily(
                    id = Reminder.UNSAVED_ID,
                    title = "Sleep time",
                    enabled = true,
                    category = Category.Health,
                    timeOfDay = TimeOfDay.of(22, 0),
                )
            },
        ),
    )
}
