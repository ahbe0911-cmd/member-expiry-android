package ir.memberexpiry.manager.core.date

import java.time.LocalDate

/** Civil Persian calendar, arithmetic 33-year cycle, supported years 1304–1468. */
data class JalaliDate(val year: Int, val month: Int, val day: Int) : Comparable<JalaliDate> {
    init {
        require(year in MIN_YEAR..MAX_YEAR) { "Unsupported Persian year: $year" }
        require(month in 1..12) { "Invalid Persian month: $month" }
        require(day in 1..daysInMonth(year, month)) { "Invalid Persian day: $day" }
    }

    fun toEpochDay(): Long = startOfYear(year) + monthOffset(month) + day - 1
    fun toGregorian(): LocalDate = LocalDate.ofEpochDay(toEpochDay())
    fun plusDays(days: Long): JalaliDate = fromGregorian(toGregorian().plusDays(days))
    fun plusYears(years: Int): JalaliDate {
        val target = Math.addExact(year, years)
        return JalaliDate(target, month, minOf(day, daysInMonth(target, month)))
    }
    fun firstOfMonth(): JalaliDate = JalaliDate(year, month, 1)
    fun lastOfMonth(): JalaliDate = JalaliDate(year, month, daysInMonth(year, month))
    fun toIsoString(): String = "${year.toString().padStart(4, '0')}/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}"
    fun toPersianString(): String = PersianText.toPersianDigits(toIsoString())
    override fun toString(): String = toIsoString()
    override fun compareTo(other: JalaliDate): Int = toEpochDay().compareTo(other.toEpochDay())

    companion object {
        const val MIN_YEAR = 1304
        const val MAX_YEAR = 1468
        private val anchor = LocalDate.of(2025, 3, 21).toEpochDay()
        private fun yearOffset(year: Int): Long = 365L * (year - 1) + (8L * year + 21) / 33
        private fun startOfYear(year: Int): Long = anchor + yearOffset(year) - yearOffset(1404)
        private fun monthOffset(month: Int): Int = if (month <= 7) (month - 1) * 31 else 186 + (month - 7) * 30

        fun isLeapYear(year: Int): Boolean {
            require(year in MIN_YEAR..MAX_YEAR)
            return startOfYear(year + 1) - startOfYear(year) == 366L
        }
        fun daysInMonth(year: Int, month: Int): Int {
            require(year in MIN_YEAR..MAX_YEAR && month in 1..12)
            return when (month) { in 1..6 -> 31; in 7..11 -> 30; else -> if (isLeapYear(year)) 30 else 29 }
        }
        fun fromEpochDay(epochDay: Long): JalaliDate {
            require(epochDay >= startOfYear(MIN_YEAR) && epochDay < startOfYear(MAX_YEAR + 1)) {
                "Date outside supported Persian calendar range"
            }
            var low = MIN_YEAR
            var high = MAX_YEAR
            while (low < high) {
                val mid = (low + high + 1) / 2
                if (startOfYear(mid) <= epochDay) low = mid else high = mid - 1
            }
            val offset = (epochDay - startOfYear(low)).toInt()
            val month = if (offset < 186) offset / 31 + 1 else (offset - 186) / 30 + 7
            return JalaliDate(low, month, offset - monthOffset(month) + 1)
        }
        fun fromGregorian(date: LocalDate): JalaliDate = fromEpochDay(date.toEpochDay())
        fun parse(text: String): JalaliDate {
            val normalized = PersianText.toAsciiDigits(text).trim()
            val match = Regex("([0-9]{4})[/.-]([0-9]{1,2})[/.-]([0-9]{1,2})").matchEntire(normalized)
                ?: throw IllegalArgumentException("Expected Persian date yyyy/MM/dd")
            val (y, m, d) = match.destructured
            return JalaliDate(y.toInt(), m.toInt(), d.toInt())
        }
    }
}
