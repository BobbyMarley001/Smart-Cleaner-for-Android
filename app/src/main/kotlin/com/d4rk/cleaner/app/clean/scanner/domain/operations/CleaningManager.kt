package com.d4rk.cleaner.app.clean.scanner.domain.operations

import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.DeleteFilesUseCase
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.UpdateTrashSizeUseCase
import com.d4rk.cleaner.app.clean.scanner.domain.`interface`.ScannerRepositoryInterface
import com.d4rk.cleaner.core.domain.model.network.Errors
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Coordinates cleaning related operations such as deleting files or
 * moving them to trash. This keeps the ViewModel focused on UI logic.
 */
class CleaningManager(
    private val repository: ScannerRepositoryInterface,
    private val deleteFilesUseCase: DeleteFilesUseCase,
    private val updateTrashSizeUseCase: UpdateTrashSizeUseCase,
) {
    suspend fun deleteFiles(
        files: Collection<File>,
        mode: DeleteFilesUseCase.Mode = DeleteFilesUseCase.Mode.PERMANENT
    ): DataState<Unit, Errors> {
        return deleteFilesUseCase(repository, files, mode).first()
    }

    suspend fun updateTrashSize(
        sizeChange: Long
    ): DataState<Unit, com.d4rk.android.libs.apptoolkit.core.domain.model.network.Errors> {
        return updateTrashSizeUseCase(sizeChange).first()
    }
}
