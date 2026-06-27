package com.bu.kebiao.data.adapter

data class SchoolInfo(
    val id: String,
    val name: String,
    val initial: String,
    val folder: String,
    val adapters: List<AdapterInfo>
)

data class AdapterInfo(
    val adapterId: String,
    val adapterName: String,
    val category: String,
    val jsPath: String,
    val importUrl: String,
    val description: String,
    val maintainer: String
) {
    val isGeneralTool: Boolean
        get() = category == "GENERAL_TOOL"
}
