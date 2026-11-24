package ir.memberexpiry.manager.data.repository

import ir.memberexpiry.manager.core.date.JalaliDate
import ir.memberexpiry.manager.core.date.PersianText

data class MemberInput(val nationalCode: String, val firstName: String, val lastName: String, val fatherName: String, val expiry: JalaliDate) {
    internal fun normalized(): MemberInput = copy(
        nationalCode = PersianText.nationalCode(nationalCode),
        firstName = firstName.trim(), lastName = lastName.trim(), fatherName = fatherName.trim()
    ).also {
        require(it.firstName.isNotBlank() && it.lastName.isNotBlank()) { "First and last names are required" }
    }
    internal fun searchText(): String = PersianText.normalizeSearch("$nationalCode $firstName $lastName $fatherName")
}

data class OperationReceipt(val memberId: String, val activityId: String)

enum class UndoResult { SUCCESS, NOT_FOUND, ALREADY_UNDONE, CONFLICT, UNSUPPORTED }
