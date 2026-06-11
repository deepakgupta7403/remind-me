package `in`.deepak.remindme.ui.screens.settings

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import `in`.deepak.remindme.RemindMeApp
import `in`.deepak.remindme.data.preferences.ThemeMode
import `in`.deepak.remindme.ui.navigation.Destination
import `in`.deepak.remindme.ui.screens.common.AppBottomNavigation
import `in`.deepak.remindme.ui.theme.BrandColors
import `in`.deepak.remindme.ui.theme.ThemeController
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Settings tab.
 *
 * Some rows are still a static surface — the backing features (accent picker,
 * backup) don't exist yet, so those taps show a "Coming soon" snackbar. The
 * Notifications and Quiet Hours rows are live: sound, vibration, snooze
 * duration, and Do Not Disturb all persist via [UserPreferences], which the
 * full-screen alarm reads at fire time. UI shipping ahead of the domain is a
 * deliberate choice — the
 * design comp drives layout, and the placeholders make it obvious which
 * features still need to be built.
 *
 * Sections: Notifications, Quiet Hours, Appearance, Data. To add a new row,
 * append to the relevant [SettingsSection] block — each row is one
 * [SettingsRow] call.
 */
@Composable
fun SettingsScreen(
    onTabSelected: (Destination) -> Unit,
    onOpenProfile: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun comingSoon(label: String) {
        scope.launch { snackbarHostState.showSnackbar("$label is coming soon.") }
    }

    val context = LocalContext.current
    val userPrefs = remember(context) {
        (context.applicationContext as RemindMeApp).container.userPreferences
    }

    // Persisted so the alarm honours it; seed from prefs, write back on toggle.
    var vibrationOn by rememberSaveable { mutableStateOf(userPrefs.vibrationEnabled) }

    // Do Not Disturb (quiet hours). Toggle silences alarms during the window;
    // tapping the row opens the start/end time editor. All three persist.
    var dndOn       by rememberSaveable { mutableStateOf(userPrefs.dndEnabled) }
    var dndStart    by rememberSaveable { mutableStateOf(userPrefs.dndStartMinute) }
    var dndEnd      by rememberSaveable { mutableStateOf(userPrefs.dndEndMinute) }
    var showDndDialog by rememberSaveable { mutableStateOf(false) }

    // Theme: the live value is ThemeController.mode (a reactive process-wide
    // state), so the row label updates the instant the app re-themes.
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }

    // Snooze duration: seed from prefs, persist on pick. A small dialog of fixed
    // options keeps it to a single tap and avoids free-form minute entry.
    var snoozeMinutes  by rememberSaveable { mutableStateOf(userPrefs.snoozeMinutes) }
    var showSnoozeDialog by rememberSaveable { mutableStateOf(false) }

    // Default-sound picker. We persist the choice immediately and surface its
    // human label as the row value; the alarm reads the URI back at fire time.
    var soundLabel by remember { mutableStateOf(userPrefs.alarmSoundLabel) }
    val soundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val picked: Uri? = result.data?.let {
                IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            // A null picked URI means the user chose "Silent"; store "" for it.
            userPrefs.alarmSoundUri = picked?.toString() ?: ""
            val label = when (picked) {
                null -> "Silent"
                else -> RingtoneManager.getRingtone(context, picked)?.getTitle(context) ?: "Custom sound"
            }
            userPrefs.alarmSoundLabel = label
            soundLabel = label
        }
    }
    fun openSoundPicker() {
        val existing = userPrefs.alarmSoundUri
            ?.takeIf { it.isNotEmpty() }
            ?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Default sound")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
        }
        soundPicker.launch(intent)
    }
    fun setVibration(enabled: Boolean) {
        vibrationOn = enabled
        userPrefs.vibrationEnabled = enabled
    }
    fun setDnd(enabled: Boolean) {
        dndOn = enabled
        userPrefs.dndEnabled = enabled
    }
    // Opens the platform time picker seeded with [initialMinute] (minutes from
    // midnight) and honours the device's 12/24-hour setting.
    fun pickTime(initialMinute: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            initialMinute / 60,
            initialMinute % 60,
            DateFormat.is24HourFormat(context),
        ).show()
    }

    Scaffold(
        containerColor = BrandColors.PageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AppBottomNavigation(
                currentRoute = Destination.Settings.route,
                onSelect = onTabSelected,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandColors.PageBackground)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BrandColors.TextHeading,
            )
            Spacer(Modifier.height(20.dp))

            SettingsSection(label = "ACCOUNT") {
                SettingsRow(
                    icon = Icons.Filled.Person,
                    title = "Profile",
                    subtitle = "Your name, greeting & avatar",
                    trailing = { TrailingValue(value = null, chevron = true) },
                    onClick = onOpenProfile,
                )
            }

            Spacer(Modifier.height(20.dp))

            SettingsSection(label = "NOTIFICATIONS") {
                SettingsRow(
                    icon = Icons.Filled.VolumeUp,
                    title = "Default sound",
                    trailing = { TrailingValue(value = soundLabel, chevron = true) },
                    onClick = { openSoundPicker() },
                )
                Divider()
                SettingsRow(
                    icon = Icons.Filled.GraphicEq,
                    title = "Vibration",
                    trailing = {
                        Switch(
                            checked = vibrationOn,
                            onCheckedChange = { setVibration(it) },
                            colors = primarySwitchColors(),
                        )
                    },
                    onClick = { setVibration(!vibrationOn) },
                )
                Divider()
                SettingsRow(
                    icon = Icons.Filled.AccessTime,
                    title = "Snooze duration",
                    trailing = { TrailingValue(value = snoozeLabel(snoozeMinutes), chevron = true) },
                    onClick = { showSnoozeDialog = true },
                )
            }

            Spacer(Modifier.height(20.dp))
            SettingsSection(label = "QUIET HOURS") {
                SettingsRow(
                    icon = Icons.Filled.Bedtime,
                    title = "Do not disturb",
                    subtitle = "${clockLabel(dndStart)} – ${clockLabel(dndEnd)}",
                    trailing = {
                        Switch(
                            checked = dndOn,
                            onCheckedChange = { setDnd(it) },
                            colors = primarySwitchColors(),
                        )
                    },
                    onClick = { showDndDialog = true },
                )
            }

            Spacer(Modifier.height(20.dp))
            SettingsSection(label = "APPEARANCE") {
                SettingsRow(
                    icon = Icons.Filled.DarkMode,
                    title = "Theme",
                    trailing = { TrailingValue(value = ThemeController.mode.label, chevron = true) },
                    onClick = { showThemeDialog = true },
                )
                Divider()
                SettingsRow(
                    icon = Icons.Filled.ColorLens,
                    title = "Accent color",
                    trailing = { AccentDots() },
                    onClick = { comingSoon("Accent color picker") },
                )
            }

            Spacer(Modifier.height(20.dp))
            SettingsSection(label = "DATA") {
                SettingsRow(
                    icon = Icons.Filled.Backup,
                    title = "Backup & sync",
                    trailing = { TrailingValue(value = null, chevron = true) },
                    onClick = { comingSoon("Backup & sync") },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSnoozeDialog) {
        SnoozeDurationDialog(
            current = snoozeMinutes,
            onSelect = { minutes ->
                snoozeMinutes = minutes
                userPrefs.snoozeMinutes = minutes
                showSnoozeDialog = false
            },
            onDismiss = { showSnoozeDialog = false },
        )
    }

    if (showDndDialog) {
        QuietHoursDialog(
            startMinute = dndStart,
            endMinute = dndEnd,
            onPickStart = { pickTime(dndStart) { dndStart = it; userPrefs.dndStartMinute = it } },
            onPickEnd = { pickTime(dndEnd) { dndEnd = it; userPrefs.dndEndMinute = it } },
            onDismiss = { showDndDialog = false },
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            current = ThemeController.mode,
            onSelect = { mode ->
                userPrefs.themeMode = mode
                ThemeController.mode = mode   // re-themes the whole app instantly
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }
}

/** The snooze durations offered in the picker, in minutes. */
private val SNOOZE_OPTIONS = listOf(5, 10, 15, 30, 60)

/** Renders a minute count as the compact row value, e.g. "10 min" or "1 hr". */
private fun snoozeLabel(minutes: Int): String = when {
    minutes % 60 == 0 -> "${minutes / 60} hr"
    else -> "$minutes min"
}

@Composable
private fun SnoozeDurationDialog(
    current: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Snooze duration") },
        text = {
            Column {
                SNOOZE_OPTIONS.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(minutes) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = minutes == current,
                            onClick = { onSelect(minutes) },
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = snoozeLabel(minutes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = BrandColors.TextHeading,
                        )
                    }
                }
            }
        },
    )
}

/** Formats minutes-from-midnight as a 12-hour clock label, e.g. "10:30 pm". */
private val CLOCK_FORMAT = DateTimeFormatter.ofPattern("h:mm a")
private fun clockLabel(minuteOfDay: Int): String =
    LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(CLOCK_FORMAT).lowercase()

@Composable
private fun ThemePickerDialog(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Theme") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = mode == current,
                            onClick = { onSelect(mode) },
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = BrandColors.TextHeading,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun QuietHoursDialog(
    startMinute: Int,
    endMinute: Int,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        title = { Text("Quiet hours") },
        text = {
            Column {
                Text(
                    text = "Reminders still appear during these hours — they just won't make a sound or vibrate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.TextBody,
                )
                Spacer(Modifier.height(8.dp))
                QuietHoursRow(label = "Start", value = clockLabel(startMinute), onClick = onPickStart)
                QuietHoursRow(label = "End", value = clockLabel(endMinute), onClick = onPickEnd)
            }
        },
    )
}

@Composable
private fun QuietHoursRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = BrandColors.TextHeading,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = BrandColors.Primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// --- Section / Row primitives ---------------------------------------------

@Composable
private fun SettingsSection(label: String, content: @Composable () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = BrandColors.TextBody,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceCard),
    ) { Column { content() } }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandColors.Primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = BrandColors.TextHeading,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandColors.TextBody,
                )
            }
        }
        trailing()
    }
}

@Composable
private fun TrailingValue(value: String?, chevron: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = BrandColors.TextBody,
            )
        }
        if (chevron) {
            Spacer(Modifier.size(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = BrandColors.TextBody,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AccentDots() {
    val accents = listOf(
        BrandColors.Primary,
        BrandColors.Tile.WeeklyFg,
        BrandColors.Tile.DailyFg,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        accents.forEach { c ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(c),
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp)
            .height(1.dp)
            .background(BrandColors.BorderSubtle),
    )
}

@Composable
private fun primarySwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = BrandColors.Primary,
    uncheckedThumbColor = BrandColors.SurfaceCard,
    uncheckedTrackColor = BrandColors.SurfaceDim,
    uncheckedBorderColor = BrandColors.BorderSubtle,
)
