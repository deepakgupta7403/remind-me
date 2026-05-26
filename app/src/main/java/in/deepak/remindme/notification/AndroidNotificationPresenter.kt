package `in`.deepak.remindme.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import `in`.deepak.remindme.R
import `in`.deepak.remindme.scheduler.ReminderAlarmService
import `in`.deepak.remindme.ui.screens.alert.ReminderAlertActivity

/**
 * Real Android implementation of [NotificationPresenter].
 *
 * Only this class touches NotificationManager / NotificationCompat. Everything
 * upstream stays on the abstract [NotificationRequest] API.
 */
class AndroidNotificationPresenter(
    private val context: Context,
) : NotificationPresenter {

    private val systemManager: NotificationManager =
        requireNotNull(context.getSystemService()) { "NotificationManager unavailable" }

    private val compatManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    override fun ensureChannelsExist() {
        // minSdk = 33, so NotificationChannel is always available — no SDK check.
        // Sound/vibration are honoured by the system ONLY at first creation;
        // see the channel-id-bumping note in NotificationChannelDefinition.
        NotificationChannelDefinition.ALL.forEach { def ->
            val channel = NotificationChannel(def.id, def.displayName, def.importance).apply {
                description = def.description
                if (def.alarmStyle) {
                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val audioAttrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setSound(alarmUri, audioAttrs)
                    enableVibration(true)
                    vibrationPattern = NotificationChannelDefinition.ALARM_VIBRATION_PATTERN
                }
            }
            systemManager.createNotificationChannel(channel)
        }
    }

    override fun show(request: NotificationRequest) {
        // POST_NOTIFICATIONS is a runtime permission on A13+. If denied,
        // notify() is a silent no-op; we short-circuit here for clarity and
        // to avoid building a notification we can't post.
        if (!compatManager.areNotificationsEnabled()) return

        val alertIntent = ReminderAlertActivity.intent(context, request.reminderId)

        // Both the tap target and the full-screen intent open the alarm-style
        // alert activity. Using the same PendingIntent for both means the
        // user always lands on the same screen regardless of how the
        // notification was dismissed/expanded.
        val alertPending = PendingIntent.getActivity(
            context,
            request.notificationId,
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, request.channel.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(request.title)
            .setContentText(request.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(request.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // CATEGORY_ALARM (vs _REMINDER) tells the OS this should bypass
            // Do-Not-Disturb the same way a clock alarm does, and it's the
            // category USE_FULL_SCREEN_INTENT is gated against on A14+.
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(alertPending)
            // The second arg `highPriority=true` is what tells the system
            // "actually take over the screen", not just file a notification.
            .setFullScreenIntent(alertPending, true)
            .setAutoCancel(true)
            .build()

        try {
            compatManager.notify(request.notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check above and the notify call.
            // Silently drop — the user will see the request flow on next launch.
            return
        }

        // Force the alarm screen to the foreground even when the device is
        // unlocked. setFullScreenIntent alone only triggers a screen takeover
        // when the device is locked or the user is in another full-screen
        // experience — Android intentionally suppresses it otherwise. And
        // calling startActivity directly from a BroadcastReceiver is blocked
        // by Android 12+ background-activity-launch rules. The reliable
        // workaround is a short-lived foreground service, which has a
        // guaranteed BAL exemption and can call startActivity itself.
        ReminderAlarmService.start(context, request.reminderId)
    }

    override fun cancel(notificationId: Int) {
        compatManager.cancel(notificationId)
    }
}
