package `in`.deepak.remindme.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.deepak.remindme.RemindMeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives alarm intents from [AndroidAlarmScheduler] and delegates to
 * [ReminderFireHandler].
 *
 * Thin on purpose: the only Android-specific concerns here are
 *  - extracting the reminder id from the Intent
 *  - keeping the receiver alive across coroutine work via [goAsync]
 * Everything else is testable plain Kotlin.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, INVALID_ID)
        if (reminderId == INVALID_ID) return

        val container = (context.applicationContext as RemindMeApp).container
        val handler = container.reminderFireHandler

        // goAsync keeps the receiver alive while we do suspend work. We get up
        // to ~10s before the system kills us — plenty for one notification + reschedule.
        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                handler.onFired(reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "in.deepak.remindme.action.FIRE_REMINDER"
        const val EXTRA_REMINDER_ID = "in.deepak.remindme.extra.REMINDER_ID"
        private const val INVALID_ID = -1L

        /**
         * Process-wide scope so coroutines launched by short-lived receivers
         * aren't tied to a CoroutineScope that dies with the receiver instance.
         * SupervisorJob → one failed reminder doesn't tear down the whole scope.
         */
        private val receiverScope: CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
