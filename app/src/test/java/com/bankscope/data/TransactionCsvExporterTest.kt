package com.bankscope.data

import org.junit.Assert.*
import org.junit.Test

class TransactionCsvExporterTest {
    @Test fun exportsHeaderAndEscapesDescription() {
        val csv = TransactionCsvExporter.toCsv(
            listOf(
                TransactionEntity(
                    transactionId = "t1",
                    accountId = null,
                    institutionName = "Access Bank",
                    accountHint = "XXXX1234",
                    direction = "DEBIT",
                    amount = "1250.00",
                    currencyCode = "NGN",
                    description = "Transfer, rent",
                    eventAt = 1_700_000_000_000,
                    receivedAt = 1_700_000_000_000,
                    sourceType = "GMAIL",
                    confidence = 0.9,
                    deduplicationFingerprint = "fp",
                    reviewStatus = "MATCHED"
                )
            )
        )
        assertTrue(csv.startsWith("\"Institution\","))
        assertTrue(csv.contains("\"Transfer, rent\""))
    }
}
