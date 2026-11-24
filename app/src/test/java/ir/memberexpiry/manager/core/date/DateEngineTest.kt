package ir.memberexpiry.manager.core.date

import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class DateEngineTest {
    @Test fun nowruzAndLeapYears() {
        assertEquals(LocalDate.of(2024, 3, 20), JalaliDate(1403, 1, 1).toGregorian())
        assertEquals(LocalDate.of(2025, 3, 20), JalaliDate(1403, 12, 30).toGregorian())
        assertEquals(LocalDate.of(2025, 3, 21), JalaliDate(1404, 1, 1).toGregorian())
        assertEquals(LocalDate.of(2026, 3, 21), JalaliDate(1405, 1, 1).toGregorian())
        assertTrue(JalaliDate.isLeapYear(1403))
        assertFalse(JalaliDate.isLeapYear(1404))
        assertEquals(JalaliDate(1404, 12, 29), JalaliDate(1403, 12, 30).plusYears(1))
        assertThrows(IllegalArgumentException::class.java) { JalaliDate(1404, 12, 30) }
    }
    @Test fun roundTripAllSupportedDays() {
        var day = JalaliDate(JalaliDate.MIN_YEAR, 1, 1).toEpochDay()
        val end = JalaliDate(JalaliDate.MAX_YEAR, 12, JalaliDate.daysInMonth(JalaliDate.MAX_YEAR, 12)).toEpochDay()
        while (day <= end) {
            val value = JalaliDate.fromEpochDay(day)
            assertEquals(day, value.toEpochDay())
            assertEquals(value, JalaliDate.parse(value.toPersianString()))
            day++
        }
    }
    @Test fun excelSerials() {
        assertEquals(LocalDate.of(1900, 2, 28), ExcelDates.toGregorian(59.75))
        assertThrows(IllegalArgumentException::class.java) { ExcelDates.toGregorian(60.0) }
        assertEquals(LocalDate.of(1900, 3, 1), ExcelDates.toGregorian(61.0))
        assertEquals(LocalDate.of(1904, 1, 1), ExcelDates.toGregorian(0.0, ExcelDateSystem.MAC_1904))
        val date = JalaliDate(1405, 9, 3)
        for (system in ExcelDateSystem.entries) {
            val serial = ExcelDates.fromJalali(date, system)
            assertEquals(date, ExcelDates.toJalali(BigDecimal.valueOf(serial).add(BigDecimal("0.75")), system))
        }
    }
    @Test fun digitsAndSystemDate() {
        assertEquals("0012345678", PersianText.nationalCode("۰۰۱۲۳۴۵۶۷۸"))
        assertEquals("علی کاظمی", PersianText.normalizeSearch("علي كاظمي"))
        assertEquals(JalaliDate(1405, 9, 3), JalaliDate.parse("۱۴۰۵/۹/۳"))
        val instant = Instant.parse("2025-03-20T21:00:00Z")
        assertEquals(JalaliDate(1404, 1, 1), DateProvider(Clock.fixed(instant, ZoneId.of("Asia/Tehran"))).today())
        assertEquals(JalaliDate(1403, 12, 30), DateProvider(Clock.fixed(instant, ZoneId.of("UTC"))).today())
    }
}
