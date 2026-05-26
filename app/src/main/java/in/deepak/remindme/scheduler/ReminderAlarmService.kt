package `in`.deepak.remindme.scheduler

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import `in`.deepak.remindme.R
import `in`.deepak.remindme.notification.NotificationChannelDefinition
import `in`.deepak.remindme.ui.screens.alert.ReminderAlertActivity

/**
 * Short-lived foreground service whose only job is to launch
 * [ReminderAlertActivity] when a reminder fires.
 *
 * Why this exists:
 *  On Android 12+, calling `context.startActivity(...)` from a
 *  [android.content.BroadcastReceiver] is blocked by the background-activity-launch
 *  (BAL) restrictions unless the device is locked or the user is in another
 *  full-screen experience. That's why the previous "post a notification with
 *  setFullScreenIntent + manual startActivity" approach only worked on the
 *  lock screen — on an unlocked device, the OS suppresses the takeover and
 *  only shows the heads-up.
 *
 *  Foreground services *do* have a reliable BAL exemption (the app has a
 *  visible window — the FGS notification — for the duration of the service).
 *  So we delegate the activity launch to this service. The service exists for
 *  a few milliseconds: `startForeground` to enter the foreground state →
 *  `startActivity` → `stopSelf`.
 *
 * Why `shortService` type: we genuinely only need the service for a few
 * milliseconds; `shortService` doesn't require any runtime permissions or
 * Play-Store category approval. The 3-minute limit is irrelevant.
 *
 * The placeholder notification posted by `startForeground` is on the
 * [NotificationChannelDefinition.Background] channel (IMPORTANCE_LOW, no
 * sound, no vibration) so it doesn't double-fire the alarm sound that the
 * real alarm notification posts on the Alarms channel.
 */
class ReminderAlarmService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reminderId = intent?.getLongExtra(EXTRA_REMINDER_ID, INVALID_ID) ?: INVALID_ID
        if (reminderId == INVALID_ID) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // FGS contract: must call startForeground within 5 seconds of being
        // started, or the system kills the process. We do it synchronously
        // so there's no chance of missing the window.
        val placeholder = NotificationCompat.Builder(
            this,
            NotificationChannelDefinition.Background.id,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Starting reminder…")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        startForeground(
            FOREGROUND_PLACEHOLDER_ID,
            placeholder,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
        )

        // Now in foreground state → BAL exemption is active → activity launch
        // succeeds even when the device is unlocked and the user is mid-task.
        try {
            startActivity(ReminderAlertActivity.intent(this, reminderId))
        } catch (_: SecurityException) {
            // Defensive — should not happen with FGS BAL exemption. The alarm
            // notification posted by the presenter is still the fallback.
        }

        // STOP_FOREGROUND_REMOVE removes the placeholder; the real alarm
        // notification on the Alarms channel stays visible because it was
        // posted with a different notification id by the presenter.
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
        return START_NOT_STICKY
    }

    companion object {
        const val EXTRA_REMINDER_ID =
            "in.deepak.remindme.extra.ALARM_SERVICE_REMINDER_ID"
        private const val INVALID_ID = -1L

        // A fixed id is fine — only one of these placeholders is ever in
        // flight, and it gets removed before the service exits.
        private const val FOREGROUND_PLACEHOLDER_ID = 9_999

        fun start(context: Context, reminderId: Long) {
            val intent = Intent(context, ReminderAlarmService::class.java)
                .putExtra(EXTRA_REMINDER_ID, reminderId)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
