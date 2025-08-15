package com.d4rk.cleaner.app.clean.scanner.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ContentPasteOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.d4rk.android.libs.apptoolkit.core.ui.components.spacers.SmallVerticalSpacer
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.dashboard.ui.components.DashboardActionCard

@Composable
fun ClipboardCleanerCard(
    clipboardText: String?,
    modifier: Modifier = Modifier,
    onCleanClick: () -> Unit,
) {
    DashboardActionCard(
        modifier = modifier,
        icon = Icons.Outlined.ContentPaste,
        title = stringResource(id = R.string.clipboard_card_title),
        subtitle = stringResource(id = R.string.clipboard_card_subtitle),
        actionLabel = stringResource(id = R.string.clean_clipboard),
        actionIcon = Icons.Outlined.ContentPasteOff,
        onActionClick = onCleanClick
    ) {
        clipboardText?.let { text ->
            SmallVerticalSpacer()
            Text(
                text = stringResource(id = R.string.clipboard_current_format, text),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            )
        }
    }
}
