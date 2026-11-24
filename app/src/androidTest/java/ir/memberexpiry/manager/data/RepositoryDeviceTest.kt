package ir.memberexpiry.manager.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import ir.memberexpiry.manager.core.date.DateProvider
import ir.memberexpiry.manager.core.date.JalaliDate
import ir.memberexpiry.manager.data.local.MemberDatabase
import ir.memberexpiry.manager.data.repository.MemberInput
import ir.memberexpiry.manager.data.repository.MemberRepository
import ir.memberexpiry.manager.data.repository.UndoResult
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RepositoryDeviceTest {
    private lateinit var db: MemberDatabase
    private lateinit var repository: MemberRepository
    private val today = JalaliDate(1405, 9, 3)

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, MemberDatabase::class.java).build()
        repository = MemberRepository(db, DateProvider(Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneId.of("Asia/Tehran"))))
    }
    @After fun cleanup() { db.close() }

    @Test fun renewalCarryoverHistoryAndUndo() = runBlocking {
        assertEquals(today, repository.today())
        val oldExpiry = JalaliDate(1405, 8, 20)
        val receipt = repository.addMember(MemberInput("۰۰۱۲۳۴۵۶۷۸", "علی", "کاظمی", "رضا", oldExpiry))
        assertEquals(1, repository.observePending().first().size)
        assertEquals(1, repository.observeCurrentQueue().first().size)
        assertEquals(0, repository.observeDashboard().first().currentMonth)
        assertEquals(1, repository.observeActivityCounts(1405, 9).first().newMemberships)
        assertEquals(1, repository.observeMembers("علي").first().size)
        val old = requireNotNull(repository.findMember(receipt.memberId))
        val renewed = repository.renew(old.id, old.revision)
        assertEquals(0, repository.observePending().first().size)
        assertEquals(0, repository.observeCurrentQueue().first().size)
        assertEquals(JalaliDate(1406, 9, 3), repository.findMember(old.id)?.expiryDate())
        assertEquals(2, repository.observeActivityCounts(1405).first().total)
        assertTrue(repository.observeExpiredHistory(1405, 8).first().single().wasRenewed)
        assertTrue(runCatching { repository.renew(old.id, old.revision) }.isFailure)
        assertEquals(UndoResult.SUCCESS, repository.undo(renewed.activityId))
        assertEquals(UndoResult.ALREADY_UNDONE, repository.undo(renewed.activityId))
        assertEquals(oldExpiry, repository.findMember(old.id)?.expiryDate())
        assertFalse(repository.observeExpiredHistory(1405, 8).first().single().wasRenewed)
        assertEquals(0, repository.observeActivityCounts(1405).first().renewals)
        assertEquals(1, repository.observeCurrentQueue().first().size)
    }

    @Test fun duplicateAndUndoNewMembership() = runBlocking {
        val input = MemberInput("0000000001", "مریم", "احمدی", "علی", today)
        val receipt = repository.addMember(input)
        assertTrue(runCatching { repository.addMember(input) }.isFailure)
        assertEquals(1, repository.observeDueToday().first().size)
        assertEquals(UndoResult.SUCCESS, repository.undo(receipt.activityId))
        assertEquals(0, repository.observeMembers().first().size)
        assertEquals(0, repository.observeActivityCounts(1405).first().total)
    }

    @Test fun earlyRenewalAndLaterEditConflict() = runBlocking {
        val expiry = today.plusDays(10)
        val receipt = repository.addMember(MemberInput("0000000002", "مریم", "احمدی", "علی", expiry))
        assertEquals(1, repository.observeNext30Days().first().size)
        val member = requireNotNull(repository.findMember(receipt.memberId))
        val renewal = repository.renew(member.id, member.revision)
        assertEquals(0, repository.observeExpiredHistory(1405, 9).first().size)
        repository.editIdentity(member.id, member.nationalCode, "مریم", "حسینی", "علی")
        assertEquals(UndoResult.CONFLICT, repository.undo(renewal.activityId))
        assertEquals(12, repository.observeYearMonths(1406).first().size)
    }
    @Test fun notificationSummaryAndCenterTrackRenewal() = runBlocking {
        val due = repository.addMember(MemberInput("0000000003", "علی", "کریمی", "رضا", today))
        repository.addMember(MemberInput("0000000004", "مریم", "احمدی", "علی", today.plusDays(-40)))
        repository.addMember(MemberInput("0000000005", "رضا", "کاظمی", "علی", today.plusDays(1)))
        val before = repository.dailyExpirySummary()
        assertEquals(today, before.date)
        assertEquals(1, before.counts.dueToday)
        assertEquals(1, before.counts.overdue)
        assertEquals(2, repository.observePendingWithDate().first().second.size)
        val member = requireNotNull(repository.findMember(due.memberId))
        val receipt = repository.renew(member.id, member.revision)
        assertEquals(1, repository.dailyExpirySummary().counts.total)
        assertEquals(UndoResult.SUCCESS, repository.undo(receipt.activityId))
        assertEquals(2, repository.dailyExpirySummary().counts.total)
    }
    @Test fun correctionIsNotRenewalAndRejectsStaleRevision() = runBlocking {
        val receipt = repository.addMember(MemberInput("0000000006", "علی", "کریمی", "رضا", today))
        val old = requireNotNull(repository.findMember(receipt.memberId))
        val changed = MemberInput(old.nationalCode, old.firstName, "احمدی", old.fatherName, today.plusDays(20))
        repository.correctMember(old.id, old.revision, changed)
        assertEquals(today.plusDays(20), repository.findMember(old.id)?.expiryDate())
        assertEquals(1, repository.observeActivityCounts(1405, 9).first().newMemberships)
        assertEquals(0, repository.observeActivityCounts(1405, 9).first().renewals)
        assertTrue(runCatching { repository.correctMember(old.id, old.revision, changed) }.isFailure)
        assertEquals(UndoResult.CONFLICT, repository.undo(receipt.activityId))
    }
}
