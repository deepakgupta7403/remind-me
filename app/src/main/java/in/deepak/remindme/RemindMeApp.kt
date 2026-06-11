package `in`.deepak.remindme

import android.app.Application
import `in`.deepak.remindme.di.AppContainer
import `in`.deepak.remindme.di.DefaultAppContainer
import `in`.deepak.remindme.ui.theme.AccentColor
import `in`.deepak.remindme.ui.theme.ThemeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Process-wide singleton.
 *
 * Owns the [AppContainer] (manual DI). Anything that needs a repository, scheduler,
 * or notification presenter resolves it from here — keeping wiring in one place
 * means swapping an implementation (e.g., file repo → Room repo) touches one file.
 *
 * On start we kick off the one-time legacy-file → Room import and template
 * seeding on a background scope (see [in.deepak.remindme.data.db.LegacyDataImporter]).
 * It's flag-guarded and idempotent, so running it on every process start —
 * including receiver-triggered ones — is cheap once done.
 */
class RemindMeApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
        // Seed the theme before any Activity composes so the first frame paints
        // in the user's chosen scheme + accent (no light→dark flash on launch).
        ThemeController.mode = container.userPreferences.themeMode
        ThemeController.accent = AccentColor.fromName(container.userPreferences.accentColorName)
        container.notificationPresenter.ensureChannelsExist()
        appScope.launch { container.legacyDataImporter.runOnce() }
    }
}
