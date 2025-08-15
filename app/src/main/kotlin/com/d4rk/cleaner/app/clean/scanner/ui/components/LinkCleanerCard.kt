package com.d4rk.cleaner.app.clean.scanner.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.d4rk.android.libs.apptoolkit.core.ui.components.buttons.IconButton
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.dashboard.ui.components.DashboardActionCard
import com.d4rk.cleaner.app.clean.link.ui.LinkCleanerActivity
import com.d4rk.cleaner.core.utils.extensions.isValidUrl
import kotlinx.coroutines.launch

@Composable
fun LinkCleanerCard(
    modifier: Modifier = Modifier,
) {
    var linkText by rememberSaveable { mutableStateOf("") }
    val clipboardManager = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val openLinkCleaner = {
        val intent = Intent(context, LinkCleanerActivity::class.java).apply {
            putExtra(Intent.EXTRA_TEXT, linkText)
        }
        context.startActivity(intent)
    }

    DashboardActionCard(
        modifier = modifier,
        icon = Icons.Outlined.Link,
        title = stringResource(id = R.string.link_cleaner_card_title),
        subtitle = stringResource(id = R.string.link_cleaner_card_subtitle),
        actionLabel = stringResource(id = R.string.clean_link),
        actionIcon = Icons.Outlined.LinkOff,
        onActionClick = openLinkCleaner,
        actionEnabled = linkText.isValidUrl(),
    ) {
        OutlinedTextField(
            value = linkText,
            onValueChange = { linkText = it },
            label = { Text(text = stringResource(id = R.string.link)) },
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = {
                        scope.launch {
                            val annotatedText = clipboardManager.nativeClipboard.primaryClip
                            annotatedText?.toString()?.let {
                                val text = it.trim()
                                if (text.isNotEmpty()) {
                                    linkText = text
                                }
                            }
                        }
                    },
                    icon = Icons.Outlined.ContentPaste,
                    iconContentDescription = stringResource(id = R.string.paste_from_clipboard)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
