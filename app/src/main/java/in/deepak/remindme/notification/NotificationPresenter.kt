package `in`.deepak.remindme.notification

/**
 * Abstraction over showing notifications.
 *
 * Callers (receivers, services) deal only in [NotificationRequest] — they never
 * touch [android.app.NotificationManager] or [androidx.core.app.NotificationCompat]
 * directly. This means:
 *  - unit tests can fake notifications without Robolectric
 *  - swapping to a different notification system (e.g., overlay, push) is one impl change
 */
interface NotificationPresenter {
    /** Idempotent: safe to call on every app start. */
    fun ensureChannelsExist()

    fun show(request: NotificationRequest)

    fun cancel(notificationId: Int)
}

/**
 * Plain-data description of a single notification to post.
 *
 * Deliberately framework-agnostic — no PendingIntent here. The presenter
 * decides how taps are routed (open the full-screen alert for the given
 * [reminderId]). If we later need a notification that isn't tied to a
 * reminder, this becomes a sealed `TapAction` field rather than leaking
 * [android.app.PendingIntent] into call sites.
 */
data class NotificationRequest(
    val notificationId: Int,
    val channel: NotificationChannelDefinition,
    val title: String,
    val body: String,
    val reminderId: Long,
)
