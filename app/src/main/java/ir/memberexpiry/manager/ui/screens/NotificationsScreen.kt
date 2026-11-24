package ir.memberexpiry.manager.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.memberexpiry.manager.data.local.MemberEntity
import ir.memberexpiry.manager.ui.viewmodel.NotificationCenterState
import ir.memberexpiry.manager.ui.viewmodel.UiState

@Composable internal fun NotificationsScreen(state: UiState<NotificationCenterState>, busy: Boolean, allowed: Boolean,
    onRetry: () -> Unit, onEnable: () -> Unit, onSettings: () -> Unit, onCheckNow: () -> Unit,
    onEdit: (MemberEntity) -> Unit, onRenew: (MemberEntity) -> Unit, onCopied: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("ALL") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionTitle("مرکز اعلان‌ها", "انقضای امروز و معوقات تمام ماه‌ها") }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (allowed) "اعلان‌های اندروید فعال است." else "اعلان‌های اندروید غیرفعال است.", style = MaterialTheme.typography.titleMedium)
                    Text("اعلان روزانه بدون اینترنت بررسی می‌شود.", style = MaterialTheme.typography.bodySmall)
                    if (!allowed) Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) { Text("فعال‌سازی اعلان‌ها") }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f)) { Text("تنظیمات اعلان") }
                        OutlinedButton(onClick = onCheckNow, modifier = Modifier.weight(1f)) { Text("بررسی اکنون") }
                    }
                }
            }
        }
        item {
            val data = state.dataOrNull()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountCard("انقضای امروز", data?.today?.size, Modifier.weight(1f)) { filter = "TODAY" }
                CountCard("معوقات", data?.overdue?.size, Modifier.weight(1f), true) { filter = "OVERDUE" }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL" to "همه", "TODAY" to "امروز", "OVERDUE" to "معوقات").forEach { (value,label) ->
                    FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(label) })
                }
            }
        }
        when (state) {
            UiState.Loading -> item { LoadingNotice() }
            is UiState.Error -> item { Notice(state.message,onRetry) }
            is UiState.Ready -> {
                val data = state.value
                val members = when (filter) { "TODAY" -> data.today; "OVERDUE" -> data.overdue; else -> data.today + data.overdue }
                if (members.isEmpty()) item { Notice("موردی برای تمدید وجود ندارد.") }
                items(members, key = { it.id }) { member -> MemberCard(member,data.date,busy,{onEdit(member)},{onRenew(member)},onCopied) }
            }
        }
    }
}
