package `in`.deepak.remindme.ui

import androidx.compose.runtime.compositionLocalOf
import `in`.deepak.remindme.di.AppContainer

/**
 * Compose-side access to the app's [AppContainer].
 *
 * Set once at the top of the Compose tree (see MainActivity). ViewModels and
 * any composable that needs a dependency reads it from here — no static
 * singletons, no ApplicationContext-fishing inside composables.
 */
val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("LocalAppContainer not provided — wrap your content in CompositionLocalProvider")
}
