package com.bu.kebiao.data.adapter.cloud

data class AdapterCloudManifest(
    val schemaVersion: Int,
    val generatedAt: String,
    val adapters: List<AdapterCloudEntry>
)

data class AdapterCloudEntry(
    val adapterName: String,
    val schoolId: String,
    val folder: String,
    val adapterId: String,
    val jsPath: String,
    val version: String,
    val downloadUrl: String,
    val sha256: String,
    val enabled: Boolean,
    val notes: String
)

fun AdapterCloudEntry.cacheSchoolId(): String =
    schoolId.ifBlank { folder }.ifBlank { adapterName }

fun AdapterCloudEntry.cacheAdapterId(): String =
    adapterId.ifBlank { adapterName }.ifBlank { jsPath }

fun AdapterCloudEntry.identityKey(): String =
    "${cacheSchoolId().lowercase()}::${cacheAdapterId().lowercase()}"
