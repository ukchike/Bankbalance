package com.bankscope.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey val accountId: String,
    val institutionName: String,
    val accountIdentifier: String,
    val currencyCode: String,
    val accountType: String?,
    val latestBalanceAmount: String?,
    val latestBalanceType: String = "UNKNOWN",
    val latestBalanceEventAt: Long? = null,
    val lastReceivedAt: Long? = null,
    val isIncluded: Boolean = true,
    val isHidden: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
