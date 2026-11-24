package ir.memberexpiry.manager.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ir.memberexpiry.manager.MainActivity
import ir.memberexpiry.manager.R
import ir.memberexpiry.manager.core.date.PersianText
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ExpiryNotifier {
    const val CHANNEL_ID = "member_expiry_daily"
    const val ACTION_OPEN_CENTER = "ir.memberexpiry.manager.OPEN_EXPIRY_CENTER"
    private const val NOTIFICATION_ID = 7301
    private val mutex = Mutex()

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID,
            context.getString(R.string.expiry_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.expiry_channel_description)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            setShowBadge(true)
        })
    }

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val channel = context.getSystemService(NotificationManager::class.java)?.getNotificationChannel(CHANNEL_ID)
        return channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    // Same process lock prevents an immediate and periodic worker from posting duplicates.
    // No personal information is put in the notification or preferences.
    @android.annotation.SuppressLint("MissingPermission") // Permission is checked twice, and revocation is caught.
    suspend fun publish(context: Context, loadSummary: suspend () -> DailyExpirySummary) = mutex.withLock {
        val summary = loadSummary()
        createChannel(context)
        val manager = NotificationManagerCompat.from(context)
        val prefs = context.getSharedPreferences("expiry_delivery", Context.MODE_PRIVATE)
        val counts = summary.counts
        val day = summary.date.toIsoString()
        val key = "$day:${counts.dueToday}:${counts.overdue}"
        when (decideDelivery(counts, canNotify(context), prefs.getString("last_key", null), key)) {
            Delivery.CANCEL -> {
                manager.cancel(NOTIFICATION_ID)
                prefs.edit().remove("last_key").apply()
            }
            Delivery.SKIP -> Unit
            Delivery.POST -> {
                // Permission may be revoked between the initial check and posting.
                if (!canNotify(context)) return@withLock
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_OPEN_CENTER
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val pending = PendingIntent.getActivity(context, NOTIFICATION_ID, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                fun digits(n: Int) = PersianText.toPersianDigits(n.toString())
                val title = context.getString(R.string.expiry_notification_title, digits(counts.total))
                val body = context.getString(R.string.expiry_notification_body, digits(counts.dueToday), digits(counts.overdue))
                val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_expiry_notification)
                    .setContentTitle(context.getString(R.string.app_name)).build()
                val sameDay = prefs.getString("last_day", null) == day
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_expiry_notification)
                    .setContentTitle(title).setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setContentIntent(pending).setAutoCancel(true)
                    .setNumber(counts.total).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(publicVersion).setOnlyAlertOnce(sameDay)
                    .setSilent(sameDay).build()
                try {
                    manager.notify(NOTIFICATION_ID, notification)
                    prefs.edit().putString("last_key", key).putString("last_day", day).apply()
                } catch (_: SecurityException) {
                    // A revoked permission is not a transient database failure.
                }
            }
        }
    }
}
