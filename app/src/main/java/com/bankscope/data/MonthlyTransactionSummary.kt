package com.bankscope.data

data class MonthlyTransactionSummary(
    val currencyCode: String,
    val direction: String?,
    val monthKey: String,
    val total: Double,
    val transactionCount: Int
)
