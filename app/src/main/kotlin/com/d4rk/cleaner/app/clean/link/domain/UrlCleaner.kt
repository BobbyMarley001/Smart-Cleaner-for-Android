package com.d4rk.cleaner.app.clean.link.domain

import androidx.core.net.toUri

object UrlCleaner {
    private val trackingParams = setOf(
        "fbclid", "gclid", "igshid", "mc_cid", "mc_eid", "sfnsn"
    )

    private val trackingParamPrefixes = listOf(
        "utm_", "ga_", "fb_", "cx_", "wt_", "sfmc_", "cxrecs_s", "mibextid",
        "jsessionid", "sessionid"
    )

    fun clean(url: String): String {
        val uri = runCatching {
            url.toUri()
        }.getOrElse { return url }
        if (uri.scheme == null) return url
        val builder = uri.buildUpon().clearQuery()
        uri.queryParameterNames
            .forEach { name ->
                val value = uri.getQueryParameter(name)
                if (value.isNullOrEmpty()) return@forEach

                val lower = name.lowercase()
                if (lower in trackingParams) return@forEach
                if (trackingParamPrefixes.any { lower.startsWith(it) }) return@forEach

                builder.appendQueryParameter(name, value)
            }
        return builder.build().toString()
    }
}
