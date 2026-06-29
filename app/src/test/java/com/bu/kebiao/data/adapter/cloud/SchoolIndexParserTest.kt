package com.bu.kebiao.data.adapter.cloud

import org.junit.Assert.assertEquals
import org.junit.Test

class SchoolIndexParserTest {

    @Test
    fun parsesSchoolIndexEntriesAndAdapters() {
        val schools = SchoolIndexParser.parse(
            """
            {
              "schools": [
                {
                  "id": "NEWCAMPUS",
                  "name": "新学校",
                  "initial": "新",
                  "folder": "NEWCAMPUS",
                  "adapters": [
                    {
                      "adapter_id": "NEWCAMPUS_WEBVPN",
                      "adapter_name": "新学校教务适配",
                      "category": "BACHELOR_AND_ASSOCIATE",
                      "js_path": "newcampus-v2.0.0.js",
                      "import_url": "https://jw.newcampus.edu.cn/",
                      "description": "test",
                      "maintainer": "codex"
                    }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(1, schools.size)
        assertEquals("NEWCAMPUS", schools.single().id)
        assertEquals("新学校教务适配", schools.single().adapters.single().adapterName)
    }
}
