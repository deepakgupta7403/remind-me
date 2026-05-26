package `in`.deepak.remindme.scheduler

import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.repository.ReminderRepository
import `in`.deepak.remindme.notification.NotificationChannelDefinition
import `in`.deepak.remindme.notification.NotificationPresenter
import `in`.deepak.remindme.notification.NotificationRequest

/**
 * Orchestrates the "an alarm just fired" path.
 *
 * Pulled out of [ReminderReceiver] so the receiver itself stays a thin Android
 * shell (easy to test the handler in isolation; receiver only needs Robolectric
 * if we ever want to test it end-to-end).
 *
 * Decision matrix per reminder type after firing:
 *   Interval / Daily / Weekly → reschedule next occurrence
 *   OneTime                   → mark disabled so it doesn't re-fire on reboot
 *
 * Adding a new reminder type forces a compile error in [postFireAction] —
 * the sealed `when` won't be exhaustive.
 */
class ReminderFireHandler(
    private val repository: ReminderRepository,
    private val scheduler: AlarmScheduler,
    private val presenter: NotificationPresenter,
) {

    suspend fun onFired(reminderId: Long) {
        val reminder = repository.get(reminderId) ?: return
        if (!reminder.enabled) return

        presenter.show(
            NotificationRequest(
                notificationId = notificationIdFor(reminder.id),
                channel = NotificationChannelDefinition.Alarms,
                title = reminder.title.ifBlank { "Reminder" },
                body = bodyFor(reminder),
                reminderId = reminder.id,
            )
        )

        when (postFireAction(reminder)) {
            PostFireAction.Reschedule -> scheduler.schedule(reminder)
            PostFireAction.DisableSelf -> {
                repository.setEnabled(reminder.id, enabled = false)
                scheduler.cancel(reminder.id)
            }
        }
    }

    private fun bodyFor(reminder: Reminder): String = when (reminder) {
        is Reminder.Interval -> "Every ${reminder.intervalMinutes} min between " +
            "${reminder.activeWindow.start} and ${reminder.activeWindow.end}"
        is Reminder.OneTime  -> "One-time reminder"
        is Reminder.Daily    -> "Daily at ${reminder.timeOfDay}"
        is Reminder.Weekly   -> "Weekly · ${reminder.daysOfWeek.joinToString { it.name.take(3) }}"
    }

    private fun postFireAction(reminder: Reminder): PostFireAction = when (reminder) {
        is Reminder.Interval -> PostFireAction.Reschedule
        is Reminder.Daily    -> PostFireAction.Reschedule
        is Reminder.Weekly   -> PostFireAction.Reschedule
        is Reminder.OneTime  -> PostFireAction.DisableSelf
    }

    private enum class PostFireAction { Reschedule, DisableSelf }

    companion object {
        /**
         * Map a long reminder id to the int [NotificationManager] wants.
         * Truncates the high 32 bits — fine for any plausible reminder count.
         */
        fun notificationIdFor(reminderId: Long): Int = reminderId.toInt()
    }
}
