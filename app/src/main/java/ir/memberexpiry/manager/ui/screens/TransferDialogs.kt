package ir.memberexpiry.manager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.memberexpiry.manager.core.transfer.*
import ir.memberexpiry.manager.ui.viewmodel.*

/** Keep launchers at the app root, not inside a scrolling item. */
@Composable internal fun rememberTransferActions(model: TransferViewModel): TransferActions {
    val spreadsheet = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri != null) model.readSpreadsheet(uri) }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri != null) model.readBackup(uri) }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TransferFormat.CSV.mime)) { uri -> if(uri!=null) model.export(uri) else model.cancelExport() }
    val xlsx = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TransferFormat.XLSX.mime)) { uri -> if(uri!=null) model.export(uri) else model.cancelExport() }
    val json = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TransferFormat.JSON.mime)) { uri -> if(uri!=null) model.export(uri) else model.cancelExport() }
    return TransferActions(
        importFile = { spreadsheet.launch(arrayOf("*/*")) },
        restore = { backup.launch(arrayOf("*/*")) },
        export = { format,rows,name ->
            model.prepare(format,rows)
            val filename = "$name.${format.extension}"
            when(format) { TransferFormat.CSV -> csv.launch(filename); TransferFormat.XLSX -> xlsx.launch(filename); TransferFormat.JSON -> json.launch(filename) }
        }
    )
}
internal data class TransferActions(val importFile: () -> Unit, val restore: () -> Unit, val export: (TransferFormat,List<List<String>>?,String) -> Unit)
@Composable internal fun TransferMenu(actions: TransferActions, enabled: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text("مدیریت فایل و پشتیبان",style=MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick=actions.importFile,enabled=enabled,modifier=Modifier.fillMaxWidth()) { Text("ورود Excel / CSV") }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick={actions.export(TransferFormat.JSON,null,"member-expiry-backup")},enabled=enabled,modifier=Modifier.weight(1f)) { Text("پشتیبان JSON") }
                OutlinedButton(onClick=actions.restore,enabled=enabled,modifier=Modifier.weight(1f)) { Text("بازیابی JSON") }
            }
            Text("پشتیبان شامل اطلاعات شخصی است؛ در محل امن نگهداری کنید.",style=MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable internal fun ExportButtons(enabled: Boolean, onExport: (TransferFormat) -> Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick={onExport(TransferFormat.XLSX)},enabled=enabled,modifier=Modifier.weight(1f)) { Text("خروجی Excel") }
        OutlinedButton(onClick={onExport(TransferFormat.CSV)},enabled=enabled,modifier=Modifier.weight(1f)) { Text("خروجی CSV") }
    }
}
@Composable internal fun TransferDialogs(model: TransferViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    when(val current=state) {
        TransferState.Idle -> Unit
        TransferState.Busy -> AlertDialog(onDismissRequest={},title={Text("در حال پردازش فایل")},text={Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){CircularProgressIndicator(); Text("عملیات در حال انجام است.")}},confirmButton={})
        is TransferState.Message -> AlertDialog(onDismissRequest=model::dismiss,title={Text(if(current.failed) "خطا" else "عملیات انجام شد")},text={Text(fa(current.text))},confirmButton={TextButton(onClick=model::dismiss){Text("بستن")}})
        is TransferState.Sheets -> ImportDialog(current.items,model)
        is TransferState.Backup -> RestoreDialog(current.data,model)
    }
}
@Composable private fun ImportDialog(sheets: List<SheetPreview>, model: TransferViewModel) {
    var selected by remember(sheets) { mutableIntStateOf(sheets.indexOfFirst { it.preview != null }.coerceAtLeast(0)) }
    var mode by remember { mutableStateOf(ImportMode.MERGE) }
    var acknowledged by remember(selected,mode) { mutableStateOf(false) }
    val sheet=sheets.getOrNull(selected); val preview=sheet?.preview
    AlertDialog(onDismissRequest=model::dismiss,title={Text("پیش‌نمایش ورود اطلاعات")},text={
        Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            if(sheets.size>1) {
                Text("انتخاب شیت")
                sheets.forEachIndexed { i,s -> Row(verticalAlignment=Alignment.CenterVertically){RadioButton(selected==i,{selected=i}); Text(s.name)} }
            }
            if(preview==null) Text(sheet?.error ?: "شیت معتبر پیدا نشد.") else {
                Text("${fa(preview.rows.size)} رکورد معتبر | ${fa(preview.errorCount)} ردیف نامعتبر")
                preview.errors.forEach { Text(fa(it),color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall) }
                preview.rows.take(3).forEach { Text("${it.input.firstName} ${it.input.lastName} — ${it.input.expiry.toPersianString()}",style=MaterialTheme.typography.bodySmall) }
            }
            listOf(ImportMode.MERGE to "ادغام بر اساس کد ملی",ImportMode.APPEND to "افزودن همه، حتی تکراری",ImportMode.REPLACE to "جایگزینی تمام اعضا (حفظ تاریخچه)").forEach { (v,label) -> Row(verticalAlignment=Alignment.CenterVertically){RadioButton(mode==v,{mode=v});Text(label)} }
            if(mode==ImportMode.REPLACE || (preview?.errorCount ?: 0)>0) Row(verticalAlignment=Alignment.CenterVertically){Checkbox(acknowledged,{acknowledged=it}); Text(if(mode==ImportMode.REPLACE) "حذف اعضای فعلی و ورود فقط ردیف‌های معتبر را تأیید می‌کنم." else "ردیف‌های نامعتبر نادیده گرفته شوند.")}
            Text("ورود فایل به عنوان عضویت یا تمدید امروز ثبت نمی‌شود.",style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={TextButton(enabled=preview!=null && preview.rows.isNotEmpty() && ((mode!=ImportMode.REPLACE && preview.errorCount==0)||acknowledged),onClick={if(preview!=null) model.importRows(preview,mode)}){Text("اجرای ورود")}},dismissButton={TextButton(onClick=model::dismiss){Text("انصراف")}})
}
@Composable private fun RestoreDialog(data: BackupData,model: TransferViewModel) {
    var replace by remember { mutableStateOf(false) }; var acknowledged by remember(replace) { mutableStateOf(false) }
    AlertDialog(onDismissRequest=model::dismiss,title={Text("بازیابی پشتیبان")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("${fa(data.members.size)} عضو و ${fa(data.logs.size)} سابقه در فایل وجود دارد.")
        Row(verticalAlignment=Alignment.CenterVertically){RadioButton(!replace,{replace=false});Text("ادغام بدون رکورد تکراری بر اساس شناسه")}
        Row(verticalAlignment=Alignment.CenterVertically){RadioButton(replace,{replace=true});Text("جایگزینی کامل هر دو جدول")}
        Text(if(replace) "تمام داده‌های فعلی با فایل پشتیبان جایگزین می‌شوند." else "اگر رکورد هم‌شناسه متفاوت باشد، کل بازیابی متوقف خواهد شد.")
        if(replace) Row(verticalAlignment=Alignment.CenterVertically){Checkbox(acknowledged,{acknowledged=it});Text("حذف داده‌های فعلی را تأیید می‌کنم.")}
    }},confirmButton={TextButton(enabled=!replace||acknowledged,onClick={model.restore(data,replace)}){Text("بازیابی")}},dismissButton={TextButton(onClick=model::dismiss){Text("انصراف")}})
}
