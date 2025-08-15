package com.d4rk.cleaner.app.clean.whatsapp.summary.ui

import androidx.compose.runtime.Composable
import com.d4rk.cleaner.core.ui.BaseCleanupActivity

class WhatsAppCleanerActivity : BaseCleanupActivity() {
    @Composable
    override fun ScreenContent() {
        WhatsappCleanerSummaryScreen(activity = this)
    }
}