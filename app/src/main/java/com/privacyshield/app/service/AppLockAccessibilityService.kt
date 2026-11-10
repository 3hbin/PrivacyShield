package com.privacyshield.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.privacyshield.app.data.ShieldPreferences
import com.privacyshield.app.ui.lock.LockActivity

/**
 * Accessibility service: only receives the foreground package name.
 * Does not read screen content or text.
 */
class AppLockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        CurrentForeground.set(pkg)
        IndicatorOverlayService.requestUpdate(this)

        val prefs = ShieldPreferences.get(this)
        if (prefs.isLocked(pkg) && !prefs.isTemporarilyUnlocked(pkg)) {
            val intent = Intent(this, LockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(LockActivity.EXTRA_PACKAGE, pkg)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        IndicatorOverlayService.requestUpdate(this)
    }
}

object CurrentForeground {
    @Volatile
    var packageName: String? = null
        private set

    fun set(pkg: String) {
        packageName = pkg
    }
}
