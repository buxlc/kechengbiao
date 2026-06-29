package com.bu.kebiao.data.adapter.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

interface AdapterCloudHttpClient {
    suspend fun getText(url: String): String
    suspend fun getBytes(url: String): ByteArray
}

class OkHttpAdapterCloudHttpClient(
    private val client: OkHttpClient
) : AdapterCloudHttpClient {
    override suspend fun getText(url: String): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code} when fetching manifest")
            response.body?.string() ?: error("Empty response body for $url")
        }
    }

    override suspend fun getBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code} when fetching script")
            response.body?.bytes() ?: error("Empty response body for $url")
        }
    }
}
