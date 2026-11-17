package com.bankscope.data

import com.bankscope.parser.ParsedBankMessage
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.UUID

class TransactionReviewRepository(private val dao: BankAccountDao) {
    suspend fun record(
        parsed: ParsedBankMessage,
        sourceType: String,
        receivedAt: Long,
        sourceFingerprint: String? = null
    ): TransactionEntity? {
        val amount = parsed.transactionAmount?.let {
            runCatching { BigDecimal(it).toPlainString() }.getOrNull()
        }
        if (amount == null || parsed.transactionDirection !in setOf("DEBIT", "CREDIT")) return null

        val accounts = dao.getAccountsForInstitution(parsed.institution, parsed.currencyCode)
        val candidates = accounts.filter { account ->
            accountHintMatches(parsed.accountHint, account.accountIdentifier)
        }
        val matchedAccount = candidates.singleOrNull()
        val fingerprint = sourceFingerprint ?: canonicalFingerprint(parsed)
        val row = TransactionEntity(
            transactionId = UUID.randomUUID().toString(),
            accountId = matchedAccount?.accountId,
            institutionName = parsed.institution,
            accountHint = parsed.accountHint,
            direction = parsed.transactionDirection,
            amount = amount,
            currencyCode = parsed.currencyCode,
            description = parsed.descriptionHint,
            eventAt = parsed.eventAtEpochMillis,
            receivedAt = receivedAt,
            sourceType = sourceType,
            confidence = parsed.confidence,
            deduplicationFingerprint = fingerprint,
            category = TransactionCategoriser.suggest(parsed.descriptionHint, parsed.transactionDirection).name,
            isInternalTransfer = TransactionCategoriser.suggest(parsed.descriptionHint, parsed.transactionDirection) == TransactionCategory.TRANSFER,
            reviewStatus = when {
                matchedAccount == null -> "NEEDS_ACCOUNT_REVIEW"
                parsed.confidence < 0.85 -> "NEEDS_CONFIRMATION"
                else -> "MATCHED"
            }
        )
        dao.insertTransaction(row)
        return row
    }

    private fun accountHintMatches(hint: String?, accountIdentifier: String): Boolean {
        if (hint.isNullOrBlank()) return false
        val a = hint.filter(Char::isDigit)
        val b = accountIdentifier.filter(Char::isDigit)
        return a.length >= 4 && b.length >= 4 && b.endsWith(a.takeLast(4))
    }

    private fun canonicalFingerprint(message: ParsedBankMessage): String {
        val stable = listOf(
            message.institution.lowercase(),
            message.accountHint?.filter(Char::isDigit)?.takeLast(4).orEmpty(),
            message.currencyCode,
            message.transactionDirection.orEmpty(),
            message.transactionAmount.orEmpty(),
            message.eventAtEpochMillis?.toString().orEmpty(),
            message.descriptionHint.orEmpty().lowercase()
        ).joinToString("|")
        return MessageDigest.getInstance("SHA-256")
            .digest(stable.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
