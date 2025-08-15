package com.d4rk.cleaner.core.work

import android.app.Application
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.d4rk.cleaner.app.clean.scanner.work.FileCleanupWorker
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Helper that chunks file paths, enqueues [FileCleanupWorker] jobs and persists the work ID.
 */
class FileCleanWorkEnqueuer(
    private val application: Application,
    private val workManager: WorkManager = WorkManager.getInstance(application)
) {

    sealed class Result {
        object AlreadyRunning : Result()
        data class Enqueued(val id: UUID) : Result()
        data class Error(val throwable: Throwable) : Result()
    }

    /**
     * Enqueue cleanup work for the given paths and action.
     */
    suspend fun enqueue(
        paths: Collection<String>,
        action: String,
        getWorkId: suspend () -> String?,
        saveWorkId: suspend (String) -> Unit,
        clearWorkId: suspend () -> Unit
    ): Result {
        val activeId = getWorkId()
        if (activeId != null) {
            val info = workManager.getWorkInfoByIdFlow(UUID.fromString(activeId)).first()
            if (info != null && !info.state.isFinished) {
                return Result.AlreadyRunning
            } else {
                clearWorkId()
            }
        }

        if (paths.isEmpty()) {
            return Result.Error(IllegalArgumentException("No paths provided"))
        }

        val chunks = paths.chunked(FileCleanupWorker.MAX_PATHS_PER_WORKER)
        var continuation: androidx.work.WorkContinuation? = null
        var lastRequest: OneTimeWorkRequest? = null
        for (chunk in chunks) {
            val request = OneTimeWorkRequestBuilder<FileCleanupWorker>()
                .setInputData(
                    workDataOf(
                        FileCleanupWorker.KEY_ACTION to action,
                        FileCleanupWorker.KEY_PATHS to chunk.toTypedArray()
                    )
                ).build()
            lastRequest = request
            continuation = continuation?.then(request) ?: workManager.beginWith(request)
        }

        val finalRequest = lastRequest ?: return Result.Error(IllegalStateException("No work created"))

        return try {
            continuation?.enqueue()
            saveWorkId(finalRequest.id.toString())
            Result.Enqueued(finalRequest.id)
        } catch (t: Throwable) {
            workManager.cancelWorkById(finalRequest.id)
            Result.Error(t)
        }
    }
}

