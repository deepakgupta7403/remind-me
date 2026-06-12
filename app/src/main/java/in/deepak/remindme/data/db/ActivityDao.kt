package `in`.deepak.remindme.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Activity-stats access.
 *
 * Recording an event is an "ensure row, then increment" upsert: [insertIgnore]
 * creates today's zero-row if absent (no-op if it exists), then the keyed
 * `UPDATE ... + 1` bumps the counter. Wrapping both — plus the age [prune] — in
 * a [Transaction] keeps a fire/done racing across threads from losing a count.
 */
@Dao
interface ActivityDao {

    @Query("SELECT * FROM activity_stats ORDER BY epochDay")
    fun observeAll(): Flow<List<ActivityStatEntity>>

    /** One-shot snapshot — used by backup export. */
    @Query("SELECT * FROM activity_stats ORDER BY epochDay")
    suspend fun getAll(): List<ActivityStatEntity>

    /** Wipe the table — used by backup restore (replace-all). */
    @Query("DELETE FROM activity_stats")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(row: ActivityStatEntity)

    @Query("UPDATE activity_stats SET fired = fired + 1 WHERE epochDay = :epochDay AND reminderId = :reminderId")
    suspend fun incrementFired(epochDay: Long, reminderId: Long)

    @Query("UPDATE activity_stats SET done = done + 1 WHERE epochDay = :epochDay AND reminderId = :reminderId")
    suspend fun incrementDone(epochDay: Long, reminderId: Long)

    @Query("DELETE FROM activity_stats WHERE epochDay < :cutoffEpochDay")
    suspend fun prune(cutoffEpochDay: Long)

    /** Bulk insert — used by [LegacyDataImporter]. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<ActivityStatEntity>)

    @Transaction
    suspend fun recordFired(epochDay: Long, reminderId: Long, cutoffEpochDay: Long) {
        insertIgnore(ActivityStatEntity(epochDay, reminderId))
        incrementFired(epochDay, reminderId)
        prune(cutoffEpochDay)
    }

    @Transaction
    suspend fun recordDone(epochDay: Long, reminderId: Long, cutoffEpochDay: Long) {
        insertIgnore(ActivityStatEntity(epochDay, reminderId))
        incrementDone(epochDay, reminderId)
        prune(cutoffEpochDay)
    }
}
