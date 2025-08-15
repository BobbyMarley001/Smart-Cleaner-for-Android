package com.d4rk.cleaner.app.clean.scanner.domain.usecases

import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.cleaner.core.domain.model.network.Errors
import com.d4rk.cleaner.core.domain.repository.FileDeletionRepository
import com.d4rk.cleaner.core.utils.extensions.toError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class DeleteFilesUseCase {
    enum class Mode { TRASH, PERMANENT }

    operator fun <T> invoke(
        repository: FileDeletionRepository<T>,
        files: Collection<File>,
        mode: Mode = Mode.PERMANENT,
        onSuccess: ((T) -> Unit)? = null,
        onError: ((Errors) -> Unit)? = null
    ): Flow<DataState<T, Errors>> = flow {
        runCatching {
            when (mode) {
                Mode.TRASH -> repository.moveToTrash(files)
                Mode.PERMANENT -> repository.deleteFiles(files)
            }
        }.onSuccess { result ->
            onSuccess?.invoke(result)
            emit(DataState.Success(result))
        }.onFailure { throwable ->
            if (throwable is CancellationException) throw throwable
            val error = throwable.toError()
            onError?.invoke(error)
            emit(DataState.Error(error = error))
        }
    }
}
