package com.bu.kebiao.data.adapter.cloud

import java.io.File

sealed class SchoolIndexRefreshResult {
    data class Success(val schoolCount: Int) : SchoolIndexRefreshResult()
    data class Failed(val reason: String) : SchoolIndexRefreshResult()
}

class SchoolIndexCloudRepository(
    private val httpClient: AdapterCloudHttpClient,
    private val indexUrl: String
) {
    suspend fun refreshSchoolIndex(cacheDir: File): SchoolIndexRefreshResult {
        if (indexUrl.isBlank()) {
            return SchoolIndexRefreshResult.Failed("云端学校预设地址未配置")
        }
        val json = runCatching { httpClient.getText(indexUrl) }
            .getOrElse { return SchoolIndexRefreshResult.Failed(it.message ?: "school_index fetch failed") }
        SchoolIndexStore.write(cacheDir, json)
        return SchoolIndexRefreshResult.Success(SchoolIndexParser.parse(json).size)
    }
}
