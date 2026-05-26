package `in`.deepak.remindme.data.dto

/**
 * Flat, Gson-friendly representation of [in.deepak.remindme.domain.model.Reminder].
 *
 * All fields nullable so missing keys in older JSON (forward compatibility) decode
 * cleanly. The mapper enforces the per-type required fields. We deliberately keep
 * this DTO flat instead of leaning on Gson polymorphism — flat is trivial to
 * migrate to Room columns later.
 *
 * `type` is a string, not an enum, so reading a JSON file written by a newer
 * version that introduced new types fails gracefully (mapper returns null).
 */
data class ReminderDto(
    val id: Long? = null,
    val title: String? = null,
    val enabled: Boolean? = null,
    val type: String? = null,
    /** Category enum NAME — null on older files defaults to "Other" in the mapper. */
    val category: String? = null,

    // INTERVAL
    val intervalMinutes: Int? = null,
    val activeStartMinute: Int? = null,
    val activeEndMinute: Int? = null,

    // ONE_TIME
    val triggerAtEpochMillis: Long? = null,

    // DAILY / WEEKLY
    val timeOfDayMinute: Int? = null,

    // DAILY extras (UI for now; skipWeekends is honoured by NextFireTime)
    val minutesBefore: Int? = null,
    val skipWeekends: Boolean? = null,

    // WEEKLY
    val daysOfWeekBitmask: Int? = null,
) {
    companion object {
        const val TYPE_INTERVAL = "INTERVAL"
        const val TYPE_ONE_TIME = "ONE_TIME"
        const val TYPE_DAILY = "DAILY"
        const val TYPE_WEEKLY = "WEEKLY"
    }
}

/** Envelope for the on-disk file — versioned for forward-compatible migrations. */
data class ReminderFileEnvelope(
    val schemaVersion: Int = SCHEMA_VERSION,
    val nextId: Long = 1L,
    val reminders: List<ReminderDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
