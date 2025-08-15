package com.d4rk.cleaner.app.apps.manager.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.d4rk.android.libs.apptoolkit.core.ui.components.buttons.IconButton
import com.d4rk.android.libs.apptoolkit.core.ui.components.dropdown.CommonDropdownMenuItem
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.apps.manager.domain.actions.AppManagerEvent
import com.d4rk.cleaner.app.apps.manager.domain.data.model.AppManagerItem
import com.d4rk.cleaner.app.apps.manager.ui.AppManagerViewModel
import com.d4rk.cleaner.app.clean.scanner.utils.helpers.FilePreviewHelper
import com.d4rk.cleaner.core.utils.helpers.FileSizeFormatter
import java.io.File

@Composable
fun ApkItem(apkPath: String, viewModel: AppManagerViewModel, modifier: Modifier) {
    val apkFile = File(apkPath)
    var showMenu: Boolean by remember { mutableStateOf(value = false) }

    OutlinedCard(modifier = modifier) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SizeConstants.LargeSize)
                .clip(RoundedCornerShape(SizeConstants.LargeSize)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilePreviewHelper.Preview(
                file = apkFile,
                modifier = Modifier.size(48.dp)
            )
            Column(
                modifier = Modifier
                    .padding(SizeConstants.LargeSize)
                    .weight(1f)
            ) {
                Text(
                    text = apkFile.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = FileSizeFormatter.format(apkFile.length()),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box {
                IconButton(
                    icon = Icons.Outlined.MoreVert,
                    onClick = {
                        showMenu = true
                    })

                DropdownMenu(expanded = showMenu, onDismissRequest = {
                    showMenu = false
                }) {
                    CommonDropdownMenuItem(
                        textResId = com.d4rk.android.libs.apptoolkit.R.string.share,
                        icon = Icons.Outlined.Share,
                        onClick = {
                            viewModel.onEvent(
                                AppManagerEvent.ShareItem(
                                    AppManagerItem.ApkFile(apkPath)
                                )
                            )
                        }
                    )

                    CommonDropdownMenuItem(
                        textResId = R.string.install,
                        icon = Icons.Outlined.InstallMobile,
                        onClick = {
                            viewModel.installApk(apkPath)
                        }
                    )
                }
            }
        }
    }
}