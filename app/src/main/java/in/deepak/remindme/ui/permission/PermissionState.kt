package `in`.deepak.remindme.ui.permission

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/**
 * Snapshot of the two permissions reminders depend on.
 *
 * Kept as a plain value type so the UI can re-read it on every resume without
 * subscribing to a lifecycle-aware Flow — permissions can change while the
 * user is in system Settings, and Android doesn't give us a callback.
 */
data class PermissionState(
    val notificationsGranted: Boolean,
    val exactAlarmsAllowed: Boolean,
) {
    val allGranted: Boolean get() = notificationsGranted && exactAlarmsAllowed

    companion object {
        fun read(context: Context): PermissionState {
            val notifGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

            val alarmManager: AlarmManager? = context.getSystemService()
            val exactAllowed = alarmManager?.canScheduleExactAlarms() ?: false

            return PermissionState(
                notificationsGranted = notifGranted,
                exactAlarmsAllowed = exactAllowed,
            )
        }
    }
}
