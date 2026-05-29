package `in`.deepak.remindme

import android.app.Application
import `in`.deepak.remindme.di.AppContainer
import `in`.deepak.remindme.di.DefaultAppContainer
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
        container.notificationPresenter.ensureChannelsExist()
        appScope.launch { container.legacyDataImporter.runOnce() }
    }
}
