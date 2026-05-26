package `in`.deepak.remindme.scheduler

import `in`.deepak.remindme.domain.model.Reminder

/**
 * Abstracts the OS alarm subsystem.
 *
 * Callers (ViewModels, BootReceiver, ReminderFireHandler) ask for "schedule
 * this reminder" — they don't compute trigger times or touch AlarmManager.
 * This makes:
 *  - the trigger-time math testable as pure functions ([nextFireTimeMillis])
 *  - the OS plumbing swappable (e.g., WorkManager-backed) without churning callers
 */
interface AlarmScheduler {
    /**
     * Schedule (or re-schedule) [reminder]. If it's recurring, only the next
     * fire is registered with the OS — the receiver re-schedules itself after
     * each fire. If [Reminder.enabled] is false or there is no future fire,
     * any existing alarm is cancelled.
     */
    fun schedule(reminder: Reminder)

    /** Cancel any pending alarm for this id. Safe to call when none exists. */
    fun cancel(reminderId: Long)

    /**
     * Re-arm this reminder to fire once after [delayMillis], replacing any
     * pending alarm. Used by the alert screen's "Snooze" action. After the
     * snooze fires, the normal post-fire path takes over and re-schedules the
     * next regular occurrence (for recurring types).
     */
    fun snooze(reminderId: Long, delayMillis: Long)

    /**
     * Re-arm every enabled reminder. Called from [BootReceiver] and after the
     * user grants exact-alarm permission. Suspending so the impl can read the
     * repository without blocking the caller's thread.
     */
    suspend fun rescheduleAll()
}
