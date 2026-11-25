package com.bankscope.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("UPDATE bank_transactions SET category = :category, isInternalTransfer = :isInternalTransfer WHERE transactionId = :transactionId")
    suspend fun updateTransactionCategory(transactionId: String, category: String, isInternalTransfer: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Query("SELECT * FROM category_budgets WHERE monthKey = :monthKey ORDER BY category")
    fun observeBudgets(monthKey: String): Flow<List<BudgetEntity>>

    @Query("DELETE FROM category_budgets WHERE budgetId = :budgetId")
    suspend fun deleteBudget(budgetId: String)

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

    @Query("DELETE FROM bank_accounts WHERE accountId = :accountId")
    suspend fun deleteAccountRow(accountId: String)

    @Query("DELETE FROM balance_observations WHERE accountId = :accountId")
    suspend fun deleteObservationsForAccount(accountId: String)

    @Query("DELETE FROM bank_transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsForAccount(accountId: String)

    /**
     * Deletes an account along with the balance observations and transactions tied to it.
     * Transactions still pending account assignment (accountId IS NULL) are left untouched
     * since they aren't this account's history.
     */
    @Transaction
    suspend fun deleteAccountAndHistory(accountId: String) {
        deleteObservationsForAccount(accountId)
        deleteTransactionsForAccount(accountId)
        deleteAccountRow(accountId)
    }

    @Query("SELECT * FROM bank_accounts ORDER BY institutionName COLLATE NOCASE, accountIdentifier")
    suspend fun getAllAccountsOnce(): List<BankAccountEntity>

    @Query("SELECT * FROM balance_observations ORDER BY receivedAt")
    suspend fun getAllObservationsOnce(): List<BalanceObservationEntity>

    @Query("SELECT * FROM bank_transactions ORDER BY receivedAt")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Query("SELECT * FROM category_budgets ORDER BY monthKey, category")
    suspend fun getAllBudgetsOnce(): List<BudgetEntity>
}
