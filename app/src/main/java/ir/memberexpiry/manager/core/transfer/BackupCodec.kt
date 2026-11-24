package ir.memberexpiry.manager.core.transfer

import ir.memberexpiry.manager.core.date.JalaliDate
import ir.memberexpiry.manager.core.date.PersianText
import ir.memberexpiry.manager.data.local.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

data class BackupData(val members: List<MemberEntity>, val logs: List<ActivityLogEntity>)
object BackupCodec {
    private fun obj(vararg values: Pair<String, Any?>) = JSONObject().apply { values.forEach { (k,v) -> put(k,v ?: JSONObject.NULL) } }
    private fun JSONObject.optionalString(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.optionalLong(key: String): Long? = if (isNull(key)) null else getLong(key)
    private fun JSONObject.text(key: String): String = getString(key).also { require(it.length <= MAX_CELL) }
    fun write(output: OutputStream, data: BackupData) {
        require(data.members.size <= 50000 && data.logs.size <= 200000) { "تعداد رکوردهای پشتیبان بیش از حد مجاز است." }
        val members = JSONArray(); val logs = JSONArray()
        data.members.forEach { m -> members.put(obj("id" to m.id,"nationalCode" to m.nationalCode,"firstName" to m.firstName,"lastName" to m.lastName,"fatherName" to m.fatherName,"expiryJalali" to m.expiryJalali,"expiryEpochDay" to m.expiryEpochDay,"status" to m.status,"searchText" to m.searchText,"createdAtMillis" to m.createdAtMillis,"updatedAtMillis" to m.updatedAtMillis,"revision" to m.revision)) }
        data.logs.forEach { l -> logs.put(obj("id" to l.id,"memberId" to l.memberId,"type" to l.type,"nationalCode" to l.nationalCode,"firstName" to l.firstName,"lastName" to l.lastName,"fatherName" to l.fatherName,"operationJalali" to l.operationJalali,"operationEpochDay" to l.operationEpochDay,"operationYear" to l.operationYear,"operationMonth" to l.operationMonth,"operationAtMillis" to l.operationAtMillis,"previousExpiryJalali" to l.previousExpiryJalali,"previousExpiryEpochDay" to l.previousExpiryEpochDay,"previousStatus" to l.previousStatus,"newExpiryJalali" to l.newExpiryJalali,"newExpiryEpochDay" to l.newExpiryEpochDay,"resultingRevision" to l.resultingRevision,"isUndone" to l.isUndone,"undoneAtMillis" to l.undoneAtMillis)) }
        val bytes = obj("format" to "member-expiry-backup","version" to 1,"members" to members,"activity_logs" to logs).toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_FILE_BYTES) { "حجم پشتیبان بیش از حد مجاز است." }; output.write(bytes)
    }
    fun read(input: InputStream): BackupData {
        val root = JSONObject(input.readBounded().toString(Charsets.UTF_8).removePrefix("\uFEFF"))
        require(root.getString("format") == "member-expiry-backup" && root.getInt("version") == 1) { "نسخه پشتیبان نامعتبر است." }
        val ma = root.getJSONArray("members"); val la = root.getJSONArray("activity_logs")
        require(ma.length() <= 50000 && la.length() <= 200000)
        val members = (0 until ma.length()).map { i -> ma.getJSONObject(i).run {
            val code = PersianText.nationalCode(text("nationalCode")); val first = text("firstName"); val last = text("lastName"); val father = text("fatherName")
            require(first.isNotBlank() && last.isNotBlank())
            val m = MemberEntity(text("id"),code,first,last,father,text("expiryJalali"),getLong("expiryEpochDay"),text("status"),PersianText.normalizeSearch("$code $first $last $father"),getLong("createdAtMillis"),getLong("updatedAtMillis"),getLong("revision"))
            require(m.id.isNotBlank() && m.revision >= 0 && m.revision < Long.MAX_VALUE && m.status in listOf("ACTIVE","INACTIVE")); verifyDate(m.expiryJalali,m.expiryEpochDay); m
        } }
        val logs = (0 until la.length()).map { i -> la.getJSONObject(i).run {
            val l = ActivityLogEntity(text("id"),text("memberId"),text("type"),PersianText.nationalCode(text("nationalCode")),text("firstName"),text("lastName"),text("fatherName"),text("operationJalali"),getLong("operationEpochDay"),getInt("operationYear"),getInt("operationMonth"),getLong("operationAtMillis"),optionalString("previousExpiryJalali"),optionalLong("previousExpiryEpochDay"),optionalString("previousStatus"),text("newExpiryJalali"),getLong("newExpiryEpochDay"),getLong("resultingRevision"),getBoolean("isUndone"),optionalLong("undoneAtMillis"))
            require(l.id.isNotBlank() && l.memberId.isNotBlank() && l.resultingRevision >= 0 && l.resultingRevision < Long.MAX_VALUE && l.isUndone == (l.undoneAtMillis != null))
            val date = verifyDate(l.operationJalali,l.operationEpochDay); require(date.year == l.operationYear && date.month == l.operationMonth)
            verifyDate(l.newExpiryJalali,l.newExpiryEpochDay)
            when(l.type) {
                ActivityType.RENEWAL -> { verifyDate(requireNotNull(l.previousExpiryJalali),requireNotNull(l.previousExpiryEpochDay)); require(l.previousStatus in listOf("ACTIVE","INACTIVE") && l.newExpiryEpochDay > l.previousExpiryEpochDay) }
                ActivityType.NEW_MEMBERSHIP -> require(l.previousExpiryJalali == null && l.previousExpiryEpochDay == null && l.previousStatus == null)
                else -> error("نوع عملیات نامعتبر است.")
            }; l
        } }
        require(members.map { it.id }.toSet().size == members.size && logs.map { it.id }.toSet().size == logs.size) { "شناسه تکراری در پشتیبان." }
        return BackupData(members,logs)
    }
    private fun verifyDate(s: String, epoch: Long): JalaliDate = JalaliDate.parse(s).also { require(it.toIsoString() == s && it.toEpochDay() == epoch) { "تاریخ پشتیبان ناسازگار است." } }
}
