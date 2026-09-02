package com.storage.redirect.x.data.model

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

// 应用重定向模式：白名单（默认隔离，放行允许项）/ 黑名单（默认放行，隔离黑名单项）
enum class RedirectMode {
    Whitelist,
    Blacklist;

    fun toJsonValue(): String = if (this == Blacklist) "black" else "whitelist"

    companion object {
        fun fromJsonValue(value: String?): RedirectMode =
            if (value.equals("black", ignoreCase = true) || value.equals("blacklist", ignoreCase = true)) {
                Blacklist
            } else {
                Whitelist
            }
    }
}

// 应用重定向配置
data class AppRedirectConfig(
    val packageName: String,
    val isEnabled: Boolean = true,
    val mode: RedirectMode = RedirectMode.Whitelist,
    val allowedRealPaths: List<String> = emptyList(),
    val pathMappings: List<StoragePathMapping> = emptyList()
) {
    val isActive: Boolean get() = isEnabled

    // 序列化为 JSON（用于写入用户维度配置）
    fun toUserJson(): JsonObject {
        val obj = JsonObject()
        obj.addProperty("enabled", isEnabled)
        if (mode != RedirectMode.Whitelist) {
            obj.addProperty("mode", mode.toJsonValue())
        }
        if (allowedRealPaths.isNotEmpty()) {
            val arr = JsonArray()
            allowedRealPaths.forEach { arr.add(it) }
            obj.add("allowed_real_paths", arr)
        }
        if (pathMappings.isNotEmpty()) {
            val map = JsonObject()
            pathMappings.forEach { map.addProperty(it.requestPath, it.finalPath) }
            obj.add("path_mappings", map)
        }
        return obj
    }

    companion object {
        // 从用户维度 JSON 解析
        fun fromUserJson(packageName: String, json: JsonObject): AppRedirectConfig {
            val isEnabled = json.get("enabled")?.asBoolean ?: true
            val mode = RedirectMode.fromJsonValue(json.get("mode")?.asString)

            val allowedRealPaths = json.getAsJsonArray("allowed_real_paths")
                ?.mapNotNull { it.asString?.trim()?.takeIf { s -> s.isNotEmpty() } }
                ?: emptyList()

            val mappings = mutableListOf<StoragePathMapping>()
            val mappingsObj = json.get("path_mappings")
            if (mappingsObj != null && mappingsObj.isJsonObject) {
                mappingsObj.asJsonObject.entrySet().forEach { (key, value) ->
                    val requestPath = key.trim()
                    val finalPath = value.asString?.trim() ?: return@forEach
                    if (requestPath.isNotEmpty() && finalPath.isNotEmpty()) {
                        mappings.add(StoragePathMapping(requestPath, finalPath))
                    }
                }
            }

            return AppRedirectConfig(
                packageName = packageName,
                isEnabled = isEnabled,
                mode = mode,
                allowedRealPaths = allowedRealPaths,
                pathMappings = mappings.sortedBy { it.requestPath },
            )
        }
    }
}
