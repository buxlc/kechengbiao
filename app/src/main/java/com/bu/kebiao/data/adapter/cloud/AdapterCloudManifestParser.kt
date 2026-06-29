package com.bu.kebiao.data.adapter.cloud

import org.json.JSONArray
import org.json.JSONObject

object AdapterCloudManifestParser {
    fun parse(json: String): AdapterCloudManifest {
        val root = JSONObject(json)
        val adaptersArray = root.optJSONArray("adapters") ?: JSONArray()
        val adapters = adaptersArray.toList().map { item ->
            AdapterCloudEntry(
                adapterName = item.optString("adapterName", item.optString("adapter_name", "")),
                schoolId = item.optString("schoolId", ""),
                folder = item.optString("folder", ""),
                adapterId = item.optString("adapterId", ""),
                jsPath = item.optString("jsPath", ""),
                version = item.optString("version", ""),
                downloadUrl = item.optString("downloadUrl", ""),
                sha256 = item.optString("sha256", ""),
                enabled = item.optBoolean("enabled", true),
                notes = item.optString("notes", "")
            )
        }
        return AdapterCloudManifest(
            schemaVersion = root.optInt("schemaVersion", 1),
            generatedAt = root.optString("generatedAt", ""),
            adapters = adapters
        )
    }

    private fun JSONArray.toList(): List<JSONObject> {
        val items = ArrayList<JSONObject>(length())
        for (index in 0 until length()) {
            items += getJSONObject(index)
        }
        return items
    }
}
