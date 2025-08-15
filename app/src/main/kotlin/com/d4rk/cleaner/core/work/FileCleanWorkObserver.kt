package com.d4rk.cleaner.core.work

import android.app.Application
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import java.util.UUID

/**
 * Observes a [WorkManager] file clean work and relays state changes through callbacks.
 *
 * Cancels any previously active observer before registering a new one to avoid
 * leaking collectors. Internally delegates to [WorkObserver.observe].
 */
fun observeFileCleanWork(
    previousObserver: Job?,
    application: Application,
    dispatcher: CoroutineDispatcher,
    workId: UUID,
    clearWorkId: suspend () -> Unit,
    onRunning: suspend () -> Unit = {},
    onSuccess: suspend (WorkInfo) -> Unit = {},
    onFailed: suspend () -> Unit = {},
    onCancelled: suspend () -> Unit = {},
    onProgress: suspend (WorkInfo) -> Unit = {},
): Job {
    previousObserver?.cancel()
    return WorkObserver.observe(
        scope = MainScope(),
        workManager = WorkManager.getInstance(application),
        workId = workId,
        dispatcher = dispatcher,
        clearWorkId = clearWorkId,
        onRunning = onRunning,
        onSuccess = onSuccess,
        onFailed = onFailed,
        onCancelled = onCancelled,
        onProgress = onProgress,
    )
}

