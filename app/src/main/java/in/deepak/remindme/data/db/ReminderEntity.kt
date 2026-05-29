package `in`.deepak.remindme.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for a [in.deepak.remindme.domain.model.Reminder].
 *
 * Flat columns, mirroring the now-legacy [in.deepak.remindme.data.dto.ReminderDto]
 * one-to-one (that DTO was deliberately kept flat for exactly this move). Type
 * is a string, not an enum, so a column written by a newer build with an unknown
 * type decodes to null rather than crashing — the mapper enforces per-type
 * required fields.
 *
 * [id] is `autoGenerate`: passing [in.deepak.remindme.domain.model.Reminder.UNSAVED_ID]
 * (0) on insert makes Room assign the next id, which is exactly the contract the
 * old file repository implemented by hand with a `nextId` counter.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val enabled: Boolean,
    val type: String,
    val category: String,
    val iconKey: String?,

    // INTERVAL
    val intervalMinutes: Int? = null,
    val activeStartMinute: Int? = null,
    val activeEndMinute: Int? = null,

    // ONE_TIME
    val triggerAtEpochMillis: Long? = null,

    // DAILY / WEEKLY
    val timeOfDayMinute: Int? = null,

    // DAILY extras
    val minutesBefore: Int? = null,
    val skipWeekends: Boolean? = null,

    // WEEKLY
    val daysOfWeekBitmask: Int? = null,
)
