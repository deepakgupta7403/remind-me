package `in`.deepak.remindme.data.templates

import androidx.compose.ui.graphics.vector.ImageVector
import `in`.deepak.remindme.domain.model.Reminder

/**
 * One-tap blueprint for a common reminder. Tapping a template on the Templates
 * screen creates a [Reminder] via [build] and persists it; the user can still
 * edit afterwards from Home.
 *
 * Why a data class with a factory lambda instead of mapping a tag → Reminder
 * elsewhere: each template's "what does it become?" decision is co-located with
 * its title, schedule label, and visuals — adding a new template is one entry
 * in [TemplateCatalog], not a swing through three files.
 */
data class ReminderTemplate(
    val id: String,
    val title: String,
    val scheduleSummary: String,
    val tags: Set<TemplateTag>,
    val icon: ImageVector,
    val tone: TemplateTone,
    val build: () -> Reminder,
)

/**
 * Filter chips on the Templates screen. "All" is implicit (no filter); the
 * other values are the chips that actually narrow the grid.
 */
enum class TemplateTag(val label: String) {
    Health("Health"),
    Work("Work"),
    Fitness("Fitness"),
}

/**
 * Colour palette key for a template tile. Resolved to actual `BrandColors.Tile`
 * pairs at render time so the data layer stays @Composable-free.
 */
enum class TemplateTone { Lavender, Peach, Mint, Pink, Sky }
