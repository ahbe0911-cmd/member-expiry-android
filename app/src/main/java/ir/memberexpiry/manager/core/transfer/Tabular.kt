package ir.memberexpiry.manager.core.transfer

import ir.memberexpiry.manager.core.date.*
import ir.memberexpiry.manager.data.repository.MemberInput
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

const val MAX_FILE_BYTES = 32 * 1024 * 1024
const val MAX_ROWS = 50001
const val MAX_COLUMNS = 128
const val MAX_CELL = 32768

data class Sheet(val name: String, val rows: List<List<String>>, val dateSystem: ExcelDateSystem = ExcelDateSystem.WINDOWS_1900)
data class ImportRow(val input: MemberInput, val status: String = "ACTIVE")
data class ImportPreview(val rows: List<ImportRow>, val errors: List<String>, val errorCount: Int)
enum class ImportMode { MERGE, APPEND, REPLACE }
enum class TransferFormat(val extension: String, val mime: String) {
    CSV("csv", "text/csv"), XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), JSON("json", "application/json")
}
fun InputStream.readBounded(limit: Int = MAX_FILE_BYTES): ByteArray {
    val result = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val n = read(buffer)
        if (n < 0) break
        require(result.size() + n <= limit) { "حجم فایل بیش از حد مجاز است." }
        result.write(buffer, 0, n)
    }
    return result.toByteArray()
}
object CsvCodec {
    fun read(input: InputStream): Sheet {
        val bytes = input.readBounded()
        val text = when {
            bytes.size >= 2 && bytes[0] == 0xff.toByte() && bytes[1] == 0xfe.toByte() -> bytes.toString(Charsets.UTF_16LE).removePrefix("\uFEFF")
            bytes.size >= 2 && bytes[0] == 0xfe.toByte() && bytes[1] == 0xff.toByte() -> bytes.toString(Charsets.UTF_16BE).removePrefix("\uFEFF")
            else -> Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
        }
        val candidates = listOf(',', ';', '\t')
        val counts = IntArray(3)
        var quoted = false
        for (c in text) {
            if (c == '"') quoted = !quoted
            else if (!quoted && (c == '\r' || c == '\n')) break
            else if (!quoted && c in candidates) counts[candidates.indexOf(c)]++
        }
        val delimiter = candidates[counts.indices.maxByOrNull { counts[it] } ?: 0]
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var closed = false
        fun endCell() { require(row.size < MAX_COLUMNS); row.add(cell.toString()); cell.clear(); closed = false }
        fun endRow() { endCell(); require(rows.size < MAX_ROWS); rows.add(row.toList()); row.clear() }
        var i = 0
        while (i < text.length) {
            val c = text[i++]
            if (inQuotes) {
                if (c == '"') {
                    if (i < text.length && text[i] == '"') { cell.append('"'); i++ } else { inQuotes = false; closed = true }
                } else cell.append(c)
            } else when (c) {
                delimiter -> endCell()
                '\r', '\n' -> { if (c == '\r' && i < text.length && text[i] == '\n') i++; endRow() }
                '"' -> { require(cell.isEmpty() && !closed) { "ساختار CSV نامعتبر است." }; inQuotes = true }
                else -> { require(!closed) { "ساختار CSV نامعتبر است." }; cell.append(c) }
            }
            require(cell.length <= MAX_CELL)
        }
        require(!inQuotes) { "نقل‌قول CSV بسته نشده است." }
        if (row.isNotEmpty() || cell.isNotEmpty() || closed) endRow()
        return Sheet("CSV", rows)
    }
    // Guard spreadsheet formula injection. National codes are written verbatim; Excel may infer numeric types in CSV.
    fun write(output: OutputStream, rows: List<List<String>>) {
        output.write(byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()))
        val writer = output.bufferedWriter(Charsets.UTF_8)
        for (row in rows) {
            writer.write(row.joinToString(",") { original ->
                val safe = if (original.trimStart().firstOrNull() in listOf('=', '+', '-', '@') || original.firstOrNull() in listOf('\t','\r','\n')) "'$original" else original
                "\"${safe.replace("\"", "\"\"")}\""
            }); writer.write("\r\n")
        }
        writer.flush()
    }
}
object MemberColumns {
    val headers = listOf("کد ملی", "نام", "نام خانوادگی", "نام پدر", "تاریخ انقضا", "وضعیت")
    private val aliases = listOf(
        listOf("کد ملی","کدملی","شماره ملی","nationalcode","nationalid","national code","national id","کد ملی عضو"),
        listOf("نام","نام عضو","first name","firstname","name"),
        listOf("نام خانوادگی","نامخانوادگی","فامیل","last name","lastname","surname","family name"),
        listOf("نام پدر","نامپدر","پدر","father name","fathername"),
        listOf("تاریخ انقضا","تاریخ انقضاء","تاریخ پایان عضویت","تاریخ اعتبار","اعتبار تا","انقضا","expiry","expiry date","expiryjalali","expiration date","تاریخ انقضای عضویت","تاریخ اتمام عضویت","تاریخ انقضا شمسی","تاریخ انقضای شمسی"),
        listOf("وضعیت","status","وضعیت عضویت")
    ).map { group -> group.map(::key).toSet() }
    private fun key(s: String) = PersianText.normalizeSearch(s).replace(Regex("[\\s_\\-]+"), "")
    fun detect(sheet: Sheet): ImportPreview {
        val headerIndex = sheet.rows.take(30).indexOfFirst { row ->
            listOf(0,1,2,4).all { idx -> row.any { key(it) in aliases[idx] } }
        }
        require(headerIndex >= 0) { "ستون‌های کد ملی، نام، نام خانوادگی و تاریخ انقضا پیدا نشد." }
        val header = sheet.rows[headerIndex]
        val columns = aliases.map { a -> header.indexOfFirst { key(it) in a } }
        val results = mutableListOf<ImportRow>(); val errors = mutableListOf<String>(); var errorCount = 0
        for (i in headerIndex + 1 until sheet.rows.size) {
            val row = sheet.rows[i]
            if (row.all { it.isBlank() }) continue
            fun field(index: Int) = row.getOrNull(columns[index]).orEmpty().trim()
            try {
                val code = PersianText.toAsciiDigits(field(0))
                // Numeric Excel cells can lose leading zeroes; restore them only for exact nonnegative integers.
                val normalized = if (code.matches(Regex("[0-9]{1,10}"))) code.padStart(10,'0') else {
                    require(code.length <= 40)
                    val decimal = code.toBigDecimal()
                    require(decimal >= java.math.BigDecimal.ZERO && decimal <= java.math.BigDecimal("9999999999") && decimal.scale() in -20..40)
                    val number = decimal.toBigIntegerExact().toString()
                    require(number.matches(Regex("[0-9]{1,10}"))); number.padStart(10,'0')
                }
                val status = when (key(field(5))) { "", "active", "فعال", "منقضی", "معوق", "تمدیدنشده" -> "ACTIVE"; "inactive", "غیرفعال", "غیر فعال" -> "INACTIVE"; else -> error("وضعیت نامعتبر") }
                val value = MemberInput(normalized, field(1), field(2), field(3), parseDate(field(4),sheet.dateSystem)).normalized()
                results.add(ImportRow(value,status))
            } catch (_: Exception) { errorCount++; if (errors.size < 20) errors.add("ردیف ${i+1}: کد ملی، نام، وضعیت یا تاریخ نامعتبر است.") }
        }
        return ImportPreview(results,errors,errorCount)
    }
    fun parseDate(value: String, system: ExcelDateSystem): JalaliDate {
        val s = PersianText.toAsciiDigits(value).trim()
        val match = Regex("([0-9]{4})[/.-]([0-9]{1,2})[/.-]([0-9]{1,2})(?:[ T].*)?").matchEntire(s)
        if (match != null) {
            val (y,m,d) = match.destructured
            return if (y.toInt() in JalaliDate.MIN_YEAR..JalaliDate.MAX_YEAR) JalaliDate(y.toInt(),m.toInt(),d.toInt())
            else JalaliDate.fromGregorian(LocalDate.of(y.toInt(),m.toInt(),d.toInt()))
        }
        return ExcelDates.toJalali(s.toBigDecimal(),system)
    }
}
