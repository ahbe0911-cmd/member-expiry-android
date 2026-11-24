package ir.memberexpiry.manager.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpiryPolicyTest {
    @Test fun noPendingCancelsExistingNotification() {
        assertEquals(Delivery.CANCEL, decideDelivery(ExpiryCounts(0, 0), true, "old", "new"))
    }
    @Test fun noPermissionSkipsWithoutRecordingDelivery() {
        assertEquals(Delivery.SKIP, decideDelivery(ExpiryCounts(1, 2), false, null, "new"))
    }
    @Test fun sameDateAndCountsAreNotPostedAgain() {
        assertEquals(Delivery.SKIP, decideDelivery(ExpiryCounts(1, 2), true, "same", "same"))
    }
    @Test fun changedCountsOrNewDatePost() {
        assertEquals(Delivery.POST, decideDelivery(ExpiryCounts(1, 2), true, "old", "new"))
        assertEquals(Delivery.POST, decideDelivery(ExpiryCounts(0, 2), true, null, "new"))
        assertEquals(3, ExpiryCounts(1, 2).total)
    }
}
