package com.d4rk.cleaner.app.clean.contacts.ui.components.dialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.d4rk.android.libs.apptoolkit.core.ui.components.dialogs.BasicAlertDialog
import com.d4rk.cleaner.R

@Composable
fun PermissionRationaleDialog(onRequest: () -> Unit, onDismiss: () -> Unit) {
    BasicAlertDialog(
        onDismiss = onDismiss,
        onConfirm = onRequest,
        onCancel = onDismiss,
        icon = Icons.Outlined.Contacts,
        title = stringResource(id = R.string.contacts_cleaner_title),
        confirmButtonText = stringResource(id = R.string.button_grant_permission),
        content = { Text(text = stringResource(id = R.string.contacts_permission_rationale)) },
    )
}