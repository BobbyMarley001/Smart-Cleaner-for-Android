package com.d4rk.cleaner.app.clean.scanner.domain.`interface`

import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileTypesData
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import com.d4rk.cleaner.core.domain.repository.FileDeletionRepository
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ScannerRepositoryInterface : FileDeletionRepository<Unit> {
    suspend fun getStorageInfo(): UiScannerModel
    suspend fun getFileTypes(): FileTypesData
    fun getAllFiles(onLockedDir: ((File) -> Unit)? = null): Flow<File>
    fun getEmptyFolders(): Flow<File>
    suspend fun getTrashFiles(): List<File>
    suspend fun getLargestFiles(limit: Int): List<File>
    suspend fun restoreFromTrash(filesToRestore: Set<File>)
    suspend fun addTrashSize(size: Long)
    suspend fun subtractTrashSize(size: Long)
}