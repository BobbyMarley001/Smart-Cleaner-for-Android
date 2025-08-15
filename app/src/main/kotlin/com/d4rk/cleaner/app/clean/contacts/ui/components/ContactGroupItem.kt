package com.d4rk.cleaner.app.clean.contacts.ui.components

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import com.d4rk.android.libs.apptoolkit.core.ui.components.buttons.IconButton
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.contacts.domain.actions.ContactsCleanerEvent
import com.d4rk.cleaner.app.clean.contacts.domain.data.model.DuplicateContactGroup
import com.d4rk.cleaner.app.clean.contacts.ui.ContactsCleanerViewModel

@Composable
fun ContactGroupItem(
    group: DuplicateContactGroup,
    viewModel: ContactsCleanerViewModel
) {
    var isExpanded by remember { mutableStateOf(false) }
    val view : View = LocalView.current
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SizeConstants.LargeSize)
            .animateContentSize(
                animationSpec = tween(durationMillis = 300)
            ),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onEvent(
                            ContactsCleanerEvent.ToggleGroupSelection(group.contacts)
                        )
                    }
                    .padding(SizeConstants.MediumSize),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val allSelected = group.contacts.all { it.isSelected }
                val noneSelected = group.contacts.none { it.isSelected }
                val toggleState = when {
                    allSelected -> ToggleableState.On
                    noneSelected -> ToggleableState.Off
                    else -> ToggleableState.Indeterminate
                }
                TriStateCheckbox(
                    state = toggleState,
                    onClick = {
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onEvent(
                            ContactsCleanerEvent.ToggleGroupSelection(group.contacts)
                        )
                    }
                )
                Column(modifier = Modifier.padding(start = SizeConstants.MediumSize)) {
                    Text(text = group.contacts.firstOrNull()?.displayName ?: "")
                    AssistChip(
                        onClick = {},
                        label = { Text(text = "${group.contacts.size} ${stringResource(id = R.string.duplicates)}") })
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    icon = if (isExpanded) Icons.Outlined.ArrowDropDown else Icons.AutoMirrored.Filled.ArrowLeft,
                    onClick = {
                        isExpanded = !isExpanded
                    })
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SizeConstants.MediumSize),
                    verticalArrangement = Arrangement.spacedBy(SizeConstants.SmallSize)
                ) {
                    group.contacts.forEach { contact ->
                        ContactDetailRow(contact = contact, viewModel = viewModel)
                    }
                }
            }
        }
    }
}