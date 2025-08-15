package com.d4rk.cleaner.app.clean.scanner.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.d4rk.android.libs.apptoolkit.core.ui.components.spacers.SmallVerticalSpacer
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.dashboard.ui.components.DashboardActionCard
import java.io.File

@Composable
fun EmptyFolderCleanerCard(
    folders: List<File>,
    modifier: Modifier = Modifier,
    onCleanClick: (List<File>) -> Unit,
) {
    DashboardActionCard(
        modifier = modifier,
        icon = Icons.Outlined.Folder,
        title = stringResource(id = R.string.empty_folder_card_title),
        subtitle = stringResource(id = R.string.empty_folder_card_subtitle),
        actionLabel = stringResource(id = R.string.clean_empty_folders),
        actionIcon = Icons.Outlined.DeleteSweep,
        onActionClick = { onCleanClick(folders) },
        onHeaderClick = { onCleanClick(folders) }
    ) {
        SmallVerticalSpacer()
        Text(
            text = stringResource(id = R.string.empty_folders_found_format, folders.size),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.animateContentSize()
        )
    }
}
