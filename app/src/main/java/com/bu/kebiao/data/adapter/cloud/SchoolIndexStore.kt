package com.bu.kebiao.data.adapter.cloud

import java.io.File

object SchoolIndexStore {
    fun cacheFile(baseDir: File): File = File(File(baseDir, "adapter-cloud"), "school_index.json")

    fun read(baseDir: File): String? {
        val file = cacheFile(baseDir)
        return if (file.exists()) file.readText(Charsets.UTF_8) else null
    }

    fun write(baseDir: File, json: String) {
        val file = cacheFile(baseDir)
        file.parentFile?.mkdirs()
        file.writeText(json, Charsets.UTF_8)
    }
}
