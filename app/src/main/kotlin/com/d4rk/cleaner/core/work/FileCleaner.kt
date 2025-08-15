package com.d4rk.cleaner.core.work

import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiSnackbar
import com.d4rk.android.libs.apptoolkit.core.utils.helpers.UiTextHelper
import com.d4rk.cleaner.R
import java.util.UUID

/**
 * Helper to enqueue file cleaning work while handling common UI feedback.
 */
object FileCleaner {
    /**
     * Wraps [FileCleanWorkEnqueuer.enqueue] to emit standardized UI feedback and callbacks.
     */
    suspend fun enqueue(
        enqueuer: FileCleanWorkEnqueuer,
        paths: Collection<String>,
        action: String,
        getWorkId: suspend () -> String?,
        saveWorkId: suspend (String) -> Unit,
        clearWorkId: suspend () -> Unit,
        showSnackbar: (UiSnackbar) -> Unit,
        onEnqueued: (UUID) -> Unit = {},
        onError: () -> Unit = {},
        inProgressMessage: UiTextHelper = UiTextHelper.StringResource(R.string.cleaning_in_progress),
        errorMessage: UiTextHelper = UiTextHelper.StringResource(R.string.failed_to_delete_files)
    ) {
        when (
            val result = enqueuer.enqueue(
                paths = paths,
                action = action,
                getWorkId = getWorkId,
                saveWorkId = saveWorkId,
                clearWorkId = clearWorkId
            )
        ) {
            FileCleanWorkEnqueuer.Result.AlreadyRunning -> {
                showSnackbar(
                    UiSnackbar(
                        message = inProgressMessage,
                        isError = false,
                    ),
                )
            }
            is FileCleanWorkEnqueuer.Result.Enqueued -> {
                onEnqueued(result.id)
                showSnackbar(
                    UiSnackbar(
                        message = inProgressMessage,
                        isError = false,
                    ),
                )
            }
            is FileCleanWorkEnqueuer.Result.Error -> {
                onError()
                showSnackbar(
                    UiSnackbar(
                        message = errorMessage,
                        isError = true
                    )
                )
            }
        }
    }
}

