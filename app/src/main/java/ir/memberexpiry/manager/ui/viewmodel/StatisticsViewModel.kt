package ir.memberexpiry.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import ir.memberexpiry.manager.core.date.JalaliDate
import ir.memberexpiry.manager.data.repository.MemberRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

data class ActivityPeriod(val year: Int, val month: Int?) { init { JalaliDate(year, month ?: 1, 1) } }

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(repository: MemberRepository) : ViewModel() {
    private val refresh = MutableStateFlow(0L)
    private val _activityPeriod = MutableStateFlow<ActivityPeriod?>(null)
    private val _expiryPeriod = MutableStateFlow<PersianPeriod?>(null)
    val activityPeriod = _activityPeriod.asStateFlow()
    val expiryPeriod = _expiryPeriod.asStateFlow()
    private val activitySelection = combine(repository.observeToday(), _activityPeriod, refresh) { today, period, _ ->
        period ?: ActivityPeriod(today.year, today.month)
    }
    private val expirySelection = combine(repository.observeToday(), _expiryPeriod, refresh) { today, period, _ ->
        period ?: PersianPeriod(today.year, today.month)
    }
    val activities = asUiState(activitySelection.flatMapLatest { repository.observeActivities(it.year, it.month) })
    val activityCounts = asUiState(activitySelection.flatMapLatest { repository.observeActivityCounts(it.year, it.month) })
    val expiredHistory = asUiState(expirySelection.flatMapLatest { repository.observeExpiredHistory(it.year, it.month) })
    fun selectActivityPeriod(year: Int, month: Int? = null) { _activityPeriod.value = ActivityPeriod(year, month) }
    fun selectExpiryMonth(year: Int, month: Int) { _expiryPeriod.value = PersianPeriod(year, month) }
    fun resetPeriods() { _activityPeriod.value = null; _expiryPeriod.value = null }
    fun retry() { refresh.value += 1 }
}
