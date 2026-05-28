package `in`.deepak.remindme.data.templates

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.deepak.remindme.data.icons.ReminderIconPack
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
 *
 * The tile's [icon] is resolved from [iconKey] against [ReminderIconPack] — the
 * same key the built [Reminder] carries — so a template names its icon once and
 * the catalog stays free of icon imports. An unknown key falls back to a bell.
 */
data class ReminderTemplate(
    val id: String,
    val title: String,
    val scheduleSummary: String,
    val tags: Set<TemplateTag>,
    val iconKey: String,
    val tone: TemplateTone,
    val build: () -> Reminder,
) {
    /** Tile icon, resolved from [iconKey]; bell fallback if the key is unknown. */
    val icon: ImageVector
        get() = ReminderIconPack.imageOrNull(iconKey) ?: Icons.Filled.NotificationsActive
}

/**
 * Filter chips on the Templates screen. "All" is implicit (no filter); the
 * other values are the chips that actually narrow the grid.
 *
 * Each value is a curation category for the catalog — order here is the order
 * the chips appear in (the chip row scrolls horizontally to fit them all).
 */
enum class TemplateTag(val label: String) {
    Health("Health"),
    MindMood("Mind & mood"),
    Fitness("Fitness"),
    SleepRoutine("Sleep & routine"),
    Work("Work & productivity"),
    Home("Home & chores"),
    Finance("Personal finance"),
    Social("Family & social"),
    Maintenance("Maintenance"),
}

/**
 * Colour palette key for a template tile. Resolved to actual `BrandColors.Tile`
 * pairs at render time so the data layer stays @Composable-free.
 */
enum class TemplateTone { Lavender, Peach, Mint, Pink, Sky }
