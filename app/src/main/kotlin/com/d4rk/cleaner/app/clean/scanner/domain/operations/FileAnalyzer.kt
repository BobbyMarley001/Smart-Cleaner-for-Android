package com.d4rk.cleaner.app.clean.scanner.domain.operations

import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileEntry
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileTypesData
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.GetDuplicatesUseCase
import com.d4rk.cleaner.app.settings.cleaning.utils.constants.ExtensionsConstants
import java.io.File

/**
 * Provides functions used during scanning to analyze files on disk.
 */
class FileAnalyzer(
    private val getDuplicatesUseCase: GetDuplicatesUseCase
) {
    fun computeGroupedFiles(
        scannedFiles: List<File>,
        emptyFolders: List<File>,
        fileTypesData: FileTypesData,
        preferences: Map<String, Boolean>,
        includeDuplicates: Boolean
    ): Triple<Map<String, List<FileEntry>>, Set<FileEntry>, List<List<FileEntry>>> {
        val knownExtensions: Set<String> = (
            fileTypesData.imageExtensions + fileTypesData.videoExtensions +
                fileTypesData.audioExtensions + fileTypesData.officeExtensions +
                fileTypesData.archiveExtensions + fileTypesData.apkExtensions +
                fileTypesData.fontExtensions + fileTypesData.windowsExtensions
            ).toSet()

        val baseDefaultTitles = listOf(
            "Images",
            "Videos",
            "Audios",
            "Documents",
            "Archives",
            "APKs",
            "Fonts",
            "Windows Files",
            "Empty Folders",
            "Other Files"
        )

        val baseFinalTitles = baseDefaultTitles.mapIndexed { index, fallback ->
            fileTypesData.fileTypesTitles.getOrElse(index) { fallback }
        }

        val duplicatesTitle = if (includeDuplicates) {
            fileTypesData.fileTypesTitles.getOrElse(10) { "Duplicates" }
        } else null

        val filesMap: LinkedHashMap<String, MutableList<FileEntry>> = linkedMapOf()
        filesMap.putAll(baseFinalTitles.associateWith { mutableListOf() })
        duplicatesTitle?.let { filesMap[it] = mutableListOf() }

        val duplicateGroups: List<List<FileEntry>> = if (includeDuplicates) {
            getDuplicatesUseCase(scannedFiles)
        } else emptyList()
        val duplicateFiles: Set<String> = if (includeDuplicates) {
            duplicateGroups.flatten().map { it.path }.toSet()
        } else emptySet()
        val duplicateOriginals: Set<FileEntry> = if (includeDuplicates) {
            duplicateGroups.mapNotNull { group -> group.minByOrNull { it.modified } }.toSet()
        } else emptySet()

        scannedFiles.forEach { file ->
            val entry = FileEntry(
                path = file.absolutePath,
                size = if (file.isDirectory) 0 else file.length(),
                modified = file.lastModified()
            )
            if (includeDuplicates && entry.path in duplicateFiles) {
                duplicatesTitle?.let { title -> filesMap.getOrPut(title) { mutableListOf() }.add(entry) }
            } else {
                val category = when (val extension = file.extension.lowercase()) {
                    in fileTypesData.imageExtensions -> if (preferences[ExtensionsConstants.IMAGE_EXTENSIONS] == true) baseFinalTitles[0] else null
                    in fileTypesData.videoExtensions -> if (preferences[ExtensionsConstants.VIDEO_EXTENSIONS] == true) baseFinalTitles[1] else null
                    in fileTypesData.audioExtensions -> if (preferences[ExtensionsConstants.AUDIO_EXTENSIONS] == true) baseFinalTitles[2] else null
                    in fileTypesData.officeExtensions -> if (preferences[ExtensionsConstants.OFFICE_EXTENSIONS] == true) baseFinalTitles[3] else null
                    in fileTypesData.archiveExtensions -> if (preferences[ExtensionsConstants.ARCHIVE_EXTENSIONS] == true) baseFinalTitles[4] else null
                    in fileTypesData.apkExtensions -> if (preferences[ExtensionsConstants.APK_EXTENSIONS] == true) baseFinalTitles[5] else null
                    in fileTypesData.fontExtensions -> if (preferences[ExtensionsConstants.FONT_EXTENSIONS] == true) baseFinalTitles[6] else null
                    in fileTypesData.windowsExtensions -> if (preferences[ExtensionsConstants.WINDOWS_EXTENSIONS] == true) baseFinalTitles[7] else null
                    else -> if (!knownExtensions.contains(extension) && preferences[ExtensionsConstants.OTHER_EXTENSIONS] == true) baseFinalTitles[9] else null
                }
                category?.let { filesMap[it]?.add(entry) }
            }
        }

        val emptyFoldersTitle = baseFinalTitles[8]
        if (preferences[ExtensionsConstants.EMPTY_FOLDERS] == true && emptyFolders.isNotEmpty()) {
            filesMap[emptyFoldersTitle] = emptyFolders
                .map { FileEntry(it.absolutePath, 0, it.lastModified()) }
                .toMutableList()
        }

        val filteredMap = filesMap.filterValues { it.isNotEmpty() }

        return Triple(filteredMap, duplicateOriginals, duplicateGroups)
    }

}
