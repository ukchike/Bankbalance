package com.bankscope.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BankAccountRepositoryTest {
    private lateinit var dao: FakeBankAccountDao
    private lateinit var repository: BankAccountRepository

    @Before
    fun setUp() {
        dao = FakeBankAccountDao()
        repository = BankAccountRepository(dao)
    }

    @Test
    fun `adjusting balance updates the account immediately`() = runTest {
        repository.addAccount("GTBank", "0031234567", "NGN", null, "1000.00")
        val accountId = dao.accounts.single().accountId

        repository.adjustBalance(accountId, "NGN", "2,500.75")

        val updated = dao.accounts.single()
        assertEquals("2500.75", updated.latestBalanceAmount)
        assertEquals("MANUAL", updated.latestBalanceType)
    }

    @Test
    fun `adjusting balance overrides even a more recently reported automatic balance`() = runTest {
        repository.addAccount("GTBank", "0031234567", "NGN", null, "1000.00")
        val accountId = dao.accounts.single().accountId

        // Simulate a live notification reporting a balance with a far-future event time, the
        // kind of thing the "only if newer" guard would normally protect against being clobbered.
        dao.setIncluded(accountId, true) // no-op, just touching dao for clarity of test setup
        dao.updateLatestBalanceIfNewer(accountId, "9999.00", "AVAILABLE", eventAt = 9_000_000_000_000L, receivedAt = 9_000_000_000_000L)
        assertEquals("9999.00", dao.accounts.single().latestBalanceAmount)

        repository.adjustBalance(accountId, "NGN", "123.45")

        val corrected = dao.accounts.single()
        assertEquals(
            "a manual correction must win even over a chronologically newer automatic observation",
            "123.45",
            corrected.latestBalanceAmount
        )
        assertEquals("MANUAL", corrected.latestBalanceType)
    }

    @Test
    fun `adjusting balance is recorded as a manual observation`() = runTest {
        repository.addAccount("GTBank", "0031234567", "NGN", null, null)
        val accountId = dao.accounts.single().accountId

        repository.adjustBalance(accountId, "NGN", "500.00")

        val manualObservations = dao.observations.filter { it.sourceType == "MANUAL_ADJUSTMENT" }
        assertTrue(manualObservations.isNotEmpty())
        assertEquals("500.00", manualObservations.single().amount)
    }
}
