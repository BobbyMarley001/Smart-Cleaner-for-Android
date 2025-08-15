package com.d4rk.cleaner.app.clean.scanner.ui

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.d4rk.android.libs.apptoolkit.core.di.DispatcherProvider
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.ScreenState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiSnackbar
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.applyResult
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.dismissSnackbar
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.setLoading
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.showSnackbar
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.successData
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.updateData
import com.d4rk.android.libs.apptoolkit.core.ui.base.ScreenViewModel
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.ScreenMessageType
import com.d4rk.android.libs.apptoolkit.core.utils.helpers.UiTextHelper
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.memory.domain.data.model.StorageInfo
import com.d4rk.cleaner.app.clean.scanner.domain.actions.ScannerAction
import com.d4rk.cleaner.app.clean.scanner.domain.actions.ScannerEvent
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningState
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningType
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileEntry
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileTypesData
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.WhatsAppMediaSummary
import com.d4rk.cleaner.app.clean.scanner.domain.operations.CleaningManager
import com.d4rk.cleaner.app.clean.scanner.domain.operations.FileAnalyzer
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.AnalyzeFilesUseCase
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.GetEmptyFoldersUseCase
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.GetFileTypesUseCase
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.GetLargestFilesUseCase
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.GetPromotedAppUseCase
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.GetStorageInfoUseCase
import com.d4rk.cleaner.app.clean.scanner.utils.helpers.getWhatsAppMediaSummary
import com.d4rk.cleaner.app.clean.scanner.work.FileCleanupWorker
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.d4rk.cleaner.core.domain.model.network.Errors
import com.d4rk.cleaner.core.utils.helpers.CleaningEventBus
import com.d4rk.cleaner.core.utils.helpers.FileSizeFormatter
import com.d4rk.cleaner.core.utils.helpers.isProtectedAndroidDir
import com.d4rk.cleaner.core.work.FileCleanWorkEnqueuer
import com.d4rk.cleaner.core.work.FileCleaner
import com.d4rk.cleaner.core.work.observeFileCleanWork
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.yield
import java.io.File
import java.util.UUID

// Making this configurable clarifies the wait after cleaning tasks.
private const val EMPTY_FOLDERS_HIDE_DURATION_MS = 5 * 60 * 1000L

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModel(
    private val application: Application,
    private val getStorageInfoUseCase: GetStorageInfoUseCase,
    private val getFileTypesUseCase: GetFileTypesUseCase,
    private val analyzeFilesUseCase: AnalyzeFilesUseCase,
    private val cleaningManager: CleaningManager,
    private val fileAnalyzer: FileAnalyzer,
    private val getPromotedAppUseCase: GetPromotedAppUseCase,
    private val getLargestFilesUseCase: GetLargestFilesUseCase,
    private val getEmptyFoldersUseCase: GetEmptyFoldersUseCase,
    private val dispatchers: DispatcherProvider,
    private val dataStore: DataStore,
    private val fileCleanWorkEnqueuer: FileCleanWorkEnqueuer
) : ScreenViewModel<UiScannerModel, ScannerEvent, ScannerAction>(
    initialState = UiStateScreen(data = UiScannerModel())
) {

    val clipboardPreview: StateFlow<String?> get() = clipboardHandler.clipboardPreview

    val cleanStreak: StateFlow<Int> get() = streakHandler.cleanStreak
    val streakRecord: StateFlow<Int> get() = streakHandler.streakRecord
    val showStreakCard: StateFlow<Boolean> get() = streakHandler.showStreakCard
    val streakHideUntil: StateFlow<Long> get() = streakHandler.streakHideUntil

    private val _whatsAppMediaSummary = MutableStateFlow(WhatsAppMediaSummary())
    val whatsAppMediaSummary: StateFlow<WhatsAppMediaSummary> = _whatsAppMediaSummary
    private val _whatsAppMediaLoaded = MutableStateFlow(false)
    val whatsAppMediaLoaded: StateFlow<Boolean> = _whatsAppMediaLoaded
    private val _isWhatsAppInstalled = MutableStateFlow(false)
    val isWhatsAppInstalled: StateFlow<Boolean> = _isWhatsAppInstalled


    private val _largestFiles = MutableStateFlow<List<File>>(emptyList())
    val largestFiles: StateFlow<List<File>> = _largestFiles

    private val _emptyFolders = MutableStateFlow<List<File>>(emptyList())
    val emptyFolders: StateFlow<List<File>> = _emptyFolders

    private val _emptyFoldersHideUntil = MutableStateFlow(0L)
    val emptyFoldersHideUntil: StateFlow<Long> = _emptyFoldersHideUntil

    private val _cleaningApks = MutableStateFlow(false)
    val cleaningApks: StateFlow<Boolean> = _cleaningApks

    private var pendingEmptyFoldersCleanup = false

    private val clipboardHandler = ClipboardHandler(application)
    private val streakHandler = StreakHandler(
        dataStore = dataStore,
        scope = viewModelScope,
        dispatchers = dispatchers,
        updateHideDialogVisibility = { visible ->
            _uiState.updateData(ScreenState.Success()) { current ->
                current.copy(isHideStreakDialogVisible = visible)
            }
        }
    )
    private val cleanOperationHandler = CleanOperationHandler(
        scope = viewModelScope,
        dispatchers = dispatchers,
        dataStore = dataStore,
        fileCleanWorkEnqueuer = fileCleanWorkEnqueuer,
        analyzeFilesUseCase = analyzeFilesUseCase,
        getEmptyFoldersUseCase = getEmptyFoldersUseCase,
        fileAnalyzer = fileAnalyzer,
        uiState = _uiState,
        postSnackbar = ::postSnackbar,
        updateTrashSize = ::updateTrashSize,
        onWorkEnqueued = ::observeCleaningWork
    )

    private var activeCleanWorkObserver: Job? = null


    init {
        onEvent(ScannerEvent.LoadInitialData)
        loadCleanedSpace()
        checkWhatsAppInstalled()
        loadWhatsAppMedia()
        loadPromotedApp()
        loadLargestFilesPreview()
        loadEmptyFoldersPreview()
        loadEmptyFoldersHideUntil()
        launch(dispatchers.io) {
            dataStore.scannerCleanWorkId.first()?.let { id ->
                val uuid = UUID.fromString(id)
                val info = WorkManager.getInstance(application)
                    .getWorkInfoByIdFlow(uuid)
                    .first()
                if (info == null || info.state.isFinished) {
                    dataStore.clearScannerCleanWorkId()
                } else {
                    observeCleaningWork(uuid)
                }
            }
        }
        launch(dispatchers.io) {
            CleaningEventBus.events.collectLatest {
                onEvent(ScannerEvent.RefreshData)
            }
        }
    }

    override fun onEvent(event: ScannerEvent) {
        when (event) {
            is ScannerEvent.LoadInitialData -> loadInitialData()
            is ScannerEvent.AnalyzeFiles -> cleanOperationHandler.analyzeFiles()
            is ScannerEvent.RefreshData -> refreshData()
            is ScannerEvent.DeleteFiles -> deleteFiles(files = event.files)
            is ScannerEvent.MoveToTrash -> {
                cleanOperationHandler.moveToTrash(files = event.files)
            }
            is ScannerEvent.ToggleAnalyzeScreen -> toggleAnalyzeScreen(visible = event.visible)
            is ScannerEvent.OnFileSelectionChange -> onFileSelectionChange(
                file = event.file,
                isChecked = event.isChecked
            )

            is ScannerEvent.ToggleSelectAllFiles -> toggleSelectAllFiles()
            is ScannerEvent.OnGlobalSelectAllClick -> onGlobalSelectAllClick()
            is ScannerEvent.ConfirmGlobalSelectAll -> confirmGlobalSelectAll(event.dontShowAgain)
            is ScannerEvent.DismissGlobalSelectAllWarning -> setGlobalSelectAllWarningDialogVisibility(false)
            is ScannerEvent.ToggleSelectFilesForCategory -> toggleSelectFilesForCategory(category = event.category)
            is ScannerEvent.ToggleSelectFilesForDate -> toggleSelectFilesForDate(
                event.files,
                event.isChecked
            )

            is ScannerEvent.CleanFiles -> {
                cleanOperationHandler.cleanFiles(screenData)
            }
            is ScannerEvent.CleanWhatsAppFiles -> onCleanWhatsAppFiles()
            is ScannerEvent.CleanCache -> onCleanCache()
            is ScannerEvent.MoveSelectedToTrash -> moveSelectedToTrash()
            is ScannerEvent.SetDeleteForeverConfirmationDialogVisibility -> setDeleteForeverConfirmationDialogVisibility(
                isVisible = event.isVisible
            )

            is ScannerEvent.SetMoveToTrashConfirmationDialogVisibility -> setMoveToTrashConfirmationDialogVisibility(
                isVisible = event.isVisible
            )

            is ScannerEvent.SetHideStreakDialogVisibility -> streakHandler.setHideStreakDialogVisibility(event.isVisible)
            ScannerEvent.HideStreakForNow -> streakHandler.hideStreakForNow()
            ScannerEvent.HideStreakPermanently -> streakHandler.hideStreakPermanently()
            is ScannerEvent.DismissSnackbar -> screenState.dismissSnackbar()
        }
    }

    private fun loadInitialData() {
        launch(context = dispatchers.io) {
            _uiState.setLoading()
            _uiState.updateData(newState = _uiState.value.screenState) { currentData: UiScannerModel ->
                currentData.copy(
                    storageInfo = currentData.storageInfo.copy(
                        isCleanedSpaceLoading = true,
                        isFreeSpaceLoading = true
                    )
                )
            }

            combine(
                flow = getStorageInfoUseCase(),
                flow2 = getFileTypesUseCase(),
                flow3 = dataStore.cleanedSpace.distinctUntilChanged()
            ) { storageState: DataState<UiScannerModel, Errors>, fileTypesState: DataState<FileTypesData, Errors>, cleanedSpaceValue: Long ->

                Triple(first = storageState, second = fileTypesState, third = cleanedSpaceValue)
            }.collect { (storageState: DataState<UiScannerModel, Errors>, fileTypesState: DataState<FileTypesData, Errors>, cleanedSpaceValue: Long) ->
                _uiState.update { currentState: UiStateScreen<UiScannerModel> ->
                    val currentData: UiScannerModel = currentState.data ?: UiScannerModel()
                    val updatedStorageInfo: StorageInfo = currentData.storageInfo.copy(
                        isFreeSpaceLoading = storageState is DataState.Loading,
                        isCleanedSpaceLoading = false,
                        cleanedSpace = FileSizeFormatter.format(cleanedSpaceValue)
                    )
                    val finalStorageInfo = if (storageState is DataState.Success) {
                        storageState.data.storageInfo.copy(
                            cleanedSpace = updatedStorageInfo.cleanedSpace,
                            isCleanedSpaceLoading = updatedStorageInfo.isCleanedSpaceLoading,
                            isFreeSpaceLoading = false
                        )
                    } else {
                        updatedStorageInfo
                    }

                    val updatedData: UiScannerModel = currentData.copy(
                        storageInfo = finalStorageInfo,
                        analyzeState = currentData.analyzeState.copy(fileTypesData = if (fileTypesState is DataState.Success) fileTypesState.data else currentData.analyzeState.fileTypesData)
                    )
                    val errors: List<UiSnackbar> = buildList {
                        if (storageState is DataState.Error) {
                            add(
                                element = UiSnackbar(
                                    message = UiTextHelper.StringResource(R.string.failed_to_load_storage_info),
                                    isError = true
                                )
                            )
                        }
                        if (fileTypesState is DataState.Error) {
                            if (storageState !is DataState.Error || storageState.error != fileTypesState.error) {
                                add(
                                    element = UiSnackbar(
                                        message = UiTextHelper.StringResource(R.string.failed_to_load_file_types),
                                        isError = true
                                    )
                                )
                            }
                        }
                    }.takeIf { it.isNotEmpty() } ?: emptyList()

                    currentState.copy(
                        screenState = when {
                            storageState is DataState.Loading || fileTypesState is DataState.Loading -> ScreenState.IsLoading()
                            storageState is DataState.Error || fileTypesState is DataState.Error -> ScreenState.Error()
                            storageState is DataState.Success && fileTypesState is DataState.Success -> ScreenState.Success()
                            else -> currentState.screenState
                        },
                        data = updatedData,
                        errors = currentState.errors + errors,
                        snackbar = currentState.snackbar
                    )
                }
            }
        }
    }

    private fun refreshData() {
        loadInitialData()
        loadWhatsAppMedia()
        clipboardHandler.refresh()
        loadLargestFilesPreview()
        loadEmptyFoldersPreview()
        if (screenData?.analyzeState?.isAnalyzeScreenVisible == true) {
            cleanOperationHandler.analyzeFiles()
        }
    }




    private fun deleteFiles(files: Set<FileEntry>, fromApkCleaner: Boolean = false) {
        if (files.isEmpty()) {
            postSnackbar(
                message = UiTextHelper.StringResource(R.string.no_files_selected_to_delete),
                isError = false,
            )
            return
        }

        val accessible = files.filterNot { File(it.path).isProtectedAndroidDir() }
        if (accessible.isEmpty()) {
            postSnackbar(
                message = UiTextHelper.StringResource(R.string.protected_android_folder),
                isError = false,
            )
            return
        }

        val paths = accessible.map { it.path }

        launch(context = dispatchers.io) {
            FileCleaner.enqueue(
                enqueuer = fileCleanWorkEnqueuer,
                paths = paths,
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
                    if (fromApkCleaner) _cleaningApks.value = true
                    _uiState.update { state ->
                        val currentData: UiScannerModel = state.data ?: UiScannerModel()
                        state.copy(
                            data = currentData.copy(
                                analyzeState = currentData.analyzeState.copy(
                                    state = CleaningState.Cleaning,
                                    cleaningType = CleaningType.DELETE,
                                ),
                            ),
                        )
                    }
                    observeCleaningWork(id)
                },
                onError = {
                    if (fromApkCleaner) _cleaningApks.value = false
                    if (pendingEmptyFoldersCleanup) pendingEmptyFoldersCleanup = false
                },
            )
        }
    }
    fun onCloseAnalyzeComposable() {
        _uiState.update { state: UiStateScreen<UiScannerModel> ->
            val currentData: UiScannerModel = state.data ?: UiScannerModel()
            state.copy(
                data = currentData.copy(
                    analyzeState = currentData.analyzeState.copy(
                        isAnalyzeScreenVisible = false,
                        scannedFileList = emptyList(),
                        emptyFolderList = emptyList(),
                        groupedFiles = emptyMap(),
                        filesByDateForCategory = emptyMap(),
                        duplicateGroups = emptyList(),
                        duplicateGroupsByDate = emptyMap(),
                        selectedFiles = mutableSetOf(),
                        selectedFilesCount = 0,
                        areAllFilesSelected = false,
                        state = CleaningState.Idle,
                        cleaningType = CleaningType.NONE
                    )
                )
            )
        }
    }

    fun onFileSelectionChange(file: File, isChecked: Boolean) {
        launch(context = dispatchers.default) {
            _uiState.update { state: UiStateScreen<UiScannerModel> ->
                val currentData: UiScannerModel = state.data ?: UiScannerModel()
                val path = file.absolutePath
                val updatedFileSelectionStates: MutableSet<String> =
                    currentData.analyzeState.selectedFiles.toMutableSet().apply {
                        if (isChecked) add(path) else remove(path)
                    }
                val visibleFiles: List<FileEntry> =
                    currentData.analyzeState.groupedFiles.values.flatten()
                val visiblePaths = visibleFiles.map { it.path }
                val selectedVisibleCount: Int =
                    updatedFileSelectionStates.count { it in visiblePaths && !File(it).isProtectedAndroidDir() }
                state.copy(
                    data = currentData.copy(
                        analyzeState = currentData.analyzeState.copy(
                            selectedFiles = updatedFileSelectionStates,
                            selectedFilesCount = selectedVisibleCount,
                            areAllFilesSelected = selectedVisibleCount == visibleFiles.size && visibleFiles.isNotEmpty()
                        )
                    )
                )
            }
        }
    }

    fun toggleSelectAllFiles() {
        launch(context = dispatchers.default) {
            _uiState.update { state: UiStateScreen<UiScannerModel> ->
                val currentData: UiScannerModel = state.data ?: UiScannerModel()
                val visibleFiles: List<FileEntry> =
                    currentData.analyzeState.groupedFiles.values.flatten()
                val visiblePaths = visibleFiles.map { it.path }
                val hasFiles = visibleFiles.isNotEmpty()
                val newState: Boolean =
                    if (hasFiles) !currentData.analyzeState.areAllFilesSelected else false
                val duplicateOriginals =
                    currentData.analyzeState.duplicateOriginals.map { it.path }.toSet()

                val updatedSet: MutableSet<String> = if (newState) {
                    val base = currentData.analyzeState.selectedFiles.toMutableSet()
                    visibleFiles.chunked(200).forEach { chunk ->
                        chunk.forEach { file ->
                            val path = file.path
                            if (path !in duplicateOriginals) base.add(path)
                        }
                        yield()
                    }
                    base
                } else mutableSetOf()

                val selectedVisibleCount = if (newState) {
                    updatedSet.count { it in visiblePaths && !File(it).isProtectedAndroidDir() }
                } else 0

                state.copy(
                    data = currentData.copy(
                        analyzeState = currentData.analyzeState.copy(
                            areAllFilesSelected = newState,
                            selectedFiles = updatedSet,
                            selectedFilesCount = selectedVisibleCount
                        )
                    )
                )
            }
        }
    }

    private fun setGlobalSelectAllWarningDialogVisibility(isVisible: Boolean) {
        _uiState.update { state: UiStateScreen<UiScannerModel> ->
            val currentData: UiScannerModel = state.data ?: UiScannerModel()
            state.copy(
                data = currentData.copy(
                    analyzeState = currentData.analyzeState.copy(
                        isGlobalSelectAllWarningDialogVisible = isVisible
                    )
                )
            )
        }
    }

    private fun onGlobalSelectAllClick() {
        launch(context = dispatchers.io) {
            // Only show the warning dialog when the user is selecting all items.
            val isSelectingAll = _uiState.value.data?.analyzeState?.areAllFilesSelected != true
            val showDialog = isSelectingAll && dataStore.showGlobalSelectAllWarning.first()
            if (showDialog) {
                setGlobalSelectAllWarningDialogVisibility(true)
            } else {
                toggleSelectAllFiles()
            }
        }
    }

    private fun confirmGlobalSelectAll(dontShowAgain: Boolean) {
        launch(context = dispatchers.io) {
            if (dontShowAgain) {
                dataStore.saveShowGlobalSelectAllWarning(false)
            }
            toggleSelectAllFiles()
            setGlobalSelectAllWarningDialogVisibility(false)
        }
    }

    fun toggleSelectFilesForCategory(category: String) {
        launch(context = dispatchers.default) {
            _uiState.update { currentState: UiStateScreen<UiScannerModel> ->
                val currentData: UiScannerModel = currentState.data ?: UiScannerModel()
                val filesInCategory: List<FileEntry> =
                    currentData.analyzeState.groupedFiles[category] ?: emptyList()
                if (filesInCategory.isEmpty()) return@update currentState
                val duplicateOriginals =
                    currentData.analyzeState.duplicateOriginals.map { it.path }.toSet()
                val currentSelection: Set<String> = currentData.analyzeState.selectedFiles
                val allSelected: Boolean =
                    filesInCategory.filterNot { it.path in duplicateOriginals }
                        .all { it.path in currentSelection }
                val updatedSelection: MutableSet<String> = currentSelection.toMutableSet().apply {
                    filesInCategory.forEach { file: FileEntry ->
                        val path = file.path
                        if (path in duplicateOriginals) {
                            if (allSelected) remove(path)
                        } else {
                            if (allSelected) remove(path) else add(path)
                        }
                    }
                }

                val visibleFiles: List<FileEntry> =
                    currentData.analyzeState.groupedFiles.values.flatten()
                val visiblePaths = visibleFiles.map { it.path }
                val selectedVisibleCount: Int =
                    updatedSelection.count { it in visiblePaths && !File(it).isProtectedAndroidDir() }

                currentState.copy(
                    data = currentData.copy(
                        analyzeState = currentData.analyzeState.copy(
                            selectedFiles = updatedSelection,
                            selectedFilesCount = selectedVisibleCount,
                            areAllFilesSelected = selectedVisibleCount == visibleFiles.size && visibleFiles.isNotEmpty()
                        )
                    )
                )
            }
        }
    }

    fun toggleSelectFilesForDate(files: List<File>, isChecked: Boolean) {
        launch(context = dispatchers.default) {
            _uiState.update { currentState: UiStateScreen<UiScannerModel> ->
                val currentData = currentState.data ?: UiScannerModel()
                val duplicateOriginals =
                    currentData.analyzeState.duplicateOriginals.map { it.path }.toSet()
                val updatedSelectionSet =
                    currentData.analyzeState.selectedFiles.toMutableSet().apply {
                        files.forEach { file ->
                            val path = file.absolutePath
                            if (isChecked) {
                                if (path in duplicateOriginals && path !in this) {
                                    // keep unchecked
                                } else {
                                    add(path)
                                }
                            } else {
                                remove(path)
                            }
                        }
                    }

                val visibleFiles = currentData.analyzeState.groupedFiles.values.flatten()
                val visiblePaths = visibleFiles.map { it.path }
                val selectedVisibleCount =
                    updatedSelectionSet.count { it in visiblePaths && !File(it).isProtectedAndroidDir() }

                currentState.copy(
                    data = currentData.copy(
                        analyzeState = currentData.analyzeState.copy(
                            selectedFiles = updatedSelectionSet,
                            selectedFilesCount = selectedVisibleCount,
                            areAllFilesSelected = selectedVisibleCount == visibleFiles.size && visibleFiles.isNotEmpty()
                        )
                    )
                )
            }
        }
    }



    private fun observeCleaningWork(id: UUID) {
        activeCleanWorkObserver = observeFileCleanWork(
            previousObserver = activeCleanWorkObserver,
            application = application,
            dispatcher = dispatchers.io,
            workId = id,
            clearWorkId = { dataStore.clearScannerCleanWorkId() },
            onRunning = {
                _uiState.update { state ->
                    val current = state.data ?: UiScannerModel()
                    state.copy(
                        data = current.copy(
                            analyzeState = current.analyzeState.copy(
                                state = CleaningState.Cleaning
                            )
                        )
                    )
                }
            },
            onSuccess = { info ->
                _uiState.update { state ->
                    val current = state.data ?: UiScannerModel()
                    state.copy(
                        data = current.copy(
                            analyzeState = current.analyzeState.copy(
                                state = CleaningState.Result
                            )
                        )
                    )
                }

                _cleaningApks.value = false

                val failedPaths =
                    info.outputData.getStringArray(FileCleanupWorker.KEY_FAILED_PATHS)
                val failedCount = failedPaths?.size ?: 0
                val selectedCount =
                    _uiState.value.data?.analyzeState?.selectedFiles
                        ?.count { !File(it).isProtectedAndroidDir() } ?: 0
                val successCount = selectedCount - failedCount

                val message = if (failedCount > 0) {
                    UiTextHelper.StringResource(
                        R.string.cleanup_partial,
                        listOf(successCount, failedCount)
                    )
                } else {
                    UiTextHelper.StringResource(R.string.all_clean)
                }
                postSnackbar(message, false)
                if (pendingEmptyFoldersCleanup) {
                    _emptyFolders.value = emptyList()
                    dataStore.saveEmptyFoldersHideUntil(
                        System.currentTimeMillis() + EMPTY_FOLDERS_HIDE_DURATION_MS
                    )
                    pendingEmptyFoldersCleanup = false
                }
                dataStore.saveLastScanTimestamp(System.currentTimeMillis())

                onEvent(ScannerEvent.RefreshData)
                onCloseAnalyzeComposable()
            },
            onFailed = {
                cleanOperationHandler.onCleaningFailed()
                _cleaningApks.value = false
                pendingEmptyFoldersCleanup = false
            },
            onCancelled = {
                _cleaningApks.value = false
                pendingEmptyFoldersCleanup = false
                _uiState.update { state ->
                    val current = state.data ?: UiScannerModel()
                    state.copy(
                        data = current.copy(
                            analyzeState = current.analyzeState.copy(
                                state = CleaningState.Idle
                            )
                        )
                    )
                }
            },
            onProgress = { info ->
                val processed = info.progress.getInt(FileCleanupWorker.KEY_PROGRESS_CURRENT, 0)
                val total = info.progress.getInt(FileCleanupWorker.KEY_PROGRESS_TOTAL, 0)
                _uiState.update { state ->
                    val current = state.data ?: UiScannerModel()
                    state.copy(
                        data = current.copy(
                            analyzeState = current.analyzeState.copy(
                                cleanedFilesCount = processed,
                                totalFilesToClean = total
                            )
                        )
                    )
                }
            }
        )
    }

    fun moveSelectedToTrash() {
        if (_uiState.value.data?.analyzeState?.state != CleaningState.ReadyToClean) {
            return
        }
        val currentScreenData = screenData ?: run {
            postSnackbar(
                message = UiTextHelper.StringResource(R.string.data_not_available),
                isError = true
            )
            return
        }
        val selectedEntries = currentScreenData.analyzeState.scannedFileList.filter {
            it.path in currentScreenData.analyzeState.selectedFiles
        }
        cleanOperationHandler.moveToTrash(selectedEntries)
    }

    private fun updateTrashSize(sizeChange: Long) {
        launch(dispatchers.io) {
            val result = cleaningManager.updateTrashSize(sizeChange)
            _uiState.applyResult(
                result = result,
                errorMessage = UiTextHelper.StringResource(R.string.failed_to_update_trash_size)
            ) { data, currentData ->
                currentData
            }
        }
    }

    private fun loadCleanedSpace() {
        launch(dispatchers.io) {
            dataStore.cleanedSpace.collect { cleanedSpace ->
                _uiState.successData {
                    copy(
                        storageInfo = storageInfo.copy(
                            cleanedSpace = FileSizeFormatter.format(cleanedSpace)
                        )
                    )
                }
            }
        }
    }

    private fun loadWhatsAppMedia() {
        _whatsAppMediaLoaded.value = false
        launch(context = dispatchers.io) {
            val (images, videos, docs) = getWhatsAppMediaSummary()
            _whatsAppMediaSummary.value = WhatsAppMediaSummary(
                images = images,
                videos = videos,
                documents = docs
            )
            _whatsAppMediaLoaded.value = true
        }
    }

    private fun checkWhatsAppInstalled() {
        launch(context = dispatchers.io) {
            val installed = runCatching {
                val uri = "content://com.whatsapp.provider.media".toUri()
                application.contentResolver.getType(uri)
                true
            }.getOrElse { false }
            _isWhatsAppInstalled.value = installed
        }
    }

    private fun loadPromotedApp() {
        launch(dispatchers.io) {
            val promoted = getPromotedAppUseCase(application.packageName)
            promoted?.let {
                _uiState.updateData(ScreenState.Success()) { current ->
                    current.copy(promotedApp = promoted)
                }
            }
        }
    }


    private fun loadLargestFilesPreview() {
        launch(dispatchers.io) {
            getLargestFilesUseCase(5).collectLatest { result ->
                if (result is DataState.Success) {
                    _largestFiles.value = result.data
                }
            }
        }
    }

    private fun loadEmptyFoldersPreview() {
        launch(dispatchers.io) {
            val folders = mutableListOf<File>()
            getEmptyFoldersUseCase().collect { result ->
                when (result) {
                    is DataState.Loading -> folders.clear()
                    is DataState.Success -> {
                        if (_emptyFoldersHideUntil.value <= System.currentTimeMillis()) {
                            folders.add(result.data)
                            _emptyFolders.value = folders.toList()
                        } else {
                            _emptyFolders.value = emptyList()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun resetAfterError() {
        cleanOperationHandler.resetAfterError()
    }

    private fun loadEmptyFoldersHideUntil() {
        launch(dispatchers.io) {
            dataStore.emptyFoldersHideUntil.collect { timestamp ->
                _emptyFoldersHideUntil.value = timestamp
            }
        }
    }

    private fun toggleAnalyzeScreen(visible: Boolean) {
        if (visible) {
            screenState.dismissSnackbar()
            launch(dispatchers.io) {
                val anyEnabled = listOf(
                    dataStore.genericFilter.first(),
                    dataStore.deleteEmptyFolders.first(),
                    dataStore.deleteArchives.first(),
                    dataStore.deleteInvalidMedia.first(),
                    dataStore.deleteCorpseFiles.first(),
                    dataStore.deleteApkFiles.first(),
                    dataStore.deleteAudioFiles.first(),
                    dataStore.deleteVideoFiles.first(),
                    dataStore.deleteWindowsFiles.first(),
                    dataStore.deleteOfficeFiles.first(),
                    dataStore.deleteFontFiles.first(),
                    dataStore.deleteOtherFiles.first(),
                    dataStore.deleteImageFiles.first(),
                    dataStore.deleteDuplicateFiles.first()
                ).any { it }

                if (!anyEnabled) {
                    postSnackbar(
                        message = UiTextHelper.StringResource(R.string.no_cleaning_options_selected),
                        isError = false
                    )
                    return@launch
                }

                _uiState.update { state: UiStateScreen<UiScannerModel> ->
                    val currentData: UiScannerModel = state.data ?: UiScannerModel()
                    state.copy(
                        data = currentData.copy(
                            analyzeState = currentData.analyzeState.copy(
                                isAnalyzeScreenVisible = true,
                                scannedFileList = emptyList(),
                                emptyFolderList = emptyList(),
                                groupedFiles = emptyMap(),
                                filesByDateForCategory = emptyMap(),
                                duplicateGroups = emptyList(),
                                duplicateGroupsByDate = emptyMap(),
                                selectedFiles = mutableSetOf(),
                                selectedFilesCount = 0,
                                areAllFilesSelected = false,
                                state = CleaningState.Idle,
                                cleaningType = CleaningType.NONE
                            )
                        )
                    )
                }
                cleanOperationHandler.analyzeFiles()
            }
        } else {
            _uiState.updateData(ScreenState.Success()) { currentData ->
                currentData.copy(
                    analyzeState = currentData.analyzeState.copy(
                        selectedFiles = mutableSetOf(),
                        selectedFilesCount = 0,
                        areAllFilesSelected = false,
                        duplicateGroups = emptyList(),
                        duplicateGroupsByDate = emptyMap(),
                        isAnalyzeScreenVisible = false,
                        state = CleaningState.Idle,
                        cleaningType = CleaningType.NONE
                    )
                )
            }
        }
    }

    fun setDeleteForeverConfirmationDialogVisibility(isVisible: Boolean) {
        _uiState.updateData(ScreenState.Success()) { currentData ->
            currentData.copy(
                analyzeState = currentData.analyzeState.copy(
                    isDeleteForeverConfirmationDialogVisible = isVisible
                )
            )
        }
    }

    fun setMoveToTrashConfirmationDialogVisibility(isVisible: Boolean) {
        _uiState.updateData(ScreenState.Success()) { currentData ->
            currentData.copy(
                analyzeState = currentData.analyzeState.copy(
                    isMoveToTrashConfirmationDialogVisible = isVisible
                )
            )
        }
    }

    fun onCleanApks(apkFiles: List<File>) {
        val (validFiles, invalidFiles) = apkFiles.partition { it.exists() && it.canWrite() }

        if (invalidFiles.isNotEmpty()) {
            postSnackbar(
                UiTextHelper.StringResource(R.string.cleanup_failed_details),
                isError = true
            )
        }

        val entries = validFiles
            .map { FileEntry(it.absolutePath, it.length(), it.lastModified()) }
            .toSet()
        if (entries.isNotEmpty()) {
            deleteFiles(entries, fromApkCleaner = true)
        }
    }

    fun onCleanEmptyFolders(folders: List<File>) {
        val entries =
            folders.map { FileEntry(it.absolutePath, 0, it.lastModified()) }.toSet()
        pendingEmptyFoldersCleanup = true
        deleteFiles(entries)
    }

    fun onCleanWhatsAppFiles() {
        _whatsAppMediaSummary.value = WhatsAppMediaSummary()
        CleaningEventBus.notifyCleaned(success = true)
        postSnackbar(UiTextHelper.StringResource(R.string.feature_not_available), isError = false)
    }

    fun onCleanCache() {
        postSnackbar(UiTextHelper.StringResource(R.string.feature_not_available), isError = false)
    }

    fun onClipboardClear() {
        clipboardHandler.onClipboardClear()
    }



    private fun postSnackbar(message: UiTextHelper, isError: Boolean) {
        screenState.showSnackbar(
            snackbar = UiSnackbar(
                message = message,
                isError = isError,
                timeStamp = System.currentTimeMillis(),
                type = ScreenMessageType.SNACKBAR
            )
        )
    }

    override fun onCleared() {
        clipboardHandler.unregister()
        super.onCleared()
    }
}
