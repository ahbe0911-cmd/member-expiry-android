package ir.memberexpiry.manager.core.date

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

enum class ExcelDateSystem { WINDOWS_1900, MAC_1904 }

object ExcelDates {
    private val windowsBase = LocalDate.of(1899, 12, 31)
    private val macBase = LocalDate.of(1904, 1, 1)
    private val lastExcelDate = LocalDate.of(9999, 12, 31)

    /** Time fractions are discarded; fictitious 1900-02-29 is explicitly rejected. */
    fun toGregorian(serial: BigDecimal, system: ExcelDateSystem = ExcelDateSystem.WINDOWS_1900): LocalDate {
        require(serial.signum() >= 0 && serial < BigDecimal("3000000")) { "Invalid Excel serial" }
        val whole = serial.setScale(0, RoundingMode.FLOOR).longValueExact()
        val date = when (system) {
            ExcelDateSystem.WINDOWS_1900 -> {
                require(whole != 60L) { "Excel serial 60 is the nonexistent 1900-02-29" }
                windowsBase.plusDays(if (whole > 60L) whole - 1 else whole)
            }
            ExcelDateSystem.MAC_1904 -> macBase.plusDays(whole)
        }
        require(!date.isAfter(lastExcelDate)) { "Excel date exceeds 9999-12-31" }
        return date
    }
    fun toGregorian(serial: Double, system: ExcelDateSystem = ExcelDateSystem.WINDOWS_1900): LocalDate {
        require(serial.isFinite())
        return toGregorian(BigDecimal.valueOf(serial), system)
    }
    fun toJalali(serial: BigDecimal, system: ExcelDateSystem = ExcelDateSystem.WINDOWS_1900): JalaliDate =
        JalaliDate.fromGregorian(toGregorian(serial, system))
    fun fromGregorian(date: LocalDate, system: ExcelDateSystem = ExcelDateSystem.WINDOWS_1900): Long {
        val base = if (system == ExcelDateSystem.WINDOWS_1900) windowsBase else macBase
        require(!date.isBefore(base) && !date.isAfter(lastExcelDate)) { "Date outside Excel range" }
        val days = date.toEpochDay() - base.toEpochDay()
        return if (system == ExcelDateSystem.WINDOWS_1900 && date >= LocalDate.of(1900, 3, 1)) days + 1 else days
    }
    fun fromJalali(date: JalaliDate, system: ExcelDateSystem = ExcelDateSystem.WINDOWS_1900): Long =
        fromGregorian(date.toGregorian(), system)
}
