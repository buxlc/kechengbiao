package com.bu.kebiao.data.adapter.cloud

import com.bu.kebiao.data.adapter.AdapterInfo
import com.bu.kebiao.data.adapter.SchoolInfo
import org.json.JSONArray
import org.json.JSONObject

object SchoolIndexParser {
    fun parse(json: String): List<SchoolInfo> {
        val root = JSONObject(json)
        val schoolsArray = root.optJSONArray("schools") ?: JSONArray()
        val schools = mutableListOf<SchoolInfo>()
        for (i in 0 until schoolsArray.length()) {
            val s = schoolsArray.getJSONObject(i)
            val adaptersArray = s.optJSONArray("adapters") ?: JSONArray()
            val adapters = mutableListOf<AdapterInfo>()
            for (j in 0 until adaptersArray.length()) {
                val a = adaptersArray.getJSONObject(j)
                adapters.add(
                    AdapterInfo(
                        adapterId = a.optString("adapter_id", a.optString("adapterId", "")),
                        adapterName = a.optString("adapter_name", a.optString("adapterName", "")),
                        category = a.optString("category", ""),
                        jsPath = a.optString("js_path", a.optString("jsPath", "")),
                        importUrl = a.optString("import_url", a.optString("importUrl", "")),
                        description = a.optString("description", ""),
                        maintainer = a.optString("maintainer", "")
                    )
                )
            }
            schools.add(
                SchoolInfo(
                    id = s.optString("id", ""),
                    name = s.optString("name", ""),
                    initial = s.optString("initial", ""),
                    folder = s.optString("folder", ""),
                    adapters = adapters
                )
            )
        }
        return schools
    }
}
