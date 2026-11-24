package ir.memberexpiry.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.memberexpiry.manager.MemberExpiryApplication
import ir.memberexpiry.manager.notifications.ExpiryScheduler

class MemberViewModelFactory(private val application: MemberExpiryApplication) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        MembersViewModel::class.java -> MembersViewModel(application.repository) { ExpiryScheduler.checkNow(application) }
        StatisticsViewModel::class.java -> StatisticsViewModel(application.repository)
        NotificationCenterViewModel::class.java -> NotificationCenterViewModel(application.repository)
        TransferViewModel::class.java -> TransferViewModel(application.repository,application.contentResolver) { ExpiryScheduler.checkNow(application) }
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    } as T
}
