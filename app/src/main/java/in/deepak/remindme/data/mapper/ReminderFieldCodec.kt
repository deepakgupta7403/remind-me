package `in`.deepak.remindme.data.mapper

import `in`.deepak.remindme.domain.model.Category
import java.time.DayOfWeek

/**
 * Shared field encoders used by every reminder mapper (legacy DTO, Room entity,
 * and template). Kept in one place so the on-disk encoding of categories and
 * day-of-week sets is defined exactly once.
 *
 * DayOfWeek bitmask: Monday = bit 0 (value 1), …, Sunday = bit 6 (value 64).
 */

internal fun encodeDays(days: Set<DayOfWeek>): Int =
    days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }

internal fun decodeDays(mask: Int): Set<DayOfWeek> =
    DayOfWeek.values()
        .filter { (mask shr (it.value - 1)) and 1 == 1 }
        .toSet()

/** Unknown/null category name → [Category.Other], never throws. */
internal fun decodeCategory(raw: String?): Category =
    raw?.let { runCatching { Category.valueOf(it) }.getOrNull() } ?: Category.Other
