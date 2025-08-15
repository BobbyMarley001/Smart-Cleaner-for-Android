package com.d4rk.cleaner.app.images.compressor.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil3.compose.AsyncImage
import com.d4rk.android.libs.apptoolkit.core.domain.model.ads.AdsConfig
import com.d4rk.android.libs.apptoolkit.core.ui.components.ads.AdBanner
import com.d4rk.android.libs.apptoolkit.core.ui.components.buttons.OutlinedIconButtonWithText
import com.d4rk.android.libs.apptoolkit.core.ui.components.navigation.LargeTopAppBarWithScaffold
import com.d4rk.android.libs.apptoolkit.core.ui.components.spacers.SmallHorizontalSpacer
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.android.libs.apptoolkit.core.utils.helpers.ScreenHelper
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.images.compressor.domain.data.model.ui.UiImageOptimizerState
import com.d4rk.cleaner.app.images.compressor.ui.components.tabs.FileSizeTab
import com.d4rk.cleaner.app.images.compressor.ui.components.tabs.ManualModeTab
import com.d4rk.cleaner.app.images.compressor.ui.components.tabs.QuickCompressTab
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.d4rk.cleaner.core.utils.helpers.FileSizeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageOptimizerScreen(
    activity: ImageOptimizerActivity,
    viewModel: ImageOptimizerViewModel,
    adsConfig: AdsConfig = koinInject()
) {
    val uiState: UiImageOptimizerState by viewModel.uiState.collectAsState()
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val dataStore: DataStore = koinInject()
    val adsState: Boolean by remember { dataStore.ads(default = true) }.collectAsState(initial = true)
    val context = LocalContext.current
    val isTabletOrLandscape: Boolean = ScreenHelper.isLandscapeOrTablet(context = context)
    val tabs: List<String> = listOf(
        stringResource(id = R.string.quick_compress),
        stringResource(id = R.string.file_size),
        stringResource(id = R.string.manual),
    )
    val pagerState: PagerState = rememberPagerState(pageCount = { tabs.size })

    var showPreview by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = uiState.selectedImageUri) {
        showPreview = false
    }

    LaunchedEffect(key1 = pagerState.currentPage) {
        viewModel.setCurrentTab(pagerState.currentPage)
    }

    val scrollBehavior: TopAppBarScrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    LargeTopAppBarWithScaffold(
        scrollBehavior = scrollBehavior,
        title = stringResource(id = R.string.image_optimizer),
        onBackClicked = { activity.finish() }) { paddingValues ->
        if (isTabletOrLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    TabRow(selectedTabIndex = pagerState.currentPage) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                text = { Text(text = title) },
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(page = index) }
                                })
                        }
                    }

                    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                        when (page) {
                            0 -> QuickCompressTab(viewModel = viewModel)
                            1 -> FileSizeTab(viewModel = viewModel)
                            2 -> ManualModeTab(viewModel = viewModel)
                        }
                    }

                    OptimizerButtons(
                        enabled = if (pagerState.currentPage == 1) {
                            uiState.fileSizeKB != 0
                        } else {
                            true
                        },
                        onOptimize = {
                            coroutineScope.launch { viewModel.optimizeImage() }
                        },
                        onReset = {
                            showPreview = false
                            viewModel.resetSettings()
                        },
                        modifier = Modifier
                            .padding(all = SizeConstants.MediumSize)
                            .align(Alignment.CenterHorizontally)
                    )

                    if (adsState) {
                        AdBanner(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            adsConfig = adsConfig
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(24.dp)
                ) {
                    ImageDisplay(viewModel = viewModel, showPreview = showPreview, onTogglePreview = { showPreview = it })
                }
            }
        } else {
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = paddingValues)
            ) {
                val (imageCardView: ConstrainedLayoutReference, tabLayout: ConstrainedLayoutReference, viewPager: ConstrainedLayoutReference, compressButton: ConstrainedLayoutReference, adView: ConstrainedLayoutReference) = createRefs()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .constrainAs(imageCardView) {
                            top.linkTo(anchor = parent.top)
                            start.linkTo(anchor = parent.start)
                            end.linkTo(anchor = parent.end)
                            bottom.linkTo(anchor = tabLayout.top)
                        }
                        .padding(all = 24.dp),
                ) {
                    ImageDisplay(viewModel = viewModel, showPreview = showPreview, onTogglePreview = { showPreview = it })
                }

                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.constrainAs(ref = tabLayout) {
                        top.linkTo(anchor = imageCardView.bottom)
                        start.linkTo(anchor = parent.start)
                        end.linkTo(anchor = parent.end)
                    }) {
                    tabs.forEachIndexed { index, title ->
                        Tab(text = { Text(text = title) },
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(page = index)
                                }
                            })
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.constrainAs(ref = viewPager) {
                        top.linkTo(anchor = tabLayout.bottom)
                        start.linkTo(anchor = parent.start)
                        end.linkTo(anchor = parent.end)
                        bottom.linkTo(anchor = compressButton.top)
                        height = Dimension.fillToConstraints
                    }) { page ->
                    when (page) {
                        0 -> QuickCompressTab(viewModel = viewModel)
                        1 -> FileSizeTab(viewModel = viewModel)
                        2 -> ManualModeTab(viewModel = viewModel)
                    }
                }

                OptimizerButtons(
                    enabled = if (pagerState.currentPage == 1) {
                        uiState.fileSizeKB != 0
                    } else {
                        true
                    },
                    onOptimize = {
                        coroutineScope.launch { viewModel.optimizeImage() }
                    },
                    onReset = {
                        showPreview = false
                        viewModel.resetSettings()
                    },
                    modifier = Modifier
                        .constrainAs(ref = compressButton) {
                            start.linkTo(anchor = parent.start)
                            end.linkTo(anchor = parent.end)
                            if (adsState) {
                                bottom.linkTo(anchor = adView.top)
                            } else {
                                bottom.linkTo(anchor = parent.bottom)
                            }
                        }
                        .padding(all = SizeConstants.MediumSize)
                )

                if (adsState) {
                    AdBanner(modifier = Modifier.constrainAs(ref = adView) {
                        bottom.linkTo(anchor = parent.bottom)
                        start.linkTo(anchor = parent.start)
                        end.linkTo(anchor = parent.end)
                    }, adsConfig = adsConfig)
                }
            }
        }
        //  Snackbar(message = stringResource(id = R.string.image_saved) + " " + (uiState.compressedImageUri?.path ?: "") , showSnackbar = uiState.showSaveSnackbar , onDismiss = { coroutineScope.launch { viewModel.updateShowSaveSnackbar(false) } })
    }
}

@Composable
fun ImageDisplay(
    viewModel: ImageOptimizerViewModel,
    showPreview: Boolean,
    onTogglePreview: (Boolean) -> Unit,
) {
    val state: State<UiImageOptimizerState> = viewModel.uiState.collectAsState()
    val imageUri = if (showPreview) state.value.compressedImageUri else state.value.selectedImageUri

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f), contentAlignment = Alignment.Center
    ) {
        if (state.value.isLoading) {
            CircularProgressIndicator()
        } else {
            imageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Selected Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(SizeConstants.SmallSize),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(checked = showPreview, onCheckedChange = onTogglePreview)
            SmallHorizontalSpacer()
            Text(
                text = stringResource(id = R.string.show_compressed_preview),
                modifier = Modifier
                    .clip(RoundedCornerShape(SizeConstants.ExtraSmallSize))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(SizeConstants.ExtraSmallSize))
        }

        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart
        ) {
            Card(
                shape = RoundedCornerShape(topEnd = SizeConstants.MediumSize),
            ) {
                val size = if (showPreview) state.value.compressedSizeKB else state.value.originalSizeKB
                Text(
                    text = FileSizeFormatter.format((size * 1024).toLong()),
                    modifier = Modifier
                        .padding(all = SizeConstants.ExtraSmallSize)
                        .animateContentSize(),
                )
            }
        }
    }
}

@Composable
fun OptimizerButtons(
    enabled: Boolean,
    onOptimize: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(SizeConstants.MediumSize),
        horizontalArrangement = Arrangement.spacedBy(SizeConstants.MediumSize)
    ) {
        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
            Text(text = stringResource(id = R.string.reset))
        }
        OutlinedIconButtonWithText(
            onClick = onOptimize,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.optimize_image)
        )
    }
}
