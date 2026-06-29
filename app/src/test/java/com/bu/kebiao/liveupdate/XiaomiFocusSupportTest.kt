package com.bu.kebiao.liveupdate

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XiaomiFocusSupportTest {
    @Test
    fun detectsXiaomiManufacturer() {
        assertTrue(XiaomiFocusSupport.isLikelyXiaomiDevice("Xiaomi"))
        assertTrue(XiaomiFocusSupport.isLikelyXiaomiDevice("XIAOMI"))
        assertTrue(XiaomiFocusSupport.isLikelyXiaomiDevice("Redmi"))
        assertFalse(XiaomiFocusSupport.isLikelyXiaomiDevice("OPPO"))
    }

    @Test
    fun os3SupportsIsland() {
        assertFalse(XiaomiFocusSupport.supportsIslandProtocol(0))
        assertFalse(XiaomiFocusSupport.supportsIslandProtocol(2))
        assertTrue(XiaomiFocusSupport.supportsIslandProtocol(3))
        assertTrue(XiaomiFocusSupport.supportsIslandProtocol(4))
    }
}
