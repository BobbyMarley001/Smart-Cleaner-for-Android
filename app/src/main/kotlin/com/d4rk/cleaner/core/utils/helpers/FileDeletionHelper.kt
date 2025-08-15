package com.d4rk.cleaner.core.utils.helpers

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import java.io.File

/**
 * Deletes files using native [File.deleteRecursively].
 * Returns [FileDeletionResult] for each input file describing success or failure.
 */
data class FileDeletionResult(val file: File, val success: Boolean)

object FileDeletionHelper {
    fun deleteFiles(files: Collection<File>, contentResolver: ContentResolver): List<FileDeletionResult> {
        val results = mutableListOf<FileDeletionResult>()

        files.forEach { file ->
            val success = runCatching {
                if (!file.exists()) {
                    false
                } else if (file.deleteRecursively()) {
                    true
                } else {
                    val uri = resolveMediaStoreUri(file, contentResolver)
                    uri != null && contentResolver.delete(uri, null, null) > 0
                }
            }.getOrDefault(false)
            results.add(FileDeletionResult(file, success))
        }

        return results
    }

    private fun resolveMediaStoreUri(file: File, contentResolver: ContentResolver) = run {
        val volume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.VOLUME_EXTERNAL
        } else {
            "external"
        }
        val collection = MediaStore.Files.getContentUri(volume)
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = MediaStore.Files.FileColumns.DATA + "=?"
        val selectionArgs = arrayOf(file.absolutePath)
        contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val id = cursor.getLong(idColumn)
                ContentUris.withAppendedId(collection, id)
            } else {
                null
            }
        }
    }
}

