package `in`.deepak.remindme.scheduler

import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.model.TimeOfDay
import `in`.deepak.remindme.domain.model.TimeWindow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure function: given the current instant, returns the next instant at which
 * [reminder] should fire — or null if there is no future fire (one-time
 * already in the past, disabled reminder, etc.).
 *
 * Pure on purpose: unit-testable by passing fixed clocks. The scheduler
 * impl uses it; the receiver uses it to re-arm; you can test all the
 * date/time math without ever booting an emulator.
 *
 * The single `when` keeps every type's rule in one file so a code reviewer
 * can see them side by side. Each branch delegates to a private function so
 * adding a new reminder type is "add a branch + add a function".
 */
fun nextFireTimeMillis(
    now: Instant,
    reminder: Reminder,
    zone: ZoneId = ZoneId.systemDefault(),
): Long? {
    if (!reminder.enabled) return null
    return when (reminder) {
        is Reminder.Interval -> nextInterval(now, reminder, zone)
        is Reminder.OneTime  -> nextOneTime(now, reminder)
        is Reminder.Daily    -> nextDaily(now, reminder, zone)
        is Reminder.Weekly   -> nextWeekly(now, reminder, zone)
    }
}

// --- per-type rules -------------------------------------------------------

private fun nextOneTime(now: Instant, r: Reminder.OneTime): Long? =
    r.triggerAtEpochMillis.takeIf { it > now.toEpochMilli() }

private fun nextDaily(now: Instant, r: Reminder.Daily, zone: ZoneId): Long {
    val nowLocal = now.atZone(zone)
    val targetTime = r.timeOfDay.toLocalTime()
    // Look up to two weeks ahead so we never overshoot even with weekends
    // skipped and the user's timezone offset crossing a date boundary.
    for (i in 0..14) {
        val date = nowLocal.toLocalDate().plusDays(i.toLong())
        if (r.skipWeekends && date.dayOfWeek in WEEKEND) continue
        val candidate = LocalDateTime.of(date, targetTime).atZone(zone).toInstant()
        if (candidate.isAfter(now)) return candidate.toEpochMilli()
    }
    error("Daily reminder with no fire in next 2 weeks: $r")
}

private val WEEKEND: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

private fun nextWeekly(now: Instant, r: Reminder.Weekly, zone: ZoneId): Long {
    val nowLocal = now.atZone(zone)
    val targetTime = r.timeOfDay.toLocalTime()
    // Search up to 7 days ahead; one of them must match.
    for (i in 0..7) {
        val date = nowLocal.toLocalDate().plusDays(i.toLong())
        if (date.dayOfWeek !in r.daysOfWeek) continue
        val candidate = LocalDateTime.of(date, targetTime).atZone(zone).toInstant()
        if (candidate.isAfter(now)) return candidate.toEpochMilli()
    }
    // Defensive — should be unreachable given the loop covers a full week.
    error("Weekly reminder with no fire in next 8 days: $r")
}

private fun nextInterval(now: Instant, r: Reminder.Interval, zone: ZoneId): Long {
    val nowLocal = now.atZone(zone)
    val today = nowLocal.toLocalDate()
    val nowTime = nowLocal.toLocalTime()

    val windowStartToday = LocalDateTime.of(today, r.activeWindow.start.toLocalTime())
        .atZone(zone).toInstant()
    val windowEndToday = LocalDateTime.of(today, r.activeWindow.end.toLocalTime())
        .atZone(zone).toInstant()
    val intervalSec = r.intervalMinutes * 60L

    return when {
        // Before today's window: fire at start of window.
        nowTime < r.activeWindow.start.toLocalTime() ->
            windowStartToday.toEpochMilli()

        // Inside today's window: snap to the interval grid anchored on the
        // window's start. Old logic returned `now + interval`, which slid the
        // target forward with the clock and made "in X min" labels never tick
        // down. The grid approach gives a *stable* next-fire (e.g. 9:00, 10:00,
        // 11:00 for a 60-min interval starting at 8:00), so the banner can
        // count down predictably and the scheduler's alarm time matches what
        // the UI says.
        nowTime <= r.activeWindow.end.toLocalTime() -> {
            val elapsedSec = (now.epochSecond - windowStartToday.epochSecond)
                .coerceAtLeast(0)
            val slotsPassed = elapsedSec / intervalSec
            val nextSlot = windowStartToday.plusSeconds((slotsPassed + 1) * intervalSec)
            if (!nextSlot.isAfter(windowEndToday)) {
                nextSlot.toEpochMilli()
            } else {
                // Next grid slot would overshoot today's window: defer to tomorrow.
                tomorrowsWindowStart(today, r.activeWindow, zone).toEpochMilli()
            }
        }

        // Past today's window: tomorrow's start.
        else -> tomorrowsWindowStart(today, r.activeWindow, zone).toEpochMilli()
    }
}

private fun tomorrowsWindowStart(today: LocalDate, window: TimeWindow, zone: ZoneId): Instant =
    LocalDateTime.of(today.plusDays(1), window.start.toLocalTime())
        .atZone(zone)
        .toInstant()

// --- bridge helpers -------------------------------------------------------

private fun TimeOfDay.toLocalTime(): LocalTime = LocalTime.of(hour, minute)
