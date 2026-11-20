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

    /**
     * Resolve known institutions using the package identifier first, then notification
     * title and installed app label. This avoids silently dropping bank apps whose
     * Android package identifier differs from the small initial allow-list.
     */
    fun resolve(packageName: String, notificationTitle: String, appLabel: String, messageText: String = ""): String? {
        displayName(packageName)?.let { return it }
        val candidates = listOf(notificationTitle, appLabel, packageName, messageText)
        for (candidate in candidates) {
            val key = candidate.lowercase().replace(Regex("[^a-z0-9]"), "")
            when {
                "ecobank" in key -> return "Ecobank"
                "firstbank" in key || "firstonline" in key -> return "FirstBank"
                "gtbank" in key || key == "gtb" -> return "GTBank"
                "keystone" in key -> return "Keystone Bank"
                "alat" in key || "wema" in key -> return "Wema Bank"
                "accessbank" in key || "accessmore" in key -> return "Access Bank"
                "providus" in key -> return "Providus Bank"
                "zenith" in key -> return "Zenith Bank"
                "opay" in key -> return "OPay"
                "palmpay" in key -> return "PalmPay"
                "moniepoint" in key -> return "Moniepoint"
            }
        }
        return null
    }
}
