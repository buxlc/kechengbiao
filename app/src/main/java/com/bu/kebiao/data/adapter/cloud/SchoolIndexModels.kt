package com.bu.kebiao.data.adapter.cloud

data class SchoolIndex(
    val schools: List<SchoolIndexEntry>
)

data class SchoolIndexEntry(
    val id: String,
    val name: String,
    val initial: String,
    val folder: String,
    val adapters: List<SchoolIndexAdapter>
)

data class SchoolIndexAdapter(
    val adapterId: String,
    val adapterName: String,
    val category: String,
    val jsPath: String,
    val importUrl: String,
    val description: String,
    val maintainer: String
)
