package ir.memberexpiry.manager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.memberexpiry.manager.core.browser.RegistrationLauncher
import ir.memberexpiry.manager.notifications.ExpiryNotifier
import ir.memberexpiry.manager.notifications.ExpiryScheduler
import ir.memberexpiry.manager.ui.screens.MemberApp
import ir.memberexpiry.manager.ui.theme.MemberExpiryTheme
import ir.memberexpiry.manager.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    private var notificationRequest by mutableStateOf(0)
    private var notificationAllowed by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        notificationRequest = savedInstanceState?.getInt("notification_request")
            ?: if (intent.action == ExpiryNotifier.ACTION_OPEN_CENTER) 1 else 0
        notificationAllowed = ExpiryNotifier.canNotify(this)
        val factory = MemberViewModelFactory(application as MemberExpiryApplication)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MemberExpiryTheme {
                    val members: MembersViewModel = viewModel(factory = factory)
                    val statistics: StatisticsViewModel = viewModel(factory = factory)
                    val center: NotificationCenterViewModel = viewModel(factory = factory)
                    val transfer: TransferViewModel = viewModel(factory = factory)
                    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                        notificationAllowed = ExpiryNotifier.canNotify(this)
                        if (notificationAllowed) ExpiryScheduler.checkNow(this)
                    }
                    MemberApp(members, statistics, center, transfer, notificationRequest, notificationAllowed,
                        onRegistration = ::openRegistration,
                        onEnableNotifications = {
                            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else openNotificationSettings()
                        }, onSettings = ::openNotificationSettings,
                        onCheckNow = {
                            ExpiryScheduler.checkNow(this)
                            center.retry()
                            Toast.makeText(this, R.string.expiry_check_queued, Toast.LENGTH_SHORT).show()
                        })
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ExpiryNotifier.ACTION_OPEN_CENTER) notificationRequest++
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("notification_request", notificationRequest)
        super.onSaveInstanceState(outState)
    }
    override fun onResume() {
        super.onResume()
        val allowed = ExpiryNotifier.canNotify(this)
        if (allowed && !notificationAllowed) ExpiryScheduler.checkNow(this)
        notificationAllowed = allowed
    }
    private fun openRegistration() {
        if (RegistrationLauncher.open(this) == RegistrationLauncher.Outcome.UNAVAILABLE) {
            Toast.makeText(this, R.string.no_browser, Toast.LENGTH_LONG).show()
        }
    }
    private fun openNotificationSettings() {
        val settings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        try { startActivity(settings) } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(this, R.string.notification_settings_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}
