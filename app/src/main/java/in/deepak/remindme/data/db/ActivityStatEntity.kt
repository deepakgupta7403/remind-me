package `in`.deepak.remindme.data.db

import androidx.room.Entity

/**
 * One row per (calendar day, reminder) carrying that day's fire/done counts —
 * the normalized form of the old `DayStat` blob (which held `Map<Long, Int>`
 * for fired and done). Aggregating back into a per-day
 * [in.deepak.remindme.data.preferences.DayStat] happens in
 * [in.deepak.remindme.data.preferences.ReminderActivityLog].
 *
 * No foreign key to [ReminderEntity] on purpose: stats outlive the reminder
 * that produced them (a deleted reminder's history still counts toward past
 * heatmap/streak days). Old rows are pruned by age, not by reminder lifecycle.
 *
 * [epochDay] is `LocalDate.toEpochDay()`; the composite primary key makes the
 * "increment today's count for this reminder" upsert a single keyed row.
 */
@Entity(tableName = "activity_stats", primaryKeys = ["epochDay", "reminderId"])
data class ActivityStatEntity(
    val epochDay: Long,
    val reminderId: Long,
    val fired: Int = 0,
    val done: Int = 0,
)
