package com.d4rk.cleaner.app.clean.whatsapp.summary.ui

import android.app.Application
import androidx.work.WorkManager
import com.d4rk.android.libs.apptoolkit.core.di.DispatcherProvider
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.ScreenState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiSnackbar
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.ui.base.ScreenViewModel
import com.d4rk.android.libs.apptoolkit.core.utils.helpers.UiTextHelper
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningState
import com.d4rk.cleaner.app.clean.scanner.work.FileCleanupWorker
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.actions.WhatsAppCleanerAction
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.actions.WhatsAppCleanerEvent
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.model.UiWhatsAppCleanerModel
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.usecases.GetWhatsAppMediaFilesUseCase
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.usecases.GetWhatsAppMediaSummaryUseCase
import com.d4rk.cleaner.app.clean.whatsapp.utils.constants.WhatsAppMediaConstants
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.d4rk.cleaner.core.utils.helpers.CleaningEventBus
import com.d4rk.cleaner.core.utils.helpers.FileSizeFormatter
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

class WhatsappCleanerSummaryViewModel(
    private val application: Application,
    private val getSummaryUseCase: GetWhatsAppMediaSummaryUseCase,
    private val getFilesUseCase: GetWhatsAppMediaFilesUseCase,
    private val dataStore: DataStore,
    private val dispatchers: DispatcherProvider,
    private val fileCleanWorkEnqueuer: FileCleanWorkEnqueuer
) : ScreenViewModel<UiWhatsAppCleanerModel, WhatsAppCleanerEvent, WhatsAppCleanerAction>(
    initialState = UiStateScreen(data = UiWhatsAppCleanerModel())
) {

    private var activeWorkObserver: Job? = null
    private var pendingDeleteSizes: Map<String, Long> = emptyMap()
    private val filesCache = object : LinkedHashMap<String, List<File>>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<File>>?): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }

    companion object {
        private const val MAX_CACHE_SIZE = 20
    }

    init {
        onEvent(WhatsAppCleanerEvent.LoadMedia)
        launch(dispatchers.io) {
            dataStore.whatsappCleanWorkId.first()?.let { id ->
                val uuid = UUID.fromString(id)
                val info = WorkManager.getInstance(application)
                    .getWorkInfoByIdFlow(uuid)
                    .first()
                if (info == null || info.state.isFinished) {
                    dataStore.clearWhatsAppCleanWorkId()
                } else {
                    observeWork(uuid)
                }
            }
        }
    }

    override fun onEvent(event: WhatsAppCleanerEvent) {
        when (event) {
            WhatsAppCleanerEvent.LoadMedia -> loadSummary()
            WhatsAppCleanerEvent.CleanAll -> cleanAll()
            is WhatsAppCleanerEvent.DeleteSelected -> deleteSelected(event.files)
        }
    }

    private fun loadSummary() {
        launch(context = dispatchers.io) {
            getSummaryUseCase().collectLatest { result ->
                when (result) {
                    is DataState.Loading -> _uiState.update { current ->
                        current.copy(
                            screenState = ScreenState.IsLoading(),
                            data = current.data?.copy(cleaningState = CleaningState.Analyzing)
                                ?: UiWhatsAppCleanerModel(cleaningState = CleaningState.Analyzing)
                        )
                    }

                    is DataState.Success -> {
                        _uiState.update { current ->
                            current.copy(
                                screenState = if (result.data.totalBytes != 0L) ScreenState.Success() else ScreenState.NoData(),
                                data = current.data?.copy(
                                    mediaSummary = result.data,
                                    totalSize = result.data.formattedTotalSize,
                                    cleaningState = CleaningState.Idle
                                ) ?: UiWhatsAppCleanerModel(
                                    mediaSummary = result.data,
                                    totalSize = result.data.formattedTotalSize,
                                    cleaningState = CleaningState.Idle
                                )
                            )
                        }
                        filesCache.clear()
                        populateCache()
                    }

                    is DataState.Error -> _uiState.update { current ->
                        current.copy(
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

    private suspend fun populateCache() {
        val types = WhatsAppMediaConstants.DIRECTORIES.keys
        for (type in types) {
            getFilesUseCase(type, 0, Int.MAX_VALUE).collectLatest { res ->
                if (res is DataState.Success) {
                    filesCache[type] = res.data
                }
            }
        }
    }

    private fun cleanAll() {
        launch(context = dispatchers.io) {
            val types = WhatsAppMediaConstants.DIRECTORIES.keys
            val files = mutableListOf<File>()
            for (type in types) {
                val cached = filesCache[type]
                if (cached != null) {
                    files.addAll(cached)
                } else {
                    getFilesUseCase(type, 0, Int.MAX_VALUE).collectLatest { res ->
                        if (res is DataState.Success) {
                            files.addAll(res.data)
                            filesCache[type] = res.data
                        }
                    }
                }
            }

            if (files.isEmpty()) return@launch

            val accessible = files.filterNot { it.isProtectedAndroidDir() }
            if (accessible.isEmpty()) {
                sendAction(
                    WhatsAppCleanerAction.ShowSnackbar(
                        UiSnackbar(message = UiTextHelper.StringResource(R.string.protected_android_folder))
                    )
                )
                return@launch
            }

            pendingDeleteSizes = accessible.associate { it.absolutePath to it.length() }

            FileCleaner.enqueue(
                enqueuer = fileCleanWorkEnqueuer,
                paths = accessible.map { it.absolutePath },
                action = FileCleanupWorker.ACTION_DELETE,
                getWorkId = { dataStore.whatsappCleanWorkId.first() },
                saveWorkId = { dataStore.saveWhatsAppCleanWorkId(it) },
                clearWorkId = { dataStore.clearWhatsAppCleanWorkId() },
                showSnackbar = { sendAction(WhatsAppCleanerAction.ShowSnackbar(it)) },
                onEnqueued = { id -> observeWork(id) },
                onError = {
                    _uiState.update { state ->
                        state.copy(data = state.data?.copy(cleaningState = CleaningState.Error))
                    }
                }
            )
        }
    }

    private fun deleteSelected(files: List<File>) {
        if (files.isEmpty()) return
        launch(context = dispatchers.io) {
            val accessible = files.filterNot { it.isProtectedAndroidDir() }
            if (accessible.isEmpty()) {
                sendAction(
                    WhatsAppCleanerAction.ShowSnackbar(
                        UiSnackbar(message = UiTextHelper.StringResource(R.string.protected_android_folder))
                    )
                )
                return@launch
            }

            pendingDeleteSizes = accessible.associate { it.absolutePath to it.length() }

            FileCleaner.enqueue(
                enqueuer = fileCleanWorkEnqueuer,
                paths = accessible.map { it.absolutePath },
                action = FileCleanupWorker.ACTION_DELETE,
                getWorkId = { dataStore.whatsappCleanWorkId.first() },
                saveWorkId = { dataStore.saveWhatsAppCleanWorkId(it) },
                clearWorkId = { dataStore.clearWhatsAppCleanWorkId() },
                showSnackbar = { sendAction(WhatsAppCleanerAction.ShowSnackbar(it)) },
                onEnqueued = { id -> observeWork(id) },
                onError = {
                    _uiState.update { state ->
                        state.copy(data = state.data?.copy(cleaningState = CleaningState.Error))
                    }
                },
            )
        }
    }

    private fun observeWork(id: UUID) {
        activeWorkObserver = observeFileCleanWork(
            previousObserver = activeWorkObserver,
            application = application,
            dispatcher = dispatchers.io,
            workId = id,
            clearWorkId = { dataStore.clearWhatsAppCleanWorkId() },
            onRunning = {
                _uiState.update {
                    it.copy(
                        screenState = ScreenState.IsLoading(),
                        data = it.data?.copy(cleaningState = CleaningState.Cleaning)
                    )
                }
            },
            onSuccess = { info ->
                val failedPaths =
                    info.outputData.getStringArray(FileCleanupWorker.KEY_FAILED_PATHS)
                        ?.toSet() ?: emptySet()
                val failedCount = failedPaths.size
                val successSize = pendingDeleteSizes
                    .filterKeys { it !in failedPaths }
                    .values
                    .sum()
                pendingDeleteSizes = emptyMap()

                onEvent(WhatsAppCleanerEvent.LoadMedia)

                val message = buildString {
                    append("Cleaned ${FileSizeFormatter.format(successSize)}")
                    if (failedCount > 0) append(", $failedCount failed")
                }

                _uiState.update { current ->
                    current.copy(
                        data = current.data?.copy(cleaningState = CleaningState.Result)
                    )
                }

                sendAction(
                    WhatsAppCleanerAction.ShowSnackbar(
                        UiSnackbar(message = UiTextHelper.DynamicString(message))
                    )
                )
                CleaningEventBus.notifyCleaned(success = failedCount == 0)
            },
            onFailed = {
                pendingDeleteSizes = emptyMap()
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
                CleaningEventBus.notifyCleaned(success = false)
            },
            onCancelled = {
                pendingDeleteSizes = emptyMap()
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
