package com.bu.kebiao.data.adapter.cloud

sealed class AdapterCloudSyncResult {
    data class Success(val addedCount: Int, val totalCount: Int) : AdapterCloudSyncResult()
    data class Failed(val reason: String) : AdapterCloudSyncResult()
}
