package com.bankscope.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TransactionCsvExporter {
    fun toCsv(rows: List<TransactionEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val header = listOf(
            "Institution", "Account suffix", "Direction", "Amount", "Currency",
            "Description", "Event date", "Received date", "Source", "Confidence", "Review status"
        )
        val lines = mutableListOf(header.joinToString(","))
        rows.forEach { row ->
            val values = listOf(
                row.institutionName,
                row.accountHint?.filter(Char::isDigit)?.takeLast(4).orEmpty(),
                row.direction.orEmpty(),
                row.amount.orEmpty(),
                row.currencyCode,
                row.description.orEmpty(),
                row.eventAt?.let { dateFormat.format(Date(it)) }.orEmpty(),
                dateFormat.format(Date(row.receivedAt)),
                row.sourceType,
                row.confidence.toString(),
                row.reviewStatus
            )
            lines += values.joinToString(",") { csvEscape(it) }
        }
        return lines.joinToString("\r\n")
    }

    private fun csvEscape(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""
}
