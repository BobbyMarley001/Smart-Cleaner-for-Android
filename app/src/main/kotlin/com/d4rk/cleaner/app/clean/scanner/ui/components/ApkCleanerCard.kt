package com.d4rk.cleaner.app.clean.scanner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d4rk.android.libs.apptoolkit.core.ui.components.spacers.SmallVerticalSpacer
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.apps.manager.domain.data.model.ApkInfo
import com.d4rk.cleaner.app.clean.dashboard.ui.components.DashboardActionCard
import com.d4rk.cleaner.app.clean.scanner.utils.helpers.FilePreviewHelper
import com.d4rk.cleaner.core.utils.helpers.FileSizeFormatter
import java.io.File

@Composable
fun ApkCleanerCard(
    apkFiles: List<ApkInfo>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onCleanClick: (List<ApkInfo>) -> Unit
) {
    val preview = apkFiles.take(4)

    DashboardActionCard(
        modifier = modifier,
        icon = Icons.Outlined.Android,
        title = stringResource(id = R.string.apk_card_title),
        subtitle = stringResource(id = R.string.apk_card_subtitle),
        actionLabel = stringResource(id = R.string.clean_apks),
        actionIcon = Icons.Outlined.DeleteSweep,
        onActionClick = { onCleanClick(apkFiles) },
        actionEnabled = !isLoading
    ) {
        SmallVerticalSpacer()
        AnimatedVisibility(visible = preview.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .animateContentSize(),
                horizontalArrangement = Arrangement.spacedBy(SizeConstants.SmallSize),
                verticalAlignment = Alignment.CenterVertically
            ) {
                preview.forEach { apk ->
                    val apkFile = File(apk.path)
                    ApkPreviewItem(
                        file = apkFile,
                        name = apkFile.name,
                        size = FileSizeFormatter.format(apk.size)
                    )
                }
                if (apkFiles.size > preview.size) {
                    Text(
                        text = pluralStringResource(
                            id = R.plurals.apk_card_more_format,
                            count = apkFiles.size - preview.size,
                            apkFiles.size - preview.size
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = SizeConstants.MediumSize)
                    .size(SizeConstants.ButtonIconSize)
            )
        }
    }
}

@Composable
private fun ApkPreviewItem(file: File, name: String, size: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        FilePreviewHelper.Preview(
            file = file,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = size,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
