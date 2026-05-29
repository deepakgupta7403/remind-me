package `in`.deepak.remindme.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for a starter template.
 *
 * The runtime type [in.deepak.remindme.data.templates.ReminderTemplate] carries
 * a `build: () -> Reminder` lambda and a Compose `ImageVector` — neither is
 * storable. So we persist the *ingredients* instead: the schedule fields needed
 * to reconstruct the reminder, plus the [iconKey]/[tone]/[tags] names. The
 * mapper rebuilds the lambda and resolves the icon from
 * [in.deepak.remindme.data.icons.ReminderIconPack] on read.
 *
 * The catalog in [in.deepak.remindme.data.templates.TemplateCatalog] remains the
 * curated *seed source*; [displayOrder] preserves its hand-tuned ordering. This
 * table is what enables user-created/edited templates later.
 *
 * [tags] is a comma-joined list of `TemplateTag.name`s (every catalog entry has
 * exactly one today, but the column models the set).
 */
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val title: String,
    val scheduleSummary: String,
    val tags: String,
    val iconKey: String,
    val tone: String,
    val displayOrder: Int,

    /** Reminder type this template builds (see [in.deepak.remindme.data.dto.ReminderDto] constants). */
    val reminderType: String,
    val category: String,

    // Schedule ingredients (which are set depends on reminderType).
    val intervalMinutes: Int? = null,
    val activeStartMinute: Int? = null,
    val activeEndMinute: Int? = null,
    val timeOfDayMinute: Int? = null,
    val daysOfWeekBitmask: Int? = null,
)
