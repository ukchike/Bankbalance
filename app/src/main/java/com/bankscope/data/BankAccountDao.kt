package com.bankscope.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY institutionName COLLATE NOCASE, accountIdentifier")
    fun observeAllAccounts(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE lower(institutionName) = lower(:institution) AND upper(currencyCode) = upper(:currency)")
    suspend fun getAccountsForInstitution(institution: String, currency: String): List<BankAccountEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: BankAccountEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("SELECT * FROM bank_transactions ORDER BY COALESCE(eventAt, receivedAt) DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM bank_transactions WHERE reviewStatus != 'MATCHED' ORDER BY receivedAt DESC")
    fun observeTransactionsNeedingReview(): Flow<List<TransactionEntity>>

    @Query("UPDATE bank_transactions SET accountId = :accountId, reviewStatus = 'MATCHED' WHERE transactionId = :transactionId")
    suspend fun assignTransaction(transactionId: String, accountId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertObservation(observation: BalanceObservationEntity): Long

    @Query("UPDATE bank_accounts SET isIncluded = :included WHERE accountId = :accountId")
    suspend fun setIncluded(accountId: String, included: Boolean)

    @Query("UPDATE bank_accounts SET isHidden = :hidden WHERE accountId = :accountId")
    suspend fun setHidden(accountId: String, hidden: Boolean)

    @Query("""
        UPDATE bank_accounts
        SET latestBalanceAmount = :amount,
            latestBalanceType = :balanceType,
            latestBalanceEventAt = :eventAt,
            lastReceivedAt = :receivedAt
        WHERE accountId = :accountId
          AND (
            latestBalanceEventAt IS NULL
            OR (:eventAt IS NOT NULL AND :eventAt >= latestBalanceEventAt)
            OR (:eventAt IS NULL AND latestBalanceEventAt IS NULL AND (lastReceivedAt IS NULL OR :receivedAt >= lastReceivedAt))
          )
    """)
    suspend fun updateLatestBalanceIfNewer(
        accountId: String,
        amount: String,
        balanceType: String,
        eventAt: Long?,
        receivedAt: Long
    )
}
