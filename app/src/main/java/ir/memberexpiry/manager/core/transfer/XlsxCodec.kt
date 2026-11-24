package ir.memberexpiry.manager.core.transfer

import ir.memberexpiry.manager.core.date.ExcelDateSystem
import java.io.*
import java.util.zip.*
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Minimal OOXML reader/writer: no Excel runtime, formulas are never evaluated. */
object XlsxCodec {
    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private fun elements(root: Element, name: String): List<Element> {
        val nodes = root.getElementsByTagNameNS("*",name)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }
    private fun xml(bytes: ByteArray): Element {
        // OOXML is normally UTF-8. Reject DTD and NUL (including UTF-16) before parsing untrusted XML.
        require(bytes.none { it == 0.toByte() }) { "XML encoding not supported" }
        val value = Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
        require(!value.contains("<!DOCTYPE",true) && !value.contains("<!ENTITY",true)) { "Unsafe XML" }
        return DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(org.xml.sax.InputSource(StringReader(value))).documentElement
    }
    fun read(input: InputStream): List<Sheet> {
        val files = mutableMapOf<String,ByteArray>(); var total = 0; var entries = 0
        ZipInputStream(ByteArrayInputStream(input.readBounded())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(++entries <= 3000)
                if (!entry.isDirectory) {
                    val data = zip.readBounded(MAX_FILE_BYTES)
                    total += data.size; require(total <= 64 * 1024 * 1024) { "فایل اکسل پس از بازکردن بیش از حد بزرگ است." }
                    require(files.put(entry.name,data) == null) { "Duplicate ZIP entry" }
                }
                zip.closeEntry()
            }
        }
        fun doc(path: String) = xml(requireNotNull(files[path]) { "فایل اکسل استاندارد نیست." })
        val book = doc("xl/workbook.xml")
        val system = if (elements(book,"workbookPr").firstOrNull()?.getAttribute("date1904") in listOf("1","true")) ExcelDateSystem.MAC_1904 else ExcelDateSystem.WINDOWS_1900
        val relationships = elements(doc("xl/_rels/workbook.xml.rels"),"Relationship").filter { it.getAttribute("TargetMode") != "External" }.associate { it.getAttribute("Id") to it.getAttribute("Target") }
        val shared = files["xl/sharedStrings.xml"]?.let { elements(xml(it),"si").map { si -> elements(si,"t").joinToString("") { it.textContent } } }.orEmpty()
        val sheets = elements(book,"sheet"); require(sheets.size in 1..50)
        var totalRows = 0
        return sheets.map { sheet ->
            val target = requireNotNull(relationships[sheet.getAttributeNS(REL,"id")])
            val path = if (target.startsWith('/')) target.removePrefix("/") else "xl/$target"
            require(path.split('/').none { it == ".." })
            val data = elements(doc(path),"row"); totalRows += data.size; require(totalRows <= MAX_ROWS)
            val rows = data.map { row ->
                val cells = mutableListOf<String>()
                for (c in elements(row,"c")) {
                    val ref = c.getAttribute("r").takeWhile { it in 'A'..'Z' }
                    val col = if (ref.isEmpty()) cells.size else ref.fold(0) { acc,ch -> Math.addExact(Math.multiplyExact(acc,26),ch-'A'+1) }-1
                    require(col in 0 until MAX_COLUMNS)
                    while (cells.size <= col) cells.add("")
                    val v = elements(c,"v").firstOrNull()?.textContent.orEmpty()
                    val value = when (c.getAttribute("t")) {
                        "s" -> shared[v.toInt()]
                        "inlineStr" -> elements(c,"t").joinToString("") { it.textContent }
                        "e" -> "#ERROR"
                        else -> v
                    }
                    require(value.length <= MAX_CELL); cells[col] = value
                }
                cells.toList()
            }
            Sheet(sheet.getAttribute("name"), rows,system)
        }
    }
    private fun escape(s: String): String = buildString {
        for (c in s) append(when(c) { '&' -> "&amp;"; '<' -> "&lt;"; '>' -> "&gt;"; '"' -> "&quot;"; '\r' -> "&#13;"; else -> if (c < ' ' && c != '\t' && c != '\n') "" else c.toString() })
    }
    private fun column(index: Int): String { var i=index+1; var s=""; while(i>0){ i--; s=('A'+i%26)+s; i/=26 }; return s }
    fun write(output: OutputStream, rows: List<List<String>>) {
        require(rows.size <= MAX_ROWS && rows.all { it.size <= MAX_COLUMNS && it.all { s -> s.length <= MAX_CELL } })
        ZipOutputStream(output).use { zip ->
            fun entry(name: String, value: String) { zip.putNextEntry(ZipEntry(name)); zip.write(value.toByteArray(Charsets.UTF_8)); zip.closeEntry() }
            entry("[Content_Types].xml", """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
            entry("_rels/.rels", """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="$REL/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            entry("xl/workbook.xml", """<workbook xmlns="$NS" xmlns:r="$REL"><bookViews><workbookView/></bookViews><sheets><sheet name="Report" sheetId="1" r:id="rId1"/></sheets></workbook>""")
            entry("xl/_rels/workbook.xml.rels", """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="$REL/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val w = zip.bufferedWriter(Charsets.UTF_8)
            w.write("<worksheet xmlns=\"$NS\"><sheetViews><sheetView workbookViewId=\"0\" rightToLeft=\"1\"/></sheetViews><sheetData>")
            rows.forEachIndexed { ri,row ->
                w.write("<row r=\"${ri+1}\">")
                row.forEachIndexed { ci,value -> w.write("<c r=\"${column(ci)}${ri+1}\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${escape(value)}</t></is></c>") }
                w.write("</row>")
            }
            w.write("</sheetData></worksheet>"); w.flush(); zip.closeEntry()
        }
    }
}
