package `in`.deepak.remindme.domain.model

/**
 * Daily active window, e.g. 09:00–18:00 for water reminders.
 *
 * Same-day only — [end] must be ≥ [start]. We deliberately don't model
 * overnight windows (22:00–06:00) in v1; if a user asks for that, we'll add
 * a sealed subclass `OvernightWindow` so the constraint stays explicit at the
 * type level rather than via a boolean flag.
 */
data class TimeWindow(
    val start: TimeOfDay,
    val end: TimeOfDay,
) {
    init {
        require(end >= start) {
            "TimeWindow.end ($end) must be >= start ($start)"
        }
    }

    fun contains(time: TimeOfDay): Boolean =
        time >= start && time <= end

    companion object {
        /** Sensible default for interval reminders: 09:00–21:00. */
        val DefaultActiveDay: TimeWindow = TimeWindow(
            start = TimeOfDay.of(9, 0),
            end = TimeOfDay.of(21, 0),
        )
    }
}
