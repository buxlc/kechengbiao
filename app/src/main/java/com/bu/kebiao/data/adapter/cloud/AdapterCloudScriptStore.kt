package com.bu.kebiao.data.adapter.cloud

import java.io.File

object AdapterCloudScriptStore {
    fun cacheFile(baseDir: File, schoolId: String, adapterId: String): File =
        File(cacheRoot(baseDir, schoolId), "$adapterId.js")

    fun manifestFile(baseDir: File): File =
        File(cacheRoot(baseDir, "manifest"), "manifest.json")

    fun read(baseDir: File, schoolId: String, adapterId: String): String? {
        val file = cacheFile(baseDir, schoolId, adapterId)
        return if (file.exists()) file.readText(Charsets.UTF_8) else null
    }

    fun write(baseDir: File, schoolId: String, adapterId: String, script: String) {
        val file = cacheFile(baseDir, schoolId, adapterId)
        file.parentFile?.mkdirs()
        file.writeText(script, Charsets.UTF_8)
    }

    fun readManifest(baseDir: File): String? {
        val file = manifestFile(baseDir)
        return if (file.exists()) file.readText(Charsets.UTF_8) else null
    }

    fun writeManifest(baseDir: File, json: String) {
        val file = manifestFile(baseDir)
        file.parentFile?.mkdirs()
        file.writeText(json, Charsets.UTF_8)
    }

    private fun cacheRoot(baseDir: File, schoolId: String): File =
        File(File(baseDir, "adapter-cloud"), sanitize(schoolId))

    private fun sanitize(value: String): String =
        value.trim().replace(Regex("[^A-Za-z0-9._-]+"), "_")
}
