package ir.memberexpiry.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.memberexpiry.manager.core.date.JalaliDate
import ir.memberexpiry.manager.core.date.PersianText
import ir.memberexpiry.manager.data.local.MemberEntity
import ir.memberexpiry.manager.data.repository.MemberInput

@Composable fun MemberForm(member: MemberEntity?, today: JalaliDate?, busy: Boolean, serverError: String?, onClose: () -> Unit, onSubmit: (MemberInput) -> Unit) {
    val initial = member?.expiryDate() ?: today?.let { runCatching { it.plusYears(1) }.getOrDefault(it) }
    var code by rememberSaveable(member?.id) { mutableStateOf(fa(member?.nationalCode.orEmpty())) }
    var first by rememberSaveable(member?.id) { mutableStateOf(member?.firstName.orEmpty()) }
    var last by rememberSaveable(member?.id) { mutableStateOf(member?.lastName.orEmpty()) }
    var father by rememberSaveable(member?.id) { mutableStateOf(member?.fatherName.orEmpty()) }
    var expiry by rememberSaveable(member?.id) { mutableStateOf(initial?.toPersianString().orEmpty()) }
    var tried by rememberSaveable(member?.id) { mutableStateOf(false) }
    val parsed = remember(expiry) { runCatching { JalaliDate.parse(expiry) }.getOrNull() }
    val codeValid = PersianText.toAsciiDigits(code).matches(Regex("[0-9]{10}"))
    AlertDialog(onDismissRequest = onClose, title = { Text(if (member == null) "افزودن عضو جدید" else "ویرایش اطلاعات عضو") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(value = code, onValueChange = { new -> code = fa(PersianText.toAsciiDigits(new).filter { it in '0'..'9' }.take(10)) },
                    label = { Text("کد ملی (۱۰ رقم)") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = tried && !codeValid)
                OutlinedTextField(value = first, onValueChange = { first = it }, label = { Text("نام") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(), isError = tried && first.isBlank())
                OutlinedTextField(value = last, onValueChange = { last = it }, label = { Text("نام خانوادگی") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(), isError = tried && last.isBlank())
                OutlinedTextField(value = father, onValueChange = { father = it }, label = { Text("نام پدر") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                DateField(expiry, { expiry = it }, busy, tried && parsed == null)
                if (member != null) Text("اصلاح تاریخ انقضا در این فرم، تمدید محسوب نمی‌شود. برای ثبت تمدید از دکمه تمدید استفاده کنید.", style = MaterialTheme.typography.bodySmall)
                else Text("عضویت جدید در آمار امروز ثبت می‌شود.", style = MaterialTheme.typography.bodySmall)
                if (tried && (!codeValid || first.isBlank() || last.isBlank() || parsed == null)) Text("کد ملی، نام، نام خانوادگی و تاریخ معتبر را وارد کنید.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (serverError != null) Text(serverError, color = MaterialTheme.colorScheme.error)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }, confirmButton = {
            Button(enabled = !busy, onClick = {
                tried = true
                if (codeValid && first.isNotBlank() && last.isNotBlank() && parsed != null) onSubmit(MemberInput(code, first, last, father, parsed))
            }) { Text(if (member == null) "ثبت عضویت" else "ذخیره اصلاحات") }
        }, dismissButton = { TextButton(enabled = !busy, onClick = onClose) { Text("انصراف") } })
}
@Composable private fun DateField(value: String, onChange: (String) -> Unit, busy: Boolean, invalid: Boolean) {
    OutlinedTextField(value = value, onValueChange = { new -> onChange(fa(new.take(10))) },
        label = { Text("تاریخ انقضای شمسی") }, placeholder = { Text("۱۴۰۶/۰۹/۰۳") },
        supportingText = { Text("فرمت: سال/ماه/روز") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(), isError = invalid)
}
@Composable internal fun RenewalForm(member: MemberEntity, today: JalaliDate, suggested: JalaliDate?, busy: Boolean, serverError: String?, onClose: () -> Unit, onSubmit: (JalaliDate) -> Unit) {
    var expiry by rememberSaveable(member.id, member.revision) { mutableStateOf(suggested?.toPersianString().orEmpty()) }
    var tried by rememberSaveable(member.id, member.revision) { mutableStateOf(false) }
    val date = remember(expiry) { runCatching { JalaliDate.parse(expiry) }.getOrNull() }
    val valid = date != null && date > today && date > member.expiryDate()
    AlertDialog(onDismissRequest = onClose, title = { Text("تمدید عضویت") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${member.firstName} ${member.lastName}", style = MaterialTheme.typography.titleMedium)
            Text("انقضای قبلی: ${fa(member.expiryJalali)}")
            Text("تاریخ انجام عملیات: ${today.toPersianString()}")
            DateField(expiry, { expiry = it }, busy, tried && !valid)
            if (suggested != null) TextButton(enabled = !busy, onClick = { expiry = suggested.toPersianString() }) { Text("پیشنهاد تمدید یک‌ساله") }
            if (tried && !valid) Text("تاریخ جدید باید معتبر و بعد از امروز و تاریخ قبلی باشد.", color = MaterialTheme.colorScheme.error)
            if (serverError != null) Text(serverError, color = MaterialTheme.colorScheme.error)
            Text("تمدید در تاریخچه ثبت می‌شود و قابل بازگردانی است، مشروط به اینکه عضو بعداً تغییر نکرده باشد.", style = MaterialTheme.typography.bodySmall)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }, confirmButton = { Button(enabled = !busy, onClick = { tried = true; if (valid && date != null) onSubmit(date) }) { Text("تأیید تمدید") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onClose) { Text("انصراف") } })
}
