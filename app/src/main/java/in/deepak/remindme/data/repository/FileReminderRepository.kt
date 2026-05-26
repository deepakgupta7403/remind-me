package `in`.deepak.remindme.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import `in`.deepak.remindme.data.dto.ReminderDto
import `in`.deepak.remindme.data.dto.ReminderFileEnvelope
import `in`.deepak.remindme.data.mapper.toDomainOrNull
import `in`.deepak.remindme.data.mapper.toDto
import `in`.deepak.remindme.domain.model.Reminder
import `in`.deepak.remindme.domain.repository.ReminderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * JSON file backed repo. Production swap target = Room.
 *
 * Concurrency model:
 *   - All writes serialised through [mutex] so we never lose updates from
 *     racing coroutines.
 *   - [observeAll] is fed by [stateFlow] (a hot in-memory cache); reads are
 *     instant after first load.
 *   - File I/O is on [Dispatchers.IO].
 *
 * Why a single file vs one-per-reminder: simpler atomic writes, and we don't
 * expect more than a few dozen reminders.
 */
class FileReminderRepository(
    context: Context,
    fileName: String = DEFAULT_FILE_NAME,
    private val gson: Gson = Gson(),
) : ReminderRepository {

    private val file: File = File(context.filesDir, fileName)
    private val mutex = Mutex()
    private val stateFlow = MutableStateFlow<List<Reminder>>(emptyList())

    @Volatile private var nextId: Long = 1L
    @Volatile private var loaded: Boolean = false

    // onStart so the first subscriber triggers a file read — otherwise the
    // initial emptyList() shadows any reminders persisted in a previous session
    // until the next mutating call happens to call ensureLoaded.
    override fun observeAll(): Flow<List<Reminder>> = stateFlow
        .asStateFlow()
        .onStart { ensureLoaded() }

    override suspend fun getAll(): List<Reminder> {
        ensureLoaded()
        return stateFlow.value
    }

    override suspend fun get(id: Long): Reminder? {
        ensureLoaded()
        return stateFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun upsert(reminder: Reminder): Reminder = mutex.withLock {
        ensureLoadedLocked()
        val assigned: Reminder = if (reminder.id == Reminder.UNSAVED_ID) {
            reminder.withId(nextId++)
        } else {
            reminder
        }
        val updated = stateFlow.value
            .filterNot { it.id == assigned.id }
            .plus(assigned)
            .sortedBy { it.id }
        stateFlow.value = updated
        persist(updated)
        assigned
    }

    override suspend fun delete(id: Long): Unit = mutex.withLock {
        ensureLoadedLocked()
        val updated = stateFlow.value.filterNot { it.id == id }
        stateFlow.value = updated
        persist(updated)
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean): Unit = mutex.withLock {
        ensureLoadedLocked()
        val updated = stateFlow.value.map {
            if (it.id == id) it.withEnabled(enabled) else it
        }
        stateFlow.value = updated
        persist(updated)
    }

    // --- internals --------------------------------------------------------

    private suspend fun ensureLoaded() {
        if (!loaded) mutex.withLock { ensureLoadedLocked() }
    }

    /** Must be called under [mutex]. */
    private suspend fun ensureLoadedLocked() {
        if (loaded) return
        val envelope = withContext(Dispatchers.IO) { readEnvelope() }
        nextId = envelope.nextId.coerceAtLeast(1L)
        stateFlow.value = envelope.reminders
            .mapNotNull(ReminderDto::toDomainOrNull)
            .sortedBy { it.id }
        loaded = true
    }

    private fun readEnvelope(): ReminderFileEnvelope {
        if (!file.exists()) return ReminderFileEnvelope()
        return try {
            file.bufferedReader().use { reader ->
                gson.fromJson(reader, ReminderFileEnvelope::class.java)
                    ?: ReminderFileEnvelope()
            }
        } catch (_: JsonSyntaxException) {
            // Corrupted file: start fresh rather than crashing. Worst case the
            // user re-adds their reminders.
            ReminderFileEnvelope()
        }
    }

    /** Must be called under [mutex]. Performs an atomic-rename write. */
    private suspend fun persist(reminders: List<Reminder>) = withContext(Dispatchers.IO) {
        val envelope = ReminderFileEnvelope(
            schemaVersion = ReminderFileEnvelope.SCHEMA_VERSION,
            nextId = nextId,
            reminders = reminders.map(Reminder::toDto),
        )
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.bufferedWriter().use { gson.toJson(envelope, it) }
        if (!tmp.renameTo(file)) {
            // Fallback if rename fails (rare on internal storage).
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    companion object {
        const val DEFAULT_FILE_NAME = "reminders.json"
    }
}

// --- private helpers ------------------------------------------------------

private fun Reminder.withId(newId: Long): Reminder = when (this) {
    is Reminder.Interval -> copy(id = newId)
    is Reminder.OneTime  -> copy(id = newId)
    is Reminder.Daily    -> copy(id = newId)
    is Reminder.Weekly   -> copy(id = newId)
}

private fun Reminder.withEnabled(newEnabled: Boolean): Reminder = when (this) {
    is Reminder.Interval -> copy(enabled = newEnabled)
    is Reminder.OneTime  -> copy(enabled = newEnabled)
    is Reminder.Daily    -> copy(enabled = newEnabled)
    is Reminder.Weekly   -> copy(enabled = newEnabled)
}
