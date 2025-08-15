package com.d4rk.cleaner.app.clean.contacts.ui

import android.app.Activity
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import com.d4rk.android.libs.apptoolkit.core.domain.model.ads.AdsConfig
import com.d4rk.android.libs.apptoolkit.core.ui.components.ads.AdBanner
import com.d4rk.android.libs.apptoolkit.core.ui.components.buttons.AnimatedIconButtonDirection
import com.d4rk.android.libs.apptoolkit.core.ui.components.buttons.IconButtonWithText
import com.d4rk.android.libs.apptoolkit.core.ui.components.buttons.TonalIconButtonWithText
import com.d4rk.android.libs.apptoolkit.core.ui.components.layouts.LoadingScreen
import com.d4rk.android.libs.apptoolkit.core.ui.components.layouts.ScreenStateHandler
import com.d4rk.android.libs.apptoolkit.core.ui.components.spacers.SmallHorizontalSpacer
import com.d4rk.android.libs.apptoolkit.core.ui.effects.LifecycleEventsEffect
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.contacts.domain.actions.ContactsCleanerEvent
import com.d4rk.cleaner.app.clean.contacts.domain.data.model.UiContactsCleanerModel
import com.d4rk.cleaner.app.clean.contacts.ui.components.ContactGroupItem
import com.d4rk.cleaner.app.clean.contacts.ui.components.dialogs.PermissionRationaleDialog
import com.d4rk.cleaner.app.clean.contacts.ui.components.states.ContactsEmptyState
import com.d4rk.cleaner.app.clean.contacts.ui.components.states.ContactsErrorState
import com.d4rk.cleaner.core.utils.helpers.PermissionsHelper
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named

private enum class ContactsPermissionState { CHECKING, GRANTED, RATIONALE, DENIED }
private enum class SelectionState { SINGLE, SAME_GROUP, MULTIPLE_GROUPS }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsCleanerScreen(activity: Activity) {
    val viewModel: ContactsCleanerViewModel = koinViewModel()
    val state = viewModel.uiState.collectAsState().value
    val scrollBehavior: TopAppBarScrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val context = LocalContext.current
    var permissionState by remember { mutableStateOf(ContactsPermissionState.CHECKING) }

    val bottomBarAdConfig: AdsConfig = koinInject(qualifier = named(name = "full_banner"))

    LaunchedEffect(Unit) {
        when {
            PermissionsHelper.hasContactsPermissions(context) -> {
                permissionState = ContactsPermissionState.GRANTED
            }
            PermissionsHelper.shouldShowContactsPermissionRationale(activity) -> {
                permissionState = ContactsPermissionState.RATIONALE
            }
            else -> {
                PermissionsHelper.requestContactsPermissions(activity)
            }
        }
    }

    LifecycleEventsEffect(Lifecycle.Event.ON_RESUME) {
        permissionState = when {
            PermissionsHelper.hasContactsPermissions(context) -> ContactsPermissionState.GRANTED
            PermissionsHelper.shouldShowContactsPermissionRationale(activity) -> ContactsPermissionState.RATIONALE
            else -> ContactsPermissionState.DENIED
        }
    }

    val selectedCount by remember(state.data) {
        derivedStateOf {
            state.data?.duplicates
                ?.flatMap { it.contacts }
                ?.count { it.isSelected } ?: 0
        }
    }

    val selectionState by remember(state.data) {
        derivedStateOf {
            val groups = state.data?.duplicates ?: emptyList()
            val selectedGroups = groups.filter { group -> group.contacts.any { it.isSelected } }
            val total =
                selectedGroups.sumOf { group -> group.contacts.count { contact -> contact.isSelected } }
            when {
                total == 1 -> SelectionState.SINGLE
                selectedGroups.size == 1 -> SelectionState.SAME_GROUP
                else -> SelectionState.MULTIPLE_GROUPS
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = {
                Text(
                    modifier = Modifier.animateContentSize(),
                    text = stringResource(id = R.string.contacts_cleaner_title)
                )
            }, navigationIcon = {
                AnimatedIconButtonDirection(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = com.d4rk.android.libs.apptoolkit.R.string.go_back),
                    onClick = { activity.finish() })
            }, scrollBehavior = scrollBehavior)
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedCount > 0,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Column {
                    AdBanner(modifier = Modifier.fillMaxWidth(), adsConfig = bottomBarAdConfig)

                    BottomAppBar(
                        actions = {
                            Text(
                                modifier = Modifier.weight(weight = 1f, fill = true),
                                text = pluralStringResource(
                                    R.plurals.items_selected,
                                    selectedCount,
                                    selectedCount
                                ),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                            SmallHorizontalSpacer()
                            when (selectionState) {
                                SelectionState.SINGLE -> {
                                    TonalIconButtonWithText(
                                        onClick = { viewModel.onEvent(ContactsCleanerEvent.DeleteSelectedContacts) },
                                        label = stringResource(id = R.string.delete)
                                    )
                                }

                                SelectionState.SAME_GROUP -> {
                                    TonalIconButtonWithText(
                                        onClick = { viewModel.onEvent(ContactsCleanerEvent.MergeSelectedContacts) },
                                        enabled = selectedCount >= 2,
                                        label = stringResource(id = R.string.merge)
                                    )
                                    SmallHorizontalSpacer()
                                    TonalIconButtonWithText(
                                        onClick = { viewModel.onEvent(ContactsCleanerEvent.DeleteSelectedContacts) },
                                        icon = Icons.Default.AutoAwesome,
                                        iconContentDescription = null,
                                        label = stringResource(id = R.string.smart_clean)
                                    )
                                }

                                SelectionState.MULTIPLE_GROUPS -> {
                                    val canMerge = state.data?.duplicates?.any { group ->
                                        group.contacts.count { it.isSelected } >= 2
                                    } ?: false
                                    TonalIconButtonWithText(
                                        onClick = { viewModel.onEvent(ContactsCleanerEvent.MergeSelectedContacts) },
                                        enabled = canMerge,
                                        label = stringResource(id = R.string.merge_groups)
                                    )
                                    SmallHorizontalSpacer()
                                    TonalIconButtonWithText(
                                        onClick = { viewModel.onEvent(ContactsCleanerEvent.DeleteSelectedContacts) },
                                        icon = Icons.Default.AutoAwesome,
                                        iconContentDescription = null,
                                        label = stringResource(id = R.string.smart_clean_all)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (permissionState) {
            ContactsPermissionState.GRANTED -> {
                ScreenStateHandler(
                    screenState = state,
                    onLoading = {
                        LoadingScreen()
                    },
                    onEmpty = {
                        ContactsEmptyState(paddingValues = paddingValues)
                    },
                    onSuccess = { data: UiContactsCleanerModel ->
                        ContactsCleanerContent(
                            data = data,
                            viewModel = viewModel,
                            paddingValues = paddingValues
                        )
                    },
                    onError = {
                        ContactsErrorState(paddingValues = paddingValues) {
                            viewModel.onEvent(ContactsCleanerEvent.LoadDuplicates)
                        }
                    }
                )

                LifecycleEventsEffect(Lifecycle.Event.ON_RESUME) {
                    viewModel.onEvent(ContactsCleanerEvent.LoadDuplicates)
                }
            }

            ContactsPermissionState.RATIONALE -> {
                PermissionRationaleDialog(onRequest = {
                    PermissionsHelper.requestContactsPermissions(activity)
                }) {
                    permissionState = ContactsPermissionState.DENIED
                }
            }

            ContactsPermissionState.DENIED -> {
                PermissionDeniedScreen(onOpenSettings = { PermissionsHelper.openAppSettings(activity) })
            }

            ContactsPermissionState.CHECKING -> Unit
        }
    }
}

@Composable
private fun ContactsCleanerContent(
    data: UiContactsCleanerModel,
    viewModel: ContactsCleanerViewModel,
    paddingValues: PaddingValues,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(SizeConstants.LargeSize),
        contentPadding = PaddingValues(vertical = SizeConstants.LargeSize)
    ) {
        stickyHeader {
            val allSelected = data.duplicates.flatMap { it.contacts }.all { it.isSelected }
            val noneSelected = data.duplicates.flatMap { it.contacts }.none { it.isSelected }
            val toggleState = when {
                allSelected -> ToggleableState.On
                noneSelected -> ToggleableState.Off
                else -> ToggleableState.Indeterminate
            }
            val haptic = LocalHapticFeedback.current
            val view: View = LocalView.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .clip(CircleShape)
                    .padding(horizontal = SizeConstants.LargeSize)
                    .clickable {
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onEvent(ContactsCleanerEvent.ToggleSelectAll)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                TriStateCheckbox(
                    state = toggleState,
                    onClick = {
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onEvent(ContactsCleanerEvent.ToggleSelectAll)
                    }
                )
                Text(
                    text = stringResource(id = R.string.select_all),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = SizeConstants.SmallSize)
                )
            }
        }
        items(
            items = data.duplicates,
            key = { group ->
                group.contacts.firstOrNull()?.rawContactId ?: group.hashCode()
            }) { group ->
            ContactGroupItem(group = group, viewModel = viewModel)
        }
    }
}

@Composable
private fun PermissionDeniedScreen(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SizeConstants.LargeSize),
        verticalArrangement = Arrangement.spacedBy(SizeConstants.LargeSize),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Contacts,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(id = R.string.contacts_permission_denied),
            style = MaterialTheme.typography.bodyMedium
        )
        IconButtonWithText(
            onClick = onOpenSettings,
            label = stringResource(id = R.string.open_settings)
        )
    }
}