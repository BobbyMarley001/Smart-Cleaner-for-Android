package com.d4rk.cleaner.app.clean.analyze.ui

import android.view.View
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.d4rk.android.libs.apptoolkit.core.ui.components.layouts.LoadingScreen
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.analyze.ui.components.CleaningAnimationScreen
import com.d4rk.cleaner.app.clean.analyze.ui.components.StatusRowSelectAll
import com.d4rk.cleaner.app.clean.analyze.ui.components.dialogs.DeleteOrTrashConfirmation
import com.d4rk.cleaner.app.clean.analyze.ui.components.dialogs.GlobalSelectAllWarningDialog
import com.d4rk.cleaner.app.clean.analyze.ui.components.tabs.TabsContent
import com.d4rk.cleaner.app.clean.nofilesfound.ui.NoFilesFoundScreen
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningState
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileEntry
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import com.d4rk.cleaner.app.clean.scanner.domain.actions.ScannerEvent
import com.d4rk.cleaner.app.clean.scanner.ui.ScannerViewModel
import com.d4rk.cleaner.app.clean.scanner.ui.components.TwoRowButtons
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnalyzeScreen(
    view: View,
    viewModel: ScannerViewModel,
    data: UiScannerModel,
) {
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val hasSelectedFiles: Boolean = data.analyzeState.selectedFilesCount > 0
    val groupedFiles: Map<String, List<FileEntry>> =
        data.analyzeState.groupedFiles.filterValues { it.isNotEmpty() }
    val showActions: Boolean =
        data.analyzeState.state == CleaningState.ReadyToClean && hasSelectedFiles

    Column(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth()
            .padding(vertical = SizeConstants.LargeSize), horizontalAlignment = Alignment.End
    ) {
        OutlinedCard(
            modifier = Modifier
                .weight(weight = 1f)
                .fillMaxWidth(),
        ) {
            println(message = "The state of the screen is ${data.analyzeState.state}")
            when (data.analyzeState.state) {

                CleaningState.Analyzing -> {
                    LoadingScreen()
                }

                CleaningState.Cleaning -> {
                    CleaningAnimationScreen(
                        cleaned = data.analyzeState.cleanedFilesCount,
                        total = data.analyzeState.totalFilesToClean,
                    )
                }

                CleaningState.ReadyToClean -> {
                    if (groupedFiles.isNotEmpty()) {
                        TabsContent(
                            groupedFiles = groupedFiles,
                            viewModel = viewModel,
                            view = view,
                            coroutineScope = coroutineScope,
                            data = data,
                        )
                    } else {
                        NoFilesFoundScreen(viewModel = viewModel)
                    }
                }

                CleaningState.Result -> {
                    NoFilesFoundScreen(viewModel = viewModel)
                }

                CleaningState.Error -> {
                    ErrorScreen(onRetry = {
                        viewModel.resetAfterError()
                        viewModel.onEvent(ScannerEvent.AnalyzeFiles)
                    })
                }

                CleaningState.Idle -> {}
            }
        }
        if (groupedFiles.isNotEmpty() && data.analyzeState.state == CleaningState.ReadyToClean) {
            StatusRowSelectAll(
                data = data,
                onClickSelectAll = { viewModel.onEvent(ScannerEvent.OnGlobalSelectAllClick) },
            )
        }

        if (data.analyzeState.state == CleaningState.ReadyToClean) {
            TwoRowButtons(
                modifier = Modifier,
                enabled = showActions,
                onStartButtonClick = {
                    viewModel.setMoveToTrashConfirmationDialogVisibility(isVisible = true)
                },
                onStartButtonIcon = Icons.Outlined.Delete,
                onStartButtonText = R.string.move_to_trash,
                onEndButtonClick = {
                    viewModel.setDeleteForeverConfirmationDialogVisibility(true)
                },
                onEndButtonIcon = Icons.Outlined.DeleteForever,
                onEndButtonText = R.string.delete_forever,
            )
        }

        if (data.analyzeState.isDeleteForeverConfirmationDialogVisible || data.analyzeState.isMoveToTrashConfirmationDialogVisible) {
            DeleteOrTrashConfirmation(data = data, viewModel = viewModel)
        }

        if (data.analyzeState.isGlobalSelectAllWarningDialogVisible) {
            GlobalSelectAllWarningDialog(
                onConfirm = { dontShowAgain ->
                    viewModel.onEvent(ScannerEvent.ConfirmGlobalSelectAll(dontShowAgain))
                },
                onDismiss = { viewModel.onEvent(ScannerEvent.DismissGlobalSelectAllWarning) }
            )
        }
    }
}

@Composable
private fun ErrorScreen(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(id = R.string.cleanup_failed))
            Spacer(modifier = Modifier.size(SizeConstants.MediumSize))
            Button(onClick = onRetry) {
                Text(text = stringResource(id = com.d4rk.android.libs.apptoolkit.R.string.try_again))
            }
        }
    }
}