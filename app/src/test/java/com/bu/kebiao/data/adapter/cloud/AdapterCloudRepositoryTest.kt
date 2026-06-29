package com.bu.kebiao.data.adapter.cloud

import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdapterCloudRepositoryTest {

    @Test
    fun syncsNewScriptAndReturnsAddedCount() = runBlocking {
        val script = "console.log('cloud v2');"
        val manifestUrl = "https://updates.example.com/manifest.json"
        val scriptUrl = "https://updates.example.com/adapters/LNTU/LNTU_01-v2.0.0.js"
        val sha256 = sha256Hex(script.toByteArray())
        val httpClient = FakeHttpClient(
            textResponses = mapOf(
                manifestUrl to """
                    {
                      "schemaVersion": 1,
                      "generatedAt": "2026-06-29T20:00:00+08:00",
                      "adapters": [
                        {
                          "adapterName": "辽宁工程技术大学 WebVPN/EAMS",
                          "schoolId": "LNTU",
                          "folder": "LNTU",
                          "adapterId": "LNTU_01",
                          "jsPath": "lntu_eams_webvpn.js",
                          "version": "2.0.0",
                          "downloadUrl": "$scriptUrl",
                          "sha256": "$sha256",
                          "enabled": true
                        }
                      ]
                    }
                """.trimIndent()
            ),
            byteResponses = mapOf(scriptUrl to script.toByteArray())
        )
        val cacheDir = createTempDir(prefix = "adapter-cloud-cache")
        try {
            val repository = AdapterCloudRepository(httpClient, manifestUrl)
            val result = repository.syncCloudScripts(cacheDir)

            assertTrue(result is AdapterCloudSyncResult.Success)
            assertEquals(1, (result as AdapterCloudSyncResult.Success).addedCount)
            assertEquals(script, AdapterCloudScriptStore.read(cacheDir, "LNTU", "LNTU_01"))
            assertEquals(1, httpClient.textCalls.size)
            assertEquals(1, httpClient.byteCalls.size)
        } finally {
            cacheDir.deleteRecursively()
        }
    }

    @Test
    fun skipsDownloadingWhenLocalCacheMatchesManifest() = runBlocking {
        val script = "console.log('cloud v2');"
        val manifestUrl = "https://updates.example.com/manifest.json"
        val scriptUrl = "https://updates.example.com/adapters/LNTU/LNTU_01-v2.0.0.js"
        val sha256 = sha256Hex(script.toByteArray())
        val httpClient = FakeHttpClient(
            textResponses = mapOf(
                manifestUrl to """
                    {
                      "schemaVersion": 1,
                      "generatedAt": "2026-06-29T20:00:00+08:00",
                      "adapters": [
                        {
                          "adapterName": "辽宁工程技术大学 WebVPN/EAMS",
                          "schoolId": "LNTU",
                          "folder": "LNTU",
                          "adapterId": "LNTU_01",
                          "jsPath": "lntu_eams_webvpn.js",
                          "version": "2.0.0",
                          "downloadUrl": "$scriptUrl",
                          "sha256": "$sha256",
                          "enabled": true
                        }
                      ]
                    }
                """.trimIndent()
            ),
            byteResponses = mapOf()
        )
        val cacheDir = createTempDir(prefix = "adapter-cloud-cache")
        try {
            AdapterCloudScriptStore.write(cacheDir, "LNTU", "LNTU_01", script)
            AdapterCloudScriptStore.writeManifest(
                cacheDir,
                """
                    {
                      "schemaVersion": 1,
                      "generatedAt": "2026-06-29T20:00:00+08:00",
                      "adapters": [
                        {
                          "adapterName": "辽宁工程技术大学 WebVPN/EAMS",
                          "schoolId": "LNTU",
                          "folder": "LNTU",
                          "adapterId": "LNTU_01",
                          "jsPath": "lntu_eams_webvpn.js",
                          "version": "2.0.0",
                          "downloadUrl": "$scriptUrl",
                          "sha256": "$sha256",
                          "enabled": true
                        }
                      ]
                    }
                """.trimIndent()
            )

            val repository = AdapterCloudRepository(httpClient, manifestUrl)
            val result = repository.syncCloudScripts(cacheDir)

            assertTrue(result is AdapterCloudSyncResult.Success)
            assertEquals(0, (result as AdapterCloudSyncResult.Success).addedCount)
            assertEquals(0, httpClient.byteCalls.size)
        } finally {
            cacheDir.deleteRecursively()
        }
    }

    @Test
    fun treatsExistingScriptWithoutLocalManifestAsAlreadySyncedWhenShaMatches() = runBlocking {
        val script = "console.log('cloud v2');"
        val manifestUrl = "https://updates.example.com/manifest.json"
        val scriptUrl = "https://updates.example.com/adapters/LNTU/LNTU_01-v2.0.0.js"
        val sha256 = sha256Hex(script.toByteArray())
        val httpClient = FakeHttpClient(
            textResponses = mapOf(
                manifestUrl to """
                    {
                      "schemaVersion": 1,
                      "generatedAt": "2026-06-29T20:00:00+08:00",
                      "adapters": [
                        {
                          "adapterName": "辽宁工程技术大学 WebVPN/EAMS",
                          "schoolId": "LNTU",
                          "folder": "LNTU",
                          "adapterId": "LNTU_01",
                          "jsPath": "lntu_eams_webvpn.js",
                          "version": "2.0.0",
                          "downloadUrl": "$scriptUrl",
                          "sha256": "$sha256",
                          "enabled": true
                        }
                      ]
                    }
                """.trimIndent()
            ),
            byteResponses = mapOf()
        )
        val cacheDir = createTempDir(prefix = "adapter-cloud-cache")
        try {
            AdapterCloudScriptStore.write(cacheDir, "LNTU", "LNTU_01", script)

            val repository = AdapterCloudRepository(httpClient, manifestUrl)
            val result = repository.syncCloudScripts(cacheDir)

            assertTrue(result is AdapterCloudSyncResult.Success)
            assertEquals(0, (result as AdapterCloudSyncResult.Success).addedCount)
            assertEquals(0, httpClient.byteCalls.size)
        } finally {
            cacheDir.deleteRecursively()
        }
    }

    @Test
    fun blankManifestUrlFailsWithoutNetworkRequest() = runBlocking {
        val httpClient = FakeHttpClient(textResponses = emptyMap(), byteResponses = emptyMap())
        val cacheDir = createTempDir(prefix = "adapter-cloud-cache")
        try {
            val repository = AdapterCloudRepository(httpClient, "")
            val result = repository.syncCloudScripts(cacheDir)

            assertTrue(result is AdapterCloudSyncResult.Failed)
            assertEquals("云端脚本地址未配置", (result as AdapterCloudSyncResult.Failed).reason)
            assertTrue(httpClient.textCalls.isEmpty())
            assertTrue(httpClient.byteCalls.isEmpty())
        } finally {
            cacheDir.deleteRecursively()
        }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private class FakeHttpClient(
        val textResponses: Map<String, String>,
        val byteResponses: Map<String, ByteArray>
    ) : AdapterCloudHttpClient {
        val textCalls = mutableListOf<String>()
        val byteCalls = mutableListOf<String>()

        override suspend fun getText(url: String): String {
            textCalls += url
            return textResponses[url] ?: error("missing text response for $url")
        }

        override suspend fun getBytes(url: String): ByteArray {
            byteCalls += url
            return byteResponses[url] ?: error("missing byte response for $url")
        }
    }
}
