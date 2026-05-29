package `in`.deepak.remindme.data.mapper

import `in`.deepak.remindme.data.db.ReminderEntity
import `in`.deepak.remindme.domain.model.Category
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.model.TimeOfDay
import `in`.deepak.remindme.domain.model.TimeWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek

/**
 * Round-trips every reminder type through [toEntity] → [toDomainOrNull] to prove
 * the Room mapping is lossless (this is the data that moved off the JSON file),
 * plus the forgiving decode of a malformed / unknown-type row.
 */
class ReminderEntityMapperTest {

    @Test
    fun interval_roundTrips() {
        val original = Reminder.Interval(
            id = 7,
            title = "Drink water",
            enabled = true,
            category = Category.Health,
            intervalMinutes = 60,
            activeWindow = TimeWindow(TimeOfDay.of(8, 0), TimeOfDay.of(22, 0)),
            iconKey = "water",
        )
        assertEquals(original, original.toEntity().toDomainOrNull())
    }

    @Test
    fun oneTime_roundTrips() {
        val original = Reminder.OneTime(
            id = 3,
            title = "Dentist",
            enabled = false,
            category = Category.Personal,
            triggerAtEpochMillis = 1_700_000_000_000L,
            iconKey = null,
        )
        assertEquals(original, original.toEntity().toDomainOrNull())
    }

    @Test
    fun daily_roundTrips() {
        val original = Reminder.Daily(
            id = 11,
            title = "Meditate",
            enabled = true,
            category = Category.Personal,
            timeOfDay = TimeOfDay.of(7, 30),
            minutesBefore = 5,
            skipWeekends = true,
            iconKey = "meditate",
        )
        assertEquals(original, original.toEntity().toDomainOrNull())
    }

    @Test
    fun weekly_roundTrips() {
        val original = Reminder.Weekly(
            id = 42,
            title = "Workout",
            enabled = true,
            category = Category.Work,
            timeOfDay = TimeOfDay.of(18, 0),
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            iconKey = "workout",
        )
        assertEquals(original, original.toEntity().toDomainOrNull())
    }

    @Test
    fun unknownType_decodesToNull() {
        val row = ReminderEntity(
            id = 1, title = "x", enabled = true, type = "MONTHLY",
            category = Category.Other.name, iconKey = null,
        )
        assertNull(row.toDomainOrNull())
    }

    @Test
    fun missingRequiredField_decodesToNull() {
        // INTERVAL row with no intervalMinutes/window → can't reconstruct.
        val row = ReminderEntity(
            id = 1, title = "x", enabled = true, type = "INTERVAL",
            category = Category.Health.name, iconKey = null,
        )
        assertNull(row.toDomainOrNull())
    }
}
