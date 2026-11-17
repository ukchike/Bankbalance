package com.bankscope.gmail

import com.bankscope.parser.BankParseResult
import com.bankscope.parser.NigerianBankMessageParser
import com.bankscope.parser.ParsedBankMessage
import java.security.MessageDigest

/**
 * Source-neutral adapter for Gmail message content.
 * OAuth and Gmail API retrieval are intentionally kept outside this parser boundary.
 */
class GmailMessageAdapter(
    private val parser: NigerianBankMessageParser = NigerianBankMessageParser()
) {
    fun parse(
        from: String,
        subject: String,
        body: String,
        receivedAtEpochMillis: Long
    ): GmailParseResult {
        val senderContext = "$from $subject"
        return when (val result = parser.parse(senderContext, body, receivedAtEpochMillis)) {
            is BankParseResult.Success -> GmailParseResult.Success(
                message = result.message,
                canonicalFingerprint = canonicalFingerprint(result.message),
                receivedAtEpochMillis = receivedAtEpochMillis
            )
            is BankParseResult.Rejected -> GmailParseResult.Rejected(result.reason)
        }
    }

    private fun canonicalFingerprint(message: ParsedBankMessage): String? {
        val eventAt = message.eventAtEpochMillis ?: return null
        val accountSuffix = message.accountHint
            ?.filter(Char::isDigit)
            ?.takeLast(4)
            .orEmpty()
        val stableContent = listOf(
            message.institution.lowercase(),
            accountSuffix,
            message.currencyCode,
            message.reportedBalance,
            message.balanceType,
            eventAt.toString(),
            message.transactionDirection.orEmpty(),
            message.transactionAmount.orEmpty()
        ).joinToString("|")
        return MessageDigest.getInstance("SHA-256")
            .digest(stableContent.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

sealed interface GmailParseResult {
    data class Success(
        val message: ParsedBankMessage,
        val canonicalFingerprint: String?,
        val receivedAtEpochMillis: Long
    ) : GmailParseResult

    data class Rejected(val reason: String) : GmailParseResult
}
