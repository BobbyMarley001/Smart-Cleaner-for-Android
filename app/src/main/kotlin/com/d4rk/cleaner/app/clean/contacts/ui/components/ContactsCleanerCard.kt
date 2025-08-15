package com.d4rk.cleaner.app.clean.contacts.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.dashboard.ui.components.DashboardActionCard

@Composable
fun ContactsCleanerCard(modifier: Modifier = Modifier, onOpen: () -> Unit) {
    DashboardActionCard(
        modifier = modifier,
        icon = Icons.Outlined.Contacts,
        title = stringResource(id = R.string.contacts_cleaner_card_title),
        subtitle = stringResource(id = R.string.contacts_cleaner_card_subtitle),
        actionLabel = stringResource(id = R.string.open_contacts_cleaner),
        actionIcon = Icons.Outlined.PersonSearch,
        onActionClick = onOpen,
        onHeaderClick = onOpen
    )
}
