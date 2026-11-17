package com.bankscope.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "balance_observations",
    indices = [Index("accountId"), Index(value = ["deduplicationFingerprint"], unique = true)]
)
data class BalanceObservationEntity(
    @PrimaryKey val observationId: String,
    val accountId: String,
    val amount: String,
    val currencyCode: String,
    val balanceType: String,
    val eventAt: Long?,
    val receivedAt: Long,
    val sourceType: String,
    val parserVersion: String?,
    val confidence: Double,
    val deduplicationFingerprint: String
)
