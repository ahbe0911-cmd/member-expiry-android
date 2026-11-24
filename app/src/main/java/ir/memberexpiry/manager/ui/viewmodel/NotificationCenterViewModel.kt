package ir.memberexpiry.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import ir.memberexpiry.manager.core.date.JalaliDate
import ir.memberexpiry.manager.data.local.MemberEntity
import ir.memberexpiry.manager.data.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi

// This is a live view of the two existing Room tables, not a third table.
data class NotificationCenterState(val date: JalaliDate, val today: List<MemberEntity>, val overdue: List<MemberEntity>) {
    val total: Int get() = today.size + overdue.size
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationCenterViewModel(repository: MemberRepository) : ViewModel() {
    private val refresh = MutableStateFlow(0L)
    val center = asUiState(refresh.flatMapLatest {
        repository.observePendingWithDate().map { (date, pending) ->
            val (today, overdue) = pending.partition { it.expiryEpochDay == date.toEpochDay() }
            NotificationCenterState(date, today, overdue)
        }
    })
    fun retry() { refresh.value += 1 }
}
