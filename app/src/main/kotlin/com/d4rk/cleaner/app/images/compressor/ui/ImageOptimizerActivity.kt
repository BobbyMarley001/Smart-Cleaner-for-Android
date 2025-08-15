package com.d4rk.cleaner.app.images.compressor.ui

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.d4rk.cleaner.core.ui.BaseCleanupActivity
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ImageOptimizerActivity : BaseCleanupActivity() {
    private val viewModel: ImageOptimizerViewModel by viewModel()

    private var selectedImageUriString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        selectedImageUriString = intent.getStringExtra("selectedImageUri")
        if (!selectedImageUriString.isNullOrEmpty()) {
            lifecycleScope.launch {
                viewModel.onImageSelected(selectedImageUriString!!.toUri())
            }
        }
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        ImageOptimizerScreen(activity = this@ImageOptimizerActivity, viewModel)
    }
}