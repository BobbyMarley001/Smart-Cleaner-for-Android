package com.d4rk.cleaner.core.domain.repository

import java.io.File

interface FileDeletionRepository<T> {
    suspend fun deleteFiles(files: Collection<File>): T
    suspend fun moveToTrash(files: Collection<File>): T {
        throw UnsupportedOperationException("Move to trash not supported")
    }
}
