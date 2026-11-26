package com.bankscope.data

import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class BankAccountRepository(private val dao: BankAccountDao) {
    fun observeAll(): Flow<List<BankAccountEntity>> = dao.observeAllAccounts()

    suspend fun addAccount(
        institution: String,
        identifier: String,
        currency: String,
        accountType: String?,
        openingBalance: String?
    ) {
        val now = System.currentTimeMillis()
        val normalizedCurrency = currency.trim().uppercase()
        val amount = openingBalance?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { BigDecimal(it).toPlainString() }
        val id = UUID.randomUUID().toString()
        dao.insertAccount(
            BankAccountEntity(
                accountId = id,
                institutionName = institution.trim(),
                accountIdentifier = identifier.trim(),
                currencyCode = normalizedCurrency,
                accountType = accountType?.trim()?.takeIf { it.isNotEmpty() },
                latestBalanceAmount = amount,
                latestBalanceType = if (amount == null) "UNKNOWN" else "MANUAL",
                latestBalanceEventAt = if (amount == null) null else now,
                lastReceivedAt = if (amount == null) null else now
            )
        )
        if (amount != null) {
            dao.insertObservation(
                BalanceObservationEntity(
                    observationId = UUID.randomUUID().toString(),
                    accountId = id,
                    amount = amount,
                    currencyCode = normalizedCurrency,
                    balanceType = "MANUAL",
                    eventAt = now,
                    receivedAt = now,
                    sourceType = "MANUAL",
                    parserVersion = null,
                    confidence = 1.0,
                    deduplicationFingerprint = "manual:$id:$now"
                )
            )
        }
    }

    suspend fun setIncluded(id: String, included: Boolean) = dao.setIncluded(id, included)
    suspend fun setHidden(id: String, hidden: Boolean) = dao.setHidden(id, hidden)

    /**
     * Manually corrects an account's balance, e.g. when the user notices the last
     * SMS/notification-reported figure is wrong. Always takes effect immediately, and is
     * recorded as a MANUAL balance observation alongside the automatically-collected history.
     */
    suspend fun adjustBalance(accountId: String, currencyCode: String, newAmount: String) {
        val now = System.currentTimeMillis()
        val amount = BigDecimal(newAmount.trim().replace(",", "")).toPlainString()
        dao.insertObservation(
            BalanceObservationEntity(
                observationId = UUID.randomUUID().toString(),
                accountId = accountId,
                amount = amount,
                currencyCode = currencyCode,
                balanceType = "MANUAL",
                eventAt = now,
                receivedAt = now,
                sourceType = "MANUAL_ADJUSTMENT",
                parserVersion = null,
                confidence = 1.0,
                deduplicationFingerprint = "manual-adjust:$accountId:$now"
            )
        )
        dao.setManualBalance(accountId, amount, now)
    }

    /** Permanently deletes an account and its balance/transaction history. Not reversible. */
    suspend fun deleteAccount(id: String) = dao.deleteAccountAndHistory(id)
}
