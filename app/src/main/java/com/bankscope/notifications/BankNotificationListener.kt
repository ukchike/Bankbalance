package com.bankscope.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.bankscope.data.BankBalanceDatabase
import com.bankscope.data.BankAccountEntity
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
        val packageName = sbn.packageName ?: return
        val store = NotificationCaptureStore(this)
        if (!store.isEnabled()) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }
            .orEmpty()
        val appLabel = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault("")
        val sourceName = NotificationSources.resolve(packageName, title, appLabel) ?: return
        if (!store.isSourceEnabled(packageName)) return
        val body = (listOf(title, text) + lines).filter { it.isNotBlank() }.distinct().joinToString("\n")
        if (body.isBlank()) return
        val receivedAt = sbn.postTime
        serviceScope.launch {
            try {
                when (val result = parser.parse(sourceName, body, receivedAt)) {
                    is BankParseResult.Success -> persistObservation(result.message, receivedAt, packageName, body)
                    is BankParseResult.Rejected -> Log.d(TAG, "Ignored notification: ${result.reason}")
                }
            } catch (error: Exception) {
                Log.e(TAG, "Notification processing failed", error)
            }
        }
    }

    private suspend fun persistObservation(
        parsed: com.bankscope.parser.ParsedBankMessage,
        receivedAt: Long,
        packageName: String,
        body: String
    ) {
        val db = BankBalanceDatabase.getInstance(applicationContext)
        val dao = db.bankAccountDao()
        if (parsed.reportedBalance == null) {
            TransactionReviewRepository(dao).record(
                parsed = parsed,
                sourceType = "APP_NOTIFICATION",
                receivedAt = receivedAt
            )
            Log.i(TAG, "Recorded transaction alert without a balance from $packageName")
            return
        }
        val accounts = dao.getAccountsForInstitution(parsed.institution, parsed.currencyCode)
        val matched = accounts.firstOrNull { account ->
            accountHintMatches(parsed.accountHint, account.accountIdentifier)
        } ?: if (accounts.size == 1 && parsed.accountHint == null) accounts.first() else null

        if (matched == null) {
            Log.w(TAG, "No unambiguous account match for ${parsed.institution}; notification not applied")
            return
        }

        val fingerprint = sha256("$packageName|$body|$receivedAt")
        val observation = BalanceObservationEntity(
            observationId = UUID.randomUUID().toString(),
            accountId = matched.accountId,
            amount = BigDecimal(parsed.reportedBalance!!).toPlainString(),
            currencyCode = parsed.currencyCode,
            balanceType = parsed.balanceType,
            eventAt = parsed.eventAtEpochMillis,
            receivedAt = receivedAt,
            sourceType = "APP_NOTIFICATION",
            parserVersion = parsed.parserVersion,
            confidence = parsed.confidence,
            deduplicationFingerprint = fingerprint
        )
        val inserted = dao.insertObservation(observation)
        TransactionReviewRepository(dao).record(
            parsed = parsed,
            sourceType = "APP_NOTIFICATION",
            receivedAt = receivedAt
        )
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
        if (digitsHint.length < 4 || digitsAccount.length < 4) return false
        val suffix = digitsHint.takeLast(4)
        return digitsAccount.endsWith(suffix)
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BankBalanceListener"
    }
}
