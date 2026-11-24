package ir.memberexpiry.manager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Query("DELETE FROM activity_logs") suspend fun clearAll()
    @Insert suspend fun insertBatch(rows: List<ActivityLogEntity>)

    @Insert suspend fun insert(log: ActivityLogEntity)
    @Query("SELECT * FROM activity_logs WHERE id = :id LIMIT 1")
    suspend fun find(id: String): ActivityLogEntity?
    @Query("UPDATE activity_logs SET isUndone = 1, undoneAtMillis = :at WHERE id = :id AND isUndone = 0")
    suspend fun markUndone(id: String, at: Long): Int

    @Query("""
        SELECT * FROM activity_logs WHERE isUndone = 0 AND operationYear = :year
        AND (:month IS NULL OR operationMonth = :month)
        ORDER BY operationEpochDay DESC, operationAtMillis DESC, id
    """)
    fun observeActivities(year: Int, month: Int?): Flow<List<ActivityLogEntity>>

    @Query("""
        SELECT COUNT(CASE WHEN type = 'RENEWAL' THEN 1 END) AS renewals,
               COUNT(CASE WHEN type = 'NEW_MEMBERSHIP' THEN 1 END) AS newMemberships,
               COUNT(*) AS total
        FROM activity_logs WHERE isUndone = 0 AND operationYear = :year
        AND (:month IS NULL OR operationMonth = :month)
    """)
    fun observeCounts(year: Int, month: Int?): Flow<ActivityCounts>

    @Query("SELECT * FROM activity_logs ORDER BY operationAtMillis, id")
    suspend fun allForBackup(): List<ActivityLogEntity>
}
