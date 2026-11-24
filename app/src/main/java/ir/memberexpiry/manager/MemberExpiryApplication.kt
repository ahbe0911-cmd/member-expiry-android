package ir.memberexpiry.manager

import android.app.Application
import ir.memberexpiry.manager.data.local.MemberDatabase
import ir.memberexpiry.manager.data.repository.MemberRepository
import ir.memberexpiry.manager.notifications.ExpiryScheduler
import ir.memberexpiry.manager.notifications.ExpiryNotifier

class MemberExpiryApplication : Application() {
    val repository: MemberRepository by lazy { MemberRepository(MemberDatabase.getInstance(this)) }
    override fun onCreate() {
        super.onCreate()
        ExpiryNotifier.createChannel(this)
        ExpiryScheduler.scheduleDaily(this)
    }
}
