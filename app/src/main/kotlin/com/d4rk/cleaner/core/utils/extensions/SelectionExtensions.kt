package com.d4rk.cleaner.core.utils.extensions

import java.io.File
import com.d4rk.cleaner.core.utils.helpers.isProtectedAndroidDir

/**
 * Converts a map of file path selections into a set of existing [File]s.
 *
 * Only entries with a `true` value are considered. Null maps or paths
 * pointing to non-existent files are ignored.
 *
 * Example:
 * ```
 * val files: Set<File> = state.fileSelectionStates.selectedFiles()
 * ```
 */
fun Map<String, Boolean>?.selectedFiles(): Set<File> {
    return this?.mapNotNull { (path, isSelected) ->
        if (!isSelected || path.isBlank()) return@mapNotNull null
        File(path).takeIf { it.exists() && !it.isProtectedAndroidDir() }
    }?.toSet() ?: emptySet()
}

