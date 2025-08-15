package com.d4rk.cleaner.app.clean.whatsapp.details.ui

import android.os.Bundle
import androidx.compose.runtime.Composable
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.actions.WhatsAppCleanerEvent
import com.d4rk.cleaner.app.clean.whatsapp.summary.ui.WhatsappCleanerSummaryViewModel
import com.d4rk.cleaner.core.ui.BaseCleanupActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class WhatsAppDetailsActivity : BaseCleanupActivity() {

    companion object {
        const val EXTRA_TYPE = "type"
    }

    private val viewModel: WhatsappCleanerSummaryViewModel by viewModel()
    private val detailsViewModel: DetailsViewModel by viewModel()
    private var type: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        type = intent.getStringExtra(EXTRA_TYPE) ?: ""
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        DetailsScreen(
            viewModel = viewModel,
            detailsViewModel = detailsViewModel,
            title = type,
            onDelete = { viewModel.onEvent(WhatsAppCleanerEvent.DeleteSelected(it)) },
            activity = this
        )
    }
}