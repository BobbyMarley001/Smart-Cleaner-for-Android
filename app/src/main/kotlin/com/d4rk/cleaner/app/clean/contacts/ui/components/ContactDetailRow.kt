package com.d4rk.cleaner.app.clean.contacts.ui.components

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.d4rk.android.libs.apptoolkit.core.ui.components.spacers.SmallHorizontalSpacer
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.app.clean.contacts.domain.actions.ContactsCleanerEvent
import com.d4rk.cleaner.app.clean.contacts.domain.data.model.RawContactInfo
import com.d4rk.cleaner.app.clean.contacts.ui.ContactsCleanerViewModel

@Composable
fun ContactDetailRow(contact: RawContactInfo, viewModel: ContactsCleanerViewModel) {
    val haptic = LocalHapticFeedback.current
    val view : View = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.onEvent(
                    ContactsCleanerEvent.ToggleContactSelection(contact)
                )
            }
            .padding(horizontal = SizeConstants.LargeSize),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = contact.isSelected,
            onCheckedChange = {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.onEvent(
                    ContactsCleanerEvent.ToggleContactSelection(contact)
                )
            }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = SizeConstants.MediumSize)
        ) {
            Text(text = contact.displayName)
            contact.phones.firstOrNull()?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    SmallHorizontalSpacer()
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            contact.emails.firstOrNull()?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    SmallHorizontalSpacer()
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        val icon = when {
            contact.accountType?.contains("sim", true) == true -> Icons.Default.SimCard
            contact.accountType?.contains("google", true) == true -> Icons.Default.AccountCircle
            else -> Icons.Default.Person
        }
        Icon(imageVector = icon, contentDescription = null)
    }
}