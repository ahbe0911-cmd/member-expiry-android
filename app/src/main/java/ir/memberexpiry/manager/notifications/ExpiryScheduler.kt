package ir.memberexpiry.manager.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ExpiryScheduler {
    const val DAILY_WORK = "member-expiry-daily-v1"
    const val IMMEDIATE_WORK = "member-expiry-check-now-v1"

    fun scheduleDaily(context: Context) {
        val request = PeriodicWorkRequestBuilder<ExpiryWorker>(24, TimeUnit.HOURS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(DAILY_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun checkNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<ExpiryWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}
