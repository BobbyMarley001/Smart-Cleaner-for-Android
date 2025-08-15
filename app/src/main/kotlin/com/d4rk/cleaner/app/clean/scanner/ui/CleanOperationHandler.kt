package com.d4rk.cleaner.app.clean.scanner.ui

import com.d4rk.android.libs.apptoolkit.core.di.DispatcherProvider
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.ScreenState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiSnackbar
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.utils.helpers.UiTextHelper
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningState
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningType
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileEntry
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiAnalyzeModel
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import com.d4rk.cleaner.app.clean.scanner.domain.operations.FileAnalyzer
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.AnalyzeFilesUseCase
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.GetEmptyFoldersUseCase
import com.d4rk.cleaner.app.clean.scanner.work.FileCleanupWorker
import com.d4rk.cleaner.app.settings.cleaning.utils.constants.ExtensionsConstants
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.d4rk.cleaner.core.domain.model.network.Errors
import com.d4rk.cleaner.core.utils.helpers.FileGroupingHelper
import com.d4rk.cleaner.core.utils.extensions.selectedFiles
import com.d4rk.cleaner.core.utils.helpers.isProtectedAndroidDir
import com.d4rk.cleaner.core.work.FileCleanWorkEnqueuer
import com.d4rk.cleaner.core.work.FileCleaner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Encapsulates heavy cleaning operations so the ViewModel stays lean.
 */
class CleanOperationHandler(
    private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val dataStore: DataStore,
    private val fileCleanWorkEnqueuer: FileCleanWorkEnqueuer,
    private val analyzeFilesUseCase: AnalyzeFilesUseCase,
    private val getEmptyFoldersUseCase: GetEmptyFoldersUseCase,
    private val fileAnalyzer: FileAnalyzer,
    private val uiState: MutableStateFlow<UiStateScreen<UiScannerModel>>,
    private val postSnackbar: (UiTextHelper, Boolean) -> Unit,
    private val updateTrashSize: (Long) -> Unit,
    private val onWorkEnqueued: (UUID) -> Unit,
) {

    fun analyzeFiles() {
        if (uiState.value.data?.analyzeState?.state != CleaningState.Idle) {
            return
        }
        scope.launch(dispatchers.io) {
            val scannedFiles = mutableListOf<File>()
            val emptyFolders = mutableListOf<File>()

            analyzeFilesUseCase().collect { result ->
                uiState.update { currentState ->
                    val currentData: UiScannerModel = currentState.data ?: UiScannerModel()
                    when (result) {
                        is DataState.Loading -> currentState.copy(
                            screenState = ScreenState.IsLoading(),
                            data = currentData.copy(
                                analyzeState = currentData.analyzeState.copy(
                                    state = CleaningState.Analyzing,
                                    cleaningType = CleaningType.NONE
                                )
                            )
                        )
                        is DataState.Success -> {
                            scannedFiles.add(result.data)
                            currentState
                        }
                        is DataState.Error -> currentState.copy(
                            screenState = ScreenState.Error(),
                            data = currentData.copy(
                                analyzeState = currentData.analyzeState.copy(
                                    state = CleaningState.Error,
                                    cleaningType = CleaningType.NONE
                                )
                            ),
                            errors = currentState.errors + UiSnackbar(
                                message = UiTextHelper.StringResource(R.string.failed_to_analyze_files),
                                isError = true
                            )
                        )
                    }
                }
            }

            getEmptyFoldersUseCase().collect { result: DataState<File, Errors> ->
                if (result is DataState.Success) {
                    emptyFolders.add(result.data)
                }
            }

            val currentData = uiState.value.data ?: UiScannerModel()
            val fileTypesData = currentData.analyzeState.fileTypesData
            val preferences = mapOf(
                ExtensionsConstants.GENERIC_EXTENSIONS to dataStore.genericFilter.first(),
                ExtensionsConstants.IMAGE_EXTENSIONS to dataStore.deleteImageFiles.first(),
                ExtensionsConstants.VIDEO_EXTENSIONS to dataStore.deleteVideoFiles.first(),
                ExtensionsConstants.AUDIO_EXTENSIONS to dataStore.deleteAudioFiles.first(),
                ExtensionsConstants.OFFICE_EXTENSIONS to dataStore.deleteOfficeFiles.first(),
                ExtensionsConstants.ARCHIVE_EXTENSIONS to dataStore.deleteArchives.first(),
                ExtensionsConstants.APK_EXTENSIONS to dataStore.deleteApkFiles.first(),
                ExtensionsConstants.FONT_EXTENSIONS to dataStore.deleteFontFiles.first(),
                ExtensionsConstants.WINDOWS_EXTENSIONS to dataStore.deleteWindowsFiles.first(),
                ExtensionsConstants.EMPTY_FOLDERS to dataStore.deleteEmptyFolders.first(),
                ExtensionsConstants.OTHER_EXTENSIONS to dataStore.deleteOtherFiles.first()
            )

            val includeDuplicates = dataStore.deleteDuplicateFiles.first() &&
                    dataStore.duplicateScanEnabled.first()
            val (groupedFiles, duplicateOriginals, duplicateGroups) =
                withContext(dispatchers.io) {
                    fileAnalyzer.computeGroupedFiles(
                        scannedFiles = scannedFiles,
                        emptyFolders = emptyFolders,
                        fileTypesData = fileTypesData,
                        preferences = preferences,
                        includeDuplicates = includeDuplicates
                    )
                }

            val filesByDateForCategory = groupedFiles.mapValues { (_, entries) ->
                FileGroupingHelper.groupFileEntriesByDate(entries)
            }
            val duplicateGroupsByDate = FileGroupingHelper.groupDuplicateGroupsByDate(duplicateGroups)

            uiState.update { state ->
                val data = state.data ?: UiScannerModel()
                state.copy(
                    screenState = ScreenState.Success(),
                    data = data.copy(
                        analyzeState = data.analyzeState.copy(
                            scannedFileList = scannedFiles.map {
                                FileEntry(it.absolutePath, it.length(), it.lastModified())
                            },
                            emptyFolderList = emptyFolders.map {
                                FileEntry(it.absolutePath, 0, it.lastModified())
                            },
                            groupedFiles = groupedFiles,
                            filesByDateForCategory = filesByDateForCategory,
                            duplicateOriginals = duplicateOriginals,
                            duplicateGroups = duplicateGroups,
                            duplicateGroupsByDate = duplicateGroupsByDate,
                            state = CleaningState.ReadyToClean,
                            cleaningType = CleaningType.NONE
                        )
                    )
                )
            }
        }
    }

    fun cleanFiles(screenData: UiScannerModel?) {
        if (uiState.value.data?.analyzeState?.state != CleaningState.ReadyToClean) {
            return
        }

        val currentScreenData: UiScannerModel = screenData ?: run {
            postSnackbar(UiTextHelper.StringResource(R.string.data_not_available), true)
            return
        }

        val selected = currentScreenData.analyzeState.selectedFiles
        val filesToDelete = selected.associateWith { true }.selectedFiles()
        if (filesToDelete.isEmpty()) {
            val resId = if (selected.isNotEmpty()) {
                R.string.protected_android_folder
            } else {
                R.string.no_files_selected_to_clean
            }
            postSnackbar(UiTextHelper.StringResource(resId), false)
            return
        }

        scope.launch(dispatchers.io) {
            val validPaths = filesToDelete.map { it.absolutePath }
            FileCleaner.enqueue(
                enqueuer = fileCleanWorkEnqueuer,
                paths = validPaths,
                action = FileCleanupWorker.ACTION_DELETE,
                getWorkId = { dataStore.scannerCleanWorkId.first() },
                saveWorkId = { dataStore.saveScannerCleanWorkId(it) },
                clearWorkId = { dataStore.clearScannerCleanWorkId() },
                showSnackbar = { snackbar ->
                    if (snackbar.isError) {
                        postSnackbar(snackbar.message, true)
                    }
                },
                onEnqueued = { id ->
                    uiState.update { state ->
                        val currentData = state.data ?: UiScannerModel()
                        val total = validPaths.size
                        state.copy(
                            data = currentData.copy(
                                analyzeState = currentData.analyzeState.copy(
                                    state = CleaningState.Cleaning,
                                    cleaningType = CleaningType.DELETE,
                                    totalFilesToClean = total,
                                    cleanedFilesCount = 0
                                )
                            )
                        )
                    }
                    onWorkEnqueued(id)
                }
            )
        }
    }

    fun moveToTrash(files: List<FileEntry>) {
        if (files.isEmpty()) {
            postSnackbar(UiTextHelper.StringResource(R.string.no_files_selected_move_to_trash), false)
            return
        }

        val accessible = files.filterNot { File(it.path).isProtectedAndroidDir() }
        if (accessible.isEmpty()) {
            postSnackbar(UiTextHelper.StringResource(R.string.protected_android_folder), false)
            return
        }

        val paths = accessible.map { it.path }
        val totalFileSizeToMove: Long = accessible.sumOf { it.toFile().length() }

        scope.launch(dispatchers.io) {
            FileCleaner.enqueue(
                enqueuer = fileCleanWorkEnqueuer,
                paths = paths,
                action = FileCleanupWorker.ACTION_TRASH,
                getWorkId = { dataStore.scannerCleanWorkId.first() },
                saveWorkId = { dataStore.saveScannerCleanWorkId(it) },
                clearWorkId = { dataStore.clearScannerCleanWorkId() },
                showSnackbar = { snackbar ->
                    if (snackbar.isError) {
                        postSnackbar(snackbar.message, true)
                    }
                },
                onEnqueued = { id ->
                    uiState.update { state: UiStateScreen<UiScannerModel> ->
                        val currentData: UiScannerModel = state.data ?: UiScannerModel()
                        val total = paths.size
                        state.copy(
                            data = currentData.copy(
                                analyzeState = currentData.analyzeState.copy(
                                    state = CleaningState.Cleaning,
                                    cleaningType = CleaningType.MOVE_TO_TRASH,
                                    totalFilesToClean = total,
                                    cleanedFilesCount = 0
                                )
                            )
                        )
                    }
                    onWorkEnqueued(id)
                    updateTrashSize(totalFileSizeToMove)
                },
                errorMessage = UiTextHelper.StringResource(R.string.failed_to_move_files_to_trash)
            )
        }
    }

    fun onCleaningFailed() {
        uiState.update { state ->
            val current = state.data ?: UiScannerModel()
            state.copy(
                data = current.copy(
                    analyzeState = UiAnalyzeModel(state = CleaningState.Error)
                )
            )
        }
        postSnackbar(UiTextHelper.StringResource(R.string.cleanup_failed), true)
    }

    fun resetAfterError() {
        uiState.update { state ->
            val current = state.data ?: UiScannerModel()
            state.copy(
                data = current.copy(
                    analyzeState = UiAnalyzeModel()
                )
            )
        }
    }
}
