package `in`.deepak.remindme.di

import android.content.Context
import androidx.room.Room
import `in`.deepak.remindme.data.backup.BackupManager
import `in`.deepak.remindme.data.db.LegacyDataImporter
import `in`.deepak.remindme.data.db.MigrationPreferences
import `in`.deepak.remindme.data.db.RemindMeDatabase
import `in`.deepak.remindme.data.preferences.OnboardingPreferences
import `in`.deepak.remindme.data.preferences.ReminderActivityLog
import `in`.deepak.remindme.data.preferences.SearchPreferences
import `in`.deepak.remindme.data.preferences.UserPreferences
import `in`.deepak.remindme.data.repository.RoomReminderRepository
import `in`.deepak.remindme.data.repository.TemplateRepository
import `in`.deepak.remindme.domain.repository.ReminderRepository
import `in`.deepak.remindme.notification.AndroidNotificationPresenter
import `in`.deepak.remindme.notification.NotificationPresenter
import `in`.deepak.remindme.scheduler.AlarmScheduler
import `in`.deepak.remindme.scheduler.AndroidAlarmScheduler
import `in`.deepak.remindme.scheduler.ReminderFireHandler

/**
 * Production wiring. Lazy so heavy dependencies (the database, scheduler)
 * aren't built until first use — important because [RemindMeApp.onCreate]
 * runs on every process start including receiver-triggered ones.
 *
 * Room is the system of record. The single [RemindMeDatabase] instance is
 * shared by every data-layer dependency (reminders, stats, templates).
 */
class DefaultAppContainer(
    private val appContext: Context,
) : AppContainer {

    private val database: RemindMeDatabase by lazy {
        Room.databaseBuilder(appContext, RemindMeDatabase::class.java, RemindMeDatabase.NAME)
            .build()
    }

    override val notificationPresenter: NotificationPresenter by lazy {
        AndroidNotificationPresenter(appContext)
    }

    override val reminderRepository: ReminderRepository by lazy {
        RoomReminderRepository(database.reminderDao())
    }

    override val templateRepository: TemplateRepository by lazy {
        TemplateRepository(database.templateDao())
    }

    override val alarmScheduler: AlarmScheduler by lazy {
        AndroidAlarmScheduler(appContext, reminderRepository)
    }

    override val reminderFireHandler: ReminderFireHandler by lazy {
        ReminderFireHandler(
            repository = reminderRepository,
            scheduler = alarmScheduler,
            presenter = notificationPresenter,
            activityLog = reminderActivityLog,
        )
    }

    override val onboardingPreferences: OnboardingPreferences by lazy {
        OnboardingPreferences(appContext)
    }

    override val userPreferences: UserPreferences by lazy {
        UserPreferences(appContext)
    }

    override val searchPreferences: SearchPreferences by lazy {
        SearchPreferences(appContext)
    }

    override val reminderActivityLog: ReminderActivityLog by lazy {
        ReminderActivityLog(database.activityDao())
    }

    override val backupManager: BackupManager by lazy {
        BackupManager(
            database = database,
            userPreferences = userPreferences,
            alarmScheduler = alarmScheduler,
        )
    }

    override val legacyDataImporter: LegacyDataImporter by lazy {
        LegacyDataImporter(
            appContext = appContext,
            database = database,
            migrationPrefs = MigrationPreferences(appContext),
            templateRepository = templateRepository,
        )
    }
}
