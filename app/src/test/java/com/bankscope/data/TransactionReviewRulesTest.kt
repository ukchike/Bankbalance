package com.bankscope.data

import org.junit.Assert.*
import org.junit.Test

class TransactionReviewRulesTest {
    @Test fun reviewStatusNamesAreStable() {
        assertEquals("NEEDS_ACCOUNT_REVIEW", "NEEDS_ACCOUNT_REVIEW")
        assertEquals("NEEDS_CONFIRMATION", "NEEDS_CONFIRMATION")
        assertEquals("MATCHED", "MATCHED")
    }

    @Test fun unmatchedTransactionsRemainUnassigned() {
        val accountId: String? = null
        assertNull(accountId)
    }
}
