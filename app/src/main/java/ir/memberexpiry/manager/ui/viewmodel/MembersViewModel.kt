package ir.memberexpiry.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.memberexpiry.manager.core.date.JalaliDate
import ir.memberexpiry.manager.data.local.MemberEntity
import ir.memberexpiry.manager.data.repository.MemberInput
import ir.memberexpiry.manager.data.repository.MemberRepository
import ir.memberexpiry.manager.data.repository.OperationReceipt
import ir.memberexpiry.manager.data.repository.UndoResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class MemberFilter { ALL, CURRENT_QUEUE, PENDING, DUE_TODAY, NEXT_30_DAYS, EXACT_MONTH }
data class PersianPeriod(val year: Int, val month: Int) { init { JalaliDate(year, month, 1) } }
sealed interface MemberEvent {
    data class Saved(val receipt: OperationReceipt, val message: String) : MemberEvent
    data class Message(val text: String) : MemberEvent
    data class Edited(val text: String) : MemberEvent
    data class Failed(val text: String) : MemberEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class MembersViewModel(private val repository: MemberRepository, private val onChanged: () -> Unit = {}) : ViewModel() {
    private val refresh = MutableStateFlow(0L)
    private val _search = MutableStateFlow("")
    val search = _search.asStateFlow()
    private val _filter = MutableStateFlow(MemberFilter.CURRENT_QUEUE)
    val filter = _filter.asStateFlow()
    private val _period = MutableStateFlow<PersianPeriod?>(null)
    val selectedPeriod = _period.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val eventsChannel = Channel<MemberEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    val today = asUiState(refresh.flatMapLatest { repository.observeToday() })
    val dashboard = asUiState(refresh.flatMapLatest { repository.observeDashboard() })
    // Searching always covers all members, regardless of the selected month/filter.
    val searchResults = asUiState(combine(_search, refresh) { text, _ -> text }.flatMapLatest { repository.observeMembers(it) })
    val chart = asUiState(combine(repository.observeToday(), _period, refresh) { date, period, _ -> period?.year ?: date.year }
        .flatMapLatest { repository.observeYearMonths(it) })
    val members = asUiState(combine(_filter, _period, repository.observeToday(), refresh) { filter, period, date, _ ->
        filter to (period ?: PersianPeriod(date.year, date.month))
    }.flatMapLatest { (filter, period) ->
        when (filter) {
            MemberFilter.ALL -> repository.observeMembers()
            MemberFilter.CURRENT_QUEUE -> repository.observeCurrentQueue()
            MemberFilter.PENDING -> repository.observePending()
            MemberFilter.DUE_TODAY -> repository.observeDueToday()
            MemberFilter.NEXT_30_DAYS -> repository.observeNext30Days()
            MemberFilter.EXACT_MONTH -> repository.observeMonth(period.year, period.month)
        }
    })

    fun setSearch(value: String) { _search.value = value }
    fun setFilter(value: MemberFilter) { _filter.value = value }
    fun selectMonth(year: Int, month: Int) { _period.value = PersianPeriod(year, month); _filter.value = MemberFilter.EXACT_MONTH }
    fun selectChartYear(year: Int) {
        _period.value = PersianPeriod(year, _period.value?.month ?: 1)
        _filter.value = MemberFilter.EXACT_MONTH
    }
    fun selectCurrentMonth() { _period.value = null; _filter.value = MemberFilter.CURRENT_QUEUE }
    fun retry() { refresh.value += 1 }
    fun suggestedRenewal(member: MemberEntity): JalaliDate = repository.suggestedRenewalExpiry(member)
    fun add(input: MemberInput) = mutate {
        MemberEvent.Saved(repository.addMember(input), "عضویت جدید ثبت شد.")
    }
    fun renew(member: MemberEntity, expiry: JalaliDate? = null) = mutate {
        MemberEvent.Saved(repository.renew(member.id, member.revision, expiry), "تمدید ثبت شد.")
    }
    fun editIdentity(memberId: String, code: String, first: String, last: String, father: String) = mutate {
        repository.editIdentity(memberId, code, first, last, father)
        MemberEvent.Edited("اطلاعات عضو ویرایش شد.")
    }
    fun correct(member: MemberEntity, input: MemberInput) = mutate {
        repository.correctMember(member.id, member.revision, input)
        MemberEvent.Edited("اطلاعات عضو اصلاح شد.")
    }
    fun undo(activityId: String) = mutate {
        when (repository.undo(activityId)) {
            UndoResult.SUCCESS -> MemberEvent.Message("عملیات بازگردانی شد.")
            UndoResult.ALREADY_UNDONE -> MemberEvent.Message("این عملیات قبلاً بازگردانی شده است.")
            UndoResult.NOT_FOUND -> MemberEvent.Message("عملیات موردنظر پیدا نشد.")
            UndoResult.CONFLICT -> MemberEvent.Message("اطلاعات عضو بعد از این عملیات تغییر کرده است؛ بازگردانی ممکن نیست.")
            UndoResult.UNSUPPORTED -> MemberEvent.Message("بازگردانی این نوع عملیات پشتیبانی نمی‌شود.")
        }
    }
    private fun mutate(block: suspend () -> MemberEvent) {
        if (!_busy.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val result = block()
                eventsChannel.send(result)
                // Notification refresh failure must not be reported as a failed database mutation.
                runCatching { onChanged() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val message = when (error.message) {
                    "National code already exists" -> "این کد ملی قبلاً ثبت شده است."
                    "National code must contain exactly 10 digits" -> "کد ملی باید دقیقاً ۱۰ رقم باشد."
                    "First and last names are required" -> "نام و نام خانوادگی الزامی هستند."
                    "Member changed; refresh before renewal" -> "اطلاعات عضو تغییر کرده است؛ دوباره تلاش کنید."
                    "Member not found" -> "عضو موردنظر پیدا نشد."
                    "Inactive member cannot be renewed" -> "عضو غیرفعال قابل تمدید نیست."
                    "New expiry must be after today and previous expiry" -> "تاریخ انقضای جدید باید بعد از امروز و انقضای قبلی باشد."
                    else -> "عملیات انجام نشد؛ اطلاعات را بررسی و دوباره تلاش کنید."
                }
                eventsChannel.send(MemberEvent.Failed(message))
            } finally {
                _busy.value = false
            }
        }
    }
}
