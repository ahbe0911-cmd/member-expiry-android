package ir.memberexpiry.manager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ir.memberexpiry.manager.notifications.ExpiryCounts

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY id") suspend fun allForBackup(): List<MemberEntity>
    @Query("DELETE FROM members") suspend fun clearAll()
    @Insert suspend fun insertBatch(rows: List<MemberEntity>)

    @Insert suspend fun insert(member: MemberEntity)
    @Update suspend fun update(member: MemberEntity): Int
    @Delete suspend fun delete(member: MemberEntity): Int
    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun find(id: String): MemberEntity?
    @Query("SELECT COUNT(*) FROM members WHERE nationalCode = :code AND id != :exceptId")
    suspend fun countByCode(code: String, exceptId: String): Int

    @Query("SELECT * FROM members WHERE instr(searchText, :term) > 0 OR :term = '' ORDER BY expiryEpochDay, lastName, firstName, id")
    fun observeAll(term: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE status = 'ACTIVE' AND expiryEpochDay <= :today ORDER BY expiryEpochDay, id")
    fun observePending(today: Long): Flow<List<MemberEntity>>
    @Query("SELECT * FROM members WHERE status = 'ACTIVE' AND expiryEpochDay <= :today ORDER BY expiryEpochDay, id")
    suspend fun pending(today: Long): List<MemberEntity>
    @Query("SELECT * FROM members WHERE status = 'ACTIVE' AND expiryEpochDay = :today ORDER BY lastName, id")
    fun observeDueToday(today: Long): Flow<List<MemberEntity>>
    @Query("SELECT * FROM members WHERE status = 'ACTIVE' AND expiryEpochDay > :today AND expiryEpochDay <= :end ORDER BY expiryEpochDay, id")
    fun observeUpcoming(today: Long, end: Long): Flow<List<MemberEntity>>

    @Query("""
        SELECT COUNT(CASE WHEN expiryEpochDay = :today THEN 1 END) AS dueToday,
               COUNT(CASE WHEN expiryEpochDay < :today THEN 1 END) AS overdue
        FROM members WHERE status = 'ACTIVE' AND expiryEpochDay <= :today
    """)
    suspend fun expiryCounts(today: Long): ExpiryCounts

    // A current-month queue includes every earlier unrenewed expiry, without modifying the expiry date.
    @Query("SELECT * FROM members WHERE status = 'ACTIVE' AND expiryEpochDay <= :end ORDER BY expiryEpochDay, id")
    fun observeCurrentQueue(end: Long): Flow<List<MemberEntity>>
    @Query("SELECT * FROM members WHERE status = 'ACTIVE' AND expiryEpochDay BETWEEN :start AND :end ORDER BY expiryEpochDay, id")
    fun observeMonth(start: Long, end: Long): Flow<List<MemberEntity>>

    @Query("""
        SELECT COUNT(CASE WHEN status = 'ACTIVE' AND expiryEpochDay < :today THEN 1 END) AS overdue,
               COUNT(CASE WHEN status = 'ACTIVE' AND expiryEpochDay = :today THEN 1 END) AS dueToday,
               COUNT(CASE WHEN status = 'ACTIVE' AND expiryEpochDay > :today AND expiryEpochDay <= :next30 THEN 1 END) AS next30Days,
               COUNT(CASE WHEN status = 'ACTIVE' AND expiryEpochDay BETWEEN :monthStart AND :monthEnd THEN 1 END) AS currentMonth,
               COUNT(*) AS totalMembers FROM members
    """)
    fun observeDashboard(today: Long, next30: Long, monthStart: Long, monthEnd: Long): Flow<DashboardCounts>

    @Query("""
        SELECT CAST(substr(expiryJalali, 6, 2) AS INTEGER) AS month, COUNT(*) AS count
        FROM members WHERE status = 'ACTIVE' AND expiryEpochDay BETWEEN :start AND :end
        GROUP BY CAST(substr(expiryJalali, 6, 2) AS INTEGER) ORDER BY month
    """)
    fun observeYearMonths(start: Long, end: Long): Flow<List<MonthCount>>

    @Query("""
        SELECT id AS memberId, nationalCode, firstName, lastName, fatherName,
               expiryJalali, expiryEpochDay, 0 AS wasRenewed
        FROM members WHERE status = 'ACTIVE' AND expiryEpochDay BETWEEN :start AND :end AND expiryEpochDay <= :today
        UNION ALL
        SELECT memberId, nationalCode, firstName, lastName, fatherName,
               COALESCE(previousExpiryJalali, newExpiryJalali) AS expiryJalali, COALESCE(previousExpiryEpochDay, newExpiryEpochDay) AS expiryEpochDay, 1 AS wasRenewed
        FROM activity_logs WHERE type = 'RENEWAL' AND isUndone = 0
          AND previousExpiryJalali IS NOT NULL AND previousExpiryEpochDay IS NOT NULL
          AND previousExpiryEpochDay BETWEEN :start AND :end AND previousExpiryEpochDay <= :today
          AND previousExpiryEpochDay <= operationEpochDay
        ORDER BY expiryEpochDay, memberId
    """)
    fun observeExpiredHistory(start: Long, end: Long, today: Long): Flow<List<ExpiryOccurrence>>
}
