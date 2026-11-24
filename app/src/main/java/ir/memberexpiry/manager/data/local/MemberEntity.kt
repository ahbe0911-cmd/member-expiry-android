package ir.memberexpiry.manager.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ir.memberexpiry.manager.core.date.JalaliDate

object MembershipStatus {
    const val ACTIVE = "ACTIVE"
    const val INACTIVE = "INACTIVE"
}

enum class ExpiryState { ACTIVE, DUE_TODAY, OVERDUE, INACTIVE }

@Entity(tableName = "members", indices = [Index(value = ["nationalCode"]), Index(value = ["status", "expiryEpochDay"])])
data class MemberEntity(
    @PrimaryKey val id: String,
    val nationalCode: String,
    val firstName: String,
    val lastName: String,
    val fatherName: String,
    val expiryJalali: String,
    val expiryEpochDay: Long,
    val status: String = MembershipStatus.ACTIVE,
    val searchText: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val revision: Long = 0
) {
    fun expiryDate(): JalaliDate = JalaliDate.fromEpochDay(expiryEpochDay)
    fun expiryState(today: JalaliDate): ExpiryState = when {
        status != MembershipStatus.ACTIVE -> ExpiryState.INACTIVE
        expiryEpochDay < today.toEpochDay() -> ExpiryState.OVERDUE
        expiryEpochDay == today.toEpochDay() -> ExpiryState.DUE_TODAY
        else -> ExpiryState.ACTIVE
    }
}
