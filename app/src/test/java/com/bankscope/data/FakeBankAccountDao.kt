package com.bankscope.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory stand-in for the Room-generated [BankAccountDao], so the matching/dedup logic in
 * [TransactionReviewRepository] can be exercised as a plain JVM unit test without Robolectric or
 * an instrumented device. Room annotations are metadata for its own code generator; nothing stops
 * a plain Kotlin class from implementing the same interface for tests.
 */
class FakeBankAccountDao : BankAccountDao {
    val accounts = mutableListOf<BankAccountEntity>()
    val transactions = mutableListOf<TransactionEntity>()
    val observations = mutableListOf<BalanceObservationEntity>()
    val budgets = mutableListOf<BudgetEntity>()

    private val accountsFlow = MutableStateFlow<List<BankAccountEntity>>(emptyList())
    private val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private val budgetsFlow = MutableStateFlow<List<BudgetEntity>>(emptyList())

    override fun observeAllAccounts(): Flow<List<BankAccountEntity>> = accountsFlow

    override suspend fun getAccountsForInstitution(institution: String, currency: String): List<BankAccountEntity> =
        accounts.filter { it.institutionName.equals(institution, ignoreCase = true) && it.currencyCode.equals(currency, ignoreCase = true) }

    override suspend fun insertAccount(account: BankAccountEntity) {
        accounts.add(account)
        accountsFlow.value = accounts.toList()
    }

    override suspend fun insertTransaction(transaction: TransactionEntity): Long {
        if (transactions.any { it.deduplicationFingerprint == transaction.deduplicationFingerprint }) return -1L
        transactions.add(transaction)
        transactionsFlow.value = transactions.toList()
        return transactions.size.toLong()
    }

    override fun observeTransactions(): Flow<List<TransactionEntity>> = transactionsFlow

    override fun observeTransactionsNeedingReview(): Flow<List<TransactionEntity>> = transactionsFlow

    override suspend fun assignTransaction(transactionId: String, accountId: String) {
        val index = transactions.indexOfFirst { it.transactionId == transactionId }
        if (index >= 0) transactions[index] = transactions[index].copy(accountId = accountId, reviewStatus = "MATCHED")
    }

    override suspend fun updateTransactionCategory(transactionId: String, category: String, isInternalTransfer: Boolean) {
        val index = transactions.indexOfFirst { it.transactionId == transactionId }
        if (index >= 0) transactions[index] = transactions[index].copy(category = category, isInternalTransfer = isInternalTransfer)
    }

    override suspend fun upsertBudget(budget: BudgetEntity) {
        budgets.removeAll { it.budgetId == budget.budgetId }
        budgets.add(budget)
        budgetsFlow.value = budgets.toList()
    }

    override fun observeBudgets(monthKey: String): Flow<List<BudgetEntity>> = budgetsFlow

    override suspend fun deleteBudget(budgetId: String) {
        budgets.removeAll { it.budgetId == budgetId }
        budgetsFlow.value = budgets.toList()
    }

    override suspend fun insertObservation(observation: BalanceObservationEntity): Long {
        if (observations.any { it.deduplicationFingerprint == observation.deduplicationFingerprint }) return -1L
        observations.add(observation)
        return observations.size.toLong()
    }

    override suspend fun setIncluded(accountId: String, included: Boolean) {
        val index = accounts.indexOfFirst { it.accountId == accountId }
        if (index >= 0) accounts[index] = accounts[index].copy(isIncluded = included)
        accountsFlow.value = accounts.toList()
    }

    override suspend fun setHidden(accountId: String, hidden: Boolean) {
        val index = accounts.indexOfFirst { it.accountId == accountId }
        if (index >= 0) accounts[index] = accounts[index].copy(isHidden = hidden)
        accountsFlow.value = accounts.toList()
    }

    override suspend fun updateLatestBalanceIfNewer(
        accountId: String,
        amount: String,
        balanceType: String,
        eventAt: Long?,
        receivedAt: Long
    ) {
        val index = accounts.indexOfFirst { it.accountId == accountId }
        if (index < 0) return
        val current = accounts[index]
        val shouldUpdate = when {
            current.latestBalanceEventAt == null -> true
            eventAt != null -> eventAt >= current.latestBalanceEventAt
            else -> current.lastReceivedAt == null || receivedAt >= current.lastReceivedAt
        }
        if (shouldUpdate) {
            accounts[index] = current.copy(
                latestBalanceAmount = amount,
                latestBalanceType = balanceType,
                latestBalanceEventAt = eventAt,
                lastReceivedAt = receivedAt
            )
            accountsFlow.value = accounts.toList()
        }
    }

    override suspend fun setManualBalance(accountId: String, amount: String, eventAt: Long) {
        val index = accounts.indexOfFirst { it.accountId == accountId }
        if (index >= 0) {
            accounts[index] = accounts[index].copy(
                latestBalanceAmount = amount,
                latestBalanceType = "MANUAL",
                latestBalanceEventAt = eventAt,
                lastReceivedAt = eventAt
            )
            accountsFlow.value = accounts.toList()
        }
    }

    override suspend fun deleteAccountRow(accountId: String) {
        accounts.removeAll { it.accountId == accountId }
        accountsFlow.value = accounts.toList()
    }

    override suspend fun deleteObservationsForAccount(accountId: String) {
        observations.removeAll { it.accountId == accountId }
    }

    override suspend fun deleteTransactionsForAccount(accountId: String) {
        transactions.removeAll { it.accountId == accountId }
        transactionsFlow.value = transactions.toList()
    }

    override suspend fun getAllAccountsOnce(): List<BankAccountEntity> = accounts.toList()
    override suspend fun getAllObservationsOnce(): List<BalanceObservationEntity> = observations.toList()
    override suspend fun getAllTransactionsOnce(): List<TransactionEntity> = transactions.toList()
    override suspend fun getAllBudgetsOnce(): List<BudgetEntity> = budgets.toList()
}
