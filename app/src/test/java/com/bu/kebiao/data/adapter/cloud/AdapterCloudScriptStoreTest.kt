package com.bu.kebiao.data.adapter.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdapterCloudScriptStoreTest {

    @Test
    fun writesAndReadsCachedScriptBySchoolAndAdapter() {
        val cacheDir = createTempDir(prefix = "adapter-cloud-cache")
        try {
            AdapterCloudScriptStore.write(cacheDir, "LNTU", "LNTU_01", "console.log('v2');")

            val script = AdapterCloudScriptStore.read(cacheDir, "LNTU", "LNTU_01")

            assertEquals("console.log('v2');", script)
            assertTrue(AdapterCloudScriptStore.cacheFile(cacheDir, "LNTU", "LNTU_01").exists())
        } finally {
            cacheDir.deleteRecursively()
        }
    }
}
