package com.d4rk.cleaner.app.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.app.core.ui.theme.GroupedGridStyle

@Composable
fun <T> GroupedGridLayout(
    items: List<T>,
    chunkSize: Int = 2,
    itemContent: @Composable (T) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SizeConstants.MediumSize)
            .animateContentSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GroupedGridStyle.gridClipShape),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SizeConstants.ExtraTinySize)) {
                items.chunked(chunkSize).forEach { chunk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = SizeConstants.ExtraTinySize,
                            alignment = Alignment.CenterHorizontally,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (item in chunk) {
                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                itemContent(item)
                            }
                        }
                    }
                }
            }
        }
    }
}

