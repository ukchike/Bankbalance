package com.bankscope.data

import java.math.BigDecimal

data class CategorySpending(
    val category: String,
    val currencyCode: String,
    val monthKey: String,
    val total: BigDecimal,
    val transactionCount: Int
)
