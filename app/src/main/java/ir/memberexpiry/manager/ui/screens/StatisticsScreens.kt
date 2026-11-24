package ir.memberexpiry.manager.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.memberexpiry.manager.core.date.JalaliDate
import ir.memberexpiry.manager.data.local.ActivityLogEntity
import ir.memberexpiry.manager.data.local.ActivityType
import ir.memberexpiry.manager.data.local.ExpiryOccurrence
import ir.memberexpiry.manager.ui.viewmodel.*
import ir.memberexpiry.manager.core.transfer.Reports
import ir.memberexpiry.manager.core.transfer.TransferFormat

@Composable internal fun PeriodPicker(year: Int, month: Int?, allowAllYear: Boolean, onSelection: (Int, Int?) -> Unit, label: String = "دوره آماری") {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        YearStepper(year) { onSelection(it, month) }
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("${periodLabel(year, month)}  ▾") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (allowAllYear) DropdownMenuItem(text = { Text("کل سال") }, onClick = { expanded = false; onSelection(year, null) })
                monthNames.forEachIndexed { i, name -> DropdownMenuItem(text = { Text(name) }, onClick = { expanded = false; onSelection(year, i + 1) }) }
            }
        }
    }
}
@Composable internal fun ActivityScreen(model: StatisticsViewModel, today: JalaliDate?, busy: Boolean, onUndo: (String,String) -> Unit, onCopied: () -> Unit, onExport: (TransferFormat,List<List<String>>) -> Unit) {
    val selection by model.activityPeriod.collectAsStateWithLifecycle()
    val selected = selection ?: today?.let { ActivityPeriod(it.year,it.month) }
    val counts by model.activityCounts.collectAsStateWithLifecycle()
    val activities by model.activities.collectAsStateWithLifecycle()
    var kind by rememberSaveable { mutableStateOf("ALL") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionTitle("تمدیدشده‌ها و عضویت جدید", "آمار بر اساس تاریخ انجام عملیات، نه تاریخ انقضا") }
        if (selected != null) item { PeriodPicker(selected.year, selected.month, true, model::selectActivityPeriod) }
        item {
            val data = counts.dataOrNull()
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CountCard("تمدید", data?.renewals, Modifier.weight(1f))
                CountCard("عضویت جدید", data?.newMemberships, Modifier.weight(1f))
                CountCard("جمع کل", data?.total, Modifier.weight(1f))
            }
        }
        if (counts is UiState.Error) item { Notice((counts as UiState.Error).message, model::retry) }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL" to "همه", ActivityType.RENEWAL to "تمدید", ActivityType.NEW_MEMBERSHIP to "عضویت جدید").forEach { (value, label) ->
                    FilterChip(selected = kind == value, onClick = { kind = value }, label = { Text(label) })
                }
            }
        }
        when (val state = activities) {
            UiState.Loading -> item { LoadingNotice() }
            is UiState.Error -> item { Notice(state.message, model::retry) }
            is UiState.Ready -> {
                val filtered = state.value.filter { kind == "ALL" || it.type == kind }
                item { ExportButtons(!busy) { onExport(it,Reports.activities(filtered)) } }
                item { Text("${fa(filtered.size)} عملیات", style = MaterialTheme.typography.labelLarge) }
                if (filtered.isEmpty()) item { Notice("در دوره انتخاب‌شده عملیاتی ثبت نشده است.") }
                items(filtered, key = { it.id }) { log -> ActivityCard(log,busy,onUndo,onCopied) }
            }
        }
    }
}
@Composable private fun ActivityCard(log: ActivityLogEntity, busy: Boolean, onUndo: (String,String) -> Unit, onCopied: () -> Unit) {
    val type = if (log.type == ActivityType.RENEWAL) "تمدید" else "عضویت جدید"
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${log.firstName} ${log.lastName}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) { Text(type, Modifier.padding(7.dp), style = MaterialTheme.typography.labelSmall) }
            }
            if (log.fatherName.isNotBlank()) Text("نام پدر: ${log.fatherName}", style = MaterialTheme.typography.bodySmall)
            Text("تاریخ عملیات: ${fa(log.operationJalali)}")
            log.previousExpiryJalali?.let { Text("انقضای قبلی: ${fa(it)}", style = MaterialTheme.typography.bodySmall) }
            Text("انقضای جدید: ${fa(log.newExpiryJalali)}")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                CopyCode(log.nationalCode, onCopied)
                TextButton(enabled = !busy, onClick = { onUndo(log.id,type) }) { Text("بازگردانی") }
            }
        }
    }
}
@Composable internal fun HistoryScreen(model: StatisticsViewModel, today: JalaliDate?, onCopied: () -> Unit, onExport: (TransferFormat,List<List<String>>) -> Unit) {
    val selection by model.expiryPeriod.collectAsStateWithLifecycle()
    val selected = selection ?: today?.let { PersianPeriod(it.year,it.month) }
    val history by model.expiredHistory.collectAsStateWithLifecycle()
    var kind by rememberSaveable { mutableStateOf("ALL") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionTitle("تاریخ انقضای تمام‌شده", "بر اساس ماه واقعی پایان عضویت؛ حتی پس از تمدید") }
        if (selected != null) item { PeriodPicker(selected.year, selected.month, false, { y,m -> if (m != null) model.selectExpiryMonth(y,m) }) }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL" to "همه", "PENDING" to "تمدیدنشده", "RENEWED" to "تمدیدشده").forEach { (value,label) ->
                    FilterChip(selected = kind == value, onClick = { kind = value }, label = { Text(label) })
                }
            }
        }
        when (val state = history) {
            UiState.Loading -> item { LoadingNotice() }
            is UiState.Error -> item { Notice(state.message, model::retry) }
            is UiState.Ready -> {
                val filtered = state.value.filter { kind == "ALL" || (kind == "RENEWED" && it.wasRenewed) || (kind == "PENDING" && !it.wasRenewed) }
                item { ExportButtons(true) { onExport(it,Reports.history(filtered)) } }
                item { Text("${fa(filtered.size)} مورد انقضا", style = MaterialTheme.typography.labelLarge) }
                if (filtered.isEmpty()) item { Notice("در این ماه موردی با فیلتر انتخاب‌شده وجود ندارد.") }
                items(filtered) { row -> HistoryCard(row,onCopied) }
            }
        }
    }
}
@Composable private fun HistoryCard(row: ExpiryOccurrence, onCopied: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${row.firstName} ${row.lastName}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Surface(shape = RoundedCornerShape(8.dp), color = if (row.wasRenewed) colors.primaryContainer else colors.errorContainer) {
                    Text(if (row.wasRenewed) "تمدیدشده" else "تمدیدنشده", Modifier.padding(7.dp), style = MaterialTheme.typography.labelSmall,
                        color = if (row.wasRenewed) colors.onPrimaryContainer else colors.onErrorContainer)
                }
            }
            if (row.fatherName.isNotBlank()) Text("نام پدر: ${row.fatherName}", style = MaterialTheme.typography.bodySmall)
            Text("انقضای ثبت‌شده: ${fa(row.expiryJalali)}")
            CopyCode(row.nationalCode, onCopied)
        }
    }
}
