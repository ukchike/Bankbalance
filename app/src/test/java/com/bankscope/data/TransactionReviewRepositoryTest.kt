package com.bankscope.data

import com.bankscope.parser.ParsedBankMessage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionReviewRepositoryTest {
    private lateinit var dao: FakeBankAccountDao
    private lateinit var repository: TransactionReviewRepository

    @Before
    fun setUp() {
        dao = FakeBankAccountDao()
        repository = TransactionReviewRepository(dao)
    }

    private fun message(
        institution: String = "GTBank",
        accountHint: String? = "1234567890",
        direction: String? = "DEBIT",
        amount: String? = "1000.00",
        currency: String = "NGN",
        balance: String? = "50000.00",
        balanceType: String = "AVAILABLE",
        eventAt: Long? = 1_700_000_000_000L,
        description: String? = "POS purchase",
        confidence: Double = 0.95
    ) = ParsedBankMessage(
        institution = institution,
        accountHint = accountHint,
        accountHintType = "ACCOUNT",
        transactionDirection = direction,
        transactionAmount = amount,
        currencyCode = currency,
        reportedBalance = balance,
        balanceType = balanceType,
        eventAtEpochMillis = eventAt,
        eventDateOnly = false,
        descriptionHint = description,
        confidence = confidence,
        warnings = emptyList(),
        parserVersion = "test-1"
    )

    private suspend fun addAccount(institution: String = "GTBank", identifier: String = "0031234567890", currency: String = "NGN"): BankAccountEntity {
        val account = BankAccountEntity(
            accountId = "acct-${dao.accounts.size}",
            institutionName = institution,
            accountIdentifier = identifier,
            currencyCode = currency,
            accountType = null,
            latestBalanceAmount = null
        )
        dao.insertAccount(account)
        return account
    }

    @Test
    fun `same event via two different channels is not double counted`() = runTest {
        addAccount()
        val parsed = message()

        val first = repository.record(parsed, sourceType = "APP_NOTIFICATION", receivedAt = 1_700_000_001_000L)
        val second = repository.record(parsed, sourceType = "SMS_INBOX_IMPORT", receivedAt = 1_700_000_050_000L)

        assertNotNull("first record of a new event should be stored", first)
        assertNull("the same event reported by a second channel must not insert a duplicate row", second)
        assertEquals(1, dao.transactions.size)
    }

    @Test
    fun `different transactions do not collide on fingerprint`() = runTest {
        addAccount()
        val first = repository.record(message(amount = "1000.00"), "APP_NOTIFICATION", 1_700_000_000_000L)
        val second = repository.record(message(amount = "2000.00"), "APP_NOTIFICATION", 1_700_000_000_000L)

        assertNotNull(first)
        assertNotNull(second)
        assertEquals(2, dao.transactions.size)
    }

    @Test
    fun `unambiguous account match marks transaction matched`() = runTest {
        addAccount()
        val row = repository.record(message(), "APP_NOTIFICATION", 1_700_000_000_000L)

        assertNotNull(row)
        assertEquals("MATCHED", row!!.reviewStatus)
        assertEquals("acct-0", row.accountId)
    }

    @Test
    fun `ambiguous account match is routed to review instead of guessed`() = runTest {
        addAccount(identifier = "0031234567890")
        addAccount(identifier = "0091234567890") // same last-4 digits as the hint, two candidates
        val row = repository.record(message(accountHint = "7890"), "APP_NOTIFICATION", 1_700_000_000_000L)

        assertNotNull(row)
        assertNull("an ambiguous match must not silently pick an account", row!!.accountId)
        assertEquals("NEEDS_ACCOUNT_REVIEW", row.reviewStatus)
    }

    @Test
    fun `balance observation from two channels for the same snapshot does not duplicate`() = runTest {
        addAccount()
        val parsed = message()

        val first = repository.recordBalanceObservation(parsed, "APP_NOTIFICATION", 1_700_000_001_000L)
        val second = repository.recordBalanceObservation(parsed, "SMS_INBOX_IMPORT", 1_700_000_050_000L)

        assertNotNull(first)
        assertNull("the same balance snapshot from a second channel must not duplicate", second)
        assertEquals(1, dao.observations.size)
    }

    @Test
    fun `a late-arriving older observation does not overwrite a newer one`() = runTest {
        val account = addAccount()
        // Newer event first.
        repository.recordBalanceObservation(
            message(balance = "90000.00", eventAt = 2_000_000_000_000L),
            "APP_NOTIFICATION",
            receivedAt = 2_000_000_001_000L
        )
        // An older event arrives late (e.g. a delayed SMS import).
        repository.recordBalanceObservation(
            message(balance = "10000.00", eventAt = 1_000_000_000_000L),
            "SMS_INBOX_IMPORT",
            receivedAt = 2_000_000_002_000L // received later, but describes an earlier event
        )

        val updated = dao.accounts.single { it.accountId == account.accountId }
        assertEquals("a late-arriving older observation must not clobber the newer balance", "90000.00", updated.latestBalanceAmount)
    }

    @Test
    fun `no balance observation recorded when account cannot be matched`() = runTest {
        // No accounts registered at all.
        val result = repository.recordBalanceObservation(message(), "APP_NOTIFICATION", 1_700_000_000_000L)
        assertNull(result)
        assertTrue(dao.observations.isEmpty())
    }
}
