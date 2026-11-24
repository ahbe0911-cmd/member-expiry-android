package ir.memberexpiry.manager.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ir.memberexpiry.manager.MemberExpiryApplication
import kotlinx.coroutines.CancellationException

class ExpiryWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        val repository = (applicationContext as MemberExpiryApplication).repository
        ExpiryNotifier.publish(applicationContext) { repository.dailyExpirySummary() }
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
