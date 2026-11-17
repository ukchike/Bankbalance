package com.bankscope.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY institutionName COLLATE NOCASE, accountIdentifier")
    fun observeAllAccounts(): Flow<List<BankAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: BankAccountEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertObservation(observation: BalanceObservationEntity): Long

    @Query("UPDATE bank_accounts SET isIncluded = :included WHERE accountId = :accountId")
    suspend fun setIncluded(accountId: String, included: Boolean)

    @Query("UPDATE bank_accounts SET isHidden = :hidden WHERE accountId = :accountId")
    suspend fun setHidden(accountId: String, hidden: Boolean)
}
