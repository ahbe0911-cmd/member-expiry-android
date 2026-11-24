package ir.memberexpiry.manager.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.memberexpiry.manager.R
import ir.memberexpiry.manager.core.date.JalaliDate
import ir.memberexpiry.manager.data.local.MemberEntity
import ir.memberexpiry.manager.core.transfer.Reports
import ir.memberexpiry.manager.ui.viewmodel.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

private enum class Page(val label: String, val icon: Int) {
    HOME("خانه", R.drawable.ic_ui_home), MEMBERS("اعضا", R.drawable.ic_ui_people),
    ACTIVITIES("عملیات", R.drawable.ic_ui_stats), HISTORY("انقضاها", R.drawable.ic_ui_history),
    NOTIFICATIONS("اعلان‌ها", R.drawable.ic_ui_bell)
}
private sealed interface Editor {
    data object Add : Editor
    data class Edit(val member: MemberEntity) : Editor
    data class Renew(val member: MemberEntity) : Editor
    data class Undo(val id: String, val label: String) : Editor
}

@Composable
fun MemberApp(members: MembersViewModel, statistics: StatisticsViewModel, notifications: NotificationCenterViewModel,
              transfer: TransferViewModel, notificationRequest: Int, notificationAllowed: Boolean, onRegistration: () -> Unit,
              onEnableNotifications: () -> Unit, onSettings: () -> Unit, onCheckNow: () -> Unit) {
    var pageName by rememberSaveable { mutableStateOf(Page.HOME.name) }
    val page = Page.valueOf(pageName)
    var lastRequest by rememberSaveable { mutableIntStateOf(0) }
    var editor by remember { mutableStateOf<Editor?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val todayState by members.today.collectAsStateWithLifecycle()
    val today = todayState.dataOrNull()
    val center by notifications.center.collectAsStateWithLifecycle()
    val memberBusy by members.busy.collectAsStateWithLifecycle()
    val transferState by transfer.state.collectAsStateWithLifecycle()
    val busy = memberBusy || transferState == TransferState.Busy
    val fileActions = rememberTransferActions(transfer)
    val search by members.search.collectAsStateWithLifecycle()
    val filter by members.filter.collectAsStateWithLifecycle()
    val period by members.selectedPeriod.collectAsStateWithLifecycle()
    fun openEditor(value: Editor) { formError = null; editor = value }
    fun navigate(target: Page) { pageName = target.name }
    fun openFilter(value: MemberFilter) {
        members.setSearch("")
        if (value == MemberFilter.CURRENT_QUEUE) members.selectCurrentMonth() else members.setFilter(value)
        navigate(Page.MEMBERS)
    }
    fun copied() { scope.launch { snack.currentSnackbarData?.dismiss(); snack.showSnackbar("کد ملی کپی شد.", duration = SnackbarDuration.Short) } }
    LaunchedEffect(notificationRequest) {
        if (notificationRequest != lastRequest) { lastRequest = notificationRequest; pageName = Page.NOTIFICATIONS.name }
    }
    LaunchedEffect(members) {
        members.events.collect { event ->
            when (event) {
                is MemberEvent.Saved -> {
                    editor = null; formError = null
                    launch {
                        snack.currentSnackbarData?.dismiss()
                        if (snack.showSnackbar(event.message, "بازگردانی", withDismissAction = true, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) members.undo(event.receipt.activityId)
                    }
                }
                is MemberEvent.Edited -> {
                    editor = null; formError = null
                    launch { snack.currentSnackbarData?.dismiss(); snack.showSnackbar(event.text) }
                }
                is MemberEvent.Failed -> {
                    if (editor != null) formError = event.text
                    else launch { snack.currentSnackbarData?.dismiss(); snack.showSnackbar(event.text) }
                }
                is MemberEvent.Message -> {
                    if (editor is Editor.Undo) editor = null
                    launch { snack.currentSnackbarData?.dismiss(); snack.showSnackbar(event.text) }
                }
            }
        }
    }
    BackHandler(page != Page.HOME && editor == null) { navigate(Page.HOME) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("مدیریت عضویت", style = MaterialTheme.typography.titleLarge)
                        Text(today?.let { "${fa(it.day)} ${monthNames[it.month - 1]} ${fa(it.year)}" } ?: "تاریخ امروز —", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { navigate(Page.NOTIFICATIONS) }) {
                        BadgedBox(badge = {
                            val total = center.dataOrNull()?.total ?: 0
                            if (total > 0) Badge { Text(if (total > 99) "۹۹+" else fa(total)) }
                        }) { AppIcon(R.drawable.ic_ui_bell, "مرکز اعلان‌ها") }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                listOf(Page.HOME, Page.MEMBERS, Page.ACTIVITIES, Page.HISTORY).forEach { target ->
                    NavigationBarItem(selected = page == target, onClick = { navigate(target) },
                        icon = { AppIcon(target.icon) }, label = { Text(target.label) })
                }
            }
        },
        floatingActionButton = {
            if (page == Page.MEMBERS) ExtendedFloatingActionButton(onClick = { if (!busy && today != null) openEditor(Editor.Add) },
                icon = { AppIcon(R.drawable.ic_ui_plus) }, text = { Text("عضو جدید") })
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            when (page) {
                Page.HOME -> DashboardScreen(members, todayState, search, onSearch = { members.setSearch(it); navigate(Page.MEMBERS) },
                    onFilter = ::openFilter, onMonth = { year, month -> members.setSearch(""); members.selectMonth(year, month); navigate(Page.MEMBERS) },
                    onActivities = { navigate(Page.ACTIVITIES) }, onHistory = { navigate(Page.HISTORY) }, onRegistration = onRegistration,
                    onAdd = { if (!busy && today != null) openEditor(Editor.Add) }, fileMenu = { TransferMenu(fileActions,!busy) })
                Page.MEMBERS -> {
                    val listState = if (search.isBlank()) members.members.collectAsStateWithLifecycle().value else members.searchResults.collectAsStateWithLifecycle().value
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item { SectionTitle("اعضا", "جستجو در تمام رکوردها و مدیریت تمدید") }
                        item { SearchBox(search, members::setSearch) }
                        item {
                            MemberFilters(filter, period, today, search.isNotBlank(), onFilter = { openFilter(it) }, onMonth = { y, m -> members.setSearch(""); members.selectMonth(y,m) })
                        }
                        item { Text(listState.dataOrNull()?.let { "${fa(it.size)} رکورد" } ?: "رکوردها", style = MaterialTheme.typography.labelLarge) }
                        when (val state = listState) {
                            UiState.Loading -> item { LoadingNotice() }
                            is UiState.Error -> item { Notice(state.message, members::retry) }
                            is UiState.Ready -> {
                                item { ExportButtons(!busy) { format -> fileActions.export(format,Reports.members(state.value),"members") } }
                                if (state.value.isEmpty()) item { Notice("عضوی با این شرایط یافت نشد.") }
                                items(state.value, key = { it.id }) { m -> MemberCard(m, today, busy, { openEditor(Editor.Edit(m)) }, { openEditor(Editor.Renew(m)) }, ::copied) }
                            }
                        }
                    }
                }
                Page.ACTIVITIES -> ActivityScreen(statistics, today, busy, onUndo = { id, label -> openEditor(Editor.Undo(id,label)) }, onCopied = ::copied, onExport = { format,rows -> fileActions.export(format,rows,"activities") })
                Page.HISTORY -> HistoryScreen(statistics, today, onCopied = ::copied, onExport = { format,rows -> fileActions.export(format,rows,"expired-history") })
                Page.NOTIFICATIONS -> NotificationsScreen(center, busy, notificationAllowed, notifications::retry, onEnableNotifications, onSettings, onCheckNow,
                    onEdit = { openEditor(Editor.Edit(it)) }, onRenew = { openEditor(Editor.Renew(it)) }, onCopied = ::copied)
            }
        }
    }
    TransferDialogs(transfer)
    val close = { if (!busy) { editor = null; formError = null } }
    when (val current = editor) {
        Editor.Add -> if (today != null) MemberForm(null, today, busy, formError, close, onSubmit = { formError = null; members.add(it) })
        is Editor.Edit -> MemberForm(current.member, today, busy, formError, close, onSubmit = { formError = null; members.correct(current.member, it) })
        is Editor.Renew -> if (today != null) RenewalForm(current.member, today, runCatching { members.suggestedRenewal(current.member) }.getOrNull(), busy, formError, close,
            onSubmit = { formError = null; members.renew(current.member, it) })
        is Editor.Undo -> AlertDialog(onDismissRequest = close, title = { Text("بازگردانی عملیات") },
            text = { Column { Text("عملیات «${current.label}» بازگردانی شود؟ بازگردانی عضویت جدید، عضو تازه ثبت‌شده را حذف می‌کند؛ سوابق آماری نیز اصلاح می‌شوند."); if (formError != null) Text(formError.orEmpty(), color = MaterialTheme.colorScheme.error) } },
            confirmButton = { Button(enabled = !busy, onClick = { members.undo(current.id) }) { Text("بازگردانی") } },
            dismissButton = { TextButton(enabled = !busy, onClick = close) { Text("انصراف") } })
        null -> Unit
    }
}
