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
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.Checkbox
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
import `in`.deepak.remindme.data.backup.BackupComponent
import `in`.deepak.remindme.data.backup.BackupSummary
import `in`.deepak.remindme.data.backup.ImportResult
import `in`.deepak.remindme.data.preferences.ThemeMode
import `in`.deepak.remindme.ui.navigation.Destination
import `in`.deepak.remindme.ui.screens.common.AppBottomNavigation
import `in`.deepak.remindme.ui.theme.AccentColor
import `in`.deepak.remindme.ui.theme.BrandColors
import `in`.deepak.remindme.ui.theme.ThemeController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Settings tab.
 *
 * Every row is now live. Notifications (sound, vibration, snooze) and Quiet
 * Hours persist via [UserPreferences] and are read by the full-screen alarm at
 * fire time; Appearance (theme, accent) drives the app-wide palette through
 * `ThemeController`; Data offers local Backup & restore via [backupManager].
 *
 * Sections: Account, Notifications, Quiet Hours, Appearance, Data. To add a new
 * row, append to the relevant [SettingsSection] block — each row is one
 * [SettingsRow] call.
 */
@Composable
fun SettingsScreen(
    onTabSelected: (Destination) -> Unit,
    onOpenProfile: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val container = remember(context) { (context.applicationContext as RemindMeApp).container }
    val userPrefs = container.userPreferences
    val backupManager = container.backupManager

    // Persisted so the alarm honours it; seed from prefs, write back on toggle.
    var vibrationOn by rememberSaveable { mutableStateOf(userPrefs.vibrationEnabled) }

    // Do Not Disturb (quiet hours). Toggle silences alarms during the window;
    // tapping the row opens the start/end time editor. All three persist.
    var dndOn       by rememberSaveable { mutableStateOf(userPrefs.dndEnabled) }
    var dndStart    by rememberSaveable { mutableStateOf(userPrefs.dndStartMinute) }
    var dndEnd      by rememberSaveable { mutableStateOf(userPrefs.dndEndMinute) }
    var showDndDialog by rememberSaveable { mutableStateOf(false) }

    // Theme + accent: the live values are ThemeController.mode/.accent (reactive
    // process-wide state), so the rows update the instant the app re-themes.
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showAccentDialog by rememberSaveable { mutableStateOf(false) }

    // Backup & restore. The chooser offers Export / Restore. Export opens a
    // component picker, then the file picker. Restore opens the file picker,
    // peeks the file's contents, then a component picker that doubles as the
    // (destructive) confirm step. The launchers open the system file picker.
    var showBackupDialog by rememberSaveable { mutableStateOf(false) }
    var showExportOptions by rememberSaveable { mutableStateOf(false) }
    var exportSelection by remember { mutableStateOf(BackupComponent.entries.toSet()) }
    // Held together: the raw file text plus what peek() found in it.
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var pendingRestoreSummary by remember { mutableStateOf<BackupSummary?>(null) }

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
    suspend fun snackbar(message: String) = snackbarHostState.showSnackbar(message)

    // --- Backup & restore --------------------------------------------------
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val components = exportSelection
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val json = backupManager.exportToJson(components)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                        ?: error("Couldn't open the chosen file")
                }.isSuccess
            }
            snackbar(if (ok) "Backup saved." else "Couldn't save the backup.")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Read + peek so the restore picker can show what's actually in the file.
        scope.launch {
            val json = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            val summary = json?.let { backupManager.peek(it) }
            if (json == null || summary == null) {
                snackbar("This file isn't a readable RemindMe backup.")
            } else {
                pendingRestoreJson = json
                pendingRestoreSummary = summary
            }
        }
    }
    fun runRestore(json: String, components: Set<BackupComponent>) {
        scope.launch {
            when (val result = backupManager.importFromJson(json, components)) {
                is ImportResult.Failure -> snackbar(result.reason)
                is ImportResult.Success -> {
                    // Pull any restored settings back into the live UI + theme
                    // (cheap no-op re-reads if settings weren't part of the restore).
                    vibrationOn = userPrefs.vibrationEnabled
                    dndOn = userPrefs.dndEnabled
                    dndStart = userPrefs.dndStartMinute
                    dndEnd = userPrefs.dndEndMinute
                    snoozeMinutes = userPrefs.snoozeMinutes
                    soundLabel = userPrefs.alarmSoundLabel
                    ThemeController.mode = userPrefs.themeMode
                    ThemeController.accent = AccentColor.fromName(userPrefs.accentColorName)
                    snackbar("Restored ${describeRestored(result.restored)}.")
                }
            }
        }
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
                    trailing = { AccentTrailing(label = ThemeController.accent.label) },
                    onClick = { showAccentDialog = true },
                )
            }

            Spacer(Modifier.height(20.dp))
            SettingsSection(label = "DATA") {
                SettingsRow(
                    icon = Icons.Filled.Backup,
                    title = "Backup & restore",
                    subtitle = "Export or import your data to a file",
                    trailing = { TrailingValue(value = null, chevron = true) },
                    onClick = { showBackupDialog = true },
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

    if (showAccentDialog) {
        AccentPickerDialog(
            current = ThemeController.accent,
            onSelect = { accent ->
                userPrefs.accentColorName = accent.name
                ThemeController.accent = accent   // recolours the whole app instantly
                showAccentDialog = false
            },
            onDismiss = { showAccentDialog = false },
        )
    }

    if (showBackupDialog) {
        BackupDialog(
            onExport = {
                showBackupDialog = false
                showExportOptions = true
            },
            onRestore = {
                showBackupDialog = false
                importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
            },
            onDismiss = { showBackupDialog = false },
        )
    }

    if (showExportOptions) {
        ComponentPickerDialog(
            title = "Export backup",
            description = "Choose what to include in the file.",
            confirmLabel = "Export",
            options = BackupComponent.entries.associateWith { null },
            initialSelection = exportSelection,
            destructive = false,
            onConfirm = { selection ->
                exportSelection = selection
                showExportOptions = false
                exportLauncher.launch("remindme-backup-${LocalDate.now()}.json")
            },
            onDismiss = { showExportOptions = false },
        )
    }

    val restoreSummary = pendingRestoreSummary
    val restoreJson = pendingRestoreJson
    if (restoreSummary != null && restoreJson != null) {
        ComponentPickerDialog(
            title = "Restore from file",
            description = "Selected data will replace what's on this device. This can't be undone.",
            confirmLabel = "Restore",
            options = restoreSummary.counts,
            initialSelection = restoreSummary.present,
            destructive = true,
            onConfirm = { selection ->
                pendingRestoreJson = null
                pendingRestoreSummary = null
                runRestore(restoreJson, selection)
            },
            onDismiss = {
                pendingRestoreJson = null
                pendingRestoreSummary = null
            },
        )
    }
}

/** Joins restored-component counts into a human snackbar fragment. */
private fun describeRestored(restored: Map<BackupComponent, Int>): String =
    restored.entries.joinToString(", ") { (component, count) ->
        if (component == BackupComponent.Settings) "settings"
        else "$count ${component.label.lowercase()}"
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
private fun BackupDialog(
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Backup & restore") },
        text = {
            Column {
                Text(
                    text = "Save all your reminders, templates, stats and settings to a file, or restore from one. Everything stays on your device — there's no cloud account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.TextBody,
                )
                Spacer(Modifier.height(8.dp))
                BackupActionRow(label = "Export backup", onClick = onExport)
                BackupActionRow(label = "Restore from file", onClick = onRestore)
            }
        },
    )
}

@Composable
private fun BackupActionRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = BrandColors.Primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}

/**
 * Checkbox picker over [BackupComponent]s, shared by export and restore. Each
 * option may carry a row count (e.g. restore shows "Reminders (5)"); a null
 * count renders just the label (export, where counts aren't meaningful yet).
 * The confirm button is disabled until at least one component is ticked, and is
 * tinted danger when [destructive] (restore).
 */
@Composable
private fun ComponentPickerDialog(
    title: String,
    description: String,
    confirmLabel: String,
    options: Map<BackupComponent, Int?>,
    initialSelection: Set<BackupComponent>,
    destructive: Boolean,
    onConfirm: (Set<BackupComponent>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selection by remember { mutableStateOf(initialSelection) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selection) },
                enabled = selection.isNotEmpty(),
            ) {
                Text(
                    text = confirmLabel,
                    color = if (destructive) BrandColors.Danger else BrandColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandColors.TextBody,
                )
                Spacer(Modifier.height(8.dp))
                options.forEach { (component, count) ->
                    val checked = component in selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selection = if (checked) selection - component else selection + component
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selection = if (checked) selection - component else selection + component
                            },
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = if (count != null) "${component.label} ($count)" else component.label,
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
private fun AccentTrailing(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(BrandColors.Primary),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = BrandColors.TextBody,
        )
        Spacer(Modifier.size(4.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = BrandColors.TextBody,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AccentPickerDialog(
    current: AccentColor,
    onSelect: (AccentColor) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Accent color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Fixed rows of four keep the grid tidy without an experimental
                // FlowRow dependency.
                AccentColor.entries.chunked(4).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { accent ->
                            AccentSwatch(
                                accent = accent,
                                selected = accent == current,
                                onClick = { onSelect(accent) },
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun AccentSwatch(accent: AccentColor, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(accent.primaryLight)
            .then(
                if (selected) Modifier.border(3.dp, BrandColors.TextHeading, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Selected",
                tint = accent.onPrimaryLight,
                modifier = Modifier.size(22.dp),
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
