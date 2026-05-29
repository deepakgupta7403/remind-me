package `in`.deepak.remindme.data.repository

import `in`.deepak.remindme.data.db.ReminderDao
import `in`.deepak.remindme.data.mapper.toDomainOrNull
import `in`.deepak.remindme.data.mapper.toEntity
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed [ReminderRepository] — the production implementation, replacing
 * the JSON-file repo. The interface is unchanged, so ViewModels, the scheduler,
 * the boot receiver, and the fire handler are untouched by the swap.
 *
 * Room runs DAO suspend/[Flow] calls on its own executor, so there's no manual
 * dispatcher or mutex here: the database serialises writes and the observed
 * query re-emits on every mutation. Rows that fail to decode (e.g. an unknown
 * future type) are dropped via [toDomainOrNull] rather than crashing reads.
 */
class RoomReminderRepository(
    private val dao: ReminderDao,
) : ReminderRepository {

    override fun observeAll(): Flow<List<Reminder>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override suspend fun getAll(): List<Reminder> =
        dao.getAll().mapNotNull { it.toDomainOrNull() }

    override suspend fun get(id: Long): Reminder? =
        dao.get(id)?.toDomainOrNull()

    override suspend fun upsert(reminder: Reminder): Reminder =
        if (reminder.id == Reminder.UNSAVED_ID) {
            // Entity carries id = UNSAVED_ID (0); autoGenerate assigns the real id.
            val assignedId = dao.insert(reminder.toEntity())
            reminder.withId(assignedId)
        } else {
            dao.update(reminder.toEntity())
            reminder
        }

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun setEnabled(id: Long, enabled: Boolean) =
        dao.setEnabled(id, enabled)
}

private fun Reminder.withId(newId: Long): Reminder = when (this) {
    is Reminder.Interval -> copy(id = newId)
    is Reminder.OneTime  -> copy(id = newId)
    is Reminder.Daily    -> copy(id = newId)
    is Reminder.Weekly   -> copy(id = newId)
}
