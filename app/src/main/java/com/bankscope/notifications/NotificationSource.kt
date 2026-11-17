package com.bankscope.notifications

data class NotificationSource(
    val packageName: String,
    val displayName: String
)

object NotificationSources {
    val supported = listOf(
        NotificationSource("com.opay.wallet", "OPay"),
        NotificationSource("com.palmpay.ng", "PalmPay"),
        NotificationSource("com.moniepoint.personal", "Moniepoint"),
        NotificationSource("com.gtbank.gtworld", "GTBank"),
        NotificationSource("com.accessbank.accessmore", "Access Bank"),
        NotificationSource("com.zenithbank.eazybanking", "Zenith Bank")
    )

    fun displayName(packageName: String): String? =
        supported.firstOrNull { it.packageName == packageName }?.displayName
}
