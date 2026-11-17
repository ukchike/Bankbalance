package com.bankscope.parser

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class NigerianBankMessageParserTest {
    private val parser = NigerianBankMessageParser(ZoneId.of("Africa/Lagos"))
    private val receivedAt = 1_795_000_000_000L

    private fun success(sender: String, body: String) =
        (parser.parse(sender, body, receivedAt) as BankParseResult.Success).message

    @Test fun parsesEcobankInterestCredit() {
        val result = success("ECOBANK", "Credit:NGN.84 A/C:345******372 SAVINGS ACCOUNT CREDIT INTEREST on 01-MAR-26 Bal:NGN950.23")
        assertEquals("Ecobank", result.institution)
        assertEquals("345******372", result.accountHint)
        assertEquals("950.23", result.reportedBalance)
        assertEquals("NGN", result.currencyCode)
        assertEquals("CREDIT", result.transactionDirection)
        assertTrue(result.eventDateOnly)
    }

    @Test fun parsesFirstBankDebitAndBalance() {
        val result = success("FirstBank", "Debit: 312XXXX901 Amt: NGN20,000.00 Date: 30-JUN-2026 19:57:34 Desc: FIP transfer Bal: NGN1,119.69CR")
        assertEquals("312XXXX901", result.accountHint)
        assertEquals("20000.00", result.transactionAmount)
        assertEquals("1119.69", result.reportedBalance)
        assertEquals("DEBIT", result.transactionDirection)
        assertNotNull(result.eventAtEpochMillis)
    }

    @Test fun parsesGtbankAvailableBalance() {
        val result = success("GTBank", "Acct: 0488758461 Amt: NGN500.00 DR Desc: transfer Avail Bal: NGN1,461.73 Date: 02-Aug-2024 17:47")
        assertEquals("AVAILABLE", result.balanceType)
        assertEquals("1461.73", result.reportedBalance)
        assertEquals("0488758461", result.accountHint)
    }

    @Test fun parsesAccessAndDoesNotConfuseTotalWithAvailable() {
        val result = success("AccessBank", "Debit Amt:NGN40,000.00 Acc:000******765 Date:07/11/2026 Avail Bal:NGN970,947.50 Total:NGN970,997.50")
        assertEquals("970947.50", result.reportedBalance)
        assertEquals("AVAILABLE", result.balanceType)
        assertEquals("000******765", result.accountHint)
    }

    @Test fun parsesAlatByWema() {
        val result = success("ALATbyWEMA", "DR:NGN 19,923.00 Acct No:0236****66 Desc:ALAT NIP TRANSFER Bal:NGN 197,257.10 15-11-2026 11:26:10")
        assertEquals("Wema Bank", result.institution)
        assertEquals("0236****66", result.accountHint)
        assertEquals("197257.10", result.reportedBalance)
        assertEquals("DEBIT", result.transactionDirection)
    }

    @Test fun parsesProvidusBalanceWithoutAssumingCardIsAccountNumber() {
        val result = success("PROVIDUS", "Debit Alert: NGN20,400.00 POS <CHRIST 71******69 on 14/11/2026 @ 07:59:02 Bal: NGN41,959.76")
        assertNull(result.accountHint)
        assertEquals("41959.76", result.reportedBalance)
        assertTrue(result.warnings.any { it.contains("account identifier") })
    }

    @Test fun parsesKeystoneCredit() {
        val result = success("KEYSTONE", "Credit! Acct:601****241 Amt:NGN175.31 Desc: NIDF PAYMENT Date:29-10-2026 00:00 Bal:NGN4,885.91")
        assertEquals("601****241", result.accountHint)
        assertEquals("4885.91", result.reportedBalance)
        assertEquals("CREDIT", result.transactionDirection)
    }

    @Test fun ignoresPromotionalMessages() {
        val result = parser.parse("FirstBank", "FirstOnline is ready when you are! Log in to firstbanknigeria.com", receivedAt)
        assertTrue(result is BankParseResult.Rejected)
    }

    @Test fun rejectsUnknownSender() {
        val result = parser.parse("Unknown Sender", "Bal:NGN100.00", receivedAt)
        assertTrue(result is BankParseResult.Rejected)
    }

    @Test fun doesNotTreatBankNameAsAnAccountIdentifier() {
        val result = success("ZenithBank", "Acct:226****071 DT:15-11-2026 04:53:28 CR Amt:9,000.00 Bal:15,135.75")
        assertEquals("226****071", result.accountHint)
        assertEquals("15135.75", result.reportedBalance)
        assertEquals("NGN", result.currencyCode)
    }
}
