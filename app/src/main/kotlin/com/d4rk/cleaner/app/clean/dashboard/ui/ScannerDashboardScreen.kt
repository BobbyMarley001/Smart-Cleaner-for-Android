package com.d4rk.cleaner.app.clean.dashboard.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.d4rk.android.libs.apptoolkit.core.domain.model.ads.AdsConfig
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.ui.components.ads.AdBanner
import com.d4rk.android.libs.apptoolkit.core.ui.components.spacers.LargeVerticalSpacer
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.android.libs.apptoolkit.core.utils.helpers.IntentsHelper
import com.d4rk.cleaner.Cleaner
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.apps.manager.domain.data.model.ui.UiAppManagerModel
import com.d4rk.cleaner.app.apps.manager.ui.AppManagerViewModel
import com.d4rk.cleaner.app.clean.contacts.ui.ContactsCleanerActivity
import com.d4rk.cleaner.app.clean.contacts.ui.components.ContactsCleanerCard
import com.d4rk.cleaner.app.clean.largefiles.ui.LargeFilesActivity
import com.d4rk.cleaner.app.clean.scanner.domain.actions.ScannerEvent
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningState
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import com.d4rk.cleaner.app.clean.scanner.ui.ScannerViewModel
import com.d4rk.cleaner.app.clean.scanner.ui.components.ApkCleanerCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.ClipboardCleanerCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.EmptyFolderCleanerCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.ImageOptimizerCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.LargeFilesCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.LinkCleanerCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.PromotedAppCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.QuickScanSummaryCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.SystemStorageManagerCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.WeeklyCleanStreakCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.WhatsAppCleanerCard
import com.d4rk.cleaner.app.clean.whatsapp.summary.ui.WhatsAppCleanerActivity
import com.d4rk.cleaner.app.images.picker.ui.ImagePickerActivity
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named
import java.io.File

private enum class AdSlot { A, B, C, D, E, F }
private enum class ContentCard {
    QUICK_SCAN,
    SYSTEM_STORAGE,
    WEEKLY_STREAK,
    LARGE_FILES,
    WHATSAPP,
    APK,
    EMPTY_FOLDERS,
    CLIPBOARD,
    CONTACTS,
    LINK_CLEANER,
    IMAGE_OPTIMIZER,
    PROMOTED_APP
}
private sealed interface HomeItem {
    data class Card(val type: ContentCard) : HomeItem
    data class Ad(val slot: AdSlot) : HomeItem
}

@Composable
fun ScannerDashboardScreen(
    uiState: UiStateScreen<UiScannerModel>,
    viewModel: ScannerViewModel,
) {
    val appManagerViewModel: AppManagerViewModel = koinViewModel()
    val context: Context = LocalContext.current

    val promotedApp = uiState.data?.promotedApp
    val mrecAdsConfig: AdsConfig = koinInject(qualifier = named("banner_medium_rectangle"))
    val largeBannerAdsConfig: AdsConfig = koinInject(qualifier = named("large_banner"))
    val leaderboardAdsConfig: AdsConfig = koinInject(qualifier = named("leaderboard"))
    val bannerAdsConfig: AdsConfig = koinInject()

    val appManagerState: UiStateScreen<UiAppManagerModel> by appManagerViewModel.uiState.collectAsState()
    val whatsappSummary by viewModel.whatsAppMediaSummary.collectAsState()
    val whatsappLoaded by viewModel.whatsAppMediaLoaded.collectAsState()
    val whatsappInstalled by viewModel.isWhatsAppInstalled.collectAsState()
    val clipboardText by viewModel.clipboardPreview.collectAsState()
    val largeFiles by viewModel.largestFiles.collectAsState()
    val emptyFolders by viewModel.emptyFolders.collectAsState()
    val emptyFoldersHideUntil by viewModel.emptyFoldersHideUntil.collectAsState()
    val streakDays by viewModel.cleanStreak.collectAsState()
    val streakRecord by viewModel.streakRecord.collectAsState()
    val showStreakCard by viewModel.showStreakCard.collectAsState()
    val cleaningApks by viewModel.cleaningApks.collectAsState()

    val dataStore: DataStore = koinInject()
    val adsState: Boolean by remember { dataStore.ads(default = true) }.collectAsState(initial = true)

    val showApkCard by remember(appManagerState) {
        derivedStateOf {
            appManagerState.data?.apkFilesLoading == false &&
                appManagerState.data?.apkFiles?.isNotEmpty() == true
        }
    }
    val showWhatsAppCard by remember(whatsappLoaded, whatsappInstalled, whatsappSummary) {
        derivedStateOf {
            whatsappLoaded && whatsappInstalled && whatsappSummary.hasData
        }
    }
    val showClipboardCard by remember(clipboardText) {
        derivedStateOf { !clipboardText.isNullOrBlank() }
    }
    val showLinkCleanerCard = true
    val showLargeFilesCard by remember(largeFiles) {
        derivedStateOf { largeFiles.isNotEmpty() }
    }
    val showEmptyFoldersCard by remember(emptyFolders, emptyFoldersHideUntil) {
        derivedStateOf {
            emptyFolders.isNotEmpty() && emptyFoldersHideUntil <= System.currentTimeMillis()
        }
    }
    val showContactsCard = true
    val storageManagerIntent = remember {
        Intent("android.os.storage.action.MANAGE_STORAGE").setPackage("com.android.storagemanager")
    }
    val showSystemStorageManagerCard by remember {
        derivedStateOf {
            storageManagerIntent.resolveActivity(context.packageManager) != null
        }
    }

    val dataLoaded = appManagerState.data?.apkFilesLoading == false && whatsappLoaded

    // Build visible content list with base order indices
    val content = mutableListOf<Pair<Int, ContentCard>>().apply {
        add(1 to ContentCard.QUICK_SCAN)
        if (showSystemStorageManagerCard) add(2 to ContentCard.SYSTEM_STORAGE)
        if (showStreakCard) add(3 to ContentCard.WEEKLY_STREAK)
        if (showLargeFilesCard) add(4 to ContentCard.LARGE_FILES)
        if (showWhatsAppCard) add(5 to ContentCard.WHATSAPP)
        if (showApkCard) add(6 to ContentCard.APK)
        if (showEmptyFoldersCard) add(7 to ContentCard.EMPTY_FOLDERS)
        if (showClipboardCard) add(8 to ContentCard.CLIPBOARD)
        if (showContactsCard) add(9 to ContentCard.CONTACTS)
        if (showLinkCleanerCard) add(10 to ContentCard.LINK_CLEANER)
        add(11 to ContentCard.IMAGE_OPTIMIZER)
        promotedApp?.let { add(12 to ContentCard.PROMOTED_APP) }
    }
    val visibleCount = content.size

    fun chooseAdsConfigFor(slot: AdSlot, visibleContent: Int, hasPromoted: Boolean): AdsConfig {
        val density = when {
            visibleContent >= 9 -> 2 // dense
            visibleContent >= 7 -> 1 // medium
            else -> 0 // sparse
        }
        val selected: AdsConfig? = when (slot) {
            AdSlot.A, AdSlot.B, AdSlot.C -> if (density == 0) largeBannerAdsConfig else mrecAdsConfig
            AdSlot.D, AdSlot.E -> if (density == 2) mrecAdsConfig else largeBannerAdsConfig
            AdSlot.F -> if (hasPromoted) leaderboardAdsConfig else largeBannerAdsConfig
        }
        return selected ?: bannerAdsConfig
    }

    val gmsAvailable = remember {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }

    val sessionDuration = remember { (System.currentTimeMillis() - Cleaner.sessionStartTime) / 1000L }
    val sessionSlots = when {
        sessionDuration < 60 -> listOf(AdSlot.A, AdSlot.C, AdSlot.F)
        sessionDuration < 180 -> listOf(AdSlot.A, AdSlot.B, AdSlot.C, AdSlot.F)
        else -> listOf(AdSlot.A, AdSlot.B, AdSlot.C, AdSlot.D, AdSlot.E, AdSlot.F)
    }
    val maxAds = when {
        sessionDuration < 60 -> 3
        sessionDuration < 180 -> 4
        else -> 5
    }
    val slotsAfterCount = when {
        visibleCount <= 6 -> listOf(AdSlot.C, AdSlot.F)
        visibleCount <= 9 -> {
            val firstAB = listOf(AdSlot.A, AdSlot.B).firstOrNull { sessionSlots.contains(it) }
            listOfNotNull(firstAB, AdSlot.C, AdSlot.F)
        }
        else -> sessionSlots
    }
    val allowedSlots = if (adsState && gmsAvailable && dataLoaded) {
        slotsAfterCount.filter { sessionSlots.contains(it) }.take(maxAds)
    } else emptyList()

    val slotAnchors = mapOf(
        AdSlot.A to 1,
        AdSlot.B to 3,
        AdSlot.C to 5,
        AdSlot.D to 9,
        AdSlot.E to 10,
        AdSlot.F to 11
    )

    val adQueue = allowedSlots.sortedBy { slotAnchors[it]!! }.toMutableList()
    val items = mutableListOf<HomeItem>()
    var lastWasAd = false
    for ((index, card) in content) {
        items.add(HomeItem.Card(card))
        lastWasAd = false
        while (adQueue.isNotEmpty() && slotAnchors[adQueue.first()]!! <= index) {
            if (!lastWasAd) {
                items.add(HomeItem.Ad(adQueue.removeAt(0)))
                lastWasAd = true
            } else {
                break
            }
        }
    }
    if (adQueue.isNotEmpty() && !lastWasAd) {
        items.add(HomeItem.Ad(adQueue.removeAt(0)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SizeConstants.LargeSize),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items.forEach { item ->
            when (item) {
                is HomeItem.Card -> when (item.type) {
                    ContentCard.QUICK_SCAN -> {
                        QuickScanSummaryCard(
                            modifier = Modifier,
                            cleanedSize = uiState.data?.storageInfo?.cleanedSpace ?: "",
                            freePercent = uiState.data?.storageInfo?.freeSpacePercentage ?: 0,
                            usedPercent = ((uiState.data?.storageInfo?.storageUsageProgress ?: 0f) * 100).toInt(),
                            progress = uiState.data?.storageInfo?.storageUsageProgress ?: 0f,
                            onQuickScanClick = {
                                viewModel.onEvent(
                                    event = ScannerEvent.ToggleAnalyzeScreen(true)
                                )
                            }
                        )
                    }
                    ContentCard.SYSTEM_STORAGE -> {
                        SystemStorageManagerCard(onOpen = {
                            if (storageManagerIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(storageManagerIntent)
                            } else {
                                Toast.makeText(context, R.string.no_application_found, Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                    ContentCard.WEEKLY_STREAK -> {
                        WeeklyCleanStreakCard(
                            streakDays = streakDays,
                            streakRecord = streakRecord,
                            onDismiss = { viewModel.onEvent(ScannerEvent.SetHideStreakDialogVisibility(true)) }
                        )
                    }
                    ContentCard.LARGE_FILES -> {
                        LargeFilesCard(
                            files = largeFiles,
                            onOpenClick = {
                                IntentsHelper.openActivity(
                                    context = context,
                                    activityClass = LargeFilesActivity::class.java
                                )
                            }
                        )
                    }
                    ContentCard.WHATSAPP -> {
                        WhatsAppCleanerCard(
                            mediaSummary = whatsappSummary,
                            onCleanClick = {
                                IntentsHelper.openActivity(
                                    context = context,
                                    activityClass = WhatsAppCleanerActivity::class.java
                                )
                            }
                        )
                    }
                    ContentCard.APK -> {
                        val isCleaningApks =
                            cleaningApks && uiState.data?.analyzeState?.state == CleaningState.Cleaning
                        ApkCleanerCard(
                            apkFiles = appManagerState.data?.apkFiles ?: emptyList(),
                            isLoading = isCleaningApks,
                            onCleanClick = { selected ->
                                val files = selected.map { File(it.path) }
                                viewModel.onCleanApks(files)
                            }
                        )
                    }
                    ContentCard.EMPTY_FOLDERS -> {
                        EmptyFolderCleanerCard(
                            folders = emptyFolders,
                            onCleanClick = { viewModel.onCleanEmptyFolders(it) }
                        )
                    }
                    ContentCard.CLIPBOARD -> {
                        ClipboardCleanerCard(
                            clipboardText = clipboardText,
                            onCleanClick = { viewModel.onClipboardClear() }
                        )
                    }
                    ContentCard.CONTACTS -> {
                        ContactsCleanerCard(onOpen = {
                            IntentsHelper.openActivity(
                                context = context,
                                activityClass = ContactsCleanerActivity::class.java
                            )
                        })
                    }
                    ContentCard.LINK_CLEANER -> {
                        LinkCleanerCard()
                    }
                    ContentCard.IMAGE_OPTIMIZER -> {
                        ImageOptimizerCard(onOptimizeClick = {
                            IntentsHelper.openActivity(
                                context = context,
                                activityClass = ImagePickerActivity::class.java
                            )
                        })
                    }
                    ContentCard.PROMOTED_APP -> {
                        promotedApp?.let { app ->
                            PromotedAppCard(app = app)
                        }
                    }
                }
                is HomeItem.Ad -> {
                    val config = chooseAdsConfigFor(item.slot, visibleCount, promotedApp != null)
                    AdBanner(adsConfig = config)
                }
            }
        }
        LargeVerticalSpacer()
    }
}
