package com.bankscope.notifications

import android.content.Context

class NotificationDiagnostics(context: Context) {
    private val prefs = context.getSharedPreferences("capture_diagnostics", Context.MODE_PRIVATE)

    fun recordReceived(source: String) {
        prefs.edit().putInt("received", prefs.getInt("received", 0) + 1)
            .putString("last_source", source.take(100))
            .putLong("last_event_at", System.currentTimeMillis()).apply()
    }

    fun recordIgnored(reason: String) {
        prefs.edit().putInt("ignored", prefs.getInt("ignored", 0) + 1)
            .putString("last_result", "Ignored: ${reason.take(180)}").apply()
    }

    fun recordParsed(institution: String, transactionRecorded: Boolean) {
        prefs.edit().putInt("parsed", prefs.getInt("parsed", 0) + 1)
            .putString("last_result", if (transactionRecorded) "Transaction recorded: $institution" else "Parsed alert: $institution; no transaction amount")
            .apply()
    }

    fun snapshot(): Map<String, String> = mapOf(
        "received" to prefs.getInt("received", 0).toString(),
        "parsed" to prefs.getInt("parsed", 0).toString(),
        "ignored" to prefs.getInt("ignored", 0).toString(),
        "last_source" to (prefs.getString("last_source", "None") ?: "None"),
        "last_result" to (prefs.getString("last_result", "No events processed yet") ?: "No events processed yet")
    )
}
