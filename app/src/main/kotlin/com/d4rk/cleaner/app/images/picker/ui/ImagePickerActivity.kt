package com.d4rk.cleaner.app.images.picker.ui

import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.d4rk.cleaner.core.ui.BaseCleanupActivity

class ImagePickerActivity : BaseCleanupActivity() {
    private val viewModel: ImagePickerViewModel by viewModels()

    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            viewModel.setSelectedImageUri(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectImage()
    }

    @Composable
    override fun ScreenContent() {
        ImagePickerComposable(activity = this@ImagePickerActivity, viewModel)
    }

    fun selectImage() {
        pickMediaLauncher.launch(input = PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}