package `in`.deepak.remindme.notification

import android.app.NotificationManager

/**
 * All notification channels the app uses.
 *
 * Each channel is a value here so:
 *  - the receiver, scheduler, and presenter all reference the same id (no stringly-typed bugs)
 *  - the user gets per-category controls in system Settings
 *  - adding a new channel = adding a new entry to [ALL] (one-file change)
 *
 * Note on channel IDs: Android *freezes* sound, vibration, and importance at
 * channel-creation time — updating those fields after the fact is a no-op.
 * If a channel's defaults change in a way users must see, bump the id (e.g.
 * `channel_alarms` → `channel_alarms_v2`) and the new one will be re-created
 * with the new defaults; the old one becomes unused.
 */
sealed class NotificationChannelDefinition(
    val id: String,
    val displayName: String,
    val description: String,
    val importance: Int,
    /** When true, the channel is configured with the system alarm sound and vibration. */
    val alarmStyle: Boolean,
) {

    /**
     * Reminders that fire as full-screen alarms.
     *
     * IMPORTANCE_HIGH is the minimum the OS will honour for `setFullScreenIntent`,
     * and [alarmStyle] tells the presenter to wire the system alarm sound and a
     * vibration pattern at creation time.
     */
    data object Alarms : NotificationChannelDefinition(
        id = "channel_alarms",
        displayName = "Alarms",
        description = "Reminders that fire as a full-screen alarm",
        importance = NotificationManager.IMPORTANCE_HIGH,
        alarmStyle = true,
    )

    /**
     * Low-importance channel for the placeholder notification a short-lived
     * foreground service must post. IMPORTANCE_LOW so it doesn't make sound
     * or vibrate — only the [Alarms] channel does that.
     */
    data object Background : NotificationChannelDefinition(
        id = "channel_background",
        displayName = "Background tasks",
        description = "Silent placeholder shown briefly while an alarm is starting",
        importance = NotificationManager.IMPORTANCE_LOW,
        alarmStyle = false,
    )

    companion object {
        val ALL: List<NotificationChannelDefinition> = listOf(Alarms, Background)

        /**
         * Vibration cadence used by the alarm channel. Short pulses keep it
         * recognisable as a reminder rather than a phone call. The Activity
         * runs its own repeating pattern while visible; this one-shot is just
         * the first heads-up nudge.
         */
        val ALARM_VIBRATION_PATTERN: LongArray = longArrayOf(0, 400, 200, 400, 200, 400)
    }
}
