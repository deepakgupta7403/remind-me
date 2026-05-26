package `in`.deepak.remindme.domain.model

/**
 * Coarse category attached to every [Reminder]. Drives the card icon/colour
 * on Home and the filter chip on the Add Reminder screen.
 *
 * Kept as a closed enum for MVP — the "+ New" chip in the design surfaces
 * custom categories, but that ships in a follow-up. Existing reminders with
 * no category in JSON deserialise to [Other].
 */
enum class Category(val label: String) {
    Health("Health"),
    Work("Work"),
    Personal("Personal"),
    Other("Other"),
}
