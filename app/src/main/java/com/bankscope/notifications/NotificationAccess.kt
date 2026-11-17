package com.bankscope.notifications

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

object NotificationAccess {
    fun isGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val component = context.packageName + "/" + BankNotificationListener::class.java.name
        return enabled.split(":").any { TextUtils.equals(it, component) }
    }

    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
