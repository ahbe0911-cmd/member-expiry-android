package ir.memberexpiry.manager.data.repository

import androidx.room.withTransaction
import ir.memberexpiry.manager.core.date.DateProvider
import ir.memberexpiry.manager.core.date.JalaliDate
import ir.memberexpiry.manager.core.date.PersianText
import ir.memberexpiry.manager.data.local.*
import java.util.UUID
import ir.memberexpiry.manager.core.transfer.*
import ir.memberexpiry.manager.notifications.DailyExpirySummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class MemberRepository(private val db: MemberDatabase, private val dates: DateProvider = DateProvider()) {
    private val members = db.members()
    private val logs = db.activities()

    fun today(): JalaliDate = dates.today()
    fun observeToday(): Flow<JalaliDate> = dates.observeToday()
    fun observeMembers(term: String = "") = members.observeAll(PersianText.normalizeSearch(term))
    fun observePending() = observeToday().flatMapLatest { members.observePending(it.toEpochDay()) }
    fun observePendingWithDate(): Flow<Pair<JalaliDate, List<MemberEntity>>> = observeToday().flatMapLatest { date ->
        members.observePending(date.toEpochDay()).map { date to it }
    }
    suspend fun dailyExpirySummary(): DailyExpirySummary {
        val date = today()
        return DailyExpirySummary(date, members.expiryCounts(date.toEpochDay()))
    }
    suspend fun pendingNow() = members.pending(today().toEpochDay())
    fun observeDueToday() = observeToday().flatMapLatest { members.observeDueToday(it.toEpochDay()) }
    fun observeNext30Days() = observeToday().flatMapLatest { members.observeUpcoming(it.toEpochDay(), it.toEpochDay() + 30) }
    fun observeCurrentQueue() = observeToday().flatMapLatest { members.observeCurrentQueue(it.lastOfMonth().toEpochDay()) }
    fun observeDashboard() = observeToday().flatMapLatest {
        members.observeDashboard(it.toEpochDay(), it.toEpochDay() + 30, it.firstOfMonth().toEpochDay(), it.lastOfMonth().toEpochDay())
    }
    fun observeMonth(year: Int, month: Int): Flow<List<MemberEntity>> {
        val first = JalaliDate(year, month, 1)
        return members.observeMonth(first.toEpochDay(), first.lastOfMonth().toEpochDay())
    }
    fun observeYearMonths(year: Int): Flow<List<MonthCount>> {
        val (start, end) = yearRange(year)
        return members.observeYearMonths(start, end).map { rows ->
            val counts = rows.associate { it.month to it.count }
            (1..12).map { MonthCount(it, counts[it] ?: 0) }
        }
    }
    fun observeExpiredHistory(year: Int, month: Int): Flow<List<ExpiryOccurrence>> {
        val first = JalaliDate(year, month, 1)
        return observeToday().flatMapLatest {
            members.observeExpiredHistory(first.toEpochDay(), first.lastOfMonth().toEpochDay(), it.toEpochDay())
        }
    }
    fun observeActivities(year: Int, month: Int? = null): Flow<List<ActivityLogEntity>> {
        validatePeriod(year, month)
        return logs.observeActivities(year, month)
    }
    fun observeActivityCounts(year: Int, month: Int? = null): Flow<ActivityCounts> {
        validatePeriod(year, month)
        return logs.observeCounts(year, month)
    }
    suspend fun findMember(id: String): MemberEntity? = members.find(id)

    /** Normal entry rejects duplicate national codes. Explicit duplicate import can opt in later. */
    suspend fun addMember(input: MemberInput, allowDuplicate: Boolean = false): OperationReceipt = db.withTransaction {
        val value = input.normalized()
        require(allowDuplicate || members.countByCode(value.nationalCode, "") == 0) { "National code already exists" }
        val date = today()
        val now = dates.nowMillis()
        val member = MemberEntity(
            id = UUID.randomUUID().toString(), nationalCode = value.nationalCode,
            firstName = value.firstName, lastName = value.lastName, fatherName = value.fatherName,
            expiryJalali = value.expiry.toIsoString(), expiryEpochDay = value.expiry.toEpochDay(),
            searchText = value.searchText(), createdAtMillis = now, updatedAtMillis = now
        )
        members.insert(member)
        val log = makeLog(member, null, ActivityType.NEW_MEMBERSHIP, date, now)
        logs.insert(log)
        OperationReceipt(member.id, log.id)
    }

    /** Identity-only edits never overwrite the previous expiry history or generate membership statistics. */
    suspend fun editIdentity(id: String, nationalCode: String, firstName: String, lastName: String, fatherName: String): MemberEntity = db.withTransaction {
        val old = requireNotNull(members.find(id)) { "Member not found" }
        val value = MemberInput(nationalCode, firstName, lastName, fatherName, old.expiryDate()).normalized()
        require(value.nationalCode == old.nationalCode || members.countByCode(value.nationalCode, id) == 0) { "National code already exists" }
        val updated = old.copy(nationalCode = value.nationalCode, firstName = value.firstName,
            lastName = value.lastName, fatherName = value.fatherName, searchText = value.searchText(),
            updatedAtMillis = dates.nowMillis(), revision = old.revision + 1)
        check(members.update(updated) == 1)
        updated
    }

    /** Correct a data entry without recording a fictitious renewal. Existing audit snapshots stay intact. */
    suspend fun correctMember(id: String, expectedRevision: Long, input: MemberInput): MemberEntity = db.withTransaction {
        val old = requireNotNull(members.find(id)) { "Member not found" }
        check(old.revision == expectedRevision) { "Member changed; refresh before renewal" }
        val value = input.normalized()
        require(value.nationalCode == old.nationalCode || members.countByCode(value.nationalCode, id) == 0) { "National code already exists" }
        val updated = old.copy(nationalCode = value.nationalCode, firstName = value.firstName,
            lastName = value.lastName, fatherName = value.fatherName, expiryJalali = value.expiry.toIsoString(),
            expiryEpochDay = value.expiry.toEpochDay(), searchText = value.searchText(),
            updatedAtMillis = dates.nowMillis(), revision = old.revision + 1)
        check(members.update(updated) == 1)
        updated
    }

    fun suggestedRenewalExpiry(member: MemberEntity, onDate: JalaliDate = today()): JalaliDate =
        maxOf(member.expiryDate(), onDate).plusYears(1)

    /** Expected revision prevents duplicate taps and stale screen updates. */
    suspend fun renew(memberId: String, expectedRevision: Long, newExpiry: JalaliDate? = null): OperationReceipt = db.withTransaction {
        val old = requireNotNull(members.find(memberId)) { "Member not found" }
        check(old.revision == expectedRevision) { "Member changed; refresh before renewal" }
        require(old.status == MembershipStatus.ACTIVE) { "Inactive member cannot be renewed" }
        val date = today()
        val expiry = newExpiry ?: suggestedRenewalExpiry(old, date)
        require(expiry > date && expiry > old.expiryDate()) { "New expiry must be after today and previous expiry" }
        val now = dates.nowMillis()
        val updated = old.copy(expiryJalali = expiry.toIsoString(), expiryEpochDay = expiry.toEpochDay(),
            updatedAtMillis = now, revision = old.revision + 1)
        check(members.update(updated) == 1)
        val log = makeLog(updated, old, ActivityType.RENEWAL, date, now)
        logs.insert(log)
        OperationReceipt(memberId, log.id)
    }

    /** Undo only when no later modification exists. Retains a cancelled audit record. */
    suspend fun undo(activityId: String): UndoResult = db.withTransaction {
        val log = logs.find(activityId) ?: return@withTransaction UndoResult.NOT_FOUND
        if (log.isUndone) return@withTransaction UndoResult.ALREADY_UNDONE
        val member = members.find(log.memberId) ?: return@withTransaction UndoResult.CONFLICT
        if (member.revision != log.resultingRevision || member.expiryEpochDay != log.newExpiryEpochDay) {
            return@withTransaction UndoResult.CONFLICT
        }
        val now = dates.nowMillis()
        when (log.type) {
            ActivityType.RENEWAL -> {
                val previousDay = log.previousExpiryEpochDay ?: return@withTransaction UndoResult.CONFLICT
                val previousStatus = log.previousStatus ?: return@withTransaction UndoResult.CONFLICT
                val previous = JalaliDate.fromEpochDay(previousDay)
                check(members.update(member.copy(expiryJalali = previous.toIsoString(), expiryEpochDay = previousDay,
                    status = previousStatus, updatedAtMillis = now, revision = member.revision + 1)) == 1)
            }
            ActivityType.NEW_MEMBERSHIP -> check(members.delete(member) == 1)
            else -> return@withTransaction UndoResult.UNSUPPORTED
        }
        check(logs.markUndone(activityId, now) == 1)
        UndoResult.SUCCESS
    }

    /** Spreadsheet rows are existing records, not evidence of operations performed today. */
    suspend fun importMembers(rows: List<ImportRow>, mode: ImportMode): Int = db.withTransaction {
        require(rows.isNotEmpty()) { "فایل فاقد رکورد معتبر است." }
        val values = rows.map { it.copy(input = it.input.normalized()) }
        require(values.all { it.status in listOf(MembershipStatus.ACTIVE, MembershipStatus.INACTIVE) })
        if (mode == ImportMode.REPLACE) members.clearAll()
        val byCode = if (mode == ImportMode.MERGE) members.allForBackup().groupBy { it.nationalCode }.toMutableMap() else mutableMapOf()
        val now = dates.nowMillis()
        val inserts = linkedMapOf<String,MemberEntity>()
        for (row in values) {
            val v = row.input
            val matches = byCode[v.nationalCode].orEmpty()
            require(matches.size <= 1) { "کد ملی تکراری در بانک موجود است؛ ادغام مبهم است." }
            val old = matches.singleOrNull()
            if (old != null) {
                val updated = old.copy(firstName=v.firstName,lastName=v.lastName,fatherName=v.fatherName,expiryJalali=v.expiry.toIsoString(),expiryEpochDay=v.expiry.toEpochDay(),status=row.status,searchText=v.searchText())
                if (updated != old) {
                    val changed = updated.copy(updatedAtMillis=now,revision=old.revision+1)
                    if (old.id in inserts) inserts[old.id] = changed else check(members.update(changed) == 1)
                    byCode[v.nationalCode] = listOf(changed)
                }
            } else {
                val member = MemberEntity(UUID.randomUUID().toString(),v.nationalCode,v.firstName,v.lastName,v.fatherName,v.expiry.toIsoString(),v.expiry.toEpochDay(),row.status,v.searchText(),now,now)
                inserts[member.id] = member
                if (mode == ImportMode.MERGE) byCode[v.nationalCode] = listOf(member)
            }
        }
        if (inserts.isNotEmpty()) members.insertBatch(inserts.values.toList())
        values.size
    }

    suspend fun backup(): BackupData = db.withTransaction { BackupData(members.allForBackup(),logs.allForBackup()) }

    /** Merge by immutable IDs; refuse divergent snapshots rather than corrupting historical statistics. */
    suspend fun restore(data: BackupData, replace: Boolean): Pair<Int,Int> = db.withTransaction {
        if (replace) { logs.clearAll(); members.clearAll() }
        val oldMembers = members.allForBackup().associateBy { it.id }
        val oldLogs = logs.allForBackup().associateBy { it.id }
        val undoneIds = oldLogs.values.filter { it.type == ActivityType.NEW_MEMBERSHIP && it.isUndone }.map { it.memberId }.toSet()
        val newMembers = data.members.filter { m ->
            val old = oldMembers[m.id]
            require(old == null || old == m) { "پشتیبان با نسخه موجود عضو تعارض دارد؛ بازیابی جایگزین یا پشتیبان دیگری انتخاب کنید." }
            require(old != null || m.id !in undoneIds) { "عضو حذف‌شده در پشتیبان قدیمی وجود دارد." }
            old == null
        }
        val newLogs = data.logs.filter { l ->
            val old = oldLogs[l.id]
            require(old == null || old == l) { "تاریخچه پشتیبان با تاریخچه موجود تعارض دارد." }
            old == null
        }
        if (newMembers.isNotEmpty()) members.insertBatch(newMembers)
        if (newLogs.isNotEmpty()) logs.insertBatch(newLogs)
        newMembers.size to newLogs.size
    }

    private fun makeLog(member: MemberEntity, previous: MemberEntity?, type: String, date: JalaliDate, now: Long) = ActivityLogEntity(
        id = UUID.randomUUID().toString(), memberId = member.id, type = type, nationalCode = member.nationalCode,
        firstName = member.firstName, lastName = member.lastName, fatherName = member.fatherName,
        operationJalali = date.toIsoString(), operationEpochDay = date.toEpochDay(), operationYear = date.year,
        operationMonth = date.month, operationAtMillis = now, previousExpiryJalali = previous?.expiryJalali,
        previousExpiryEpochDay = previous?.expiryEpochDay, previousStatus = previous?.status,
        newExpiryJalali = member.expiryJalali, newExpiryEpochDay = member.expiryEpochDay, resultingRevision = member.revision
    )

    private fun validatePeriod(year: Int, month: Int?) { JalaliDate(year, month ?: 1, 1) }
    private fun yearRange(year: Int): Pair<Long, Long> = JalaliDate(year, 1, 1).toEpochDay() to
        JalaliDate(year, 12, JalaliDate.daysInMonth(year, 12)).toEpochDay()
}
