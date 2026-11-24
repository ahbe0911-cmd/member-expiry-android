package ir.memberexpiry.manager.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.memberexpiry.manager.core.transfer.*
import ir.memberexpiry.manager.data.repository.MemberRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SheetPreview(val name: String, val preview: ImportPreview?, val error: String? = null)
sealed interface TransferState {
    data object Idle : TransferState
    data object Busy : TransferState
    data class Sheets(val items: List<SheetPreview>) : TransferState
    data class Backup(val data: BackupData) : TransferState
    data class Message(val text: String, val failed: Boolean = false) : TransferState
}
class TransferViewModel(private val repository: MemberRepository, private val resolver: ContentResolver, private val onChanged: () -> Unit) : ViewModel() {
    private val _state = MutableStateFlow<TransferState>(TransferState.Idle)
    val state = _state.asStateFlow()
    private data class Export(val format: TransferFormat, val rows: List<List<String>>?)
    private var pending: Export? = null
    fun prepare(format: TransferFormat, rows: List<List<String>>? = null) { pending = Export(format,rows) }
    fun cancelExport() { pending = null }
    fun dismiss() { if (_state.value != TransferState.Busy) _state.value = TransferState.Idle }
    fun readSpreadsheet(uri: Uri) = runJob {
        val sheets = requireNotNull(resolver.openInputStream(uri)).buffered().use { stream ->
            stream.mark(4); val a=stream.read(); val b=stream.read(); stream.reset()
            if (a==80 && b==75) XlsxCodec.read(stream) else listOf(CsvCodec.read(stream))
        }
        TransferState.Sheets(sheets.map { sheet ->
            try { SheetPreview(sheet.name,MemberColumns.detect(sheet)) } catch(e: Exception) { SheetPreview(sheet.name,null,"ستون‌های ضروری پیدا نشدند یا ساختار شیت نامعتبر است.") }
        })
    }
    fun readBackup(uri: Uri) = runJob { TransferState.Backup(requireNotNull(resolver.openInputStream(uri)).use(BackupCodec::read)) }
    fun importRows(preview: ImportPreview, mode: ImportMode) = runJob {
        val count = repository.importMembers(preview.rows,mode); runCatching { onChanged() }
        TransferState.Message("${count} رکورد پردازش شد؛ ${preview.errorCount} ردیف نامعتبر نادیده گرفته شد.")
    }
    fun restore(data: BackupData, replace: Boolean) = runJob {
        val (m,l) = repository.restore(data,replace); runCatching { onChanged() }
        TransferState.Message("${m} عضو و ${l} سابقه بازیابی شد.")
    }
    fun export(uri: Uri) {
        val job = pending ?: return; pending = null
        runJob {
            // Build first, so a codec failure does not write a partially formed report to the destination.
            val bytes = java.io.ByteArrayOutputStream().also { out ->
                when(job.format) {
                    TransferFormat.JSON -> BackupCodec.write(out,repository.backup())
                    TransferFormat.CSV -> CsvCodec.write(out,requireNotNull(job.rows))
                    TransferFormat.XLSX -> XlsxCodec.write(out,requireNotNull(job.rows))
                }
            }.toByteArray()
            require(bytes.size <= MAX_FILE_BYTES) { "حجم خروجی بیش از حد مجاز است." }
            requireNotNull(resolver.openOutputStream(uri,"wt")).use { it.write(bytes) }
            TransferState.Message("فایل با موفقیت ذخیره شد.")
        }
    }
    private fun runJob(block: suspend () -> TransferState) {
        if (_state.value == TransferState.Busy) return
        _state.value = TransferState.Busy
        viewModelScope.launch {
            try { _state.value = withContext(Dispatchers.IO) { block() } }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) {
                val safe = e.message?.takeIf { s -> s.any { it in '\u0600'..'\u06ff' } && s.length <= 180 }
                _state.value = TransferState.Message(safe ?: "عملیات فایل انجام نشد؛ قالب، دسترسی یا حجم فایل را بررسی کنید.",true)
            }
        }
    }
}
