package com.bankscope.notifications

import android.content.Context
import android.content.SharedPreferences

class NotificationCaptureStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("notification_capture", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean("capture_enabled", true)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("capture_enabled", enabled).apply()
    }

    fun isSourceEnabled(packageName: String): Boolean =
        prefs.getBoolean("source_$packageName", true)

    fun setSourceEnabled(packageName: String, enabled: Boolean) {
        prefs.edit().putBoolean("source_$packageName", enabled).apply()
    }

    fun isPackageAllowed(packageName: String): Boolean =
        NotificationSources.displayName(packageName) != null && isSourceEnabled(packageName)
}
