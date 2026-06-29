package com.bu.kebiao.data.adapter.cloud

import java.io.File
import java.security.MessageDigest

class AdapterCloudRepository(
    private val httpClient: AdapterCloudHttpClient,
    private val manifestUrl: String
) {
    suspend fun syncCloudScripts(cacheDir: File): AdapterCloudSyncResult {
        if (manifestUrl.isBlank()) {
            return AdapterCloudSyncResult.Failed("云端脚本地址未配置")
        }
        val manifestText = runCatching { httpClient.getText(manifestUrl) }
            .getOrElse { return AdapterCloudSyncResult.Failed(it.message ?: "manifest fetch failed") }
        val manifest = runCatching { AdapterCloudManifestParser.parse(manifestText) }
            .getOrElse { return AdapterCloudSyncResult.Failed(it.message ?: "manifest parse failed") }

        val localManifest = AdapterCloudScriptStore.readManifest(cacheDir)
            ?.let { runCatching { AdapterCloudManifestParser.parse(it) }.getOrNull() }
        val localEntries = localManifest?.adapters.orEmpty().associateBy { it.identityKey() }

        var addedCount = 0
        for (entry in manifest.adapters.filter { it.enabled }) {
            val key = entry.identityKey()
            val cachedScript = AdapterCloudScriptStore.read(cacheDir, entry.cacheSchoolId(), entry.cacheAdapterId())
            val localEntry = localEntries[key]
            val cachedSha256 = cachedScript?.toByteArray(Charsets.UTF_8)?.let { sha256Hex(it) }.orEmpty()
            val isNewOrChanged = cachedScript == null ||
                localEntry == null ||
                localEntry.version != entry.version ||
                localEntry.sha256 != entry.sha256 ||
                (entry.sha256.isNotBlank() && !entry.sha256.equals(cachedSha256, ignoreCase = true))

            if (!isNewOrChanged) continue

            val scriptBytes = runCatching { httpClient.getBytes(entry.downloadUrl) }
                .getOrElse { return AdapterCloudSyncResult.Failed(it.message ?: "script fetch failed") }

            val actualSha256 = sha256Hex(scriptBytes)
            if (entry.sha256.isNotBlank() && !entry.sha256.equals(actualSha256, ignoreCase = true)) {
                return AdapterCloudSyncResult.Failed("脚本校验失败，sha256 不匹配")
            }

            AdapterCloudScriptStore.write(cacheDir, entry.cacheSchoolId(), entry.cacheAdapterId(), scriptBytes.toString(Charsets.UTF_8))
            addedCount++
        }

        AdapterCloudScriptStore.writeManifest(cacheDir, manifestText)
        return AdapterCloudSyncResult.Success(addedCount, manifest.adapters.count { it.enabled })
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
