package com.bu.kebiao.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsAboutInfoTest {
    @Test
    fun aboutInfoUsesCurrentVersionAndProjectUrl() {
        assertEquals("v2.0.2", ABOUT_VERSION_NAME)
        assertEquals("https://github.com/buxlc/kechengbiao", ABOUT_GITHUB_URL)
    }
}
