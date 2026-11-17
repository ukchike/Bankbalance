package com.bankscope.gmail

import org.junit.Assert.*
import org.junit.Test

class GmailMessageAdapterTest {
    private val adapter = GmailMessageAdapter()

    @Test fun parsesSupportedBankEmailContent() {
        val result = adapter.parse(
            from = "alerts@accessbankplc.com",
            subject = "Access Bank Debit Alert",
            body = "Debit Alert\nAcc: 1234567890\nAmt: NGN 2,500.00\nDate: 17-11-2026 10:15:00\nAvail Bal: NGN 45,000.00",
            receivedAtEpochMillis = 1_795_000_000_000L
        )
        assertTrue(result is GmailParseResult.Success)
        val parsed = (result as GmailParseResult.Success)
        assertEquals("Access Bank", parsed.message.institution)
        assertEquals("45000.00", parsed.message.reportedBalance)
        assertNotNull(parsed.canonicalFingerprint)
    }

    @Test fun rejectsUnsupportedEmailSender() {
        val result = adapter.parse(
            from = "someone@example.com",
            subject = "Hello",
            body = "This is a personal email",
            receivedAtEpochMillis = 1L
        )
        assertTrue(result is GmailParseResult.Rejected)
    }
}
