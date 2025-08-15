package com.d4rk.cleaner.app.clean.scanner.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.dashboard.ui.components.DashboardActionCard

@Composable
fun SystemStorageManagerCard(modifier: Modifier = Modifier, onOpen: () -> Unit) {
    DashboardActionCard(
        modifier = modifier,
        icon = Icons.Outlined.Storage,
        title = stringResource(id = R.string.storage_manager_card_title),
        subtitle = stringResource(id = R.string.storage_manager_card_subtitle),
        actionLabel = stringResource(id = R.string.open_storage_manager),
        actionIcon = Icons.AutoMirrored.Outlined.Launch,
        onActionClick = onOpen,
        onHeaderClick = onOpen
    )
}
