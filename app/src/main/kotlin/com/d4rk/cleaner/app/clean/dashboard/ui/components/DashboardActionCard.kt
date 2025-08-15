package com.d4rk.cleaner.app.clean.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.constraintlayout.compose.ConstraintLayout
import com.d4rk.android.libs.apptoolkit.core.ui.components.buttons.TonalIconButtonWithText
import com.d4rk.android.libs.apptoolkit.core.ui.components.modifiers.bounceClick
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants

@Composable
fun DashboardActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    actionIcon: ImageVector? = null,
    actionPainter: Painter? = null,
    onActionClick: () -> Unit,
    badgeText: String? = null,
    actionEnabled: Boolean = true,
    onHeaderClick: (() -> Unit)? = null,
    headerEnabled: Boolean = actionEnabled,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SizeConstants.ExtraLargeSize),
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = SizeConstants.LargeSize),
                verticalArrangement = Arrangement.spacedBy(SizeConstants.MediumSize)
            ) {
                Row(
                    modifier = onHeaderClick?.let {
                        Modifier
                            .bounceClick()
                            .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = headerEnabled,
                            onClick = it
                        )
                    } ?: Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SizeConstants.MediumSize)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                content()
            }

            ConstraintLayout(
                modifier = Modifier.fillMaxWidth()
            ) {
                val (badge, action) = createRefs()

                badgeText?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .constrainAs(badge) {
                                start.linkTo(parent.start)
                                bottom.linkTo(parent.bottom)
                            }
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(topEnd = SizeConstants.ExtraLargeSize)
                            )
                            .padding(all = SizeConstants.MediumSize)
                    )
                }

                TonalIconButtonWithText(
                    label = actionLabel,
                    icon = actionIcon,
                    painter = actionPainter,
                    onClick = onActionClick,
                    enabled = actionEnabled,
                    modifier = Modifier
                        .constrainAs(action) {
                            end.linkTo(parent.end, margin = SizeConstants.LargeSize)
                            bottom.linkTo(parent.bottom, margin = SizeConstants.LargeSize)
                        }
                )
            }
        }
    }
}

