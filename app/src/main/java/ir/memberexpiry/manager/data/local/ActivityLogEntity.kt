package ir.memberexpiry.manager.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object ActivityType {
    const val RENEWAL = "RENEWAL"
    const val NEW_MEMBERSHIP = "NEW_MEMBERSHIP"
}

/** No cascading foreign key: historical snapshots survive future member replacement. */
@Entity(tableName = "activity_logs", indices = [
    Index(value = ["isUndone", "operationYear", "operationMonth"]),
    Index(value = ["memberId", "resultingRevision"]),
    Index(value = ["isUndone", "previousExpiryEpochDay"])
])
data class ActivityLogEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val type: String,
    val nationalCode: String,
    val firstName: String,
    val lastName: String,
    val fatherName: String,
    val operationJalali: String,
    val operationEpochDay: Long,
    val operationYear: Int,
    val operationMonth: Int,
    val operationAtMillis: Long,
    val previousExpiryJalali: String?,
    val previousExpiryEpochDay: Long?,
    val previousStatus: String?,
    val newExpiryJalali: String,
    val newExpiryEpochDay: Long,
    val resultingRevision: Long,
    val isUndone: Boolean = false,
    val undoneAtMillis: Long? = null
)
