package com.bu.kebiao.data.adapter.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolIndexStoreTest {

    @Test
    fun writesAndReadsSchoolIndexOverlay() {
        val cacheDir = createTempDir(prefix = "school-index-cache")
        try {
            val json = """{"schools":[{"id":"NEWCAMPUS","name":"新学校","initial":"新","folder":"NEWCAMPUS","adapters":[]}]}"""
            SchoolIndexStore.write(cacheDir, json)

            val read = SchoolIndexStore.read(cacheDir)

            assertEquals(json, read)
            assertTrue(SchoolIndexStore.cacheFile(cacheDir).exists())
        } finally {
            cacheDir.deleteRecursively()
        }
    }
}
