package com.bankscope.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.bankscope.data.BankBalanceDatabase
import com.bankscope.data.TransactionReviewRepository
import com.bankscope.parser.BankParseResult
import com.bankscope.parser.NigerianBankMessageParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BankNotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser = NigerianBankMessageParser()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val store = NotificationCaptureStore(this)
        if (!store.isEnabled()) return

        val packageName = sbn.packageName ?: return
        val extras = sbn.notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val lines = extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }.orEmpty()
        val ticker = sbn.notification.tickerText?.toString().orEmpty()
        val body = (listOf(title, bigText, text, ticker) + lines)
            .filter { it.isNotBlank() }.distinct().joinToString("\n")
        if (body.isBlank()) return

        val appLabel = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)

        val diagnostics = NotificationDiagnostics(this)
        diagnostics.recordReceived(appLabel)
        val sourceName = NotificationSources.resolve(packageName, title, appLabel, body)
        if (sourceName == null) {
            diagnostics.recordIgnored("Could not identify bank or wallet from app label or notification text")
            return
        }
        if (!store.isSourceEnabled(packageName)) {
            diagnostics.recordIgnored("Source disabled: $appLabel")
            return
        }

        val receivedAt = sbn.postTime
        serviceScope.launch {
            try {
                when (val result = parser.parse(sourceName, body, receivedAt)) {
                    is BankParseResult.Success -> {
                        val parsed = result.message
                        val sourceType = if (packageName == "com.android.mms" || packageName == "com.google.android.apps.messaging") "SMS_NOTIFICATION" else "APP_NOTIFICATION"
                        val dao = BankBalanceDatabase.getInstance(applicationContext).bankAccountDao()
                        val repository = TransactionReviewRepository(dao)
                        val transaction = repository.record(parsed, sourceType, receivedAt)
                        diagnostics.recordParsed(parsed.institution, transaction != null)
                        repository.recordBalanceObservation(parsed, sourceType, receivedAt)
                    }
                    is BankParseResult.Rejected -> {
                        diagnostics.recordIgnored(result.reason)
                        Log.d(TAG, "Ignored alert from $appLabel: ${result.reason}")
                    }
                }
            } catch (error: Exception) {
                diagnostics.recordIgnored("Processing error: ${error.javaClass.simpleName}")
                Log.e(TAG, "Notification processing failed", error)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object { private const val TAG = "BankBalanceListener" }
}
