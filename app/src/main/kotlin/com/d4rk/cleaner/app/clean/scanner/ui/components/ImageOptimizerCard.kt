package com.d4rk.cleaner.app.clean.scanner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.PhotoFilter
import androidx.compose.material.icons.outlined.PhotoSizeSelectLarge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.dashboard.ui.components.DashboardActionCard

@Composable
fun ImageOptimizerCard(
    modifier: Modifier = Modifier,
    onOptimizeClick: () -> Unit,
) {
    DashboardActionCard(
        modifier = modifier,
        icon = Icons.Outlined.Image,
        title = stringResource(id = R.string.image_optimizer_card_title),
        subtitle = stringResource(id = R.string.image_optimizer_card_subtitle),
        actionLabel = stringResource(id = R.string.optimize_image),
        actionIcon = Icons.Outlined.ImageSearch,
        onActionClick = onOptimizeClick,
        onHeaderClick = onOptimizeClick
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoSizeSelectLarge,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Outlined.PhotoFilter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Icon(
                imageVector = Icons.Outlined.ImageSearch,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
