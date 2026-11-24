package ir.memberexpiry.manager.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.memberexpiry.manager.R
import ir.memberexpiry.manager.core.date.JalaliDate
import ir.memberexpiry.manager.core.date.PersianText
import ir.memberexpiry.manager.data.local.ExpiryState
import ir.memberexpiry.manager.data.local.MemberEntity
import ir.memberexpiry.manager.ui.viewmodel.UiState

internal val monthNames = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
internal fun fa(value: Any): String = PersianText.toPersianDigits(value.toString())
internal fun periodLabel(year: Int, month: Int?) = if (month == null) "سال ${fa(year)}" else "${monthNames[month - 1]} ${fa(year)}"
internal fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Ready<T>)?.value

@Composable internal fun AppIcon(id: Int, description: String? = null, modifier: Modifier = Modifier) {
    Icon(painterResource(id), contentDescription = description, modifier = modifier)
}
@Composable internal fun SectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable internal fun Notice(text: String, action: (() -> Unit)? = null) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null) TextButton(onClick = action) { Text("تلاش دوباره") }
        }
    }
}
@Composable internal fun LoadingNotice() {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp)); Text("در حال خواندن اطلاعات…")
    }
}
@Composable internal fun CountCard(label: String, count: Int?, modifier: Modifier = Modifier, warning: Boolean = false, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val content: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(count?.let { fa(it) } ?: "—", style = MaterialTheme.typography.headlineMedium,
                color = if (warning) colors.error else colors.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
        }
    }
    if (onClick == null) Card(modifier, colors = CardDefaults.cardColors(containerColor = colors.surface), content = { content() })
    else Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = colors.surface), content = { content() })
}

@Composable internal fun CopyCode(code: String, onCopied: () -> Unit) {
    val context = LocalContext.current
    TextButton(onClick = {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("کد ملی", code)
        if (Build.VERSION.SDK_INT >= 33) clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
        manager.setPrimaryClip(clip)
        onCopied()
    }) {
        AppIcon(R.drawable.ic_ui_copy, "کپی کد ملی", Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp)); Text(fa(code), style = MaterialTheme.typography.bodyMedium)
    }
}
@Composable internal fun MemberCard(member: MemberEntity, today: JalaliDate?, busy: Boolean, onEdit: () -> Unit, onRenew: () -> Unit, onCopied: () -> Unit) {
    val state = today?.let { member.expiryState(it) }
    val label = when (state) {
        ExpiryState.ACTIVE -> "معتبر"; ExpiryState.DUE_TODAY -> "انقضای امروز"; ExpiryState.OVERDUE -> "معوق"; ExpiryState.INACTIVE -> "غیرفعال"; null -> "در حال بررسی"
    }
    val scheme = MaterialTheme.colorScheme
    val danger = state == ExpiryState.OVERDUE || state == ExpiryState.DUE_TODAY
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = scheme.surface)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(13.dp), color = scheme.primaryContainer) {
                    Box(Modifier.size(43.dp), contentAlignment = Alignment.Center) { AppIcon(R.drawable.ic_ui_people, modifier = Modifier.size(23.dp)) }
                }
                Column(Modifier.weight(1f)) {
                    Text("${member.firstName} ${member.lastName}", style = MaterialTheme.typography.titleMedium)
                    if (member.fatherName.isNotBlank()) Text("نام پدر: ${member.fatherName}", style = MaterialTheme.typography.bodySmall)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = if (danger) scheme.errorContainer else scheme.secondaryContainer) {
                    Text(label, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall,
                        color = if (danger) scheme.onErrorContainer else scheme.onSecondaryContainer)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("تاریخ انقضا", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    Text(fa(member.expiryJalali), style = MaterialTheme.typography.titleMedium)
                }
                CopyCode(member.nationalCode, onCopied)
            }
            HorizontalDivider(color = scheme.surfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, enabled = !busy, modifier = Modifier.weight(1f)) { Text("ویرایش") }
                Button(onClick = onRenew, enabled = !busy && today != null && state != ExpiryState.INACTIVE, modifier = Modifier.weight(1f)) { Text("تمدید") }
            }
        }
    }
}
