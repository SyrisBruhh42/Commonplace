package com.example.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.FamilyCircle
import com.example.data.PriorityMode
import com.example.data.PriorityScoredQuote
import com.example.data.QuoteAudioSceneState
import com.example.data.QuoteCategories
import com.example.data.QuoteItem
import com.example.ui.theme.LocalAccessibilitySettings

/**
 * QuoteDiscoveryScreen fetches the daily quote from the Firestore `/daily_quotes` collection
 * and displays both the Daily Quote and Priority Discovery Quote on a simulated parchment background.
 * Saving a quote captures the current location via FusedLocationProviderClient and attaches
 * a TimestampInfo object to the Quote model in Firestore, with optional Zero-Trust Family Circle sharing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuoteDiscoveryScreen(
    state: DiscoverUiState,
    familyCircles: List<FamilyCircle> = emptyList(),
    audioState: QuoteAudioSceneState = QuoteAudioSceneState(),
    dailyScheduleState: com.example.data.DailyQuoteScheduleState = com.example.data.DailyQuoteScheduleState(),
    onToggleInterest: (String) -> Unit,
    onSelectPriorityMode: (PriorityMode) -> Unit,
    onNextPriorityQuote: () -> Unit,
    onGenerateAiQuote: () -> Unit,
    onQuickTakeaway: () -> Unit,
    onSaveQuoteWithStamp: (QuoteItem, String?, String) -> Unit,
    onShareToFamilyCircle: (FamilyCircle, QuoteItem, String) -> Unit = { _, _, _ -> },
    onOpenDossier: (QuoteItem) -> Unit,
    onDiscussInChat: (QuoteItem) -> Unit,
    onRefreshLocationStamp: () -> Unit,
    onDismissStatusBanner: () -> Unit,
    onReadAloud: (QuoteItem) -> Unit = {},
    onGenerate30sMusicScene: (QuoteItem) -> Unit = {},
    onStopAudio: () -> Unit = {},
    onToggleDailyQuoteNotifications: (Boolean) -> Unit = {},
    onUpdateDailyNotificationTime: (Int, Int) -> Unit = { _, _ -> },
    onTogglePriorityEngineForDailyPush: (Boolean) -> Unit = {},
    onSendTestDailyQuoteNotification: () -> Unit = {},
    onSyncAndPinHomeScreenWidget: () -> Unit = {}
) {
    val context = LocalContext.current
    val accessibility = LocalAccessibilitySettings.current
    var quotePendingSave by remember { mutableStateOf<QuoteItem?>(null) }
    var quotePendingFamilyShare by remember { mutableStateOf<QuoteItem?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onRefreshLocationStamp()
    }

    val currentScored = state.currentScoredQuote

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quote_discovery_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        // Full-bleed simulated parchment canvas backdrop (switches to crisp AAA surface in High Contrast Mode)
        if (!accessibility.highContrastMode) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val parchmentTop = Color(0xFFFBF6EC)
                val parchmentMid = Color(0xFFF3E9D7)
                val parchmentEdge = Color(0xFFE7D8BE)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(parchmentTop, parchmentMid, parchmentEdge)
                    )
                )
                // Subtle horizontal ledger lines across the parchment
                val stepPx = 36.dp.toPx()
                var y = stepPx
                while (y < size.height) {
                    drawLine(
                        color = Color(0xFFD5C2A3).copy(alpha = 0.22f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += stepPx
                }
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Status Banner for Stamp Save, Family Circle Share, or AI Discovery
            AnimatedVisibility(visible = state.statusBannerMessage != null) {
                state.statusBannerMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("discovery_status_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Stamp notification",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissStatusBanner) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            // Daily Quote from Firestore Collection on Simulated Parchment
            DailyQuoteParchmentBanner(
                dailyQuote = state.dailyQuoteFromCollection,
                isSyncedFromFirestore = state.isDailyQuoteSyncedFromFirestore,
                previewLocation = state.lastPreviewStampLocation,
                previewDate = state.lastPreviewStampDate,
                previewTime = state.lastPreviewStampTime,
                audioState = audioState,
                onSaveDailyQuote = { quotePendingSave = state.dailyQuoteFromCollection },
                onShareDailyToFamily = { quotePendingFamilyShare = state.dailyQuoteFromCollection },
                onOpenDossier = { onOpenDossier(state.dailyQuoteFromCollection) },
                onReadAloud = onReadAloud,
                onGenerate30sMusicScene = onGenerate30sMusicScene,
                onStopAudio = onStopAudio
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Home Screen Daily Quote Widget & Scheduled 'Quote of the Day' Notification Card
            DailyQuoteWidgetAndScheduleCard(
                scheduleState = dailyScheduleState,
                dailyQuote = state.dailyQuoteFromCollection,
                previewLocation = state.lastPreviewStampLocation,
                previewDate = state.lastPreviewStampDate,
                previewTime = state.lastPreviewStampTime,
                onUpdateSchedule = { enabled, hour, minute, usePriority ->
                    if (enabled != dailyScheduleState.notificationsEnabled) {
                        onToggleDailyQuoteNotifications(enabled)
                    }
                    if (hour != dailyScheduleState.scheduledHour || minute != dailyScheduleState.scheduledMinute) {
                        onUpdateDailyNotificationTime(hour, minute)
                    }
                    if (usePriority != dailyScheduleState.usePriorityEngineForDailyPush) {
                        onTogglePriorityEngineForDailyPush(usePriority)
                    }
                },
                onSendTestNotificationNow = onSendTestDailyQuoteNotification,
                onPinOrSyncHomeWidget = onSyncAndPinHomeScreenWidget
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Weighting Mode Selector
            Text(
                text = "PRIORITY ENGINE WEIGHTING",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PriorityMode.entries.forEach { mode ->
                    val selected = state.priorityMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPriorityMode(mode) },
                        label = { Text(mode.label) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected priority mode",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("priority_mode_${mode.name.lowercase()}")
                    )
                }
            }
            Text(
                text = state.priorityMode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            // Personal Interest Filter Chips
            Text(
                text = "PERSONAL INTEREST FILTERS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                QuoteCategories.ALL.forEach { category ->
                    val isSelected = category in state.selectedInterests
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleInterest(category) },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier.testTag("interest_chip_${category.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Hero Editorial Quote Card on Simulated Parchment
            if (currentScored != null) {
                AnimatedContent(
                    targetState = currentScored,
                    label = "PriorityQuoteCardAnimation"
                ) { scoredQuote ->
                    SimulatedParchmentQuoteCard(
                        scoredQuote = scoredQuote,
                        previewLocation = state.lastPreviewStampLocation,
                        previewDate = state.lastPreviewStampDate,
                        previewTime = state.lastPreviewStampTime,
                        quickTakeawayState = state.quickTakeawayState,
                        audioState = audioState,
                        onRequestLocationRefresh = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onQuickTakeaway = onQuickTakeaway,
                        onOpenSaveDialog = { quotePendingSave = scoredQuote.quote },
                        onOpenFamilyShareDialog = { quotePendingFamilyShare = scoredQuote.quote },
                        onShare = {
                            val stampStr = "${state.lastPreviewStampLocation} • ${state.lastPreviewStampDate} • ${state.lastPreviewStampTime}"
                            shareQuoteViaIntent(
                                context = context,
                                quoteText = scoredQuote.quote.text,
                                author = scoredQuote.quote.author,
                                sourceWork = scoredQuote.quote.sourceWork,
                                stampSummary = stampStr
                            )
                        },
                        onOpenDossier = { onOpenDossier(scoredQuote.quote) },
                        onDiscussInChat = { onDiscussInChat(scoredQuote.quote) },
                        onReadAloud = onReadAloud,
                        onGenerate30sMusicScene = onGenerate30sMusicScene,
                        onStopAudio = onStopAudio
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Generator Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNextPriorityQuote,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("next_quote_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Next priority quote",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_next_priority_quote),
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                OutlinedButton(
                    onClick = onGenerateAiQuote,
                    enabled = !state.isDiscoveringFromAi,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("ai_discover_quote_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    if (state.isDiscoveringFromAi) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Discovering…")
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Generate quote with Gemini",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_ai_discover_quote),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            if (state.aiDiscoveryError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.aiDiscoveryError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    quotePendingSave?.let { targetQuote ->
        SaveQuoteWithStampDialog(
            quote = targetQuote,
            defaultLocation = state.lastPreviewStampLocation,
            dateStamp = state.lastPreviewStampDate,
            timeStamp = state.lastPreviewStampTime,
            onRequestGpsPermission = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDismiss = { quotePendingSave = null },
            onConfirmSave = { customLocation, note ->
                quotePendingSave = null
                onSaveQuoteWithStamp(targetQuote, customLocation, note)
            }
        )
    }

    quotePendingFamilyShare?.let { targetQuote ->
        ShareToFamilyCircleDialog(
            quote = targetQuote,
            familyCircles = familyCircles,
            onDismiss = { quotePendingFamilyShare = null },
            onConfirmShare = { selectedCircle, note ->
                quotePendingFamilyShare = null
                onShareToFamilyCircle(selectedCircle, targetQuote, note)
            }
        )
    }
}

@Composable
private fun DailyQuoteParchmentBanner(
    dailyQuote: QuoteItem,
    isSyncedFromFirestore: Boolean,
    previewLocation: String,
    previewDate: String,
    previewTime: String,
    audioState: QuoteAudioSceneState,
    onSaveDailyQuote: () -> Unit,
    onShareDailyToFamily: () -> Unit,
    onOpenDossier: () -> Unit,
    onReadAloud: (QuoteItem) -> Unit,
    onGenerate30sMusicScene: (QuoteItem) -> Unit,
    onStopAudio: () -> Unit
) {
    val accessibility = LocalAccessibilitySettings.current
    val cardContainerColor = if (accessibility.highContrastMode) {
        MaterialTheme.colorScheme.surface
    } else {
        Color(0xFFF6ECDA)
    }
    val cardBorderColor = if (accessibility.highContrastMode) {
        MaterialTheme.colorScheme.outline
    } else {
        Color(0xFFC8A96E)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_quote_parchment_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        ),
        border = BorderStroke(if (accessibility.highContrastMode) 2.dp else 1.5.dp, cardBorderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Simulated aged parchment corner vignette and seal
            if (!accessibility.highContrastMode) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFBF2).copy(alpha = 0.7f),
                                Color(0xFFECDCC0).copy(alpha = 0.45f)
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                            radius = size.maxDimension * 0.75f
                        )
                    )
                    drawCircle(
                        color = Color(0xFF8B3A2B).copy(alpha = 0.08f),
                        radius = 52.dp.toPx(),
                        center = Offset(size.width - 36.dp.toPx(), 36.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFC8A96E).copy(alpha = 0.4f),
                        radius = 58.dp.toPx(),
                        center = Offset(size.width - 36.dp.toPx(), 36.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isSyncedFromFirestore) {
                                "DAILY FOLIO • FIRESTORE EDITION"
                            } else {
                                "DAILY FOLIO • ARCHIVAL EDITION"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = previewDate.ifBlank { "Today's Edition" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "“${dailyQuote.text}”",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "— ${dailyQuote.author} (${dailyQuote.sourceWork})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Current Stamp: $previewLocation • $previewDate $previewTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Read Aloud & 30s Lyria Music Scene Bar for Daily Quote
                QuoteAudioSceneControlBar(
                    quote = dailyQuote,
                    audioState = audioState,
                    onReadAloud = onReadAloud,
                    onGenerate30sMusicScene = onGenerate30sMusicScene,
                    onStopAudio = onStopAudio
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSaveDailyQuote,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_daily_quote_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Save daily quote with location and timestamp",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Daily Quote")
                    }

                    OutlinedButton(
                        onClick = onShareDailyToFamily,
                        modifier = Modifier.testTag("share_daily_family_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Share daily quote to Family Circle",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Family")
                    }

                    TextButton(onClick = onOpenDossier) {
                        Text("Dossier")
                    }
                }
            }
        }
    }
}

@Composable
private fun SimulatedParchmentQuoteCard(
    scoredQuote: PriorityScoredQuote,
    previewLocation: String,
    previewDate: String,
    previewTime: String,
    quickTakeawayState: UiState<String>?,
    audioState: QuoteAudioSceneState,
    onRequestLocationRefresh: () -> Unit,
    onQuickTakeaway: () -> Unit,
    onOpenSaveDialog: () -> Unit,
    onOpenFamilyShareDialog: () -> Unit,
    onShare: () -> Unit,
    onOpenDossier: () -> Unit,
    onDiscussInChat: () -> Unit,
    onReadAloud: (QuoteItem) -> Unit,
    onGenerate30sMusicScene: (QuoteItem) -> Unit,
    onStopAudio: () -> Unit
) {
    val quote = scoredQuote.quote
    val accessibility = LocalAccessibilitySettings.current
    val watermarkColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
    val cardContainerColor = if (accessibility.highContrastMode) {
        MaterialTheme.colorScheme.surface
    } else {
        Color(0xFFF9F1E1)
    }
    val cardBorderColor = if (accessibility.highContrastMode) {
        MaterialTheme.colorScheme.outline
    } else {
        Color(0xFFD2B887)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_quote_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        ),
        border = BorderStroke(if (accessibility.highContrastMode) 2.dp else 1.5.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Simulated aged parchment radial wash & editorial crest watermark
            if (!accessibility.highContrastMode) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFDF8),
                            Color(0xFFF5E8CF),
                            Color(0xFFEAD7B7)
                        ),
                        center = Offset(size.width * 0.45f, size.height * 0.4f),
                        radius = size.maxDimension * 0.85f
                    )
                )
                    drawCircle(
                        color = watermarkColor,
                        radius = size.minDimension * 0.45f,
                        center = Offset(size.width * 0.88f, size.height * 0.25f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = quote.category.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Priority Score",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Priority ${scoredQuote.totalPriorityScore}/100",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = "Opening quotation mark",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )

                Text(
                    text = quote.text,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "— ${quote.author}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (quote.sourceWork.isNotBlank()) {
                    Text(
                        text = "${quote.sourceWork} (${quote.era})",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = scoredQuote.priorityReason,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = quote.popularOnlineContext,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Read Aloud & 30s Lyria Music Scene Bar for Hero Priority Quote
                QuoteAudioSceneControlBar(
                    quote = quote,
                    audioState = audioState,
                    onReadAloud = onReadAloud,
                    onGenerate30sMusicScene = onGenerate30sMusicScene,
                    onStopAudio = onStopAudio
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AssistChip(
                        onClick = onQuickTakeaway,
                        label = { Text(stringResource(R.string.btn_quick_spark)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Low latency Gemini spark",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            leadingIconContentColor = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier.testTag("quick_spark_button")
                    )

                    TextButton(
                        onClick = onOpenDossier,
                        modifier = Modifier.testTag("open_dossier_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Deep Thinking and Grounding Dossier",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_deep_dossier))
                    }
                }

                when (quickTakeawayState) {
                    is UiState.Loading -> {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generating low-latency insight with gemini-3.1-flash-lite…",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    is UiState.Success -> {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Text(
                                text = quickTakeawayState.data,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    is UiState.Error -> {
                        Text(
                            text = quickTakeawayState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    null -> Unit
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )

                // Archival Provenance Stamp Preview (Location via FusedLocationProviderClient + Date + Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location stamp",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$previewLocation • $previewDate • $previewTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onRequestLocationRefresh,
                        modifier = Modifier.testTag("refresh_gps_stamp_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Update GPS location stamp",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenSaveDialog,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("save_quote_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Save quote with location and time stamp",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_save_with_stamp))
                    }

                    OutlinedButton(
                        onClick = onOpenFamilyShareDialog,
                        modifier = Modifier
                            .weight(0.95f)
                            .testTag("share_family_circle_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Share to Zero-Trust Family Social Circle",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Family")
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("share_quote_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share quote externally",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDiscussInChat,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("discuss_quote_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Discuss quote in Symposium chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShareToFamilyCircleDialog(
    quote: QuoteItem,
    familyCircles: List<FamilyCircle>,
    onDismiss: () -> Unit,
    onConfirmShare: (FamilyCircle, String) -> Unit
) {
    var selectedCircle by remember(familyCircles) { mutableStateOf(familyCircles.firstOrNull()) }
    var familyNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Zero-Trust Family Share",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share to Family Social Circle",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "“${quote.text.take(90)}${if (quote.text.length > 90) "…" else ""}” — ${quote.author}",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (familyCircles.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "You haven't created a Family Social Circle yet. Open the Family tab in the bottom navigation bar to create a Zero-Trust Family Circle with your family's Google accounts.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Select Zero-Trust Family Circle:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    familyCircles.forEach { circle ->
                        val isChosen = selectedCircle?.id == circle.id
                        FilterChip(
                            selected = isChosen,
                            onClick = { selectedCircle = circle },
                            label = {
                                Text("${circle.name} (${circle.memberEmails.size} Google member(s))")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = familyNote,
                        onValueChange = { familyNote = it },
                        label = { Text("Family Dedication Note (Optional)") },
                        placeholder = { Text("Thought of everyone when reading this today…") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("family_share_note_input")
                    )
                }
            }
        },
        confirmButton = {
            if (selectedCircle != null) {
                Button(
                    onClick = { onConfirmShare(selectedCircle!!, familyNote) },
                    modifier = Modifier.testTag("confirm_family_share_button")
                ) {
                    Text("Share with Stamp")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
