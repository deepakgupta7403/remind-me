package `in`.deepak.remindme

import android.app.Application
import `in`.deepak.remindme.di.AppContainer
import `in`.deepak.remindme.di.DefaultAppContainer

/**
 * Process-wide singleton.
 *
 * Owns the [AppContainer] (manual DI). Anything that needs a repository, scheduler,
 * or notification presenter resolves it from here — keeping wiring in one place
 * means swapping an implementation (e.g., file repo → Room repo) touches one file.
 *
 * Notification channel creation is wired in [onCreate] via the container's
 * notification presenter (added in Step 3).
 */
class RemindMeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
        container.notificationPresenter.ensureChannelsExist()
    }
}
