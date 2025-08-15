package com.d4rk.cleaner.app.clean.memory.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.SnippetFolder
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.core.ui.components.GridCardItem
import com.d4rk.cleaner.app.core.ui.components.GridCardModel
import com.d4rk.cleaner.app.core.ui.components.GroupedGridLayout
import com.d4rk.cleaner.app.core.ui.theme.GroupedGridStyle
import com.d4rk.cleaner.core.utils.helpers.FileSizeFormatter.format as formatSize

@Composable
fun StorageBreakdownGrid(
    storageBreakdown: Map<String, Long>,
    onItemClick: (String) -> Unit = {},
) {
    val storageIcons: Map<String, ImageVector> = mapOf(
        stringResource(id = R.string.installed_apps) to Icons.Outlined.Apps,
        stringResource(id = R.string.system) to Icons.Outlined.Android,
        stringResource(id = R.string.music) to Icons.Outlined.MusicNote,
        stringResource(id = R.string.images) to Icons.Outlined.Image,
        stringResource(id = R.string.documents) to Icons.Outlined.FolderOpen,
        stringResource(id = R.string.downloads) to Icons.Outlined.Download,
        stringResource(id = R.string.other_files) to Icons.Outlined.FolderOpen,
    )

    GroupedGridLayout(items = storageBreakdown.entries.toList()) { (label, bytes) ->
        val model = object : GridCardModel {
            override val title = label
            override val subtitle = formatSize(bytes)
            override val iconVector = storageIcons[label] ?: Icons.Outlined.SnippetFolder
            override val iconPainter = null
        }

        GridCardItem(
            model = model,
            iconContainerColor = GroupedGridStyle.iconContainerColor,
            onClick = { onItemClick(label) },
            iconShape = MaterialTheme.shapes.medium,
        )
    }
}
