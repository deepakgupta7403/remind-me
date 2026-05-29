package `in`.deepak.remindme.di

import `in`.deepak.remindme.data.preferences.OnboardingPreferences
import `in`.deepak.remindme.data.preferences.ReminderActivityLog
import `in`.deepak.remindme.data.preferences.SearchPreferences
import `in`.deepak.remindme.data.db.LegacyDataImporter
import `in`.deepak.remindme.data.preferences.UserPreferences
import `in`.deepak.remindme.data.repository.TemplateRepository
import `in`.deepak.remindme.domain.repository.ReminderRepository
import `in`.deepak.remindme.notification.NotificationPresenter
import `in`.deepak.remindme.scheduler.AlarmScheduler
import `in`.deepak.remindme.scheduler.ReminderFireHandler

/**
 * Service locator for the whole app.
 *
 * Manual DI on purpose: zero magic, zero annotation processors, swappable in tests
 * by handing in a fake [AppContainer]. If the graph grows past ~10 entries we'll
 * migrate to Hilt — but the call sites (`app.container.someService`) stay the same,
 * because they go through this interface.
 *
 * Each step adds the dependencies it introduces here, so the wiring stays in one place.
 */
interface AppContainer {
    val notificationPresenter: NotificationPresenter
    val reminderRepository: ReminderRepository
    val templateRepository: TemplateRepository
    val alarmScheduler: AlarmScheduler
    val reminderFireHandler: ReminderFireHandler
    val onboardingPreferences: OnboardingPreferences
    val userPreferences: UserPreferences
    val searchPreferences: SearchPreferences
    val reminderActivityLog: ReminderActivityLog

    /** One-time legacy-file → Room import, run at app start. */
    val legacyDataImporter: LegacyDataImporter
}
