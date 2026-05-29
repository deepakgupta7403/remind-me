package `in`.deepak.remindme.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Reminder table access. Suspend / [Flow] APIs — Room runs them off the main
 * thread on its own executor, so the repository needs no manual dispatcher.
 *
 * Insert vs update are split (rather than a single `@Upsert`) so the repository
 * can reproduce the old file-repo contract precisely: an unsaved reminder
 * (id 0) is [insert]ed and gets a generated id back; a saved one is [update]d.
 */
@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY id")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY id")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun get(id: Long): ReminderEntity?

    /** Returns the generated row id (the assigned reminder id). */
    @Insert
    suspend fun insert(entity: ReminderEntity): Long

    @Update
    suspend fun update(entity: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE reminders SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    /** Bulk insert preserving explicit ids — used by [LegacyDataImporter]. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ReminderEntity>)
}
