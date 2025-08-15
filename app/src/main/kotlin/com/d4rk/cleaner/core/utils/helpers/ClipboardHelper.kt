package com.d4rk.cleaner.core.utils.helpers

import android.app.Application
import android.content.ClipboardManager
import com.d4rk.cleaner.core.utils.extensions.clearClipboardCompat

/**
 * Helper around [ClipboardManager] to fetch and clear clipboard content.
 */
class ClipboardHelper(
    private val application: Application,
    private val clipboardManager: ClipboardManager,
) {
    fun getClipboardText(): String? {
        return clipboardManager.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(application)
            ?.toString()
            ?.trim()
    }

    fun clearClipboard() {
        clipboardManager.clearClipboardCompat()
    }
}
