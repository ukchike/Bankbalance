package com.bankscope.notifications

import org.junit.Assert.*
import org.junit.Test

class NotificationSourcesTest {
    @Test fun knownSourcesHaveDisplayNames() {
        assertEquals("OPay", NotificationSources.displayName("com.opay.wallet"))
        assertEquals("PalmPay", NotificationSources.displayName("com.palmpay.ng"))
        assertEquals("Moniepoint", NotificationSources.displayName("com.moniepoint.personal"))
    }

    @Test fun unknownPackageIsNotAllowedByCatalogue() {
        assertNull(NotificationSources.displayName("com.example.unknown"))
    }
}
