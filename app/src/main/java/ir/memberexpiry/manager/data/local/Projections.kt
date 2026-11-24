package ir.memberexpiry.manager.data.local

data class DashboardCounts(
    val overdue: Int,
    val dueToday: Int,
    val next30Days: Int,
    val currentMonth: Int,
    val totalMembers: Int
) {
    val pendingRenewal: Int get() = overdue + dueToday
}

data class ActivityCounts(val renewals: Int, val newMemberships: Int, val total: Int)
data class MonthCount(val month: Int, val count: Int)
data class ExpiryOccurrence(
    val memberId: String,
    val nationalCode: String,
    val firstName: String,
    val lastName: String,
    val fatherName: String,
    val expiryJalali: String,
    val expiryEpochDay: Long,
    val wasRenewed: Boolean
)
