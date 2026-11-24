package ir.memberexpiry.manager.core.transfer

import ir.memberexpiry.manager.data.local.*

object Reports {
    fun members(rows: List<MemberEntity>): List<List<String>> = listOf(MemberColumns.headers) + rows.map { listOf(it.nationalCode,it.firstName,it.lastName,it.fatherName,it.expiryJalali,it.status) }
    fun activities(rows: List<ActivityLogEntity>): List<List<String>> = listOf(listOf("کد ملی","نام","نام خانوادگی","نام پدر","نوع عملیات","تاریخ عملیات","انقضای قبلی","انقضای جدید")) + rows.map { listOf(it.nationalCode,it.firstName,it.lastName,it.fatherName,if(it.type==ActivityType.RENEWAL) "تمدید" else "عضویت جدید",it.operationJalali,it.previousExpiryJalali.orEmpty(),it.newExpiryJalali) }
    fun history(rows: List<ExpiryOccurrence>): List<List<String>> = listOf(listOf("کد ملی","نام","نام خانوادگی","نام پدر","تاریخ انقضا","وضعیت تمدید")) + rows.map { listOf(it.nationalCode,it.firstName,it.lastName,it.fatherName,it.expiryJalali,if(it.wasRenewed) "تمدیدشده" else "تمدیدنشده") }
}
