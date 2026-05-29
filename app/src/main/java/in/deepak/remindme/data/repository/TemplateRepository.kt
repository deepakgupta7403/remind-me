package `in`.deepak.remindme.data.repository

import `in`.deepak.remindme.data.db.TemplateDao
import `in`.deepak.remindme.data.mapper.toEntity
import `in`.deepak.remindme.data.mapper.toTemplate
import `in`.deepak.remindme.data.templates.ReminderTemplate
import `in`.deepak.remindme.data.templates.TemplateCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Read access to the template catalog, now backed by Room.
 *
 * [TemplateCatalog] stays in code as the curated *seed source*; [seedIfEmpty]
 * loads it into the table the first time only. Runtime reads go through
 * [observeAll], so the screen renders DB rows — the seam that lets us add
 * user-created/edited templates later without touching the UI.
 */
class TemplateRepository(
    private val dao: TemplateDao,
) {
    fun observeAll(): Flow<List<ReminderTemplate>> =
        dao.observeAll().map { rows -> rows.map { it.toTemplate() } }

    /**
     * Idempotent: seeds [TemplateCatalog] into Room only when the table is empty,
     * preserving the catalog's order via `displayOrder`. Safe to call on every
     * launch (one COUNT query when already seeded).
     */
    suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.insertAll(
                TemplateCatalog.all.mapIndexed { index, template -> template.toEntity(index) }
            )
        }
    }
}
