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
}
