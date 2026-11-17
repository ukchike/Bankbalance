package com.bankscope.data

import java.math.BigDecimal

data class MonthlyTransactionSummary(
    val currencyCode: String,
    val direction: String?,
    val monthKey: String,
    val total: BigDecimal,
    val transactionCount: Int
)
