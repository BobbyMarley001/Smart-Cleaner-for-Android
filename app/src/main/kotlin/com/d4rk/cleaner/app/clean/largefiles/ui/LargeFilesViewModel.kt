package com.d4rk.cleaner.app.clean.largefiles.ui

import android.app.Application
import androidx.work.WorkManager
import com.d4rk.android.libs.apptoolkit.core.di.DispatcherProvider
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.ScreenState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiSnackbar
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.updateData
import com.d4rk.android.libs.apptoolkit.core.ui.base.ScreenViewModel
import com.d4rk.android.libs.apptoolkit.core.utils.helpers.UiTextHelper
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.largefiles.domain.actions.LargeFilesAction
import com.d4rk.cleaner.app.clean.largefiles.domain.actions.LargeFilesEvent
import com.d4rk.cleaner.app.clean.largefiles.domain.data.model.ui.UiLargeFilesModel
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningState
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.GetLargestFilesUseCase
import com.d4rk.cleaner.app.clean.scanner.work.FileCleanupWorker
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.d4rk.cleaner.core.utils.extensions.selectedFiles
import com.d4rk.cleaner.core.utils.helpers.FileGroupingHelper
import com.d4rk.cleaner.core.utils.helpers.isProtectedAndroidDir
import com.d4rk.cleaner.core.work.FileCleanWorkEnqueuer
import com.d4rk.cleaner.core.work.FileCleaner
import com.d4rk.cleaner.core.work.observeFileCleanWork
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID

class LargeFilesViewModel(
    private val application: Application,
    private val getLargestFilesUseCase: GetLargestFilesUseCase,
    private val dataStore: DataStore,
    private val dispatchers: DispatcherProvider,
    private val fileCleanWorkEnqueuer: FileCleanWorkEnqueuer
) : ScreenViewModel<UiLargeFilesModel, LargeFilesEvent, LargeFilesAction>(
    initialState = UiStateScreen(data = UiLargeFilesModel())
) {

    private val limit = 20

    private var activeWorkObserver: Job? = null

    init {
        onEvent(LargeFilesEvent.LoadLargeFiles)
        launch(dispatchers.io) {
            dataStore.largeFilesCleanWorkId.first()?.let { id ->
                val uuid = UUID.fromString(id)
                val info = WorkManager.getInstance(application)
                    .getWorkInfoByIdFlow(uuid)
                    .first()
                if (info == null || info.state.isFinished) {
                    dataStore.clearLargeFilesCleanWorkId()
                } else {
                    observeWork(uuid)
                }
            }
        }
    }

    override fun onEvent(event: LargeFilesEvent) {
        when (event) {
            LargeFilesEvent.LoadLargeFiles -> loadLargeFiles()
            is LargeFilesEvent.OnFileSelectionChange -> onFileSelectionChange(
                event.file,
                event.isChecked
            )

            LargeFilesEvent.DeleteSelectedFiles -> deleteSelected()
        }
    }

    private fun loadLargeFiles() {
        launch(context = dispatchers.io) {
            getLargestFilesUseCase(limit).collectLatest { result ->
                _uiState.update { current ->
                    when (result) {
                        is DataState.Loading -> current.copy(
                            screenState = ScreenState.IsLoading(),
                            data = current.data?.copy(cleaningState = CleaningState.Analyzing)
                                ?: UiLargeFilesModel(cleaningState = CleaningState.Analyzing)
                        )

                        is DataState.Success -> {
                            val groupedByDate = FileGroupingHelper.groupFilesByDate(result.data)
                            current.copy(
                                screenState = if (result.data.isEmpty()) ScreenState.NoData() else ScreenState.Success(),
                                data = current.data?.copy(
                                    files = result.data,
                                    filesByDate = groupedByDate,
                                    fileSelectionStates = emptyMap(),
                                    selectedFileCount = 0,
                                    cleaningState = CleaningState.Idle,
                                )
                                    ?: UiLargeFilesModel(files = result.data, filesByDate = groupedByDate)
                            )
                        }

                        is DataState.Error -> current.copy(
                            screenState = ScreenState.Error(),
                            data = current.data?.copy(cleaningState = CleaningState.Error),
                            errors = current.errors + UiSnackbar(
                                message = UiTextHelper.DynamicString("${result.error}"),
                                isError = true
                            )
                        )
                    }
                }
            }
        }
    }

    private fun onFileSelectionChange(file: File, isChecked: Boolean) {
        _uiState.updateData(newState = _uiState.value.screenState) { current ->
            val updated = current.fileSelectionStates.toMutableMap()
                .apply { this[file.absolutePath] = isChecked }
            current.copy(
                fileSelectionStates = updated,
                selectedFileCount =
                    updated.count { it.value && !File(it.key).isProtectedAndroidDir() }
            )
        }
    }

    private fun deleteSelected() {
        launch(context = dispatchers.io) {
            val selection = _uiState.value.data?.fileSelectionStates ?: emptyMap()
            val files = selection.selectedFiles()
            if (files.isEmpty()) {
                val resId = if (selection.any { it.value }) {
                    R.string.protected_android_folder
                } else {
                    R.string.no_files_selected_to_delete
                }
                sendAction(
                    LargeFilesAction.ShowSnackbar(
                        UiSnackbar(
                            message = UiTextHelper.StringResource(resId)
                        )
                    )
                )
                return@launch
            }

            FileCleaner.enqueue(
                enqueuer = fileCleanWorkEnqueuer,
                paths = files.map { it.absolutePath },
                action = FileCleanupWorker.ACTION_DELETE,
                getWorkId = { dataStore.largeFilesCleanWorkId.first() },
                saveWorkId = { dataStore.saveLargeFilesCleanWorkId(it) },
                clearWorkId = { dataStore.clearLargeFilesCleanWorkId() },
                showSnackbar = { sendAction(LargeFilesAction.ShowSnackbar(it)) },
                onEnqueued = { id -> observeWork(id) },
                onError = {
                    _uiState.update {
                        it.copy(data = it.data?.copy(cleaningState = CleaningState.Error))
                    }
                }
            )
        }
    }
    private fun observeWork(id: UUID) {
        activeWorkObserver = observeFileCleanWork(
            previousObserver = activeWorkObserver,
            application = application,
            dispatcher = dispatchers.io,
            workId = id,
            clearWorkId = { dataStore.clearLargeFilesCleanWorkId() },
            onRunning = {
                _uiState.update {
                    it.copy(
                        screenState = ScreenState.IsLoading(),
                        data = it.data?.copy(cleaningState = CleaningState.Cleaning)
                            ?: UiLargeFilesModel(cleaningState = CleaningState.Cleaning)
                    )
                }
            },
            onSuccess = { info ->
                val failedPaths =
                    info.outputData.getStringArray(FileCleanupWorker.KEY_FAILED_PATHS)
                val failedCount = failedPaths?.size ?: 0
                val selectedCount =
                    _uiState.value.data?.fileSelectionStates
                        ?.count { it.value && !File(it.key).isProtectedAndroidDir() } ?: 0
                val successCount = selectedCount - failedCount

                _uiState.update {
                    it.copy(data = it.data?.copy(cleaningState = CleaningState.Result))
                }

                onEvent(LargeFilesEvent.LoadLargeFiles)

                val message = if (failedCount > 0) {
                    UiTextHelper.StringResource(
                        R.string.cleanup_partial,
                        listOf(successCount, failedCount)
                    )
                } else {
                    UiTextHelper.StringResource(R.string.all_clean)
                }

                sendAction(
                    LargeFilesAction.ShowSnackbar(
                        UiSnackbar(message = message)
                    )
                )
            },
            onFailed = {
                _uiState.update {
                    it.copy(
                        screenState = ScreenState.Error(),
                        data = it.data?.copy(cleaningState = CleaningState.Error),
                        errors = it.errors + UiSnackbar(
                            message = UiTextHelper.StringResource(R.string.failed_to_delete_files),
                            isError = true
                        )
                    )
                }
            },
            onCancelled = {
                _uiState.update {
                    it.copy(
                        screenState = ScreenState.Success(),
                        data = it.data?.copy(cleaningState = CleaningState.Idle)
                    )
                }
            }
        )
    }
}
