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
 * Re-arms all enabled reminders after device boot or app update.
 *
 * AlarmManager forgets every alarm across reboots, and the system also clears
 * them when the app is updated. Both events deliver here; we re-read the repo
 * and re-schedule via [AlarmScheduler.rescheduleAll].
 *
 * Listed actions:
 *   ACTION_BOOT_COMPLETED     — phone restarted
 *   ACTION_LOCKED_BOOT_COMPLETED — direct-boot aware fallback (we don't store
 *                                  in direct-boot storage so this is best-effort)
 *   ACTION_MY_PACKAGE_REPLACED   — our APK was updated
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return

        val container = (context.applicationContext as RemindMeApp).container
        val scheduler = container.alarmScheduler

        val pendingResult = goAsync()
        bootScope.launch {
            try {
                scheduler.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )

        private val bootScope: CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
