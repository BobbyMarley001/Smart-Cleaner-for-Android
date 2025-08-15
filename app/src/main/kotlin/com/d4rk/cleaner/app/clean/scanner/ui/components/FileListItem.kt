package com.d4rk.cleaner.app.clean.scanner.ui.components

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.app.clean.scanner.utils.helpers.FilePreviewHelper
import java.io.File

@Composable
fun FileListItem(file: File, modifier: Modifier = Modifier) {
    val context: Context = LocalContext.current
    val size = remember(file.length()) { Formatter.formatShortFileSize(context, file.length()) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(SizeConstants.SmallSize),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilePreviewHelper.Preview(file = file, modifier = Modifier.size(40.dp))
        Column(modifier = Modifier
            .padding(start = 8.dp)
            .weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = size, style = MaterialTheme.typography.bodySmall)
        }
    }
}
