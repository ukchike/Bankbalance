package com.bankscope.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_budgets",
    indices = [Index(value = ["category", "currencyCode", "monthKey"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey val budgetId: String,
    val category: String,
    val currencyCode: String,
    val monthKey: String,
    val limitAmount: String,
    val createdAt: Long
)
