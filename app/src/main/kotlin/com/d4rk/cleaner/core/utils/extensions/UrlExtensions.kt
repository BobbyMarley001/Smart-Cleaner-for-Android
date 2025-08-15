package com.d4rk.cleaner.core.utils.extensions

import androidx.core.util.PatternsCompat

fun String.isValidUrl(): Boolean {
    return PatternsCompat.WEB_URL.matcher(this.trim()).matches()
}
