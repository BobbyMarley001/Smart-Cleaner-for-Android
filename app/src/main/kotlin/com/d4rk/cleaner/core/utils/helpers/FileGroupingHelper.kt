package com.d4rk.cleaner.core.utils.helpers

import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileGroupingHelper {

    fun groupFilesByDate(files: List<File>): Map<String, List<File>> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return files.groupBy { file ->
            dateFormat.format(Date(file.lastModified()))
        }
    }

    fun groupFileEntriesByDate(files: List<FileEntry>): Map<String, List<File>> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return files.groupBy { entry ->
            dateFormat.format(Date(entry.modified))
        }.mapValues { (_, entries) ->
            entries.map { File(it.path) }
        }
    }

    fun groupDuplicateGroupsByDate(groups: List<List<FileEntry>>): Map<String, List<List<File>>> {
        return groups.groupBy { group ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val first = group.firstOrNull()
            dateFormat.format(Date(first?.modified ?: 0L))
        }.mapValues { (_, groupList) ->
            groupList.map { list -> list.map { File(it.path) } }
        }
    }
}

