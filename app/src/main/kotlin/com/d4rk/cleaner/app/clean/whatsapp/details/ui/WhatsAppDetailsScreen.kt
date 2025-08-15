package com.d4rk.cleaner.app.clean.whatsapp.details.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.d4rk.android.libs.apptoolkit.core.domain.model.ads.AdsConfig
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.ui.components.ads.AdBanner
import com.d4rk.android.libs.apptoolkit.core.ui.components.buttons.AnimatedIconButtonDirection
import com.d4rk.android.libs.apptoolkit.core.ui.components.buttons.IconButtonWithText
import com.d4rk.android.libs.apptoolkit.core.ui.components.dialogs.BasicAlertDialog
import com.d4rk.android.libs.apptoolkit.core.ui.components.layouts.LoadingScreen
import com.d4rk.android.libs.apptoolkit.core.ui.components.layouts.NoDataScreen
import com.d4rk.android.libs.apptoolkit.core.ui.components.layouts.ScreenStateHandler
import com.d4rk.android.libs.apptoolkit.core.ui.components.navigation.LargeTopAppBarWithScaffold
import com.d4rk.android.libs.apptoolkit.core.ui.components.spacers.ExtraSmallHorizontalSpacer
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.analyze.ui.components.CleaningAnimationScreen
import com.d4rk.cleaner.app.clean.analyze.ui.components.FileCard
import com.d4rk.cleaner.app.clean.analyze.ui.components.dialogs.GlobalSelectAllWarningDialog
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningState
import com.d4rk.cleaner.app.clean.scanner.ui.components.FileListItem
import com.d4rk.cleaner.app.clean.scanner.ui.components.FilePreviewCard
import com.d4rk.cleaner.app.clean.scanner.utils.helpers.FilePreviewHelper
import com.d4rk.cleaner.app.clean.whatsapp.details.domain.actions.WhatsAppDetailsEvent
import com.d4rk.cleaner.app.clean.whatsapp.details.domain.model.UiWhatsAppDetailsModel
import com.d4rk.cleaner.app.clean.whatsapp.details.ui.components.CustomTabLayout
import com.d4rk.cleaner.app.clean.whatsapp.details.ui.components.DetailsStatusRow
import com.d4rk.cleaner.app.clean.whatsapp.details.ui.components.dialogs.SortAlertDialog
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.actions.WhatsAppCleanerEvent
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.model.UiWhatsAppCleanerModel
import com.d4rk.cleaner.app.clean.whatsapp.summary.ui.WhatsappCleanerSummaryViewModel
import com.d4rk.cleaner.app.clean.whatsapp.utils.constants.WhatsAppMediaConstants
import com.d4rk.cleaner.app.clean.whatsapp.utils.helpers.openFile
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.d4rk.cleaner.core.utils.helpers.isProtectedAndroidDir
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    title: String,
    onDelete: (List<File>) -> Unit,
    detailsViewModel: DetailsViewModel,
    viewModel: WhatsappCleanerSummaryViewModel,
    activity: Activity
) {

    val state: UiStateScreen<UiWhatsAppCleanerModel> by viewModel.uiState.collectAsState()
    val detailsState: UiStateScreen<UiWhatsAppDetailsModel> by detailsViewModel.uiState.collectAsState()
    val localizedTitle = when (title) {
        WhatsAppMediaConstants.IMAGES -> stringResource(id = R.string.images)
        WhatsAppMediaConstants.VIDEOS -> stringResource(id = R.string.videos)
        WhatsAppMediaConstants.DOCUMENTS -> stringResource(id = R.string.documents)
        WhatsAppMediaConstants.AUDIOS -> stringResource(id = R.string.audios)
        WhatsAppMediaConstants.STATUSES -> stringResource(id = R.string.statuses)
        WhatsAppMediaConstants.VOICE_NOTES -> stringResource(id = R.string.voice_notes)
        WhatsAppMediaConstants.VIDEO_NOTES -> stringResource(id = R.string.video_notes)
        WhatsAppMediaConstants.GIFS -> stringResource(id = R.string.gifs)
        WhatsAppMediaConstants.WALLPAPERS -> stringResource(id = R.string.wallpapers)
        WhatsAppMediaConstants.STICKERS -> stringResource(id = R.string.stickers)
        WhatsAppMediaConstants.PROFILE_PHOTOS -> stringResource(id = R.string.profile_photos)
        else -> title
    }
    val selected = remember { mutableStateListOf<File>() }
    val isGrid = detailsState.data?.isGridView ?: true
    var showSort by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    val dataStore: DataStore = koinInject()
    val coroutineScope = rememberCoroutineScope()
    var showGlobalSelectAllWarning by remember { mutableStateOf(false) }

    val scrollBehavior: TopAppBarScrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val sortedFiles = detailsState.data?.files ?: emptyList()
    val suggested = detailsState.data?.suggested ?: emptyList()

    val adsConfig: AdsConfig = koinInject(qualifier = named(name = "full_banner"))

    val hasFiles = sortedFiles.isNotEmpty()

    LargeTopAppBarWithScaffold(
        actions = {
            AnimatedIconButtonDirection(
                visible = hasFiles,
                icon = if (isGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                contentDescription = null,
                onClick = {
                    detailsViewModel.onEvent(WhatsAppDetailsEvent.ToggleView)
                },
                fromRight = true)

            AnimatedIconButtonDirection(
                visible = true,
                icon = Icons.AutoMirrored.Filled.Sort,
                contentDescription = null,
                onClick = {
                    showSort = hasFiles
                },
                durationMillis = 400,
                fromRight = true)

        },
        title = localizedTitle,
        onBackClicked = { activity.finish() },
        scrollBehavior = scrollBehavior,
    ) { paddingValues ->
        ScreenStateHandler(
            screenState = state,
            onLoading = {
                if (
                    state.data?.cleaningState == CleaningState.Cleaning ||
                    state.data?.cleaningState == CleaningState.Result
                ) {
                    CleaningAnimationScreen()
                } else {
                    LoadingScreen()
                }
            },
            onEmpty = {
                NoDataScreen(
                    icon = Icons.Outlined.FolderOff,
                    showRetry = true,
                    onRetry = { viewModel.onEvent(WhatsAppCleanerEvent.LoadMedia) }
                )
            },
            onSuccess = { data ->
                LaunchedEffect(title) {
                    detailsViewModel.onEvent(
                        WhatsAppDetailsEvent.LoadFiles(
                            title
                        )
                    )
                }

                val receivedFiles = remember(sortedFiles) {
                    sortedFiles.filterNot {
                        it.path.contains("${File.separator}Sent") ||
                                it.path.contains("${File.separator}Private")
                    }
                }
                val sentFiles = remember(sortedFiles) {
                    sortedFiles.filter { it.path.contains("${File.separator}Sent") }
                }
                val privateFiles = remember(sortedFiles) {
                    sortedFiles.filter { it.path.contains("${File.separator}Private") }
                }

                val hasReceived = receivedFiles.isNotEmpty()
                val hasSent = sentFiles.isNotEmpty()
                val hasPrivate = privateFiles.isNotEmpty()

                val tabs = buildList {
                    if (hasReceived) add(stringResource(id = R.string.received))
                    if (hasSent) add(stringResource(id = R.string.sent))
                    if (hasPrivate) add(stringResource(id = R.string.private_tab))
                }

                val tabFiles = buildList {
                    if (hasReceived) add(receivedFiles)
                    if (hasSent) add(sentFiles)
                    if (hasPrivate) add(privateFiles)
                }

                if (tabFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        NoDataScreen(
                            icon = Icons.Outlined.FolderOff,
                            showRetry = true,
                            onRetry = { viewModel.onEvent(WhatsAppCleanerEvent.LoadMedia) }
                        )
                    }
                } else {

                    val pagerState = rememberPagerState { tabs.size }
                    var selectedTabIndex by remember { mutableIntStateOf(0) }
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(pagerState.currentPage) {
                        selectedTabIndex = pagerState.currentPage
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        if (suggested.isNotEmpty()) {
                            SmartSuggestionsCard(
                                selected = selected,
                                suggested = suggested,
                                onShowConfirmChange = { showConfirm = it },
                                state = state
                            )
                        }

                        if (tabs.size > 1) {
                            CustomTabLayout(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                selectedItemIndex = selectedTabIndex,
                                items = tabs,
                                filesPerTab = tabFiles,
                                selectedFiles = selected,
                                onTabSelected = { index ->
                                    selectedTabIndex = index
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                onTabCheckedChange = { index, checked ->
                                    val listFiles = tabFiles.getOrNull(index) ?: emptyList()
                                    if (checked) {
                                        listFiles
                                            .filterNot { it in selected || it.isProtectedAndroidDir() }
                                            .forEach { selected.add(it) }
                                    } else {
                                        selected.removeAll(listFiles)
                                    }
                                }
                            )
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) { page ->
                            val list = tabFiles.getOrNull(page) ?: emptyList()

                            DetailsScreenContent(
                                selected = selected,
                                isGrid = isGrid,
                                files = list
                            )
                        }

                        AdBanner(modifier = Modifier.fillMaxWidth(), adsConfig = adsConfig)

                        val toggleSelectAll = {
                            val accessible = sortedFiles.filterNot { it.isProtectedAndroidDir() }
                            if (selected.count { !it.isProtectedAndroidDir() } == accessible.size && accessible.isNotEmpty()) {
                                selected.removeAll(accessible)
                            } else {
                                selected.removeAll { it.isProtectedAndroidDir() }
                                selected.addAll(accessible)
                            }
                        }

                        if (tabs.size <= 1) {
                            DetailsStatusRow(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                selectedCount = selected.count { !it.isProtectedAndroidDir() },
                                allSelected = selected.count { !it.isProtectedAndroidDir() } ==
                                        sortedFiles.count { !it.isProtectedAndroidDir() } &&
                                        sortedFiles.any { !it.isProtectedAndroidDir() },
                                onClickSelectAll = {
                                    coroutineScope.launch {
                                        val accessible = sortedFiles.filterNot { it.isProtectedAndroidDir() }
                                        val allSelected =
                                            selected.count { !it.isProtectedAndroidDir() } ==
                                                accessible.size && accessible.isNotEmpty()
                                        val showDialog =
                                            !allSelected && dataStore.showGlobalSelectAllWarning.first()
                                        if (showDialog) {
                                            showGlobalSelectAllWarning = true
                                        } else {
                                            toggleSelectAll()
                                        }
                                    }
                                }
                            )
                        }

                        IconButtonWithText(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(8.dp),
                            onClick = { showConfirm = true },
                            enabled = selected.any { !it.isProtectedAndroidDir() } &&
                                state.data?.cleaningState != CleaningState.Cleaning &&
                                state.data?.cleaningState != CleaningState.Error,
                            iconContentDescription = null,
                            label = stringResource(id = R.string.delete_selected),
                            icon = Icons.Outlined.Delete
                        )
                    }
                }
            }
        )
    }

    if (showSort) {
        SortAlertDialog(
            current = detailsState.data?.sortType ?: SortType.DATE,
            descending = detailsState.data?.descending ?: false,
            startDate = detailsState.data?.startDate,
            endDate = detailsState.data?.endDate,
            onDismiss = { showSort = false },
            onApply = { type, desc, start, end ->
                detailsViewModel.onEvent(
                    WhatsAppDetailsEvent.ApplySort(type, desc, start, end)
                )
            }
        )
    }

    if (showGlobalSelectAllWarning) {
        GlobalSelectAllWarningDialog(
            onConfirm = { dontShowAgain ->
                coroutineScope.launch {
                    if (dontShowAgain) {
                        dataStore.saveShowGlobalSelectAllWarning(false)
                    }
                }
                val accessible = sortedFiles.filterNot { it.isProtectedAndroidDir() }
                if (selected.count { !it.isProtectedAndroidDir() } == accessible.size && accessible.isNotEmpty()) {
                    selected.removeAll(accessible)
                } else {
                    selected.removeAll { it.isProtectedAndroidDir() }
                    selected.addAll(accessible)
                }
                showGlobalSelectAllWarning = false
            },
            onDismiss = { showGlobalSelectAllWarning = false }
        )
    }

    if (showConfirm) {
        BasicAlertDialog(
            onDismiss = { showConfirm = false },
            onConfirm = {
                showConfirm = false
                onDelete(selected.filterNot { it.isProtectedAndroidDir() })
                selected.clear()
            },
            onCancel = { showConfirm = false },
            title = stringResource(id = R.string.delete_confirmation_title),
            content = {
                Text(
                    text = pluralStringResource(
                        id = R.plurals.delete_confirmation_message,
                        count = selected.size,
                        selected.size
                    )
                )
            },
            confirmButtonText = stringResource(id = R.string.delete),
            dismissButtonText = stringResource(id = android.R.string.cancel)
        )
    }
}

@Composable
fun DetailsScreenContent(
    selected: MutableList<File>,
    isGrid: Boolean,
    files: List<File>
) {
    val context: Context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = context.getString(R.string.no_files_found))
            }
        } else {
            if (isGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(96.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(files) { file ->
                        val checked = file in selected
                        val isProtected = file.isProtectedAndroidDir()
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .then(
                                    if (!isProtected) Modifier.pointerInput(file, checked) {
                                        detectTapGestures(onLongPress = {
                                            if (checked) selected.remove(file) else selected.add(file)
                                        })
                                    } else Modifier
                                )
                        ) {
                            FileCard(
                                file = file,
                                isChecked = checked,
                                onCheckedChange = { isChecked ->
                                    if (!isProtected) {
                                        if (isChecked) selected.add(file) else selected.remove(file)
                                    }
                                },
                                isProtected = isProtected,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(files) { file ->
                        val checked = file in selected
                        val previewType = FilePreviewHelper.getPreviewType(file)
                        val isMedia = remember(previewType) {
                            previewType is FilePreviewHelper.PreviewType.Image ||
                                    previewType is FilePreviewHelper.PreviewType.Video
                        }
                        val isProtected = file.isProtectedAndroidDir()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .then(
                                    if (!isProtected) Modifier.pointerInput(file) {
                                        detectTapGestures(
                                            onLongPress = {
                                                if (checked) selected.remove(file) else selected.add(file)
                                            },
                                            onTap = { openFile(context, file) }
                                        )
                                    } else Modifier
                                )
                        ) {
                            if (isMedia) {
                                FilePreviewCard(file = file, modifier = Modifier.weight(1f))
                            } else {
                                FileListItem(file = file, modifier = Modifier.weight(1f))
                            }
                            TriStateCheckbox(
                                state = if (checked) ToggleableState.On else ToggleableState.Off,
                                onClick = {
                                    if (!isProtected) {
                                        if (checked) selected.remove(file) else selected.add(file)
                                    }
                                },
                                enabled = !isProtected,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartSuggestionsCard(
    selected: MutableList<File>,
    suggested: List<File>,
    onShowConfirmChange: (Boolean) -> Unit,
    state: UiStateScreen<UiWhatsAppCleanerModel>
) {
    val context: Context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_auto_fix_high),
                    contentDescription = null
                )
                ExtraSmallHorizontalSpacer()
                Text(
                    text = stringResource(id = R.string.smart_suggestions),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(suggested) { file ->
                    val checked = file in selected
                    val isProtected = file.isProtectedAndroidDir()
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .then(
                                if (!isProtected) Modifier.pointerInput(file) {
                                    detectTapGestures(
                                        onLongPress = {
                                            if (checked) selected.remove(file) else selected.add(file)
                                        },
                                        onTap = { openFile(context, file) }
                                    )
                                } else Modifier
                            )
                    ) {
                        FilePreviewCard(
                            file = file,
                            modifier = Modifier.size(64.dp)
                        )
                        TriStateCheckbox(
                            state = if (checked) ToggleableState.On else ToggleableState.Off,
                            onClick = {
                                if (!isProtected) {
                                    if (checked) selected.remove(file) else selected.add(file)
                                }
                            },
                            enabled = !isProtected,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                }
            }
            IconButtonWithText(
                onClick = {
                    selected.clear()
                    selected.addAll(suggested.filterNot { it.isProtectedAndroidDir() })
                    onShowConfirmChange(true)
                },
                modifier = Modifier.align(Alignment.End),
                enabled = state.data?.cleaningState != CleaningState.Cleaning &&
                    state.data?.cleaningState != CleaningState.Error,
                label = stringResource(id = R.string.delete_all_suggested),
                icon = Icons.Outlined.Delete
            )
        }
    }
}
