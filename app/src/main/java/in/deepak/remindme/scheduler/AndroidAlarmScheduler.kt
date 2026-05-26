package `in`.deepak.remindme.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.repository.ReminderRepository
import java.time.Clock

/**
 * Real Android AlarmManager backing for [AlarmScheduler].
 *
 * Uses one-shot exact alarms re-armed by [ReminderReceiver] on each fire — we
 * deliberately don't use [AlarmManager.setRepeating] because it became inexact
 * on API 19+, which defeats the point for a "remind me every 30 min" app.
 *
 * If [AlarmManager.canScheduleExactAlarms] returns false (user revoked
 * permission), we degrade to inexact alarms rather than silently failing.
 * The UI surfaces a warning (added in Step 9).
 */
class AndroidAlarmScheduler(
    private val context: Context,
    private val repository: ReminderRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : AlarmScheduler {

    private val alarmManager: AlarmManager =
        requireNotNull(context.getSystemService()) { "AlarmManager unavailable" }

    override fun schedule(reminder: Reminder) {
        // schedule() is idempotent — always cancel any previous alarm first.
        cancelInternal(reminder.id)

        if (!reminder.enabled) return
        val triggerAt = nextFireTimeMillis(clock.instant(), reminder) ?: return
        val pi = pendingIntentFor(reminder.id, create = true) ?: return

        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            // Inexact fallback. ~9 min batching under Doze; better than nothing.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    override fun cancel(reminderId: Long) {
        cancelInternal(reminderId)
    }

    override fun snooze(reminderId: Long, delayMillis: Long) {
        cancelInternal(reminderId)
        val triggerAt = clock.millis() + delayMillis
        val pi = pendingIntentFor(reminderId, create = true) ?: return
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /**
     * Re-arm every enabled reminder. Used by [BootReceiver] and after the
     * exact-alarm permission is granted. Suspend so the caller controls the
     * coroutine scope (BroadcastReceiver uses [BroadcastReceiver.goAsync]).
     */
    override suspend fun rescheduleAll() {
        repository.getAll().forEach { schedule(it) }
    }

    private fun cancelInternal(reminderId: Long) {
        pendingIntentFor(reminderId, create = false)?.let { pi ->
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun pendingIntentFor(reminderId: Long, create: Boolean): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val flags = if (create) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, reminderId.toInt(), intent, flags)
    }
}
