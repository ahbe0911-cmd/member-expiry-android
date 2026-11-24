package ir.memberexpiry.manager.core.browser

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent

object RegistrationLauncher {
    const val URL = "https://www.samanpl.ir/reg"
    private const val CHROME = "com.android.chrome"
    enum class Outcome { CHROME_TAB, OTHER_CUSTOM_TAB, EXTERNAL_BROWSER, UNAVAILABLE }

    fun open(activity: Activity): Outcome {
        val uri = Uri.parse(URL)
        val manager = activity.packageManager
        val chromeService = Intent("android.support.customtabs.action.CustomTabsService").setPackage(CHROME)
        val chromeAvailable = manager.resolveService(chromeService, 0) != null
        val preferred = CustomTabsClient.getPackageName(activity, null)
        val candidates = listOfNotNull(CHROME.takeIf { chromeAvailable }, preferred).distinct()
        for (pkg in candidates) {
            try {
                val tab = CustomTabsIntent.Builder().setShowTitle(true).build()
                tab.intent.setPackage(pkg)
                tab.launchUrl(activity, uri)
                return if (pkg == CHROME) Outcome.CHROME_TAB else Outcome.OTHER_CUSTOM_TAB
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        return try {
            // External HTTPS handler, rather than a WebView or an SSL-bypass implementation.
            // ACTION_VIEW + CATEGORY_BROWSABLE is the standard HTTPS fallback.
            activity.startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
            Outcome.EXTERNAL_BROWSER
        } catch (_: ActivityNotFoundException) {
            Outcome.UNAVAILABLE
        } catch (_: SecurityException) {
            Outcome.UNAVAILABLE
        }
    }
}
