package com.bankscope.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionCategoriserTest {
    @Test fun recognisesAirtimeAndUtilities() {
        assertEquals(TransactionCategory.AIRTIME_DATA, TransactionCategoriser.suggest("Airtime purchase", "DEBIT"))
        assertEquals(TransactionCategory.UTILITIES, TransactionCategoriser.suggest("Electricity token", "DEBIT"))
    }

    @Test fun identifiesOwnAccountTransfersBeforeIncome() {
        assertEquals(TransactionCategory.TRANSFER, TransactionCategoriser.suggest("Own account transfer", "CREDIT"))
    }

    @Test fun creditsDefaultToIncome() {
        assertEquals(TransactionCategory.INCOME, TransactionCategoriser.suggest("Salary", "CREDIT"))
    }
}
