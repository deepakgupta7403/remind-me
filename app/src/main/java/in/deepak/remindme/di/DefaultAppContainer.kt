package `in`.deepak.remindme.di

import android.content.Context
import `in`.deepak.remindme.data.preferences.OnboardingPreferences
import `in`.deepak.remindme.data.preferences.ReminderActivityLog
import `in`.deepak.remindme.data.preferences.SearchPreferences
import `in`.deepak.remindme.data.preferences.UserPreferences
import `in`.deepak.remindme.data.repository.FileReminderRepository
import `in`.deepak.remindme.domain.repository.ReminderRepository
import `in`.deepak.remindme.notification.AndroidNotificationPresenter
import `in`.deepak.remindme.notification.NotificationPresenter
import `in`.deepak.remindme.scheduler.AlarmScheduler
import `in`.deepak.remindme.scheduler.AndroidAlarmScheduler
import `in`.deepak.remindme.scheduler.ReminderFireHandler

/**
 * Production wiring. Lazy so heavy dependencies (repository load, scheduler)
 * aren't built until first use — important because [RemindMeApp.onCreate]
 * runs on every process start including receiver-triggered ones.
 */
class DefaultAppContainer(
    private val appContext: Context,
) : AppContainer {

    override val notificationPresenter: NotificationPresenter by lazy {
        AndroidNotificationPresenter(appContext)
    }

    override val reminderRepository: ReminderRepository by lazy {
        FileReminderRepository(appContext)
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
        ReminderActivityLog(appContext)
    }
}
