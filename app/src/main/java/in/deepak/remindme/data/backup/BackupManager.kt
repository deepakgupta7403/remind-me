package `in`.deepak.remindme.data.backup

import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import `in`.deepak.remindme.data.db.ActivityStatEntity
import `in`.deepak.remindme.data.db.RemindMeDatabase
import `in`.deepak.remindme.data.db.ReminderEntity
import `in`.deepak.remindme.data.db.TemplateEntity
import `in`.deepak.remindme.data.preferences.GreetingStyle
import `in`.deepak.remindme.data.preferences.ThemeMode
import `in`.deepak.remindme.data.preferences.UserPreferences
import `in`.deepak.remindme.scheduler.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local backup & restore. There is no cloud/account backend — this is a manual
 * export/import of the whole app to a single JSON file the user owns.
 *
 * Serialization happens at the Room *entity* level (flat rows, no `java.time` /
 * sealed-type plumbing) plus a flat [BackupSettings] snapshot of the
 * SharedPreferences scalars. Restore is **replace-all**: a backup is a snapshot,
 * so importing it makes the device match the file exactly (no merge/duplicate
 * ambiguity). The caller confirms first, since it's destructive.
 *
 * The file format is versioned ([BACKUP_VERSION]); a file from a newer build is
 * refused rather than partially applied.
 */
class BackupManager(
    private val database: RemindMeDatabase,
    private val userPreferences: UserPreferences,
    private val alarmScheduler: AlarmScheduler,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val gson: Gson = GsonBuilder().setPrettyPrinting().serializeNulls().create(),
) {

    /**
     * Build a backup document as pretty-printed JSON, including only the chosen
     * [components]. Unselected sections are written as `null` so a partial backup
     * is unambiguous on restore (null = "not in this file", distinct from an
     * empty list = "in the file, but empty").
     */
    suspend fun exportToJson(components: Set<BackupComponent>): String = withContext(Dispatchers.IO) {
        val data = BackupData(
            version = BACKUP_VERSION,
            exportedAtEpochMillis = nowMillis(),
            reminders = if (BackupComponent.Reminders in components) database.reminderDao().getAll() else null,
            templates = if (BackupComponent.Templates in components) database.templateDao().getAll() else null,
            stats = if (BackupComponent.Stats in components) database.activityDao().getAll() else null,
            settings = if (BackupComponent.Settings in components) snapshotSettings() else null,
        )
        gson.toJson(data)
    }

    /**
     * Parse [json] without applying anything, reporting which [BackupComponent]s
     * it contains and how many rows each holds. Returns null if the file isn't a
     * readable backup, or is from a newer app version. Pure/CPU-only — read the
     * file off the main thread, then call this.
     */
    fun peek(json: String): BackupSummary? {
        val data = try {
            gson.fromJson(json, BackupData::class.java)
        } catch (_: Exception) {
            return null
        }
        if (data == null || data.version > BACKUP_VERSION) return null

        val counts = buildMap {
            data.reminders?.let { put(BackupComponent.Reminders, it.size) }
            data.templates?.let { put(BackupComponent.Templates, it.size) }
            data.stats?.let { put(BackupComponent.Stats, it.size) }
            if (data.settings != null) put(BackupComponent.Settings, 1)
        }
        if (counts.isEmpty()) return null
        return BackupSummary(version = data.version, counts = counts)
    }

    /**
     * Parse [json] and replace the selected [components] with the file's
     * contents (replace-all, per component). A component is only touched when
     * it's both selected *and* present in the file — selecting a component the
     * file doesn't contain is a no-op, never a wipe. Never throws for bad input;
     * returns an [ImportResult.Failure] the UI can surface.
     */
    suspend fun importFromJson(json: String, components: Set<BackupComponent>): ImportResult =
        withContext(Dispatchers.IO) {
            val data = try {
                gson.fromJson(json, BackupData::class.java)
            } catch (_: Exception) {
                return@withContext ImportResult.Failure("This file isn't a RemindMe backup.")
            }
            if (data == null || data.version > BACKUP_VERSION) {
                return@withContext ImportResult.Failure("This file isn't a readable RemindMe backup.")
            }

            val restoreReminders = BackupComponent.Reminders in components && data.reminders != null
            val restoreTemplates = BackupComponent.Templates in components && data.templates != null
            val restoreStats = BackupComponent.Stats in components && data.stats != null
            val restoreSettings = BackupComponent.Settings in components && data.settings != null
            if (!restoreReminders && !restoreTemplates && !restoreStats && !restoreSettings) {
                return@withContext ImportResult.Failure("Nothing to restore from this file.")
            }

            // Cancel current alarms before replacing reminders, so ones that
            // won't exist after the restore don't keep firing.
            if (restoreReminders) {
                database.reminderDao().getAll().forEach { alarmScheduler.cancel(it.id) }
            }

            database.withTransaction {
                if (restoreReminders) {
                    database.reminderDao().deleteAll()
                    database.reminderDao().insertAll(data.reminders!!)
                }
                if (restoreTemplates) {
                    database.templateDao().deleteAll()
                    database.templateDao().insertAll(data.templates!!)
                }
                if (restoreStats) {
                    database.activityDao().deleteAll()
                    database.activityDao().insertAll(data.stats!!)
                }
            }

            if (restoreSettings) restoreSettings(data.settings!!)

            // Re-arm every restored reminder that came back enabled.
            if (restoreReminders) alarmScheduler.rescheduleAll()

            val counts = buildMap {
                if (restoreReminders) put(BackupComponent.Reminders, data.reminders!!.size)
                if (restoreTemplates) put(BackupComponent.Templates, data.templates!!.size)
                if (restoreStats) put(BackupComponent.Stats, data.stats!!.size)
                if (restoreSettings) put(BackupComponent.Settings, 1)
            }
            ImportResult.Success(counts)
        }

    private fun snapshotSettings() = BackupSettings(
        userName = userPreferences.userName,
        avatarColorIndex = userPreferences.avatarColorIndex,
        showGreeting = userPreferences.showGreeting,
        greetingStyle = userPreferences.greetingStyle.name,
        vibrationEnabled = userPreferences.vibrationEnabled,
        snoozeMinutes = userPreferences.snoozeMinutes,
        dndEnabled = userPreferences.dndEnabled,
        dndStartMinute = userPreferences.dndStartMinute,
        dndEndMinute = userPreferences.dndEndMinute,
        themeMode = userPreferences.themeMode.name,
        accentColorName = userPreferences.accentColorName,
        alarmSoundUri = userPreferences.alarmSoundUri,
        alarmSoundLabel = userPreferences.alarmSoundLabel,
    )

    private fun restoreSettings(s: BackupSettings) {
        userPreferences.userName = s.userName
        userPreferences.avatarColorIndex = s.avatarColorIndex
        userPreferences.showGreeting = s.showGreeting
        userPreferences.greetingStyle = GreetingStyle.fromName(s.greetingStyle)
        userPreferences.vibrationEnabled = s.vibrationEnabled
        userPreferences.snoozeMinutes = s.snoozeMinutes
        userPreferences.dndEnabled = s.dndEnabled
        userPreferences.dndStartMinute = s.dndStartMinute
        userPreferences.dndEndMinute = s.dndEndMinute
        userPreferences.themeMode = ThemeMode.fromName(s.themeMode)
        userPreferences.accentColorName = s.accentColorName
        userPreferences.alarmSoundUri = s.alarmSoundUri
        userPreferences.alarmSoundLabel = s.alarmSoundLabel
    }

    private companion object {
        const val BACKUP_VERSION = 1
    }
}

/** Root of the backup file. Lists are nullable so a malformed file degrades to empty, not a crash. */
data class BackupData(
    val version: Int,
    val exportedAtEpochMillis: Long,
    val reminders: List<ReminderEntity>?,
    val templates: List<TemplateEntity>?,
    val stats: List<ActivityStatEntity>?,
    val settings: BackupSettings?,
)

/** Flat snapshot of the SharedPreferences scalars (enums stored by name). */
data class BackupSettings(
    val userName: String,
    val avatarColorIndex: Int,
    val showGreeting: Boolean,
    val greetingStyle: String,
    val vibrationEnabled: Boolean,
    val snoozeMinutes: Int,
    val dndEnabled: Boolean,
    val dndStartMinute: Int,
    val dndEndMinute: Int,
    val themeMode: String,
    val accentColorName: String,
    val alarmSoundUri: String?,
    val alarmSoundLabel: String,
)

/** A selectable slice of a backup. Order here is the order shown in the dialogs. */
enum class BackupComponent(val label: String) {
    Reminders("Reminders"),
    Templates("Templates"),
    Stats("Stats"),
    Settings("Settings"),
}

/**
 * What a backup file contains, without applying it. [counts] maps each present
 * component to its row count (Settings is a single blob, reported as 1).
 */
data class BackupSummary(
    val version: Int,
    val counts: Map<BackupComponent, Int>,
) {
    val present: Set<BackupComponent> get() = counts.keys
}

/** Outcome of an import, surfaced to the user. */
sealed interface ImportResult {
    /** Components actually restored, mapped to how many rows each brought in. */
    data class Success(val restored: Map<BackupComponent, Int>) : ImportResult
    data class Failure(val reason: String) : ImportResult
}
