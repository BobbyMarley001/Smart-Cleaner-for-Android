package com.d4rk.cleaner.app.clean.analyze.ui.components

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4rk.android.libs.apptoolkit.core.ui.components.modifiers.bounceClick
import com.d4rk.cleaner.R

/**
 * Composable function for selecting or deselecting all items.
 *
 * This composable displays a filter chip labeled "Select All". When tapped, it toggles the
 * selection state and invokes the `onCheckedChange` callback.
 *
 */
@Composable
fun SelectAllComposable(
    selected: Boolean, onClickSelectAll: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view: View = LocalView.current
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    FilterChip(
        modifier = Modifier.bounceClick(),
        selected = selected,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            view.playSoundEffect(SoundEffectConstants.CLICK)
            onClickSelectAll()
        },
        label = { Text(text = stringResource(id = R.string.select_all)) },
        leadingIcon = {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { SelectAllTransitions.fadeScale },
                label = "Checkmark Animation"
            ) { targetChecked ->
                if (targetChecked) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(size = 18.dp)
                    )
                }
            }
        },
        interactionSource = interactionSource,
    )
}