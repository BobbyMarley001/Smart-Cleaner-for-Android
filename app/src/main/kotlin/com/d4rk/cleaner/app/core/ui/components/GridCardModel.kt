package com.d4rk.cleaner.app.core.ui.components

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Contract for lightweight models that describe the content of a [GridCardItem].
 */
interface GridCardModel {
    val title: String
    val subtitle: String
    val iconVector: ImageVector?
    val iconPainter: Painter?
}

