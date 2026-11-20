package com.bankscope.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.bankscope.data.BankBalanceDatabase
import com.bankscope.data.BalanceObservationEntity
import com.bankscope.data.TransactionReviewRepository
import com.bankscope.parser.BankParseResult
import com.bankscope.parser.NigerianBankMessageParser
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.UUID
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
                        val dao = BankBalanceDatabase.getInstance(applicationContext).bankAccountDao()
                        val transaction = TransactionReviewRepository(dao).record(
                            parsed = parsed,
                            sourceType = if (packageName == "com.android.mms" || packageName == "com.google.android.apps.messaging") "SMS_NOTIFICATION" else "APP_NOTIFICATION",
                            receivedAt = receivedAt,
                            sourceFingerprint = sha256("$packageName|$body|$receivedAt")
                        )
                        diagnostics.recordParsed(parsed.institution, transaction != null)
                        persistBalanceIfSafe(parsed, receivedAt, packageName, body)
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

    private suspend fun persistBalanceIfSafe(
        parsed: com.bankscope.parser.ParsedBankMessage,
        receivedAt: Long,
        packageName: String,
        body: String
    ) {
        val balance = parsed.reportedBalance ?: return
        val dao = BankBalanceDatabase.getInstance(applicationContext).bankAccountDao()
        val accounts = dao.getAccountsForInstitution(parsed.institution, parsed.currencyCode)
        val matched = accounts.firstOrNull { account ->
            accountHintMatches(parsed.accountHint, account.accountIdentifier)
        } ?: if (accounts.size == 1 && parsed.accountHint == null) accounts.first() else null

        if (matched == null) {
            Log.w(TAG, "Transaction retained for review; no unambiguous balance account match for ${parsed.institution}")
            return
        }

        val observation = BalanceObservationEntity(
            observationId = UUID.randomUUID().toString(),
            accountId = matched.accountId,
            amount = BigDecimal(balance).toPlainString(),
            currencyCode = parsed.currencyCode,
            balanceType = parsed.balanceType,
            eventAt = parsed.eventAtEpochMillis,
            receivedAt = receivedAt,
            sourceType = "APP_NOTIFICATION",
            parserVersion = parsed.parserVersion,
            confidence = parsed.confidence,
            deduplicationFingerprint = sha256("$packageName|$body|$receivedAt")
        )
        val inserted = dao.insertObservation(observation)
        if (inserted != -1L) {
            dao.updateLatestBalanceIfNewer(
                accountId = matched.accountId,
                amount = observation.amount,
                balanceType = observation.balanceType,
                eventAt = observation.eventAt,
                receivedAt = receivedAt
            )
        }
    }

    private fun accountHintMatches(hint: String?, accountIdentifier: String): Boolean {
        if (hint.isNullOrBlank()) return false
        val digitsHint = hint.filter(Char::isDigit)
        val digitsAccount = accountIdentifier.filter(Char::isDigit)
        return digitsHint.length >= 4 && digitsAccount.length >= 4 &&
            digitsAccount.endsWith(digitsHint.takeLast(4))
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object { private const val TAG = "BankBalanceListener" }
}
