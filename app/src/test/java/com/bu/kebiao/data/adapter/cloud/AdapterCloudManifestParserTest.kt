package com.bu.kebiao.data.adapter.cloud

import org.junit.Assert.assertEquals
import org.junit.Test

class AdapterCloudManifestParserTest {

    @Test
    fun parsesAdapterManifestWithSingleEntry() {
        val manifest = AdapterCloudManifestParser.parse(
            """
            {
              "schemaVersion": 1,
              "generatedAt": "2026-06-29T20:00:00+08:00",
              "adapters": [
                {
                  "adapterName": "辽宁工程技术大学 WebVPN/EAMS",
                  "schoolId": "LNTU",
                  "adapterId": "LNTU_01",
                  "version": "2.0.0",
                  "downloadUrl": "https://updates.example.com/adapters/LNTU/LNTU_01-v2.0.0.js",
                  "sha256": "abc123",
                  "enabled": true,
                  "notes": "辽工大 WebVPN/EAMS"
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(1, manifest.schemaVersion)
        assertEquals("2026-06-29T20:00:00+08:00", manifest.generatedAt)
        assertEquals(1, manifest.adapters.size)
        val entry = manifest.adapters.single()
        assertEquals("辽宁工程技术大学 WebVPN/EAMS", entry.adapterName)
        assertEquals("LNTU", entry.schoolId)
        assertEquals("LNTU_01", entry.adapterId)
        assertEquals("2.0.0", entry.version)
        assertEquals("https://updates.example.com/adapters/LNTU/LNTU_01-v2.0.0.js", entry.downloadUrl)
        assertEquals("abc123", entry.sha256)
    }
}
