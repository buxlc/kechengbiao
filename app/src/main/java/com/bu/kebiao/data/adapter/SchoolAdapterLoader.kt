package com.bu.kebiao.data.adapter

import android.content.Context
import com.bu.kebiao.data.adapter.cloud.AdapterCloudScriptStore
import com.bu.kebiao.data.adapter.cloud.SchoolIndexParser
import com.bu.kebiao.data.adapter.cloud.SchoolIndexStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI

object SchoolAdapterLoader {

    private var cachedSchools: List<SchoolInfo>? = null

    suspend fun loadSchools(context: Context): List<SchoolInfo> {
        cachedSchools?.let { return it }
        return withContext(Dispatchers.IO) {
            val json = context.assets.open("school_index.json").bufferedReader().use { it.readText() }
            val schools = SchoolIndexParser.parse(json)
            val remoteSchools = SchoolIndexStore.read(context.filesDir)
                ?.let(SchoolIndexParser::parse)
                .orEmpty()
            val mergedSchools = mergeBuiltInSchools((schools + remoteSchools).distinctBy { it.id })
            cachedSchools = mergedSchools
            mergedSchools
        }
    }

    fun loadJsScript(context: Context, schoolId: String, folder: String, jsPath: String, adapterId: String): String {
        AdapterCloudScriptStore.read(context.filesDir, schoolId, adapterId)?.let { return it }
        val assetPath = "adapters/$folder/$jsPath"
        return context.assets.open(assetPath).bufferedReader().use { it.readText() }
    }

    fun detectAdapterByUrl(url: String, schools: List<SchoolInfo>): Pair<SchoolInfo, AdapterInfo>? {
        if (url.isBlank() || schools.isEmpty()) return null

        val normalizedUrl = normalizeUrl(url)
        val parsedUrl = parseUrl(url)

        schools
            .asSequence()
            .flatMap { school -> school.adapters.asSequence().map { adapter -> school to adapter } }
            .mapNotNull { (school, adapter) ->
                matchImportUrl(normalizedUrl, parsedUrl, adapter.importUrl)?.let { score ->
                    Triple(score, school, adapter)
                }
            }
            .maxByOrNull { it.first }
            ?.let { return it.second to it.third }

        matchBuiltInSchoolByUrl(normalizedUrl, schools)?.let { return it }

        val fallbackFolder = when {
            containsAny(normalizedUrl, "chaoxing") -> "chaoxing_jiaowu"
            containsAny(normalizedUrl, "jwglxt", "jsxsd", "xskbcx", "xsgrkb", "kbcx", "sso.jsp") -> "zhengfang_jiaowu"
            containsAny(normalizedUrl, "eams", "/academic/", "/urp", "courseTableForStd", "homeExt.action", "for-std/course-table") -> "urp_jiaowu"
            containsAny(normalizedUrl, "qingguo") -> "qingguo_jiaowu"
            else -> null
        } ?: return null

        val school = schools.firstOrNull { it.folder.equals(fallbackFolder, ignoreCase = true) } ?: return null
        val adapter = school.adapters.firstOrNull() ?: return null
        return school to adapter
    }

    fun clearCache() {
        cachedSchools = null
    }

    private fun normalizeUrl(url: String): String =
        url.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removeSuffix("/")
            .lowercase()

    private fun parseUrl(url: String): URI? = runCatching {
        URI(if (url.startsWith("http", ignoreCase = true)) url else "https://$url")
    }.getOrNull()

    private fun matchImportUrl(normalizedUrl: String, parsedUrl: URI?, importUrl: String): Int? {
        if (importUrl.isBlank()) return null

        val normalizedImportUrl = normalizeUrl(importUrl)
        if (normalizedImportUrl.isBlank()) return null

        if (normalizedUrl == normalizedImportUrl) return 4000 + normalizedImportUrl.length
        if (normalizedUrl.startsWith(normalizedImportUrl)) return 3500 + normalizedImportUrl.length
        if (normalizedUrl.contains(normalizedImportUrl)) return 3000 + normalizedImportUrl.length

        val importUri = parseUrl(importUrl) ?: return null
        val inputHost = parsedUrl?.host?.lowercase().orEmpty()
        val targetHost = importUri.host?.lowercase().orEmpty()
        if (inputHost.isBlank() || targetHost.isBlank()) return null
        if (inputHost != targetHost) return null

        val inputPath = parsedUrl?.path.orEmpty().trimEnd('/')
        val targetPath = importUri.path.orEmpty().trimEnd('/')
        return when {
            targetPath.isNotBlank() && inputPath.startsWith(targetPath) -> 2500 + targetPath.length
            targetPath.isBlank() -> 2000
            else -> 1500
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean =
        keywords.any { keyword -> text.contains(keyword.lowercase()) }

    private fun mergeBuiltInSchools(schools: List<SchoolInfo>): List<SchoolInfo> {
        val builtInSchools = listOf(
            SchoolInfo(
                id = "LNTU",
                name = "辽宁工程技术大学",
                initial = "辽",
                folder = "LNTU",
                adapters = listOf(
                    AdapterInfo(
                        adapterId = "LNTU_01",
                        adapterName = "辽宁工程技术大学 WebVPN/EAMS",
                        category = "BACHELOR_AND_ASSOCIATE",
                        jsPath = "lntu_eams_webvpn.js",
                        importUrl = "https://webvpn.lntu.edu.cn/http-8080/77726476706e69737468656265737421fae05988693c66446b468ca88d1b203b/eams/homeExt.action",
                        description = "手动登录并进入最终课表页后执行导入，适配 WebVPN 包装下的 EAMS 课表提取。",
                        maintainer = "Codex"
                    )
                )
            )
        )
        return (schools + builtInSchools).distinctBy { it.id }
    }

    private fun matchBuiltInSchoolByUrl(
        normalizedUrl: String,
        schools: List<SchoolInfo>
    ): Pair<SchoolInfo, AdapterInfo>? {
        val isLntuUrl = normalizedUrl.contains("lntu") &&
            normalizedUrl.contains("eams") &&
            containsAny(normalizedUrl, "homeext.action", "coursetableforstd", "webvpn.lntu.edu.cn")
        if (!isLntuUrl) return null

        val school = schools.firstOrNull { it.id == "LNTU" } ?: return null
        val adapter = school.adapters.firstOrNull() ?: return null
        return school to adapter
    }
}
