package com.bankscope.data

import com.bankscope.parser.ParsedBankMessage
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.UUID

/**
 * Single entry point for turning a parsed bank message into persisted rows, regardless of
 * which channel it arrived on (SMS, app notification or email). Fingerprints here are derived
 * purely from the message's own content, never from source-specific fields like package name or
 * SMS sender, so the same real-world event reported through two different channels collides on
 * the same row instead of being double-counted.
 */
class TransactionReviewRepository(private val dao: BankAccountDao) {

    suspend fun record(
        parsed: ParsedBankMessage,
        sourceType: String,
        receivedAt: Long
    ): TransactionEntity? {
        val amount = parsed.transactionAmount?.let {
            runCatching { BigDecimal(it).toPlainString() }.getOrNull()
        }
        if (amount == null || parsed.transactionDirection !in setOf("DEBIT", "CREDIT")) return null

        val matchedAccount = matchAccount(parsed)
        val fingerprint = canonicalTransactionFingerprint(parsed)
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
        val inserted = dao.insertTransaction(row)
        return if (inserted != -1L) row else null
    }

    /**
     * Records a reported balance as an observation and, if it resolves unambiguously to one
     * account, refreshes that account's dashboard projection (subject to the event-time guard in
     * [BankAccountDao.updateLatestBalanceIfNewer]). Returns null if the message carried no
     * balance or the account could not be matched unambiguously; the transaction itself is still
     * recorded separately via [record] for manual review.
     */
    suspend fun recordBalanceObservation(
        parsed: ParsedBankMessage,
        sourceType: String,
        receivedAt: Long
    ): BalanceObservationEntity? {
        val balance = parsed.reportedBalance ?: return null
        val matched = matchAccount(parsed) ?: return null

        val observation = BalanceObservationEntity(
            observationId = UUID.randomUUID().toString(),
            accountId = matched.accountId,
            amount = BigDecimal(balance).toPlainString(),
            currencyCode = parsed.currencyCode,
            balanceType = parsed.balanceType,
            eventAt = parsed.eventAtEpochMillis,
            receivedAt = receivedAt,
            sourceType = sourceType,
            parserVersion = parsed.parserVersion,
            confidence = parsed.confidence,
            deduplicationFingerprint = canonicalObservationFingerprint(matched.accountId, parsed, receivedAt)
        )
        val inserted = dao.insertObservation(observation)
        if (inserted == -1L) return null

        dao.updateLatestBalanceIfNewer(
            accountId = matched.accountId,
            amount = observation.amount,
            balanceType = observation.balanceType,
            eventAt = observation.eventAt,
            receivedAt = receivedAt
        )
        return observation
    }

    private suspend fun matchAccount(parsed: ParsedBankMessage): BankAccountEntity? {
        val accounts = dao.getAccountsForInstitution(parsed.institution, parsed.currencyCode)
        val hintMatches = accounts.filter { accountHintMatches(parsed.accountHint, it.accountIdentifier) }
        hintMatches.singleOrNull()?.let { return it }
        return if (parsed.accountHint == null && accounts.size == 1) accounts.first() else null
    }

    private fun accountHintMatches(hint: String?, accountIdentifier: String): Boolean {
        if (hint.isNullOrBlank()) return false
        val a = hint.filter(Char::isDigit)
        val b = accountIdentifier.filter(Char::isDigit)
        return a.length >= 4 && b.length >= 4 && b.endsWith(a.takeLast(4))
    }

    private fun canonicalTransactionFingerprint(message: ParsedBankMessage): String {
        val stable = listOf(
            message.institution.lowercase(),
            message.accountHint?.filter(Char::isDigit)?.takeLast(4).orEmpty(),
            message.currencyCode,
            message.transactionDirection.orEmpty(),
            message.transactionAmount.orEmpty(),
            message.eventAtEpochMillis?.toString().orEmpty(),
            message.descriptionHint.orEmpty().lowercase()
        ).joinToString("|")
        return sha256(stable)
    }

    /**
     * Balance snapshots have no description to key on, so identity is the matched account plus
     * currency, balance type and amount. When the parser couldn't extract an event time, event
     * time is bucketed to a 5-minute window of the receipt time instead of using it exactly, so
     * the same alert delivered a few seconds apart on two channels still collides.
     */
    private fun canonicalObservationFingerprint(accountId: String, message: ParsedBankMessage, receivedAt: Long): String {
        val eventBucket = message.eventAtEpochMillis ?: ((receivedAt / FIVE_MINUTES_MILLIS) * FIVE_MINUTES_MILLIS)
        val stable = listOf(
            accountId,
            message.currencyCode,
            message.balanceType,
            message.reportedBalance.orEmpty(),
            eventBucket.toString()
        ).joinToString("|")
        return sha256(stable)
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val FIVE_MINUTES_MILLIS = 5 * 60 * 1000L
    }
}
