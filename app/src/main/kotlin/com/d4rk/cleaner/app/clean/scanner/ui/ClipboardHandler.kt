package com.d4rk.cleaner.app.clean.scanner.ui

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import com.d4rk.cleaner.core.utils.helpers.CleaningEventBus
import com.d4rk.cleaner.core.utils.helpers.ClipboardHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles clipboard monitoring and clearing logic.
 */
class ClipboardHandler(
    application: Application,
) {
    private val clipboardManager =
        application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val clipboardListener =
        ClipboardManager.OnPrimaryClipChangedListener { loadClipboardData() }
    private val clipboardHelper = ClipboardHelper(application, clipboardManager)

    private val _clipboardPreview = MutableStateFlow<String?>(null)
    val clipboardPreview: StateFlow<String?> = _clipboardPreview

    init {
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
        loadClipboardData()
    }

    /** Refresh clipboard preview manually. */
    fun refresh() {
        loadClipboardData()
    }

    fun onClipboardClear() {
        runCatching { clipboardHelper.clearClipboard() }
        _clipboardPreview.value = null
        CleaningEventBus.notifyCleaned(success = true)
    }

    private fun loadClipboardData() {
        _clipboardPreview.value = clipboardHelper.getClipboardText()
    }

    fun unregister() {
        clipboardManager.removePrimaryClipChangedListener(clipboardListener)
    }
}
