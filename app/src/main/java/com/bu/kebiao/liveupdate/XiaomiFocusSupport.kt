package com.bu.kebiao.liveupdate

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log

object XiaomiFocusSupport {
    private const val NOTIFICATION_FOCUS_PROTOCOL = "notification_focus_protocol"
    private const val MIN_ISLAND_PROTOCOL_VERSION = 3
    private const val MIUI_STATUS_BAR_PROVIDER = "content://miui.statusbar.notification.public"
    private const val METHOD_CAN_SHOW_FOCUS = "canShowFocus"
    private const val KEY_PACKAGE = "package"
    private const val KEY_CAN_SHOW_FOCUS = "canShowFocus"
    private const val TAG = "XiaomiFocusSupport"

    fun canAttachIslandParam(context: Context): Boolean {
        if (!isLikelyXiaomiDevice(Build.MANUFACTURER) && !isLikelyXiaomiDevice(Build.BRAND)) {
            return false
        }
        if (!supportsIslandProtocol(readFocusProtocolVersion(context))) {
            return false
        }
        return hasFocusPermission(context)
    }

    fun isLikelyXiaomiDevice(value: String?): Boolean {
        val normalized = value?.trim()?.lowercase().orEmpty()
        return normalized == "xiaomi" ||
            normalized == "redmi" ||
            normalized == "poco"
    }

    fun supportsIslandProtocol(protocolVersion: Int): Boolean =
        protocolVersion >= MIN_ISLAND_PROTOCOL_VERSION

    private fun readFocusProtocolVersion(context: Context): Int =
        runCatching {
            Settings.System.getInt(context.contentResolver, NOTIFICATION_FOCUS_PROTOCOL, 0)
        }.onFailure { error ->
            Log.d(TAG, "read focus protocol failed", error)
        }.getOrDefault(0)

    private fun hasFocusPermission(context: Context): Boolean =
        runCatching {
            val extras = Bundle().apply {
                putString(KEY_PACKAGE, context.packageName)
            }
            val result = context.contentResolver.call(
                Uri.parse(MIUI_STATUS_BAR_PROVIDER),
                METHOD_CAN_SHOW_FOCUS,
                null,
                extras
            )
            result?.getBoolean(KEY_CAN_SHOW_FOCUS, false) ?: false
        }.onFailure { error ->
            Log.d(TAG, "query focus permission failed", error)
        }.getOrDefault(false)
}
