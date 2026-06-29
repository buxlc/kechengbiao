package com.bu.kebiao.data.adapter.cloud

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolIndexCloudRepositoryTest {

    @Test
    fun refreshesSchoolIndexAndCachesIt() = runBlocking {
        val indexUrl = "https://example.com/school_index.json"
        val json = """{"schools":[{"id":"NEWCAMPUS","name":"新学校","initial":"新","folder":"NEWCAMPUS","adapters":[]}]}"""
        val httpClient = FakeHttpClient(mapOf(indexUrl to json))
        val cacheDir = createTempDir(prefix = "school-index-cache")
        try {
            val repository = SchoolIndexCloudRepository(httpClient, indexUrl)
            val result = repository.refreshSchoolIndex(cacheDir)

            assertTrue(result is SchoolIndexRefreshResult.Success)
            assertEquals(json, SchoolIndexStore.read(cacheDir))
            assertEquals(1, httpClient.textCalls.size)
        } finally {
            cacheDir.deleteRecursively()
        }
    }

    @Test
    fun blankIndexUrlFailsWithoutNetworkRequest() = runBlocking {
        val httpClient = FakeHttpClient(emptyMap())
        val cacheDir = createTempDir(prefix = "school-index-cache")
        try {
            val repository = SchoolIndexCloudRepository(httpClient, "")
            val result = repository.refreshSchoolIndex(cacheDir)

            assertTrue(result is SchoolIndexRefreshResult.Failed)
            assertEquals("云端学校预设地址未配置", (result as SchoolIndexRefreshResult.Failed).reason)
            assertTrue(httpClient.textCalls.isEmpty())
        } finally {
            cacheDir.deleteRecursively()
        }
    }

    private class FakeHttpClient(
        private val textResponses: Map<String, String>
    ) : AdapterCloudHttpClient {
        val textCalls = mutableListOf<String>()

        override suspend fun getText(url: String): String {
            textCalls += url
            return textResponses[url] ?: error("missing text response for $url")
        }

        override suspend fun getBytes(url: String): ByteArray {
            error("not expected")
        }
    }
}
