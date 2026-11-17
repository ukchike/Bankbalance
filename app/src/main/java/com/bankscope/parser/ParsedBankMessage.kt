package com.bankscope.parser

data class ParsedBankMessage(
    val institution: String,
    val accountHint: String?,
    val accountHintType: String?,
    val transactionDirection: String?,
    val transactionAmount: String?,
    val currencyCode: String,
    val reportedBalance: String,
    val balanceType: String,
    val eventAtEpochMillis: Long?,
    val eventDateOnly: Boolean,
    val descriptionHint: String?,
    val confidence: Double,
    val warnings: List<String>,
    val parserVersion: String
)

sealed interface BankParseResult {
    data class Success(val message: ParsedBankMessage) : BankParseResult
    data class Rejected(val reason: String) : BankParseResult
}
