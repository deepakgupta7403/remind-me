package `in`.deepak.remindme.ui.navigation

import `in`.deepak.remindme.domain.model.Reminder

/**
 * Single source of truth for navigation routes.
 *
 * Sealed so the navigator can `when`-exhaustively render destinations. Each
 * destination owns the format of its own route — callers never assemble
 * route strings by hand.
 */
sealed class Destination(val route: String) {

    data object Onboarding : Destination("onboarding")

    // Top-level bottom-nav tabs.
    data object Home      : Destination("home")
    data object Stats     : Destination("stats")
    data object Templates : Destination("templates")
    data object Settings  : Destination("settings")

    // Search.
    data object Search : Destination("search")

    // Profile editor (reached from Settings).
    data object Profile : Destination("profile")

    // Add/Edit reminder flow.
    data object Edit : Destination("edit?id={id}") {
        const val ARG_ID = "id"
        fun routeFor(reminderId: Long): String = "edit?id=$reminderId"
        fun routeForNew(): String = "edit?id=${Reminder.UNSAVED_ID}"
    }
}
