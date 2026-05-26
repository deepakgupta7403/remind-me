package `in`.deepak.remindme.domain.model

/**
 * Wall-clock time of day, independent of date/timezone.
 *
 * Stored as a single minute-of-day value (0..1439) for cheap comparison and
 * storage. Constructors validate so we can never end up with hour=25.
 */
@JvmInline
value class TimeOfDay private constructor(val minuteOfDay: Int) : Comparable<TimeOfDay> {

    val hour: Int get() = minuteOfDay / 60
    val minute: Int get() = minuteOfDay % 60

    override fun compareTo(other: TimeOfDay): Int =
        minuteOfDay.compareTo(other.minuteOfDay)

    override fun toString(): String =
        "%02d:%02d".format(hour, minute)

    companion object {
        fun of(hour: Int, minute: Int): TimeOfDay {
            require(hour in 0..23) { "hour out of range: $hour" }
            require(minute in 0..59) { "minute out of range: $minute" }
            return TimeOfDay(hour * 60 + minute)
        }

        fun fromMinuteOfDay(minuteOfDay: Int): TimeOfDay {
            require(minuteOfDay in 0..1439) { "minuteOfDay out of range: $minuteOfDay" }
            return TimeOfDay(minuteOfDay)
        }
    }
}
