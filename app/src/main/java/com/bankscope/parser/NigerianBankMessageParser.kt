package com.bankscope.parser

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Deterministic first-pass parser for the redacted Nigerian bank notification formats
 * supplied during BankBalance discovery. Sender must be supplied by the ingestion layer.
 * This parser does not identify accounts from bank name alone and never guesses missing digits.
 */
class NigerianBankMessageParser(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    companion object {
        const val VERSION = "ng-bank-parser-0.1.0"

        private val balanceRegex = Regex(
            """(?i)(avail(?:able)?\s*bal(?:ance)?|c\.?\s*bal|bal(?:ance)?)\s*[:=]?\s*(?:(NGN|₦|USD|\$|GBP|£)\s*)?([\d,]+(?:\.\d{1,2})?)\s*(CR|DR)?"""
        )
        private val amountRegex = Regex(
            """(?i)\b(?:amt|amount|cr\s*amt|debit\s*alert|credit\s*alert)\s*[:=]?\s*(?:(NGN|₦|USD|\$|GBP|£)\s*)?([\d,]+(?:\.\d{1,2})?)"""
        )
        private val accountRegexes = listOf(
            "A/C" to Regex("""(?i)\bA\s*/\s*C\s*:\s*([0-9X*]{4,})"""),
            "ACCOUNT" to Regex("""(?i)\bAcct(?:\s*No)?\s*:?\s*([0-9X*]{4,})"""),
            "ACCOUNT" to Regex("""(?i)\bAcc\s*:?\s*([0-9X*]{4,})"""),
            "ACCOUNT" to Regex("""(?i)\bAccount\s*(?:No)?\s*:?\s*([0-9X*]{4,})""")
        )
        private val dateTimePatterns = listOf(
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMM-yy HH:mm:ss", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ENGLISH)
        )
        private val datePatterns = listOf(
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH)
        )
    }

    fun identifyInstitution(sender: String): String? {
        val key = sender.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        return when {
            "ecobank" in key -> "Ecobank"
            "firstbank" in key || key == "firstonline" -> "FirstBank"
            "gtbank" in key || key == "gtb" -> "GTBank"
            "keystone" in key -> "Keystone Bank"
            "alat" in key || "wema" in key -> "Wema Bank"
            "accessbank" in key || key == "access" -> "Access Bank"
            "providus" in key -> "Providus Bank"
            "zenith" in key -> "Zenith Bank"
            "opay" in key -> "OPay"
            "palmpay" in key -> "PalmPay"
            "moniepoint" in key -> "Moniepoint"
            else -> null
        }
    }

    fun parse(sender: String, body: String, receivedAtEpochMillis: Long): BankParseResult {
        val institution = identifyInstitution(sender)
            ?: return BankParseResult.Rejected("Unrecognised bank or notification sender")

        val balanceMatch = balanceRegex.find(body)
            ?: return BankParseResult.Rejected("No explicit reported balance found")

        val rawBalance = balanceMatch.groupValues[3]
        val balance = normaliseAmount(rawBalance)
            ?: return BankParseResult.Rejected("Reported balance is not a valid decimal amount")

        val currency = currencyCode(balanceMatch.groupValues[2], body)
        val balanceLabel = balanceMatch.groupValues[1].lowercase(Locale.ROOT)
        val balanceType = when {
            "avail" in balanceLabel -> "AVAILABLE"
            "c." in balanceLabel -> "CURRENT"
            else -> "UNKNOWN"
        }

        val account = accountRegexes.firstNotNullOfOrNull { (type, regex) ->
            regex.find(body)?.let { type to it.groupValues[1] }
        }

        val amountMatch = amountRegex.find(body)
        val transactionAmount = amountMatch?.groupValues?.get(2)?.let(::normaliseAmount)
        val direction = when {
            Regex("""(?i)\bdebit\b|\bDR\b""").containsMatchIn(body) -> "DEBIT"
            Regex("""(?i)\bcredit\b|\bCR\b""").containsMatchIn(body) -> "CREDIT"
            else -> null
        }

        val (eventAt, dateOnly, dateWarning) = extractEventTime(body)
        val warnings = mutableListOf<String>()
        if (account == null) warnings += "No explicit account identifier; account must be matched manually"
        if (eventAt == null) warnings += "No reliable event timestamp found"
        if (dateOnly) warnings += "Event date has no time component"
        if (balanceType == "UNKNOWN") warnings += "Balance type is not explicitly identified as available or current"
        if (dateWarning != null) warnings += dateWarning

        val description = Regex("""(?i)\bDesc\s*:\s*([^\n]+)""")
            .find(body)?.groupValues?.get(1)?.trim()
            ?.take(120)

        val confidence = when {
            account != null && eventAt != null && balanceType != "UNKNOWN" -> 0.98
            account != null && eventAt != null -> 0.92
            account != null -> 0.85
            else -> 0.65
        }

        return BankParseResult.Success(
            ParsedBankMessage(
                institution = institution,
                accountHint = account?.second,
                accountHintType = account?.first,
                transactionDirection = direction,
                transactionAmount = transactionAmount,
                currencyCode = currency,
                reportedBalance = balance,
                balanceType = balanceType,
                eventAtEpochMillis = eventAt,
                eventDateOnly = dateOnly,
                descriptionHint = description,
                confidence = confidence,
                warnings = warnings,
                parserVersion = VERSION
            )
        )
    }

    private fun normaliseAmount(raw: String): String? = runCatching {
        BigDecimal(raw.replace(",", "").trim()).toPlainString()
    }.getOrNull()

    private fun currencyCode(marker: String?, body: String): String {
        val token = marker.orEmpty().uppercase(Locale.ROOT)
        return when (token) {
            "USD", "$" -> "USD"
            "GBP", "£" -> "GBP"
            "NGN", "₦" -> "NGN"
            else -> when {
                Regex("""(?i)\bUSD\b|\$""").containsMatchIn(body) -> "USD"
                Regex("""(?i)\bGBP\b|£""").containsMatchIn(body) -> "GBP"
                else -> "NGN"
            }
        }
    }

    private fun extractEventTime(body: String): Triple<Long?, Boolean, String?> {
        val candidates = listOf(
            Regex("""(?i)\b(?:Date|DT)\s*:\s*([0-9]{1,2}[-/][A-Za-z0-9]{3,9}[-/][0-9]{2,4}(?:\s+[0-9: ]{4,8})?)"""),
            Regex("""(?i)\b(?:Date|DT)\s*:\s*([0-9]{1,2}[-/][0-9]{1,2}[-/][0-9]{2,4}(?:\s+[0-9: ]{4,8})?)"""),
            Regex("""\bon\s+([0-9]{1,2}-[A-Za-z]{3}-[0-9]{2,4})\b""")
        )
        val raw = candidates.firstNotNullOfOrNull { it.find(body)?.groupValues?.get(1)?.trim() }
            ?: return Triple(null, false, null)

        val cleaned = raw.replace(Regex("\\s+"), " ")
        for (formatter in dateTimePatterns) {
            try {
                val dt = LocalDateTime.parse(cleaned, formatter)
                return Triple(dt.atZone(zoneId).toInstant().toEpochMilli(), false, null)
            } catch (_: DateTimeParseException) { }
        }
        for (formatter in datePatterns) {
            try {
                val date = LocalDate.parse(cleaned, formatter)
                return Triple(date.atStartOfDay(zoneId).toInstant().toEpochMilli(), true, null)
            } catch (_: DateTimeParseException) { }
        }
        return Triple(null, false, "Event date was present but could not be parsed")
    }
}
