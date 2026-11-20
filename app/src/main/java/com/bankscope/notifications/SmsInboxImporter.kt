package com.bankscope.notifications

import android.content.Context
import android.provider.Telephony
import com.bankscope.data.BankAccountDao
import com.bankscope.data.TransactionReviewRepository
import com.bankscope.parser.BankParseResult
import com.bankscope.parser.NigerianBankMessageParser
import java.security.MessageDigest

data class SmsImportResult(val scanned: Int, val imported: Int, val ignored: Int)

class SmsInboxImporter(private val context: Context, private val dao: BankAccountDao) {
    suspend fun importRecent(days: Int = 90): SmsImportResult {
        val parser = NigerianBankMessageParser()
        val repository = TransactionReviewRepository(dao)
        val cutoff = System.currentTimeMillis() - days.toLong() * 24 * 60 * 60 * 1000
        var scanned = 0
        var imported = 0
        var ignored = 0
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(cutoff.toString()),
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (cursor.moveToNext()) {
                scanned++
                val address = cursor.getString(addressIndex).orEmpty()
                val body = cursor.getString(bodyIndex).orEmpty()
                val receivedAt = cursor.getLong(dateIndex)
                val institution = NotificationSources.resolve("", address, address, body)
                if (institution == null) {
                    ignored++
                    continue
                }
                when (val result = parser.parse(institution, body, receivedAt)) {
                    is BankParseResult.Success -> {
                        val fingerprint = MessageDigest.getInstance("SHA-256")
                            .digest("SMS|$address|$receivedAt|$body".toByteArray())
                            .joinToString("") { "%02x".format(it) }
                        val row = repository.record(
                            parsed = result.message,
                            sourceType = "SMS_INBOX_IMPORT",
                            receivedAt = receivedAt,
                            sourceFingerprint = fingerprint
                        )
                        if (row != null) imported++ else ignored++
                    }
                    is BankParseResult.Rejected -> ignored++
                }
            }
        }
        return SmsImportResult(scanned, imported, ignored)
    }
}
