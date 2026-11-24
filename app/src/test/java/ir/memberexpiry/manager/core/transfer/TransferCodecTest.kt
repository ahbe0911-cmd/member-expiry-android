package ir.memberexpiry.manager.core.transfer
import ir.memberexpiry.manager.core.date.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
class TransferCodecTest {
    private val rows = listOf(MemberColumns.headers,listOf("0012345678","علی، رضا","محمدی\nکاظمی","رضا \"الف\"","1405/09/03","ACTIVE"))
    @Test fun csvRoundTrip() {
        val bytes=ByteArrayOutputStream().also { CsvCodec.write(it,rows) }.toByteArray()
        assertEquals(rows,CsvCodec.read(bytes.inputStream()).rows)
        assertEquals("0012345678",MemberColumns.detect(CsvCodec.read(bytes.inputStream())).rows.single().input.nationalCode)
    }
    @Test fun xlsxRoundTrip() {
        val bytes=ByteArrayOutputStream().also { XlsxCodec.write(it,rows) }.toByteArray()
        assertEquals(rows,XlsxCodec.read(bytes.inputStream()).single().rows)
    }
    @Test fun dateAndInvalidRows() {
        val date=JalaliDate(1405,9,3)
        val sheet=Sheet("A",listOf(MemberColumns.headers,listOf("۱۲۳","علی","رضایی","",ExcelDates.fromJalali(date,ExcelDateSystem.MAC_1904).toString()),listOf("x","","","","bad")),ExcelDateSystem.MAC_1904)
        val result=MemberColumns.detect(sheet)
        assertEquals(1,result.errorCount); assertEquals("0000000123",result.rows.single().input.nationalCode); assertEquals(date,result.rows.single().input.expiry)
        assertEquals(date,MemberColumns.parseDate("2026-11-24",ExcelDateSystem.WINDOWS_1900))
    }
    @Test fun formulaGuardAndMalformedCsv() {
        val bytes=ByteArrayOutputStream().also { CsvCodec.write(it,listOf(listOf("=2+2","@cmd"))) }.toByteArray()
        assertEquals(listOf("'=2+2","'@cmd"),CsvCodec.read(bytes.inputStream()).rows.single())
        assertTrue(runCatching { CsvCodec.read("a,\"unterminated".byteInputStream()) }.isFailure)
    }
    @Test fun rejectDtd() {
        val out=ByteArrayOutputStream(); ZipOutputStream(out).use { z -> z.putNextEntry(ZipEntry("xl/workbook.xml"));z.write("<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><x>&e;</x>".toByteArray());z.closeEntry() }
        assertTrue(runCatching { XlsxCodec.read(out.toByteArray().inputStream()) }.isFailure)
    }
}
