package com.bankscope.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds a full local export of a user's data (accounts, balance history, transactions and
 * budgets) as JSON, so a user can get their data out independent of the transactions-only CSV
 * export. Mirrors the account-deletion boundary: nothing here is uploaded anywhere, it is written
 * to wherever the caller points the resulting string.
 */
object DataExporter {
    fun toJson(
        accounts: List<BankAccountEntity>,
        observations: List<BalanceObservationEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>
    ): String {
        val root = JSONObject()
        root.put("exportedAt", System.currentTimeMillis())
        root.put("accounts", JSONArray(accounts.map(::accountJson)))
        root.put("balanceObservations", JSONArray(observations.map(::observationJson)))
        root.put("transactions", JSONArray(transactions.map(::transactionJson)))
        root.put("budgets", JSONArray(budgets.map(::budgetJson)))
        return root.toString(2)
    }

    private fun accountJson(account: BankAccountEntity) = JSONObject().apply {
        put("accountId", account.accountId)
        put("institutionName", account.institutionName)
        put("accountIdentifier", account.accountIdentifier)
        put("currencyCode", account.currencyCode)
        put("accountType", account.accountType)
        put("latestBalanceAmount", account.latestBalanceAmount)
        put("latestBalanceType", account.latestBalanceType)
        put("latestBalanceEventAt", account.latestBalanceEventAt)
        put("lastReceivedAt", account.lastReceivedAt)
        put("isIncluded", account.isIncluded)
        put("isHidden", account.isHidden)
        put("createdAt", account.createdAt)
    }

    private fun observationJson(observation: BalanceObservationEntity) = JSONObject().apply {
        put("observationId", observation.observationId)
        put("accountId", observation.accountId)
        put("amount", observation.amount)
        put("currencyCode", observation.currencyCode)
        put("balanceType", observation.balanceType)
        put("eventAt", observation.eventAt)
        put("receivedAt", observation.receivedAt)
        put("sourceType", observation.sourceType)
        put("confidence", observation.confidence)
    }

    private fun transactionJson(transaction: TransactionEntity) = JSONObject().apply {
        put("transactionId", transaction.transactionId)
        put("accountId", transaction.accountId)
        put("institutionName", transaction.institutionName)
        put("accountHint", transaction.accountHint)
        put("direction", transaction.direction)
        put("amount", transaction.amount)
        put("currencyCode", transaction.currencyCode)
        put("description", transaction.description)
        put("eventAt", transaction.eventAt)
        put("receivedAt", transaction.receivedAt)
        put("sourceType", transaction.sourceType)
        put("confidence", transaction.confidence)
        put("reviewStatus", transaction.reviewStatus)
        put("category", transaction.category)
        put("isInternalTransfer", transaction.isInternalTransfer)
    }

    private fun budgetJson(budget: BudgetEntity) = JSONObject().apply {
        put("budgetId", budget.budgetId)
        put("category", budget.category)
        put("currencyCode", budget.currencyCode)
        put("monthKey", budget.monthKey)
        put("limitAmount", budget.limitAmount)
        put("createdAt", budget.createdAt)
    }
}
