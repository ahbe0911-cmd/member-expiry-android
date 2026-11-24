package ir.memberexpiry.manager.notifications

import ir.memberexpiry.manager.core.date.JalaliDate

data class ExpiryCounts(val dueToday: Int, val overdue: Int) {
    init { require(dueToday >= 0 && overdue >= 0) }
    val total: Int get() = dueToday + overdue
}

data class DailyExpirySummary(val date: JalaliDate, val counts: ExpiryCounts)

internal enum class Delivery { CANCEL, SKIP, POST }
internal fun decideDelivery(counts: ExpiryCounts, allowed: Boolean, lastKey: String?, key: String): Delivery = when {
    counts.total == 0 -> Delivery.CANCEL
    !allowed || lastKey == key -> Delivery.SKIP
    else -> Delivery.POST
}
