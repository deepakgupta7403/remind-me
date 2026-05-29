package `in`.deepak.remindme.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Template table access. Ordered by [TemplateEntity.displayOrder] so the grid
 * keeps the catalog's curated ordering.
 */
@Dao
interface TemplateDao {

    @Query("SELECT * FROM templates ORDER BY displayOrder")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TemplateEntity>)
}
