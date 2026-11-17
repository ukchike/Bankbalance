package com.bankscope.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bank_transactions",
    indices = [
        Index("accountId"),
        Index("eventAt"),
        Index(value = ["deduplicationFingerprint"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey val transactionId: String,
    val accountId: String?,
    val institutionName: String,
    val accountHint: String?,
    val direction: String?,
    val amount: String?,
    val currencyCode: String,
    val description: String?,
    val eventAt: Long?,
    val receivedAt: Long,
    val sourceType: String,
    val confidence: Double,
    val deduplicationFingerprint: String,
    val reviewStatus: String
)
