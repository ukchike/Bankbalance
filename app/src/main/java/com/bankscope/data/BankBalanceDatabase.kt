package com.bankscope.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BankAccountEntity::class, BalanceObservationEntity::class], version = 1, exportSchema = false)
abstract class BankBalanceDatabase : RoomDatabase() {
    abstract fun bankAccountDao(): BankAccountDao

    companion object {
        @Volatile private var INSTANCE: BankBalanceDatabase? = null
        fun getInstance(context: Context): BankBalanceDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BankBalanceDatabase::class.java,
                    "bankbalance.db"
                ).build().also { INSTANCE = it }
            }
    }
}
