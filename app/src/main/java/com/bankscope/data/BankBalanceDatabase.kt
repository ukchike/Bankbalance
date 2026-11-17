package com.bankscope.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BankAccountEntity::class, BalanceObservationEntity::class, TransactionEntity::class, BudgetEntity::class],
    version = 3,
    exportSchema = false
)
abstract class BankBalanceDatabase : RoomDatabase() {
    abstract fun bankAccountDao(): BankAccountDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS bank_transactions (
                        transactionId TEXT NOT NULL PRIMARY KEY,
                        accountId TEXT,
                        institutionName TEXT NOT NULL,
                        accountHint TEXT,
                        direction TEXT,
                        amount TEXT,
                        currencyCode TEXT NOT NULL,
                        description TEXT,
                        eventAt INTEGER,
                        receivedAt INTEGER NOT NULL,
                        sourceType TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        deduplicationFingerprint TEXT NOT NULL,
                        reviewStatus TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_bank_transactions_accountId ON bank_transactions(accountId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_bank_transactions_eventAt ON bank_transactions(eventAt)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_bank_transactions_deduplicationFingerprint ON bank_transactions(deduplicationFingerprint)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE bank_transactions ADD COLUMN category TEXT NOT NULL DEFAULT 'OTHER'")
                database.execSQL("ALTER TABLE bank_transactions ADD COLUMN isInternalTransfer INTEGER NOT NULL DEFAULT 0")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS category_budgets (
                        budgetId TEXT NOT NULL PRIMARY KEY,
                        category TEXT NOT NULL,
                        currencyCode TEXT NOT NULL,
                        monthKey TEXT NOT NULL,
                        limitAmount TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_category_budgets_category_currencyCode_monthKey ON category_budgets(category, currencyCode, monthKey)")
            }
        }

        @Volatile private var INSTANCE: BankBalanceDatabase? = null
        fun getInstance(context: Context): BankBalanceDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BankBalanceDatabase::class.java,
                    "bankbalance.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}
