package com.d4rk.cleaner.app.clean.scanner.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.dashboard.ui.components.DashboardActionCard

@Composable
fun CacheCleanerCard(
    modifier: Modifier = Modifier,
    onScanClick: () -> Unit,
) {
    DashboardActionCard(
        modifier = modifier,
        icon = Icons.Outlined.Cached,
        title = stringResource(id = R.string.cache_cleaner_card_title),
        subtitle = stringResource(id = R.string.cache_cleaner_card_subtitle),
        actionLabel = stringResource(id = R.string.scan_cache),
        actionPainter = painterResource(id = R.drawable.ic_folder_search),
        onActionClick = onScanClick,
        onHeaderClick = onScanClick,
        badgeText = "W.I.P"
    )
}
