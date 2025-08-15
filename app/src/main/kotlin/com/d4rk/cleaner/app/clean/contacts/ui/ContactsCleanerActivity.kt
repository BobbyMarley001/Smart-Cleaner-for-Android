package com.d4rk.cleaner.app.clean.contacts.ui

import androidx.compose.runtime.Composable
import com.d4rk.cleaner.core.ui.BaseCleanupActivity

class ContactsCleanerActivity : BaseCleanupActivity() {
    @Composable
    override fun ScreenContent() {
        ContactsCleanerScreen(activity = this@ContactsCleanerActivity)
    }
}
