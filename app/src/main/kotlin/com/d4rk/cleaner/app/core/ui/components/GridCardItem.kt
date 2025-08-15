package com.d4rk.cleaner.app.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.d4rk.android.libs.apptoolkit.core.ui.components.modifiers.bounceClick
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.app.core.ui.theme.GroupedGridStyle

@Composable
fun GridCardItem(
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    title: String,
    subtitle: String,
    iconContainerColor: Color = GroupedGridStyle.iconContainerColor,
    iconShape: Shape = CircleShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.animateContentSize(),
        shape = GroupedGridStyle.cardShape,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(all = SizeConstants.LargeSize),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(end = SizeConstants.SmallSize)
                    .size(48.dp)
                    .clip(iconShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    iconPainter != null -> {
                        Icon(
                            modifier = Modifier.bounceClick(),
                            painter = iconPainter,
                            contentDescription = null,
                            tint = Color.Unspecified,
                        )
                    }
                    iconVector != null -> {
                        Icon(
                            modifier = Modifier.bounceClick(),
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.basicMarquee(),
                )
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun GridCardItem(
    model: GridCardModel,
    modifier: Modifier = Modifier,
    iconContainerColor: Color = GroupedGridStyle.iconContainerColor,
    iconShape: Shape = CircleShape,
    onClick: () -> Unit,
) {
    GridCardItem(
        iconPainter = model.iconPainter,
        iconVector = model.iconVector,
        title = model.title,
        subtitle = model.subtitle,
        iconContainerColor = iconContainerColor,
        iconShape = iconShape,
        onClick = onClick,
        modifier = modifier,
    )
}

