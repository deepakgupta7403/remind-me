package `in`.deepak.remindme.domain.repository

import `in`.deepak.remindme.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

/**
 * Abstract data source for reminders.
 *
 * The whole app (ViewModels, scheduler, BootReceiver) goes through this
 * interface. The current implementation is a JSON file (see
 * `FileReminderRepository`); when we revisit Room, only the impl changes.
 *
 * All mutating methods are `suspend` so callers compose with coroutines and
 * the impl can do I/O off the main thread without leaking the threading model.
 */
interface ReminderRepository {

    /** Hot stream of the full list. Emits a new list on every mutation. */
    fun observeAll(): Flow<List<Reminder>>

    /** One-shot snapshot. Used by callers that don't want to observe (BootReceiver, scheduler). */
    suspend fun getAll(): List<Reminder>

    suspend fun get(id: Long): Reminder?

    /**
     * Insert or update. If [Reminder.id] equals [Reminder.UNSAVED_ID], the
     * repo assigns a new id. Returns the persisted [Reminder] with its id.
     */
    suspend fun upsert(reminder: Reminder): Reminder

    suspend fun delete(id: Long)

    suspend fun setEnabled(id: Long, enabled: Boolean)
}
