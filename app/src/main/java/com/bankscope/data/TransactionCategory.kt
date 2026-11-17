package com.bankscope.data

enum class TransactionCategory {
    TRANSFER, AIRTIME_DATA, UTILITIES, FOOD_DINING, TRANSPORT, SHOPPING,
    SUBSCRIPTIONS, CASH_WITHDRAWAL, BANK_CHARGES, INCOME, OTHER
}

object TransactionCategoriser {
    fun suggest(description: String?, direction: String?): TransactionCategory {
        val text = description.orEmpty().lowercase()
        if (listOf("own account", "self transfer", "between accounts", "intra account").any(text::contains)) return TransactionCategory.TRANSFER
        if (listOf("airtime", "data bundle", "recharge", "top up").any(text::contains)) return TransactionCategory.AIRTIME_DATA
        if (listOf("electricity", "disco", "utility", "water bill", "waste bill").any(text::contains)) return TransactionCategory.UTILITIES
        if (listOf("restaurant", "food", "grocery", "supermarket", "cafe").any(text::contains)) return TransactionCategory.FOOD_DINING
        if (listOf("uber", "bolt", "transport", "fuel", "petrol", "parking").any(text::contains)) return TransactionCategory.TRANSPORT
        if (listOf("netflix", "spotify", "subscription", "icloud", "google one").any(text::contains)) return TransactionCategory.SUBSCRIPTIONS
        if (listOf("atm", "cash withdrawal").any(text::contains)) return TransactionCategory.CASH_WITHDRAWAL
        if (listOf("commission", "stamp duty", "sms alert", "maintenance fee", "bank charge").any(text::contains)) return TransactionCategory.BANK_CHARGES
        if (direction == "CREDIT") return TransactionCategory.INCOME
        if (listOf("pos", "purchase", "shop", "store").any(text::contains)) return TransactionCategory.SHOPPING
        return TransactionCategory.OTHER
    }
}
