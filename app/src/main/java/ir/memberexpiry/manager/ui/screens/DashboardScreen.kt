package ir.memberexpiry.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.memberexpiry.manager.R
import ir.memberexpiry.manager.core.date.JalaliDate
import ir.memberexpiry.manager.data.local.MonthCount
import ir.memberexpiry.manager.ui.viewmodel.*

@Composable internal fun SearchBox(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { onChange(fa(it)) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
        shape = RoundedCornerShape(18.dp), leadingIcon = { AppIcon(R.drawable.ic_ui_search) },
        placeholder = { Text("نام، نام خانوادگی، نام پدر یا کد ملی") },
        trailingIcon = { if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { AppIcon(R.drawable.ic_ui_close,"پاک کردن جستجو") } })
}

@Composable internal fun DashboardScreen(model: MembersViewModel, todayState: UiState<JalaliDate>, search: String,
    onSearch: (String) -> Unit, onFilter: (MemberFilter) -> Unit, onMonth: (Int, Int) -> Unit,
    onActivities: () -> Unit, onHistory: () -> Unit, onRegistration: () -> Unit, onAdd: () -> Unit, fileMenu: @Composable () -> Unit) {
    val counts by model.dashboard.collectAsStateWithLifecycle()
    val chart by model.chart.collectAsStateWithLifecycle()
    val selected by model.selectedPeriod.collectAsStateWithLifecycle()
    val today = todayState.dataOrNull()
    val year = selected?.year ?: today?.year
    val colors = MaterialTheme.colorScheme
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = colors.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("پنل مدیریت تاریخ انقضا", style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
                    Text("هیچ تمدیدی را از دست ندهید", style = MaterialTheme.typography.headlineSmall, color = colors.onPrimaryContainer)
                    Text("${today?.toPersianString() ?: "—"}  |  داده‌های محلی و آفلاین", style = MaterialTheme.typography.bodySmall, color = colors.onPrimaryContainer)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onAdd, enabled = today != null, modifier = Modifier.weight(1f)) { AppIcon(R.drawable.ic_ui_plus, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("عضو جدید") }
                        OutlinedButton(onClick = onRegistration, modifier = Modifier.weight(1f)) { AppIcon(R.drawable.ic_ui_external, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("ثبت‌نام") }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { onSearch(search) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                AppIcon(R.drawable.ic_ui_search); Spacer(Modifier.width(8.dp)); Text("جستجوی نام یا کد ملی در همه اعضا")
            }
        }
        item { fileMenu() }
        item { SectionTitle("نمای کلی", "برای مشاهده فهرست مربوط، روی هر کارت بزنید.") }
        item {
            val data = counts.dataOrNull()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CountCard("منقضی و تمدیدنشده", data?.pendingRenewal, Modifier.weight(1f), true) { onFilter(MemberFilter.PENDING) }
                    CountCard("۳۰ روز آینده", data?.next30Days, Modifier.weight(1f)) { onFilter(MemberFilter.NEXT_30_DAYS) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CountCard("انقضای ماه جاری", data?.currentMonth, Modifier.weight(1f)) { if (today != null) onMonth(today.year, today.month) }
                    CountCard("کل رکوردها", data?.totalMembers, Modifier.weight(1f)) { onFilter(MemberFilter.ALL) }
                }
                if (counts is UiState.Error) Notice((counts as UiState.Error).message, model::retry)
            }
        }
        item {
            Button(onClick = { onFilter(MemberFilter.CURRENT_QUEUE) }, modifier = Modifier.fillMaxWidth()) { Text("فهرست ماه جاری + معوقات قبلی") }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = colors.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("انقضاهای سال", "اعضای فعال بر اساس تاریخ انقضای فعلی")
                    if (year != null) YearStepper(year, { model.selectChartYear(it) })
                    when (val state = chart) {
                        UiState.Loading -> LoadingNotice()
                        is UiState.Error -> Notice(state.message, model::retry)
                        is UiState.Ready -> if (year != null) YearChart(state.value, selected?.month ?: today?.month, onClick = { onMonth(year, it) })
                    }
                }
            }
        }
        item { OutlinedButton(onClick = onActivities, modifier = Modifier.fillMaxWidth()) { AppIcon(R.drawable.ic_ui_stats); Spacer(Modifier.width(8.dp)); Text("تمدیدشده‌ها و عضویت جدید") } }
        item { OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { AppIcon(R.drawable.ic_ui_history); Spacer(Modifier.width(8.dp)); Text("تاریخ انقضای تمام‌شده") } }
    }
}
@Composable private fun YearChart(rows: List<MonthCount>, selected: Int?, onClick: (Int) -> Unit) {
    val counts = rows.associate { it.month to it.count }
    val max = (counts.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val short = listOf("فرو", "ارد", "خرد", "تیر", "مرد", "شهر", "مهر", "آبا", "آذر", "دی", "بهم", "اسف")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (1..12).forEach { month ->
                val count = counts[month] ?: 0
                val label = "${monthNames[month-1]}، ${fa(count)} نفر"
                Column(Modifier.width(40.dp).clip(RoundedCornerShape(8.dp)).clickable { onClick(month) }.semantics { contentDescription = label }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(fa(count), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    Box(Modifier.height(88.dp).width(24.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                        if (count > 0) Box(Modifier.fillMaxWidth().height((88f * count / max).coerceAtLeast(2f).dp).clip(RoundedCornerShape(5.dp))
                            .background(if (selected == month) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary))
                    }
                    Text(short[month-1], style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
        Text("نمودار قابل پیمایش است؛ هر ستون فهرست همان ماه را باز می‌کند.", style = MaterialTheme.typography.bodySmall)
    }
}
@Composable internal fun YearStepper(year: Int, onYear: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { onYear(year - 1) }, enabled = year > JalaliDate.MIN_YEAR) { Text("سال قبل") }
        Text(fa(year), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { onYear(year + 1) }, enabled = year < JalaliDate.MAX_YEAR) { Text("سال بعد") }
    }
}
@Composable internal fun MemberFilters(filter: MemberFilter, period: PersianPeriod?, today: JalaliDate?, searching: Boolean,
    onFilter: (MemberFilter) -> Unit, onMonth: (Int,Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (searching) Text("جستجو در کل اعضا؛ فیلتر ماه اعمال نمی‌شود.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(MemberFilter.CURRENT_QUEUE to "ماه جاری + معوقات", MemberFilter.ALL to "همه", MemberFilter.PENDING to "در انتظار تمدید", MemberFilter.DUE_TODAY to "امروز", MemberFilter.NEXT_30_DAYS to "۳۰ روز آینده").forEach { (value, label) ->
                FilterChip(selected = !searching && filter == value, onClick = { onFilter(value) }, label = { Text(label) })
            }
        }
        val date = period ?: today?.let { PersianPeriod(it.year,it.month) }
        if (date != null) PeriodPicker(date.year,date.month, false, { y,m -> if (m != null) onMonth(y,m) }, label = "فیلتر ماه دقیق")
    }
}
