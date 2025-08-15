package com.d4rk.cleaner.app.clean.whatsapp.summary.ui.components

import androidx.compose.runtime.Composable
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.model.DirectoryItem
import com.d4rk.cleaner.app.core.ui.components.GroupedGridLayout

@Composable
fun DirectoryGrid(
    items: List<DirectoryItem>,
    onOpenDetails: (String) -> Unit,
) {
    GroupedGridLayout(items = items) { item ->
        DirectoryCard(
            item = item,
            onOpenDetails = onOpenDetails,
        )
    }
}
