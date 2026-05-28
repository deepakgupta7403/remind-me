package `in`.deepak.remindme.ui.screens.alert

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.deepak.remindme.RemindMeApp
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.scheduler.ReminderFireHandler
import `in`.deepak.remindme.ui.theme.BrandColors
import `in`.deepak.remindme.ui.theme.RemindMeTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Full-screen alarm UI shown when a reminder fires.
 *
 * Launched in two ways:
 *  - Tap on the heads-up notification (regular launch)
 *  - Android auto-launches it via setFullScreenIntent when the device is
 *    locked or the user is in another full-screen app
 *
 * Mirrors `app/sampledata/nextstep/reminder_me.png` (Screen 4): big icon,
 * title, three actions (Mark as done / Snooze 10m / Skip).
 *
 * Window flags (showWhenLocked / turnScreenOn) are declared in the manifest so
 * the activity also works when created cold by the full-screen intent. The
 * programmatic calls below are belt-and-braces for older OEM behaviour.
 */
class ReminderAlertActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureAlarmWindow()

        val reminderId = intent?.getLongExtra(EXTRA_REMINDER_ID, INVALID_ID) ?: INVALID_ID
        if (reminderId == INVALID_ID) {
            finish()
            return
        }

        val container = (application as RemindMeApp).container
        val notificationId = ReminderFireHandler.notificationIdFor(reminderId)
        val reminderFlow = MutableStateFlow<Reminder?>(null)

        MainScope().launch {
            reminderFlow.value = container.reminderRepository.get(reminderId)
        }

        setContent {
            RemindMeTheme {
                val reminder by reminderFlow.collectAsState()
                ReminderAlertScreen(
                    reminder = reminder,
                    onDone = {
                        container.reminderActivityLog.recordDone(reminderId)
                        container.notificationPresenter.cancel(notificationId)
                        finishAndRemoveTask()
                    },
                    onSnooze = {
                        container.alarmScheduler.snooze(reminderId, SNOOZE_DELAY_MILLIS)
                        container.notificationPresenter.cancel(notificationId)
                        finishAndRemoveTask()
                    },
                    onSkip = {
                        container.notificationPresenter.cancel(notificationId)
                        finishAndRemoveTask()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleInstance: if a second reminder fires while this one is up,
        // recreate against the newer id rather than keeping the old one.
        setIntent(intent)
        recreate()
    }

    override fun onStart() {
        super.onStart()
        startAlarmEffects()
    }

    override fun onStop() {
        super.onStop()
        stopAlarmEffects()
    }

    override fun onDestroy() {
        // Belt-and-braces: onStop should have already stopped these, but if
        // the activity is killed without onStop (rare process death paths),
        // make sure we don't leak a looping ringtone or vibrator.
        stopAlarmEffects()
        super.onDestroy()
    }

    private fun startAlarmEffects() {
        // Sound. We loop the system alarm tone using USAGE_ALARM so it plays
        // on the alarm volume channel and bypasses ringer-silent / DND.
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (alarmUri != null && ringtone == null) {
            ringtone = RingtoneManager.getRingtone(this, alarmUri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                isLooping = true
                play()
            }
        }

        // Vibration. Repeating waveform: wait 0ms, pulse, gap, pulse, … from
        // index 0 of the pattern. AMPLITUDE_DEFAULT lets the OEM pick a
        // sensible intensity for each pulse.
        if (vibrator == null) {
            vibrator = resolveVibrator()?.also { v ->
                val pattern = ALARM_LOOP_VIBRATION_PATTERN
                val amplitudes = IntArray(pattern.size) { i ->
                    if (i % 2 == 0) 0 else VibrationEffect.DEFAULT_AMPLITUDE
                }
                v.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
            }
        }
    }

    private fun stopAlarmEffects() {
        ringtone?.runCatching { stop() }
        ringtone = null
        vibrator?.runCatching { cancel() }
        vibrator = null
    }

    private fun resolveVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }?.takeIf { it.hasVibrator() }
    }

    private fun configureAlarmWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "in.deepak.remindme.extra.ALERT_REMINDER_ID"
        private const val INVALID_ID = -1L
        private const val SNOOZE_DELAY_MILLIS = 10L * 60L * 1000L

        // Wait 0ms, vibrate 600ms, pause 600ms — repeats from index 0 while
        // the alert screen is visible. Tuned to be insistent but not as
        // aggressive as a phone-call pattern.
        private val ALARM_LOOP_VIBRATION_PATTERN: LongArray =
            longArrayOf(0, 600, 600)

        fun intent(context: Context, reminderId: Long): Intent =
            Intent(context, ReminderAlertActivity::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminderId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY
            }
    }
}

@Composable
private fun ReminderAlertScreen(
    reminder: Reminder?,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BrandColors.Primary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Text(
                text = "REMIND ME · NOW",
                color = BrandColors.OnPrimary.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(BrandColors.OnPrimary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = BrandColors.OnPrimary,
                        modifier = Modifier.size(72.dp),
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = reminder?.title?.takeIf { it.isNotBlank() } ?: "Reminder",
                    color = BrandColors.OnPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                val subtitle = reminder?.let(::subtitleFor) ?: "Time for your reminder"
                Text(
                    text = subtitle,
                    color = BrandColors.OnPrimary.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandColors.OnPrimary,
                        contentColor = BrandColors.Primary,
                    ),
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Mark as done", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryAlertButton(
                        text = "Snooze 10m",
                        icon = Icons.Outlined.Snooze,
                        onClick = onSnooze,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryAlertButton(
                        text = "Skip",
                        icon = Icons.Outlined.SkipNext,
                        onClick = onSkip,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondaryAlertButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.18f),
            contentColor = BrandColors.OnPrimary,
        ),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

private fun subtitleFor(reminder: Reminder): String = when (reminder) {
    is Reminder.Interval -> "Every ${reminder.intervalMinutes} min · " +
        "${reminder.activeWindow.start}–${reminder.activeWindow.end}"
    is Reminder.OneTime  -> "One-time reminder"
    is Reminder.Daily    -> "Daily at ${reminder.timeOfDay}"
    is Reminder.Weekly   -> "Weekly · ${reminder.daysOfWeek.joinToString { it.name.take(3) }}"
}
