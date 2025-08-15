package com.d4rk.cleaner.app.clean.largefiles.domain.data.model.ui

import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningState
import java.io.File

data class UiLargeFilesModel(
    val files: List<File> = emptyList(),
    val filesByDate: Map<String, List<File>> = emptyMap(),
    /** Map of file paths to selection state */
    val fileSelectionStates: Map<String, Boolean> = emptyMap(),
    val selectedFileCount: Int = 0,
    val cleaningState: CleaningState = CleaningState.Idle,
)
