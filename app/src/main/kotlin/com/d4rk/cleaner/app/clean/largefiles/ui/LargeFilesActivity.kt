package com.d4rk.cleaner.app.clean.largefiles.ui

import androidx.compose.runtime.Composable
import com.d4rk.cleaner.core.ui.BaseCleanupActivity

class LargeFilesActivity : BaseCleanupActivity() {
    @Composable
    override fun ScreenContent() {
        LargeFilesScreen(activity = this@LargeFilesActivity)
    }
}
